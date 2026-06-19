@echo off
mkdir build\classes 2>nul
javac -encoding UTF-8 -d build\classes src\redesOk\*.java
if errorlevel 1 pause && exit /b 1
java -cp build\classes redesOk.Server50TiendaGUI
pause
