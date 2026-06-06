package tetris;

import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//clase principal del servidor del juego Tetris multijugador
public class Servidor50 {
    //servidor TCP encargado de aceptar clientes y enviar/recibir mensajes
    private TCPServer50 tcpServer;

    //aca se guarda el tablero, turnos, piezas y puntajes
    private TetrisGameEngine game;

    //temporizador que ejecuta automaticamente el avance del juego cada cierto tiempo
    private ScheduledExecutorService ticker;

    //cantidad de jugadores esperados antes de iniciar automaticamente la partida
    private int expectedPlayers;

    //indica si la partida ya fue iniciada. volatile ayuda a visibilidad entre hilos
    private volatile boolean gameStarted = false;

    //punto de entrada del programa
    public static void main(String[] args) {
        new Servidor50().iniciar(args);
    }

    
    //Inicializa el servidor completo.
    public void iniciar(String[] args) {
        Scanner sc = new Scanner(System.in);

        int port;

        // Si se pasa el puerto como argumento de consola, se valida
        if (args.length >= 1) {
            port = validarPuerto(args[0]);
            if (port == -1) {
                System.out.println("Puerto inválido: " + args[0]);
                System.out.println("Uso correcto: java -cp out tetris.Servidor50 <puerto>");
                System.out.println("Ejemplo: java -cp out tetris.Servidor50 5000");
                sc.close();
                return;
            }
        } else {
            //si no se pasa el puerto, se solicita por teclado
            port = pedirEntero(sc, "Ingrese puerto del servidor:", TCPServer50.DEFAULT_PORT, 1024, 65535);
        }

        //configuracion bssica de la partida(datos de jugadores y tablero)
        expectedPlayers = pedirEntero(sc, "Ingrese número de jugadores:", 2, 1, 9);
        int rows = pedirEntero(sc, "Ingrese filas del tablero:", 20, 10, 40);
        int cols = pedirEntero(sc, "Ingrese columnas del tablero:", 10, 6, 30);

        //se crea el motor logico del juego con el tamaño indicado
        game = new TetrisGameEngine(rows, cols);

        //crea el servidor TCP y se implementa la interfaz de callbacks (estos metodos se ejecutan cuando un cliente se conecta)
        tcpServer = new TCPServer50(port, new TCPServer50.OnMessageReceived() {
            @Override
            public void messageReceived(int clientId, String message) {
                servidorRecibe(clientId, message);
            }

            @Override
            public void clientConnected(int clientId) {
                servidorClienteConectado(clientId);
            }

            @Override
            public void clientDisconnected(int clientId) {
                System.out.println("Jugador J" + clientId + " salió de la partida.");
            }
        });

        // el servidor TCP se ejecuta en un hilo separado 
        new Thread(() -> tcpServer.run(), "Servidor-TCP").start();

        System.out.println("Servidor listo en el puerto " + port + ". Esperando " + expectedPlayers + " jugador(es).");
        System.out.println("Comandos del servidor: start, state, exit");

        //bucle principal de comandos ingresados desde la consola del servidor
        boolean salir = false;
        while (!salir) {
            String cmd = sc.nextLine().trim();
            switch (cmd) {
                case "start":
                    //fuerza el inicio aunque todavia no esten todos los jugadores esperados
                    iniciarPartidaSiSePuede(true);
                    break;
                case "state":
                    //envia el estado actual del juego a todos los clientes
                    broadcastState();
                    break;
                case "exit":
                case "s":
                    //finaliza el servidor.
                    salir = true;
                    break;
                default:
                    System.out.println("Comando no reconocido. Use: start, state, exit");
                    break;
            }
        }

        detener();
        sc.close();
    }

    //convierte un texto a numero de puerto y verifica que esta en el rango permitido
    private int validarPuerto(String texto) {
        try {
            int port = Integer.parseInt(texto);
            if (port >= 1024 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
            //se ignora porque al final se retornará -1.
        }
        return -1;
    }

    //solicita un numero entero por consola, usando un valor por defecto si el usuario no escribe nada
    private int pedirEntero(Scanner sc, String mensaje, int defecto, int min, int max) {
        while (true) {
            System.out.print(mensaje + " [" + defecto + "] ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) return defecto;
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
                //si no es entero, se muestra el mensaje de error y se vuelve a pedir
            }
            System.out.println("Valor inválido. Rango permitido: " + min + " a " + max);
        }
    }

    //atiende el evento de conexion de un cliente
    //envia un mensaje WELCOME solo a ese cliente y muestra en consola que debe registrarse con JOIN
    private synchronized void servidorClienteConectado(int clientId) {
        tcpServer.sendToClient(clientId, "WELCOME|" + clientId + "|Conectado como Jugador " + clientId);
        System.out.println("J" + clientId + " conectado. Debe enviar JOIN|nombre.");
    }

    //procesa los mensajes que llegan desde un cliente
    // se espera:
    // - JOIN|nombre : registra al jugador en el motor del juego
    // - KEY|tecla   : envía una acción del jugador al motor del juego
    private synchronized void servidorRecibe(int clientId, String message) {
        System.out.println("J" + clientId + " -> " + message);

        //se separa el mensaje usando el símbolo '|'
        //el límite 3 evita partir más de lo necesario
        String[] parts = message.split("\\|", 3);
        String command = parts[0].trim();

        if ("JOIN".equals(command)) {
            String name = parts.length >= 2 ? parts[1] : "Jugador " + clientId;

            //se registra el jugador dentro del juego
            game.addPlayer(clientId, name);

            //se informa al cliente el tamaño del tablero, su id y el orden de jugadores
            tcpServer.sendToClient(clientId, game.startMessageForClient(clientId));

            //se informa a todos los clientes el orden de jugadores
            tcpServer.sendMessageTCPServer("INFO|" + game.orderText());

            //si ya estan todos los jugadores esperados, la partida puede iniciar automaticamente
            iniciarPartidaSiSePuede(false);

            //se envia una actualizacion del estado actual del juego
            broadcastState();
            return;
        }

        if ("KEY".equals(command) && parts.length >= 2) {
            //la tecla se procesa en el motor. Luego se difunde el nuevo estado
            game.handleKey(clientId, parts[1].trim());
            broadcastState();
        }
    }

    //inicia la partida si cumple las condiciones necesarias
    private synchronized void iniciarPartidaSiSePuede(boolean force) {
        //si el juego ya empezo, no se debe iniciar otra vez
        if (gameStarted) return;

        //si no se fuerza el inicio, espera hasta alcanzar la cantidad esperada de jugadores
        if (!force && game.getPlayerCount() < expectedPlayers) {
            return;
        }

        //no se puede iniciar una partida sin jugadores registrados mediante JOIN
        if (game.getPlayerCount() == 0) {
            System.out.println("No se puede iniciar: no hay jugadores registrados con JOIN.");
            return;
        }

        //inicializa el tablero, puntajes y primera pieza
        game.startGame();
        gameStarted = true;

        //envia un mensaje START a cada cliente conectado y registrado como jugador
        for (int id = 1; id <= 99; id++) {
            if (tcpServer.hasClient(id) && game.hasPlayer(id)) {
                tcpServer.sendToClient(id, game.startMessageForClient(id));
            }
        }

        //notifica a todos que empezo el juego y manda el primer estado
        tcpServer.sendMessageTCPServer("INFO|Inicio del juego. " + game.orderText());
        broadcastState();

        //crea un temporizador que avanza el juego cada 600 ms
        //en cada ciclo cae la pieza activa y se envía el nuevo estado a los clientes
        ticker = Executors.newSingleThreadScheduledExecutor();
        ticker.scheduleAtFixedRate(() -> {
            game.tick();
            broadcastState();
        }, 0, 600, TimeUnit.MILLISECONDS);
    }

    //envia el estado serializado del juego a todos los clientes conectados
    private synchronized void broadcastState() {
        if (tcpServer != null && game != null) {
            tcpServer.sendMessageTCPServer(game.serializeState());
        }
    }

    //detiene el temporizador del juego y cierra el servidor TCP
    private void detener() {
        if (ticker != null) {
            ticker.shutdownNow();
        }
        if (tcpServer != null) {
            tcpServer.stopServer();
        }
        System.out.println("Servidor detenido.");
    }
}
