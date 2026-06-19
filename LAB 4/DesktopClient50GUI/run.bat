@echo off
chcp 65001 >nul
where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven no esta instalado o no esta en el PATH.
    echo Instala Maven o ejecuta desde IntelliJ/NetBeans agregando las dependencias del pom.xml.
    pause
    exit /b 1
)
mvn -q -DskipTests compile exec:java
pause
