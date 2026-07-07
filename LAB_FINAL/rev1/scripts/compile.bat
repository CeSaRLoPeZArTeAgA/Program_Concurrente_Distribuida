@echo off
cd /d %~dp0\..
if exist build rmdir /s /q build
mkdir build
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d build @sources.txt
if errorlevel 1 exit /b 1
echo Compilado OK. Ejecuta con: java -cp build cc4p1.raft.RaftNode ...
