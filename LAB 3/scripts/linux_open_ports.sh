#!/usr/bin/env bash
set -e
PORTS="$@"
if [ -z "$PORTS" ]; then echo "Uso: ./linux_open_ports.sh 8001 3307"; exit 1; fi
if command -v ufw >/dev/null 2>&1; then
  for p in $PORTS; do sudo ufw allow ${p}/tcp; done
  sudo ufw reload || true
  sudo ufw status
else
  echo "ufw no instalado. Abre manualmente los puertos: $PORTS"
fi
