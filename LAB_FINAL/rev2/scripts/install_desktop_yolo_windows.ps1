# Ejecutar en PowerShell normal desde la raiz del proyecto.
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install ultralytics opencv-python numpy
Write-Host "[OK] Entorno YOLO instalado. Usa .\.venv\Scripts\python.exe detector\yolo_rtsp_detector.py ..."
