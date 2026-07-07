#!/usr/bin/env python3
"""
Servidor de Testeo YOLO.

Lee multiples camaras RTSP, ejecuta YOLO y envia eventos committed-candidatos
al cluster Raft mediante sockets TCP propios. No usa WebSocket, RabbitMQ ni
frameworks de comunicacion.

Ejemplo:
python detector/yolo_rtsp_detector.py \
  --cameras config/cameras.json \
  --model models/yolo11n.pt \
  --raft 192.168.0.201:7001,192.168.0.202:7001,192.168.0.203:7001
"""

import argparse
import base64
import hashlib
import json
import os
import queue
import socket
import sys
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Tuple
from urllib.parse import quote_plus, unquote_plus

# Fuerza a OpenCV/FFmpeg a preferir TCP para RTSP cuando sea posible.
os.environ.setdefault("OPENCV_FFMPEG_CAPTURE_OPTIONS", "rtsp_transport;tcp")

try:
    import cv2
except Exception as exc:
    print(f"[ERROR] No se pudo importar cv2: {exc}")
    print("Instala: python -m pip install opencv-python")
    sys.exit(2)

try:
    from ultralytics import YOLO
except Exception as exc:
    print(f"[ERROR] No se pudo importar ultralytics: {exc}")
    print("Instala: python -m pip install ultralytics")
    sys.exit(2)


def wire_line(kind: str, values: Dict[str, str]) -> str:
    parts = [kind]
    for k, v in values.items():
        parts.append(f"{quote_plus(str(k))}={quote_plus(str(v if v is not None else ''))}")
    return "|".join(parts)


def wire_parse(line: str) -> Tuple[str, Dict[str, str]]:
    if not line:
        return "", {}
    parts = line.strip().split("|")
    kind = parts[0]
    values: Dict[str, str] = {}
    for p in parts[1:]:
        if "=" not in p:
            continue
        k, v = p.split("=", 1)
        values[unquote_plus(k)] = unquote_plus(v)
    return kind, values


class RaftSender:
    def __init__(self, endpoints: List[Tuple[str, int]], timeout: float = 8.0):
        if not endpoints:
            raise ValueError("Debe indicar al menos un endpoint Raft host:port")
        self.endpoints = endpoints
        self.timeout = timeout
        self.lock = threading.Lock()
        self.index = 0
        self.leader: Tuple[str, int] | None = None

    def send_event(self, event: Dict[str, str]) -> str:
        line = wire_line("EVENT", event)
        attempts: List[Tuple[str, int]] = []
        with self.lock:
            if self.leader:
                attempts.append(self.leader)
            attempts.extend(self.endpoints[self.index:] + self.endpoints[:self.index])

        last_error = ""
        for host, port in attempts:
            try:
                resp = self._request(host, port, line)
                kind, values = wire_parse(resp)
                if kind == "OK":
                    with self.lock:
                        self.leader = (host, port)
                    return resp
                if kind == "NOT_LEADER" or (kind == "ERROR" and values.get("leaderHost")):
                    lh = values.get("leaderHost", "")
                    lp = values.get("leaderClientPort", "")
                    if lh and lp and lp != "-1":
                        try:
                            leader_ep = (lh, int(lp))
                            resp2 = self._request(leader_ep[0], leader_ep[1], line)
                            kind2, _ = wire_parse(resp2)
                            if kind2 == "OK":
                                with self.lock:
                                    self.leader = leader_ep
                                return resp2
                            last_error = resp2
                        except Exception as exc:
                            last_error = str(exc)
                    else:
                        last_error = resp
                else:
                    last_error = resp
            except Exception as exc:
                last_error = f"{host}:{port} {exc}"

        raise RuntimeError(f"No se pudo enviar evento a Raft. Ultimo error: {last_error}")

    def _request(self, host: str, port: int, line: str) -> str:
        with socket.create_connection((host, port), timeout=self.timeout) as s:
            s.settimeout(self.timeout)
            s.sendall((line + "\n").encode("utf-8"))
            data = b""
            while not data.endswith(b"\n"):
                chunk = s.recv(4096)
                if not chunk:
                    break
                data += chunk
            return data.decode("utf-8", errors="replace").strip()


def read_cameras(path: str) -> List[Dict[str, str]]:
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    cameras = data.get("cameras", data if isinstance(data, list) else [])
    if not cameras:
        raise ValueError("El archivo de camaras no contiene camaras")
    for c in cameras:
        if "id" not in c or "rtsp" not in c:
            raise ValueError("Cada camara debe tener id y rtsp")
    return cameras


def parse_raft_endpoints(raw: str) -> List[Tuple[str, int]]:
    out: List[Tuple[str, int]] = []
    for part in raw.split(","):
        part = part.strip()
        if not part:
            continue
        host, port = part.rsplit(":", 1)
        out.append((host, int(port)))
    return out


def ensure_jpeg_under_limit(image, max_bytes: int, max_width: int = 960) -> bytes:
    h, w = image.shape[:2]
    if w > max_width:
        scale = max_width / float(w)
        image = cv2.resize(image, (int(w * scale), int(h * scale)))

    for quality in (85, 75, 65, 55, 45, 35):
        ok, buf = cv2.imencode(".jpg", image, [int(cv2.IMWRITE_JPEG_QUALITY), quality])
        if ok and len(buf.tobytes()) <= max_bytes:
            return buf.tobytes()

    ok, buf = cv2.imencode(".jpg", image, [int(cv2.IMWRITE_JPEG_QUALITY), 30])
    if not ok:
        raise RuntimeError("No se pudo codificar JPEG")
    return buf.tobytes()


class CameraWorker(threading.Thread):
    def __init__(self, camera: Dict[str, str], model: YOLO, model_lock: threading.Lock, sender: RaftSender, args):
        super().__init__(daemon=True, name=f"cam-{camera['id']}")
        self.camera = camera
        self.model = model
        self.model_lock = model_lock
        self.sender = sender
        self.args = args
        self.stop_flag = threading.Event()
        self.last_detection_ts = 0.0
        self.sent_recent: Dict[str, float] = {}

    def run(self):
        cam_id = self.camera["id"]
        rtsp = self.camera["rtsp"]
        print(f"[INFO] Iniciando hilo {cam_id}: {rtsp}")

        while not self.stop_flag.is_set():
            cap = cv2.VideoCapture(rtsp, cv2.CAP_FFMPEG)
            if not cap.isOpened():
                print(f"[WARN] {cam_id}: no se pudo abrir RTSP. Reintentando en 5s")
                time.sleep(5)
                continue

            frame_counter = 0
            while not self.stop_flag.is_set():
                ok, frame = cap.read()
                if not ok or frame is None:
                    print(f"[WARN] {cam_id}: sin frame. Reconectando...")
                    break

                frame_counter += 1
                now = time.time()
                if now - self.last_detection_ts < self.args.interval:
                    continue
                self.last_detection_ts = now

                try:
                    self.process_frame(frame)
                except Exception as exc:
                    print(f"[ERROR] {cam_id}: {exc}")

            cap.release()
            time.sleep(2)

    def process_frame(self, frame):
        cam_id = self.camera["id"]
        with self.model_lock:
            results = self.model.predict(source=frame, conf=self.args.conf, imgsz=self.args.imgsz, verbose=False)

        if not results:
            return
        result = results[0]
        if result.boxes is None or len(result.boxes) == 0:
            print(f"[INFO] {cam_id}: sin detecciones")
            return

        annotated = result.plot()
        ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
        ts_file = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]

        for box in result.boxes:
            cls_id = int(box.cls[0].item())
            names = result.names
            label = names.get(cls_id, str(cls_id)) if isinstance(names, dict) else str(cls_id)
            confidence = float(box.conf[0].item())
            x1, y1, x2, y2 = [int(round(v)) for v in box.xyxy[0].tolist()]
            bbox = f"{x1},{y1},{x2},{y2}"

            key = f"{cam_id}:{label}:{bbox}"
            if self._is_duplicate(key):
                continue

            image_name = f"{cam_id}_{ts_file}_{label}.jpg".replace(" ", "_").replace("/", "_")
            camera_dir = Path(self.args.output) / cam_id
            camera_dir.mkdir(parents=True, exist_ok=True)
            image_path = camera_dir / image_name

            jpg = ensure_jpeg_under_limit(annotated, self.args.max_image_bytes, self.args.max_image_width)
            image_path.write_bytes(jpg)
            image_b64 = base64.b64encode(jpg).decode("ascii")

            event_id = hashlib.sha1(f"{cam_id}|{ts}|{label}|{bbox}|{confidence:.4f}".encode("utf-8")).hexdigest()
            event = {
                "eventId": event_id,
                "timestamp": ts,
                "camera": cam_id,
                "label": label,
                "confidence": f"{confidence:.4f}",
                "bbox": bbox,
                "image": image_name,
                "imageB64": image_b64,
            }

            resp = self.sender.send_event(event)
            print(f"[OK] {cam_id}: {label} conf={confidence:.3f} bbox={bbox} -> {resp}")

    def _is_duplicate(self, key: str) -> bool:
        now = time.time()
        ttl = self.args.duplicate_ttl
        for k, t in list(self.sent_recent.items()):
            if now - t > ttl:
                del self.sent_recent[k]
        if key in self.sent_recent:
            return True
        self.sent_recent[key] = now
        return False


def main() -> int:
    ap = argparse.ArgumentParser(description="Servidor YOLO multipcamara RTSP -> Raft")
    ap.add_argument("--cameras", required=True, help="Archivo JSON con las camaras RTSP")
    ap.add_argument("--model", default="models/yolo11n.pt", help="Modelo YOLO .pt")
    ap.add_argument("--raft", required=True, help="Endpoints cliente Raft: host:port,host:port")
    ap.add_argument("--conf", type=float, default=0.35, help="Confianza minima")
    ap.add_argument("--imgsz", type=int, default=640, help="Tamaño de inferencia YOLO")
    ap.add_argument("--interval", type=float, default=1.0, help="Segundos entre inferencias por camara")
    ap.add_argument("--output", default="captures", help="Directorio local de capturas en el servidor YOLO")
    ap.add_argument("--max-image-bytes", type=int, default=180000, help="Limite por imagen enviada a Raft")
    ap.add_argument("--max-image-width", type=int, default=960, help="Ancho maximo de imagen enviada")
    ap.add_argument("--duplicate-ttl", type=float, default=5.0, help="Ventana para evitar duplicados similares")
    args = ap.parse_args()

    cameras = read_cameras(args.cameras)
    endpoints = parse_raft_endpoints(args.raft)

    print("==============================================")
    print(" SERVIDOR DE TESTEO YOLO")
    print("==============================================")
    print(f"Modelo: {args.model}")
    print(f"Camaras: {len(cameras)}")
    for c in cameras:
        print(f"  - {c['id']}: {c['rtsp']}")
    print(f"Raft: {endpoints}")
    print("==============================================")

    model = YOLO(args.model)
    model_lock = threading.Lock()
    sender = RaftSender(endpoints)

    workers = [CameraWorker(c, model, model_lock, sender, args) for c in cameras]
    for w in workers:
        w.start()

    try:
        while True:
            time.sleep(10)
    except KeyboardInterrupt:
        print("\n[INFO] Cerrando detector...")
        for w in workers:
            w.stop_flag.set()
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
