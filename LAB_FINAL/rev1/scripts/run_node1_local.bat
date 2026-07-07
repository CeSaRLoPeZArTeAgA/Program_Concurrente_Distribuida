@echo off
cd /d %~dp0\..
java -cp build cc4p1.raft.RaftNode --id n1 --host 127.0.0.1 --client-port 7001 --raft-port 7101 --peers n2@127.0.0.1:7102:7002,n3@127.0.0.1:7103:7003 --data data\n1
