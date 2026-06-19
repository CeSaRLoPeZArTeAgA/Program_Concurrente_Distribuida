$ErrorActionPreference = "Stop"
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    Write-Host "Maven no esta instalado o no esta en el PATH." -ForegroundColor Red
    Write-Host "Instala Maven o abre este proyecto DesktopClient50GUI en un IDE Java que lea pom.xml."
    exit 1
}
mvn -q -DskipTests compile exec:java
