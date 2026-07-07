#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
rm -rf data-local
./scripts/compile_java.sh
java -cp build/classes cc4p1.raft.RaftNode --id n1 --host 127.0.0.1 --bind 127.0.0.1 --client-port 7001 --raft-port 7101 --peers "n2@127.0.0.1:7002:7102,n3@127.0.0.1:7003:7103" --data data-local/n1 > /tmp/raft-n1.log 2>&1 &
P1=$!
java -cp build/classes cc4p1.raft.RaftNode --id n2 --host 127.0.0.1 --bind 127.0.0.1 --client-port 7002 --raft-port 7102 --peers "n1@127.0.0.1:7001:7101,n3@127.0.0.1:7003:7103" --data data-local/n2 > /tmp/raft-n2.log 2>&1 &
P2=$!
java -cp build/classes cc4p1.raft.RaftNode --id n3 --host 127.0.0.1 --bind 127.0.0.1 --client-port 7003 --raft-port 7103 --peers "n1@127.0.0.1:7001:7101,n2@127.0.0.1:7002:7102" --data data-local/n3 > /tmp/raft-n3.log 2>&1 &
P3=$!
sleep 5
java -cp build/classes cc4p1.client.StatusClient --host 127.0.0.1 --port 7001 || true
java -cp build/classes cc4p1.client.StatusClient --host 127.0.0.1 --port 7002 || true
java -cp build/classes cc4p1.client.StatusClient --host 127.0.0.1 --port 7003 || true
java -cp build/classes cc4p1.client.ManualEventClient --host 127.0.0.1 --port 7001 --camera cam_test --label person
sleep 1
java -cp build/classes cc4p1.client.SnapshotClient --host 127.0.0.1 --port 7001 || true
kill $P1 $P2 $P3 2>/dev/null || true
