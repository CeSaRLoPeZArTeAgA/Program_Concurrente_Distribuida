package tetris;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Toolkit;


//ventana principal del cliente de Tetris.
// Esta clase crea la ventana Swing, contiene el panel Board y administra la
// conexion TCP con el servidor. Además interpreta los mensajes que llegan desde
// el servidor y actualiza la interfaz grafica
public class WindowGame {

    //ancho inicial de la ventana, sse recalcula cuando llega el tamaño real del tablero
    public static int WIDTH = 445;

    //alto inicial de la ventana, se recalcula cuando llega el tamaño real del tablero
    public static int HEIGHT = 629;

    //ventana Swing donde se muestra el juego
    private JFrame window;

    // panel que dibuja el tablero, puntajes, controles e informacion
    private Board board;

    // cliente TCP usado para comunicarse con el servidor
    private TCPClient50 tcpClient;

    //identificador asignado por el servidor a este jugador
    private int playerId = 0;

    //nombre elegido por el jugador
    private final String playerName;

    // IP del servidor al que se conecta este cliente
    private final String serverIp;

    //puerto TCP del servidor
    private final int serverPort;

    //construye la ventana del juego y comienza la conexión TCP
    public WindowGame(String serverIp, int serverPort, String playerName) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.playerName = playerName;

        //primero se crea la interfaz grafica
        crearVentanaInicial();

        //luego se inicia la conexion con el servidor
        conectar();
    }

    
    //crea la ventana inicial y coloca un tablero provisional de 20x10
    private void crearVentanaInicial() {
        WIDTH = 600;
        HEIGHT = 720;

        window = new JFrame("Tetris Mix Multiplayer - Cliente " + serverIp + ":" + serverPort);
        window.setSize(WIDTH, HEIGHT);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.setResizable(false);

        //crea el panel de juego inicial
        board = new Board(20, 10);
        window.add(board);
        window.setVisible(true);

        //pide el foco para que el panel pueda recibir teclas
        SwingUtilities.invokeLater(() -> board.requestFocusInWindow());
    }

    
    //crea el cliente TCP y lo ejecuta en un hilo independiente, el hilo es necesario porque la lectura del socket es bloqueante
    //si se ejecutara directamente en el hilo de swing, la ventana se congelaria
    private void conectar() {
        tcpClient = new TCPClient50(serverIp, serverPort, new TCPClient50.OnMessageReceived() {
            @Override
            public void connected() {
                //al conectarse, el cliente se registra en el servidor con JOIN|nombre
                tcpClient.sendMessage("JOIN|" + playerName);
            }

            @Override
            public void messageReceived(String message) {
                //las actualizaciones de Swing deben hacerse en el hilo de eventos de swing
                SwingUtilities.invokeLater(() -> procesarMensajeServidor(message));
            }

            @Override
            public void error(String message) {
                // Muestra errores de conexión en una ventana emergente
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(window, message));
            }
        });

        //el tablero necesita el cliente TCP para enviar las teclas al servidor
        board.setNetworkClient(tcpClient);

        //inicia la lectura TCP sin bloquear la interfaz grafica
        new Thread(() -> tcpClient.run(), "Cliente-TCP").start();
    }

    
    //interpreta mensajes enviados por el servidor
    private void procesarMensajeServidor(String message) {
        String[] parts = message.split("\\|", 9);
        if (parts.length == 0) return;

        switch (parts[0]) {
            case "WELCOME":
                //el servidor asigna un identificador a este cliente
                if (parts.length >= 2) {
                    playerId = parseInt(parts[1], 0);
                    board.setPlayerId(playerId);
                    window.setTitle("Tetris Mix Multiplayer - Jugador " + playerId + " - " + playerName + " - " + serverIp + ":" + serverPort);
                }
                break;

            case "START":
                //el servidor informa el tamaño real del tablero y el ID del jugador
                if (parts.length >= 4) {
                    int rows = parseInt(parts[1], 20);
                    int cols = parseInt(parts[2], 10);
                    playerId = parseInt(parts[3], playerId);

                    //ajusta la ventana y el panel al tamaño del tablero indicado por el servidor
                    redimensionar(rows, cols);
                    board.setPlayerId(playerId);

                    if (parts.length >= 5) {
                        board.setInfo("Orden: " + parts[4]);
                    }
                }
                break;

            case "STATE":
                //actualiza tablero, puntajes, turno y estado de fin de juego
                board.applyState(message);
                break;

            case "INFO":
                //muestra información general enviada por el servidor
                if (parts.length >= 2) {
                    board.setInfo(parts[1]);
                }
                break;

            default:
                //cualquier otro mensaje se ignora para no romper la ejecucion
                break;
        }
    }

    //ajusta el tamaño de la ventana segun las dimensiones del tablero     
    private void redimensionar(int rows, int cols) {
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();

        //margenes reservados para el panel lateral y bordes de ventana
        int margenAncho = 220;
        int margenAlto = 120;

        //calcula el tamaño maximo de bloque que cabe en la pantalla
        int blockPorAlto = (pantalla.height - margenAlto) / rows;
        int blockPorAncho = (pantalla.width - margenAncho) / cols;

        //ll bloque se limita entre 12 y 30 pixeles para mantener legibilidad
        Board.blockSize = Math.max(12, Math.min(30, Math.min(blockPorAlto, blockPorAncho)));

        WIDTH = cols * Board.blockSize + 220;
        HEIGHT = rows * Board.blockSize + 70;

        //reemplaza el tablero anterior por uno con las dimensiones definitivas
        window.remove(board);
        board = new Board(rows, cols);
        board.setPlayerId(playerId);
        board.setNetworkClient(tcpClient);
        window.add(board);

        SwingUtilities.invokeLater(() -> board.requestFocusInWindow());
        window.setSize(WIDTH, HEIGHT);
        window.setLocationRelativeTo(null);
        window.revalidate();
        window.repaint();
    }

    //convierte texto a entero usando un valor de respaldo si falla   
    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    
    //segundo punto de entrada alternativo
    //permite ejecutar directamente WindowGame como clase principal, sin usar
    //Cliente50. Pide IP, puerto y nombre igual que Cliente50
    public static void main(String[] args) {
        String ip = pedirTexto("IP del servidor:", "127.0.0.1");
        int port = pedirPuerto("Puerto del servidor:", TCPClient50.DEFAULT_SERVER_PORT);
        String name = pedirTexto("Nombre del jugador:", "Jugador");
        new WindowGame(ip.trim(), port, name.trim());
    }

    //pide texto al usuario mediante JOptionPane
    private static String pedirTexto(String mensaje, String defecto) {
        String valor = JOptionPane.showInputDialog(null, mensaje, defecto);
        if (valor == null || valor.trim().isEmpty()) {
            return defecto;
        }
        return valor.trim();
    }

    
    //pide un puerto al usuario y valida que este en el rango permitido
    private static int pedirPuerto(String mensaje, int defecto) {
        while (true) {
            String texto = JOptionPane.showInputDialog(null, mensaje, String.valueOf(defecto));
            if (texto == null || texto.trim().isEmpty()) {
                return defecto;
            }

            try {
                int puerto = Integer.parseInt(texto.trim());
                if (puerto >= 1 && puerto <= 65535) {
                    return puerto;
                }
            } catch (NumberFormatException ignored) {
                //se muestra el mensaje de error al final del ciclo
            }

            JOptionPane.showMessageDialog(
                    null,
                    "Puerto inválido. Ingrese un número entre 1 y 65535.",
                    "Error de puerto",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
