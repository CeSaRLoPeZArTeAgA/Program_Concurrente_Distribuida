# Edita las IPs de Raft y las camaras en config\cameras.example.json antes de ejecutar.
.\.venv\Scripts\python.exe detector\yolo_rtsp_detector.py `
  --cameras config\cameras.example.json `
  --model models\yolo11n.pt `
  --raft 192.168.0.201:7001,192.168.0.202:7002,192.168.0.203:7003 `
  --conf 0.35 `
  --interval 1.0
