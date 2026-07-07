#!/usr/bin/env bash
set -euo pipefail
sudo apt update
sudo apt install -y \
  python3-gi \
  gir1.2-gstreamer-1.0 \
  gir1.2-gst-rtsp-server-1.0 \
  gstreamer1.0-tools \
  gstreamer1.0-libcamera \
  gstreamer1.0-plugins-base \
  gstreamer1.0-plugins-good \
  gstreamer1.0-plugins-bad \
  gstreamer1.0-plugins-ugly

gst-inspect-1.0 libcamerasrc >/dev/null

echo "[OK] Dependencias RTSP instaladas en Raspberry."
