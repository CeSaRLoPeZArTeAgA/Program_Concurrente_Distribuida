#!/usr/bin/env bash
set -euo pipefail
sudo apt update
sudo apt install -y openjdk-21-jdk ufw unzip curl
# Ajusta estos puertos si cambias la configuracion.
sudo ufw allow 7001/tcp || true
sudo ufw allow 7002/tcp || true
sudo ufw allow 7003/tcp || true
sudo ufw allow 7101/tcp || true
sudo ufw allow 7102/tcp || true
sudo ufw allow 7103/tcp || true
sudo ufw allow OpenSSH || true
echo "[OK] Paquetes base instalados. Verifica: java -version"
