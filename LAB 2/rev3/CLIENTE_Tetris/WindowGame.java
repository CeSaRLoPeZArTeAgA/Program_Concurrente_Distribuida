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

    public WindowGame(String serverIp, String playerName) {
        this.serverIp = serverIp;
        this.playerName = playerName;
        crearVentanaInicial();
        conectar();
    }

    private void crearVentanaInicial() {
        WIDTH = 600;
        HEIGHT = 720;
        window = new JFrame("Tetris Mix Multiplayer - Cliente");
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
        tcpClient = new TCPClient50(serverIp, new TCPClient50.OnMessageReceived() {
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
                    window.setTitle("Tetris Mix Multiplayer - Jugador " + playerId + " - " + playerName);
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
        String ip = JOptionPane.showInputDialog(null, "IP del servidor:", "127.0.0.1");
        if (ip == null || ip.trim().isEmpty()) ip = "127.0.0.1";
        String name = JOptionPane.showInputDialog(null, "Nombre del jugador:", "Jugador");
        if (name == null || name.trim().isEmpty()) name = "Jugador";
        new WindowGame(ip.trim(), name.trim());
    }
}
