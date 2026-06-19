@echo off
cd /d "%~dp0"
mkdir build\classes 2>nul
javac -encoding UTF-8 -d build\classes src\redesOk\*.java
if errorlevel 1 (
    echo.
    echo ERROR: No se pudo compilar. Verifica que el JDK este instalado y javac este en el PATH.
    pause
    exit /b 1
)
if exist src\redesOk\dog_shield_crop.png copy /Y src\redesOk\dog_shield_crop.png build\classes\redesOk\dog_shield_crop.png >nul
java -cp build\classes redesOk.DesktopServer50GUI
pause
