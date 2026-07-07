#!/usr/bin/env python3
"""
Servidor RTSP para Raspberry Pi 4B con camara CSI usando GStreamer/libcamera.
Cada Raspberry ejecuta este script con un path distinto: cam1, cam2 o cam3.

Ejemplo:
python3 raspi_rtsp_server.py --path cam1 --port 8554 --width 1280 --height 720 --fps 15
"""

import argparse
import signal
import socket
import subprocess

import gi

gi.require_version("Gst", "1.0")
gi.require_version("GstRtspServer", "1.0")

from gi.repository import Gst, GstRtspServer, GLib


class CameraFactory(GstRtspServer.RTSPMediaFactory):
    def __init__(self, width: int, height: int, fps: int, bitrate: int):
        super().__init__()
        self.width = width
        self.height = height
        self.fps = fps
        self.bitrate = bitrate
        self.set_shared(True)

    def do_create_element(self, url):
        pipeline = (
            f"libcamerasrc ! "
            f"video/x-raw,width={self.width},height={self.height},framerate={self.fps}/1 ! "
            f"queue ! "
            f"videoconvert ! "
            f"video/x-raw,format=I420 ! "
            f"x264enc tune=zerolatency speed-preset=ultrafast "
            f"bitrate={self.bitrate} key-int-max={self.fps * 2} byte-stream=true ! "
            f"h264parse config-interval=1 ! "
            f"rtph264pay name=pay0 pt=96 config-interval=1"
        )
        print("[INFO] Pipeline GStreamer:")
        print(pipeline)
        return Gst.parse_launch(pipeline)


def get_interface_ip(interface_name: str):
    try:
        result = subprocess.run(
            ["ip", "-4", "addr", "show", interface_name],
            capture_output=True,
            text=True,
            check=True,
        )
        for line in result.stdout.splitlines():
            line = line.strip()
            if line.startswith("inet "):
                return line.split()[1].split("/")[0]
    except Exception:
        return None
    return None


def get_best_ip():
    for iface in ("wlan0", "eth0"):
        ip = get_interface_ip(iface)
        if ip:
            return ip, iface
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip, "auto"
    except Exception:
        return "127.0.0.1", "localhost"


def main():
    ap = argparse.ArgumentParser(description="RTSP server Raspberry Pi CSI")
    ap.add_argument("--port", default="8554")
    ap.add_argument("--path", default="cam1", help="Ruta RTSP sin slash. Ejemplo: cam1")
    ap.add_argument("--width", type=int, default=1280)
    ap.add_argument("--height", type=int, default=720)
    ap.add_argument("--fps", type=int, default=15)
    ap.add_argument("--bitrate", type=int, default=2500, help="kbps")
    args = ap.parse_args()

    Gst.init(None)

    server = GstRtspServer.RTSPServer()
    server.set_address("0.0.0.0")
    server.set_service(str(args.port))

    factory = CameraFactory(args.width, args.height, args.fps, args.bitrate)
    mounts = server.get_mount_points()
    path = "/" + args.path.strip("/")
    mounts.add_factory(path, factory)
    server.attach(None)

    ip, iface = get_best_ip()
    print("[OK] Servidor RTSP iniciado en Raspberry Pi")
    print(f"[OK] Interfaz detectada: {iface}")
    print(f"[OK] Resolucion: {args.width}x{args.height} FPS={args.fps}")
    print(f"[OK] URL RTSP: rtsp://{ip}:{args.port}{path}")
    print("[INFO] Presiona CTRL+C para detener")

    loop = GLib.MainLoop()

    def stop(sig, frame):
        print("\n[INFO] Cerrando RTSP...")
        loop.quit()

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)
    loop.run()


if __name__ == "__main__":
    main()
