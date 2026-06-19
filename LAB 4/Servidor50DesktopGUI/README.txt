DOG MESSENGER - SERVIDOR DESKTOP GUI
=====================================

Este servidor desktop es compatible con:
- Cliente50 Android
- Client50 Desktop GUI
- Server50 Android como alternativa de servidor

Ejecutar desde PowerShell:

    cd C:\Users\Usuario\AndroidStudioProjects\Project_PC4\Servidor50DesktopGUI
    mkdir build\classes -Force
    javac -encoding UTF-8 -d build\classes src\redesOk\*.java
    java -cp build\classes redesOk.DesktopServer50GUI

O ejecutar directamente:

    .\run.bat

Funcionalidades:
- Campo Nombre para identificar mensajes del servidor.
- Broadcast: todo mensaje recibido de un cliente se reenvía a todos los clientes conectados.
- Envío y recepción de archivos por protocolo FILE|nombre|archivo|mime|base64.
- Los archivos recibidos en desktop se guardan en:

    C:\Users\<usuario>\DogMessengerRecibidos

- Overflow Menu:
    Nuevo grupo de chat: genera QR con dogmsg://IP:PUERTO.

Recomendación:
Usa el mismo puerto en servidor y clientes. Por defecto se usa 8189.

INTEGRACION TIENDA VIRTUAL:
- Levanta primero Server50TiendaDesktopGUI/run.bat.
- En este Servidor Desktop coloca Tienda IP y Tienda Port.
- Cuando los clientes se conectan, el servidor les envia internamente la IP/puerto de Server50Tienda.
- Boton Tienda Virtual: detiene el servidor de chat y abre la interfaz de tienda como cliente de Server50Tienda.
