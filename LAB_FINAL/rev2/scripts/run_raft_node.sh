#!/usr/bin/env bash
set -euo pipefail
# Uso:
# ./scripts/run_raft_node.sh n1 192.168.0.201 7001 7101 "n2@192.168.0.202:7002:7102,n3@192.168.0.203:7003:7103"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ID="${1:?id}"
HOST="${2:?host_publico}"
CLIENT_PORT="${3:?client_port}"
RAFT_PORT="${4:?raft_port}"
PEERS="${5:?peers}"
java -cp build/classes cc4p1.raft.RaftNode \
  --id "$ID" \
  --host "$HOST" \
  --bind 0.0.0.0 \
  --client-port "$CLIENT_PORT" \
  --raft-port "$RAFT_PORT" \
  --peers "$PEERS" \
  --data "data/$ID"
