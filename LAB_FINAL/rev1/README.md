# CC4P1 - Sistema distribuido YOLO + RTSP + Raft en Java

Proyecto preparado para desplegar en varias maquinas dentro de una red LAN/WIFI.

## 1. Que incluye

- `src/cc4p1/raft/RaftNode.java`: nodo de consenso estilo Raft didactico.
  - Eleccion de lider.
  - Heartbeats.
  - Replicacion de entradas por mayoria.
  - Persistencia local en `data/<nodo>/raft.log` y `data/<nodo>/events.csv`.
- `src/cc4p1/detector/DetectionServer.java`: servidor de testeo por camara.
  - Lee una camara RTSP mediante `ffmpeg`.
  - Ejecuta YOLO sobre cada frame mediante `tools/yolo_detect.py`.
  - Envia registros por sockets TCP al cluster Raft.
- `src/cc4p1/client/WatcherClient.java`: cliente vigilante para ver registros en tiempo real.
- `src/cc4p1/client/ManualEventClient.java`: cliente de prueba para insertar eventos sin camara.
- `models/yolo11n(4).pt`: modelo YOLO preentrenado recibido.
- `legacy-redesOk/`: codigo original de sockets usado como referencia.

## 2. Nota tecnica importante

Java SE puro no puede cargar directamente un archivo `.pt` de Ultralytics ni decodificar RTSP/H264 solo con la biblioteca estandar del JDK. Por eso la arquitectura queda asi:

- La parte distribuida, sockets, concurrencia, persistencia, cliente y consenso estan en Java puro.
- Para RTSP se invoca el binario externo `ffmpeg`.
- Para inferencia YOLO con `.pt` se invoca Python + Ultralytics desde Java con `ProcessBuilder`.

Si el docente exige literalmente cero dependencias externas, entonces no se puede usar `.pt` ni RTSP real. En ese caso debe usarse `--mode mock` para demostrar sockets/Raft, o convertir el modelo a otro formato y aceptar una libreria de inferencia.

## 3. Requisitos por maquina

### Todos los nodos Java

- JDK 8 o superior.
- Puertos abiertos en firewall:
  - Cliente Raft: `7001`, `7002`, `7003` segun nodo.
  - Comunicacion Raft interna: `7101`, `7102`, `7103` segun nodo.

### Maquinas con camara/RTSP

- `ffmpeg` instalado y visible en PATH.
- Python 3.
- Paquetes Python:

```bash
python -m pip install -r requirements-yolo.txt
```

En Windows, si `python` no funciona, usar `py -3` o la ruta completa del ejecutable.

## 4. Compilar

### Windows PowerShell o CMD

```bat
scripts\compile.bat
```

### Linux

```bash
bash scripts/compile.sh
```

## 5. Despliegue en 3 maquinas diferentes

Supongamos:

| Maquina | IP | Rol |
|---|---:|---|
| PC1 | `192.168.1.10` | Nodo Raft n1 |
| PC2 | `192.168.1.11` | Nodo Raft n2 |
| PC3 | `192.168.1.12` | Nodo Raft n3 |
| PC4 | `192.168.1.20` | Camara/Detector cam1 |
| PC5 | `192.168.1.21` | Cliente vigilante |

Copiar esta carpeta completa en PC1, PC2 y PC3. En PC4 tambien copiarla si ejecutara YOLO.

### PC1 - Nodo n1

```bash
java -cp build cc4p1.raft.RaftNode \
  --id n1 --host 192.168.1.10 \
  --client-port 7001 --raft-port 7101 \
  --peers n2@192.168.1.11:7102:7002,n3@192.168.1.12:7103:7003 \
  --data data/n1
```

### PC2 - Nodo n2

```bash
java -cp build cc4p1.raft.RaftNode \
  --id n2 --host 192.168.1.11 \
  --client-port 7002 --raft-port 7102 \
  --peers n1@192.168.1.10:7101:7001,n3@192.168.1.12:7103:7003 \
  --data data/n2
```

### PC3 - Nodo n3

```bash
java -cp build cc4p1.raft.RaftNode \
  --id n3 --host 192.168.1.12 \
  --client-port 7003 --raft-port 7103 \
  --peers n1@192.168.1.10:7101:7001,n2@192.168.1.11:7102:7002 \
  --data data/n3
```

Esperar 3 a 5 segundos. Uno de los tres nodos debe mostrar:

```text
ES LIDER term=...
```

## 6. Probar consenso sin camara

Desde cualquier maquina que tenga acceso a PC1:

```bash
java -cp build cc4p1.client.ManualEventClient \
  --raft 192.168.1.10:7001 \
  --camera cam_prueba \
  --label persona \
  --confidence 0.95
```

Debe responder algo parecido a:

```text
OK|committed=true|index=1|acks=3|leader=n1
```

Revisar en cada nodo:

```bash
cat data/n1/events.csv
cat data/n2/events.csv
cat data/n3/events.csv
```

Los tres deben tener el mismo registro.

## 7. Ejecutar cliente vigilante

En PC5:

```bash
java -cp build cc4p1.client.WatcherClient --raft 192.168.1.10:7001
```

El cliente imprime una tabla continua:

```text
idx    fecha                  camara     objeto             conf       imagen
1      2026-07-05 10:00:00    cam1       person             0.9123     cam1_...
```

## 8. Ejecutar detector con RTSP + YOLO

Ejemplo de URL RTSP:

```text
rtsp://usuario:clave@192.168.1.101:554/stream1
```

En PC4:

```bash
java -cp build cc4p1.detector.DetectionServer \
  --camera cam1 \
  --rtsp "rtsp://usuario:clave@192.168.1.101:554/stream1" \
  --raft 192.168.1.10:7001 \
  --model "models/yolo11n(4).pt" \
  --python python \
  --script tools/yolo_detect.py \
  --interval-ms 3000 \
  --conf 0.35 \
  --out captures/cam1
```

Para tres camaras IP se levantan tres procesos, uno por camara:

```bash
java -cp build cc4p1.detector.DetectionServer --camera cam1 --rtsp "rtsp://...cam1..." --raft 192.168.1.10:7001 --model "models/yolo11n(4).pt" --out captures/cam1
java -cp build cc4p1.detector.DetectionServer --camera cam2 --rtsp "rtsp://...cam2..." --raft 192.168.1.10:7001 --model "models/yolo11n(4).pt" --out captures/cam2
java -cp build cc4p1.detector.DetectionServer --camera cam3 --rtsp "rtsp://...cam3..." --raft 192.168.1.10:7001 --model "models/yolo11n(4).pt" --out captures/cam3
```

## 9. Probar detector sin RTSP, con imagen local

Sirve para comprobar YOLO antes de conectar la camara:

```bash
java -cp build cc4p1.detector.DetectionServer \
  --camera img_test \
  --image prueba.jpg \
  --raft 127.0.0.1:7001 \
  --model "models/yolo11n(4).pt" \
  --interval-ms 5000 \
  --conf 0.35
```

## 10. Probar solo Java sin YOLO

Modo de demostracion del sistema distribuido sin instalar Python, Ultralytics ni ffmpeg:

```bash
java -cp build cc4p1.detector.DetectionServer \
  --camera cam_mock \
  --image prueba.jpg \
  --raft 127.0.0.1:7001 \
  --mode mock \
  --interval-ms 3000
```

## 11. Firewall

### Windows PowerShell como administrador

```powershell
New-NetFirewallRule -DisplayName "CC4P1 Raft Cliente" -Direction Inbound -Protocol TCP -LocalPort 7001-7003 -Action Allow
New-NetFirewallRule -DisplayName "CC4P1 Raft Interno" -Direction Inbound -Protocol TCP -LocalPort 7101-7103 -Action Allow
```

### Linux ufw

```bash
sudo ufw allow 7001:7003/tcp
sudo ufw allow 7101:7103/tcp
```

## 12. Protocolo resumido

### Entrada de deteccion

```text
EVENT|timestamp=...|camera=cam1|label=person|confidence=0.91|bbox=x1,y1,x2,y2|image=cam1_...jpg|imageB64=...
```

### Replicacion Raft

```text
APPEND|term=...|leader=n1|leaderHost=...|leaderClientPort=7001|commit=...|entryIndex=...|entryTerm=...|payload=...
```

### Cliente vigilante

```text
SUBSCRIBE
EVENT_COMMITTED|index=...|timestamp=...|camera=...|label=...|confidence=...|image=...
```

## 13. Evidencia esperada para informe

- Captura de los tres nodos mostrando lider y followers.
- Captura de `ManualEventClient` con `OK|committed=true|acks=3`.
- Captura de `events.csv` igual en los tres nodos.
- Captura del detector con YOLO enviando objetos detectados.
- Captura del `WatcherClient` recibiendo registros en tiempo real.
- Prueba de tolerancia: apagar el lider, esperar nueva eleccion, enviar otro evento a un nodo vivo y verificar que se confirme por mayoria.
