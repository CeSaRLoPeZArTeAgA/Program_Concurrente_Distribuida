@echo off
cd /d %~dp0\..
java -cp build cc4p1.raft.RaftNode --id n2 --host 127.0.0.1 --client-port 7002 --raft-port 7102 --peers n1@127.0.0.1:7101:7001,n3@127.0.0.1:7103:7003 --data data\n2
