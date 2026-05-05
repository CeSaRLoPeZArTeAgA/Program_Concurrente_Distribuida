package tetris;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Toolkit;

public class WindowGame {
    public static int WIDTH = 445;
    public static int HEIGHT = 629;

    private JFrame window;
    private Board board;
    private TCPClient50 tcpClient;
    private int playerId = 0;
    private final String playerName;
    private final String serverIp;
    private final int serverPort;

    public WindowGame(String serverIp, int serverPort, String playerName) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.playerName = playerName;
        crearVentanaInicial();
        conectar();
    }

    private void crearVentanaInicial() {
        WIDTH = 600;
        HEIGHT = 720;
        window = new JFrame("Tetris Mix Multiplayer - Cliente " + serverIp + ":" + serverPort);
        window.setSize(WIDTH, HEIGHT);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.setResizable(false);

        board = new Board(20, 10);
        window.add(board);
        window.setVisible(true);
        SwingUtilities.invokeLater(() -> board.requestFocusInWindow());
    }

    private void conectar() {
        tcpClient = new TCPClient50(serverIp, serverPort, new TCPClient50.OnMessageReceived() {
            @Override
            public void connected() {
                tcpClient.sendMessage("JOIN|" + playerName);
            }

            @Override
            public void messageReceived(String message) {
                SwingUtilities.invokeLater(() -> procesarMensajeServidor(message));
            }

            @Override
            public void error(String message) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(window, message));
            }
        });

        board.setNetworkClient(tcpClient);
        new Thread(() -> tcpClient.run(), "Cliente-TCP").start();
    }

    private void procesarMensajeServidor(String message) {
        String[] parts = message.split("\\|", 9);
        if (parts.length == 0) return;

        switch (parts[0]) {
            case "WELCOME":
                if (parts.length >= 2) {
                    playerId = parseInt(parts[1], 0);
                    board.setPlayerId(playerId);
                    window.setTitle("Tetris Mix Multiplayer - Jugador " + playerId + " - " + playerName + " - " + serverIp + ":" + serverPort);
                }
                break;

            case "START":
                if (parts.length >= 4) {
                    int rows = parseInt(parts[1], 20);
                    int cols = parseInt(parts[2], 10);
                    playerId = parseInt(parts[3], playerId);
                    redimensionar(rows, cols);
                    board.setPlayerId(playerId);
                    if (parts.length >= 5) {
                        board.setInfo("Orden: " + parts[4]);
                    }
                }
                break;

            case "STATE":
                board.applyState(message);
                break;

            case "INFO":
                if (parts.length >= 2) {
                    board.setInfo(parts[1]);
                }
                break;

            default:
                break;
        }
    }

    private void redimensionar(int rows, int cols) {
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int margenAncho = 220;
        int margenAlto = 120;
        int blockPorAlto = (pantalla.height - margenAlto) / rows;
        int blockPorAncho = (pantalla.width - margenAncho) / cols;
        Board.blockSize = Math.max(12, Math.min(30, Math.min(blockPorAlto, blockPorAncho)));

        WIDTH = cols * Board.blockSize + 220;
        HEIGHT = rows * Board.blockSize + 70;

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

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static void main(String[] args) {
        String ip = pedirTexto("IP del servidor:", "192.168.1.119");
        int port = pedirPuerto("Puerto del servidor:", TCPClient50.DEFAULT_SERVER_PORT);
        String name = pedirTexto("Nombre del jugador:", "Jugador");
        new WindowGame(ip.trim(), port, name.trim());
    }

    private static String pedirTexto(String mensaje, String defecto) {
        String valor = JOptionPane.showInputDialog(null, mensaje, defecto);
        if (valor == null || valor.trim().isEmpty()) {
            return defecto;
        }
        return valor.trim();
    }

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
