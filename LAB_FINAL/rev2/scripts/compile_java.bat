@echo off
setlocal
cd /d %~dp0\..
if not exist build\classes mkdir build\classes
powershell -NoProfile -Command "Get-ChildItem -Recurse java\src -Filter *.java | ForEach-Object { $_.FullName } | Set-Content sources.txt -Encoding ASCII"
javac -encoding UTF-8 -d build\classes @sources.txt
if errorlevel 1 exit /b 1
echo [OK] Compilado en build\classes
