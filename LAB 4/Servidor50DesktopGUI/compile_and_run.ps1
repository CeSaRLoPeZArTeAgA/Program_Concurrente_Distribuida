Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force -Path "build\classes" | Out-Null
javac -encoding UTF-8 -d build\classes src\redesOk\*.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: No se pudo compilar. Verifica que el JDK este instalado y javac este en el PATH."
    exit 1
}
if (Test-Path "src\redesOk\dog_shield_crop.png") {
    Copy-Item "src\redesOk\dog_shield_crop.png" "build\classes\redesOk\dog_shield_crop.png" -Force
}
java -cp build\classes redesOk.DesktopServer50GUI
