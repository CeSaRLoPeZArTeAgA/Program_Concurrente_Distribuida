@echo off
cd /d %~dp0\..
java -cp build cc4p1.raft.RaftNode --id n3 --host 127.0.0.1 --client-port 7003 --raft-port 7103 --peers n1@127.0.0.1:7101:7001,n2@127.0.0.1:7102:7002 --data data\n3
