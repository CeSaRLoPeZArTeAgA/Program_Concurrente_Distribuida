package tetris;

import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Servidor50 {
    private TCPServer50 tcpServer;
    private TetrisGameEngine game;
    private ScheduledExecutorService ticker;
    private int expectedPlayers;
    private volatile boolean gameStarted = false;

    public static void main(String[] args) {
        new Servidor50().iniciar(args);
    }

    public void iniciar(String[] args) {
        Scanner sc = new Scanner(System.in);

        int port;
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
            port = pedirEntero(sc, "Ingrese puerto del servidor:", TCPServer50.DEFAULT_PORT, 1024, 65535);
        }

        expectedPlayers = pedirEntero(sc, "Ingrese número de jugadores:", 2, 1, 9);
        int rows = pedirEntero(sc, "Ingrese filas del tablero:", 20, 10, 40);
        int cols = pedirEntero(sc, "Ingrese columnas del tablero:", 10, 6, 30);

        game = new TetrisGameEngine(rows, cols);

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

        new Thread(() -> tcpServer.run(), "Servidor-TCP").start();

        System.out.println("Servidor listo en el puerto " + port + ". Esperando " + expectedPlayers + " jugador(es).");
        System.out.println("Comandos del servidor: start, state, exit");

        boolean salir = false;
        while (!salir) {
            String cmd = sc.nextLine().trim();
            switch (cmd) {
                case "start":
                    iniciarPartidaSiSePuede(true);
                    break;
                case "state":
                    broadcastState();
                    break;
                case "exit":
                case "s":
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

    private int validarPuerto(String texto) {
        try {
            int port = Integer.parseInt(texto);
            if (port >= 1024 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    private int pedirEntero(Scanner sc, String mensaje, int defecto, int min, int max) {
        while (true) {
            System.out.print(mensaje + " [" + defecto + "] ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) return defecto;
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Valor inválido. Rango permitido: " + min + " a " + max);
        }
    }

    private synchronized void servidorClienteConectado(int clientId) {
        tcpServer.sendToClient(clientId, "WELCOME|" + clientId + "|Conectado como Jugador " + clientId);
        System.out.println("J" + clientId + " conectado. Debe enviar JOIN|nombre.");
    }

    private synchronized void servidorRecibe(int clientId, String message) {
        System.out.println("J" + clientId + " -> " + message);

        String[] parts = message.split("\\|", 3);
        String command = parts[0].trim();

        if ("JOIN".equals(command)) {
            String name = parts.length >= 2 ? parts[1] : "Jugador " + clientId;
            game.addPlayer(clientId, name);
            tcpServer.sendToClient(clientId, game.startMessageForClient(clientId));
            tcpServer.sendMessageTCPServer("INFO|" + game.orderText());
            iniciarPartidaSiSePuede(false);
            broadcastState();
            return;
        }

        if ("KEY".equals(command) && parts.length >= 2) {
            game.handleKey(clientId, parts[1].trim());
            broadcastState();
        }
    }

    private synchronized void iniciarPartidaSiSePuede(boolean force) {
        if (gameStarted) return;
        if (!force && game.getPlayerCount() < expectedPlayers) {
            return;
        }
        if (game.getPlayerCount() == 0) {
            System.out.println("No se puede iniciar: no hay jugadores registrados con JOIN.");
            return;
        }

        game.startGame();
        gameStarted = true;

        for (int id = 1; id <= 99; id++) {
            if (tcpServer.hasClient(id) && game.hasPlayer(id)) {
                tcpServer.sendToClient(id, game.startMessageForClient(id));
            }
        }

        tcpServer.sendMessageTCPServer("INFO|Inicio del juego. " + game.orderText());
        broadcastState();

        ticker = Executors.newSingleThreadScheduledExecutor();
        ticker.scheduleAtFixedRate(() -> {
            game.tick();
            broadcastState();
        }, 0, 600, TimeUnit.MILLISECONDS);
    }

    private synchronized void broadcastState() {
        if (tcpServer != null && game != null) {
            tcpServer.sendMessageTCPServer(game.serializeState());
        }
    }

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
