#!/usr/bin/env bash
set -euo pipefail
sudo apt update
sudo apt install -y python3 python3-venv python3-pip ffmpeg
python3 -m venv .venv
.venv/bin/python -m pip install --upgrade pip
.venv/bin/python -m pip install ultralytics opencv-python numpy
