#!/usr/bin/env python3
"""Puente minimo YOLO para Java.
Entrada: imagen local + modelo .pt de Ultralytics.
Salida por stdout: lineas DET|label=...|confidence=...|x1=...|y1=...|x2=...|y2=...
"""
import argparse
import sys


def enc(s: str) -> str:
    # Mantener compatible con Wire.parse de Java: solo evitar barras verticales y saltos.
    return str(s).replace("|", "_").replace("\n", " ").strip()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", required=True)
    ap.add_argument("--image", required=True)
    ap.add_argument("--conf", type=float, default=0.35)
    ap.add_argument("--max", type=int, default=5)
    args = ap.parse_args()

    try:
        from ultralytics import YOLO
    except Exception as e:
        print(f"ERROR: No se pudo importar ultralytics: {e}", file=sys.stderr)
        print("Instale: python -m pip install ultralytics opencv-python", file=sys.stderr)
        return 2

    try:
        model = YOLO(args.model)
        results = model.predict(source=args.image, conf=args.conf, verbose=False)
        count = 0
        for r in results:
            names = r.names
            if r.boxes is None:
                continue
            for box in r.boxes:
                cls_id = int(box.cls[0].item())
                label = names.get(cls_id, str(cls_id)) if isinstance(names, dict) else str(cls_id)
                confidence = float(box.conf[0].item())
                xyxy = box.xyxy[0].tolist()
                x1, y1, x2, y2 = [int(round(v)) for v in xyxy]
                print(f"DET|label={enc(label)}|confidence={confidence:.6f}|x1={x1}|y1={y1}|x2={x2}|y2={y2}", flush=True)
                count += 1
                if count >= args.max:
                    return 0
        return 0
    except Exception as e:
        print(f"ERROR: YOLO fallo: {e}", file=sys.stderr)
        return 3


if __name__ == "__main__":
    raise SystemExit(main())
