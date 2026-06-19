Dog Messenger - Cliente Desktop GUI

Esta version usa la camara interna del equipo para leer QR.
Para la camara se agregaron librerias Java mediante Maven:
- webcam-capture
- ZXing core
- ZXing javase

Ejecutar desde esta carpeta:

    .\run.bat

O en PowerShell:

    .\compile_and_run.ps1

Tambien se puede ejecutar manualmente:

    mvn -q -DskipTests compile exec:java

IMPORTANTE:
- Ya no se recomienda compilar este cliente con javac directo, porque el escaneo por camara requiere librerias externas.
- Si Windows bloquea la camara, habilitar el permiso en:
  Configuracion > Privacidad y seguridad > Camara.
- Si no se detecta camara, la aplicacion permite pegar el contenido del QR manualmente.

Funciones:
- Campo Nombre para identificar al dispositivo.
- Mensajes con formato [nombre]: mensaje.
- Estados con formato [nombre] conectado / [nombre] desconectado.
- Conectar/desconectar al Server50 usando IP y puerto configurables.
- Broadcast general entre servidor, cliente movil y cliente desktop.
- Enviar y recibir archivos adjuntos.
- Los archivos recibidos se guardan en la carpeta DogMessengerRecibidos del usuario de Windows.
- Menu:
  * Unirse a Grupo de Chat
  * Vincular Dispositivo
  * Scanear p/clonar Dispositivo
  * QR p/clonar Dispositivo
- Vincular Dispositivo y Scanear p/clonar Dispositivo abren la camara interna para leer el QR.

INTEGRACION TIENDA VIRTUAL:
- Conectate primero a Server50.
- Server50 enviara internamente la IP/puerto de Server50Tienda.
- Boton Tienda Virtual: desconecta este cliente del chat principal y abre la interfaz Tienda Virtual.
- Volver a chat anterior: cierra Tienda Virtual y reconecta con Server50.
