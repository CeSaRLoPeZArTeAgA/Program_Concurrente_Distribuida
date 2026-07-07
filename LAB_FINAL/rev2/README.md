# CC4P1 - Sistema distribuido YOLO + RTSP + Raft

## Arquitectura final

```text
Raspberry Pi 1 + camara CSI  -> RTSP -> Servidor de Testeo YOLO
Raspberry Pi 2 + camara CSI  -> RTSP -> Servidor de Testeo YOLO
Raspberry Pi 3 + camara CSI  -> RTSP -> Servidor de Testeo YOLO

Servidor de Testeo YOLO detecta objetos/personas/animales.
Cuando detecta algo, crea un EVENT:
  eventId, camara, objeto, confianza, bbox, fecha, hora, imagen

El EVENT se envia al cluster Raft:
  VM Ubuntu n1
  VM Ubuntu n2
  VM Ubuntu n3

Raft confirma por mayoria.
Solo los eventos committed se guardan oficialmente en events.csv e imagenes.
El cliente consulta SNAPSHOT o SUBSCRIBE a cualquier nodo Raft.
```

## Maquinas del despliegue

### 3 Raspberry Pi 4B
Cada Raspberry solo transmite video por RTSP.

Ejemplos de URL:

```text
rtsp://IP_RASPI_1:8554/cam1
rtsp://IP_RASPI_2:8554/cam2
rtsp://IP_RASPI_3:8554/cam3
```

### Desktop - Servidor de Testeo YOLO
Tiene Python, Ultralytics, OpenCV y el modelo `models/yolo11n.pt`.
Lee las 3 camaras RTSP y manda eventos a Raft.

### 3 maquinas virtuales Ubuntu Server
Cada VM ejecuta un nodo Raft Java.

Ejemplo de IPs:

```text
n1 = 192.168.0.201 clientPort=7001 raftPort=7101
n2 = 192.168.0.202 clientPort=7002 raftPort=7102
n3 = 192.168.0.203 clientPort=7003 raftPort=7103
```

## Instalacion en cada VM Ubuntu Server

Copiar este proyecto a cada VM. Luego:

```bash
cd cc4p1_yolo_raft_final
chmod +x scripts/*.sh
./scripts/install_ubuntu_raft.sh
./scripts/compile_java.sh
```

Abrir puertos si usas firewall:

```bash
sudo ufw allow 7001/tcp
sudo ufw allow 7002/tcp
sudo ufw allow 7003/tcp
sudo ufw allow 7101/tcp
sudo ufw allow 7102/tcp
sudo ufw allow 7103/tcp
```

## Ejecutar cluster Raft

### En VM n1

```bash
./scripts/run_raft_node.sh n1 192.168.0.201 7001 7101 "n2@192.168.0.202:7002:7102,n3@192.168.0.203:7003:7103"
```

### En VM n2

```bash
./scripts/run_raft_node.sh n2 192.168.0.202 7002 7102 "n1@192.168.0.201:7001:7101,n3@192.168.0.203:7003:7103"
```

### En VM n3

```bash
./scripts/run_raft_node.sh n3 192.168.0.203 7003 7103 "n1@192.168.0.201:7001:7101,n2@192.168.0.202:7002:7102"
```

Despues de 1.5 a 3 segundos debe elegirse un lider.

## Consultar estado Raft

Desde cualquier maquina con Java compilado:

```bash
java -cp build/classes cc4p1.client.StatusClient --host 192.168.0.201 --port 7001
java -cp build/classes cc4p1.client.StatusClient --host 192.168.0.202 --port 7002
java -cp build/classes cc4p1.client.StatusClient --host 192.168.0.203 --port 7003
```

## Enviar evento manual de prueba

```bash
java -cp build/classes cc4p1.client.ManualEventClient --host 192.168.0.201 --port 7001 --camera cam_test --label persona
```

Aunque lo envies a un follower, este lo reenvia al lider.

## Consultar eventos committed

```bash
java -cp build/classes cc4p1.client.SnapshotClient --host 192.168.0.201 --port 7001
```

Para escuchar en vivo:

```bash
java -cp build/classes cc4p1.client.WatcherClient --host 192.168.0.201 --port 7001
```

## Instalar en cada Raspberry Pi 4B

Copiar la carpeta `raspberry/` y ejecutar:

```bash
sudo apt update
sudo apt install -y python3-gi gir1.2-gstreamer-1.0 gir1.2-gst-rtsp-server-1.0 \
  gstreamer1.0-tools gstreamer1.0-libcamera gstreamer1.0-plugins-base \
  gstreamer1.0-plugins-good gstreamer1.0-plugins-bad gstreamer1.0-plugins-ugly
```

En Raspberry 1:

```bash
python3 raspi_rtsp_server.py --path cam1 --port 8554
```

En Raspberry 2:

```bash
python3 raspi_rtsp_server.py --path cam2 --port 8554
```

En Raspberry 3:

```bash
python3 raspi_rtsp_server.py --path cam3 --port 8554
```

Probar desde la Desktop:

```bash
ffplay -rtsp_transport tcp rtsp://IP_RASPI_1:8554/cam1
```

## Instalar Servidor de Testeo YOLO en Desktop Windows

Desde la raiz del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install_desktop_yolo_windows.ps1
```

Editar `config/cameras.example.json` con las IP reales de las 3 Raspberry.

Luego ejecutar:

```powershell
.\.venv\Scripts\python.exe detector\yolo_rtsp_detector.py `
  --cameras config\cameras.example.json `
  --model models\yolo11n.pt `
  --raft 192.168.0.201:7001,192.168.0.202:7002,192.168.0.203:7003 `
  --conf 0.35 `
  --interval 1.0
```

## Como trabaja Raft en este proyecto

Raft NO procesa video ni ejecuta YOLO.
Raft solo confirma los eventos ya detectados.

Secuencia:

```text
1. YOLO detecta objeto en frame RTSP.
2. Servidor YOLO guarda captura anotada.
3. Servidor YOLO crea EVENT.
4. EVENT llega a cualquier nodo Raft.
5. Si el nodo no es lider, lo reenvia al lider.
6. El lider agrega el EVENT a su log.
7. El lider replica el EVENT a followers.
8. Si hay mayoria, el EVENT queda committed.
9. Los nodos guardan events.csv e imagen.
10. El cliente consulta solo eventos committed.
```

Con 3 nodos, la mayoria es 2. Por eso puede fallar un nodo y el sistema todavia puede confirmar eventos.

## Archivos importantes

```text
java/src/cc4p1/raft/RaftNode.java          Nodo Raft
java/src/cc4p1/client/*.java              Clientes de estado, snapshot y watcher
detector/yolo_rtsp_detector.py            Servidor de Testeo YOLO multipcamara
raspberry/raspi_rtsp_server.py            Transmisor RTSP para Raspberry
config/cameras.example.json               Configuracion de camaras
models/yolo11n.pt                         Modelo YOLO preentrenado
```
