package tetris;

import javax.swing.JPanel;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

public class Board extends JPanel implements KeyListener {
    private static final long serialVersionUID = 1L;

    public static int blockSize = 30;

    private BufferedImage controlesImage;

    private final int boardHeight;
    private final int boardWidth;
    private int[][] grid;
    private final Map<Integer, Integer> scores = new LinkedHashMap<>();

    private int playerId = 0;
    private int currentPlayerId = 0;
    private int winnerId = 0;
    private boolean gameOver = false;
    private String info = "Conectando al servidor...";

    private TCPClient50 tcpClient;

    private final Color[] colors = {
        Color.decode("#ed1c24"),
        Color.decode("#ff7f27"),
        Color.decode("#fff200"),
        Color.decode("#22b14c"),
        Color.decode("#00a2e8"),
        Color.decode("#a349a4"),
        Color.decode("#3f48cc"),
        Color.PINK,
        Color.CYAN
    };

    public Board(int boardHeight, int boardWidth) {
        this.boardHeight = boardHeight;
        this.boardWidth = boardWidth;
        this.grid = new int[boardHeight][boardWidth];
         try {
        controlesImage = ImageLoader.loadImage("/controles.png");
        } catch (Exception e) {
            controlesImage = null;
            System.out.println("No se pudo cargar la imagen de controles: " + e.getMessage());
        }
        setFocusable(true);
        setRequestFocusEnabled(true);
        installKeyBindings();
    }

    private void installKeyBindings() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        bindKey(inputMap, actionMap, KeyEvent.VK_LEFT, "LEFT");
        bindKey(inputMap, actionMap, KeyEvent.VK_RIGHT, "RIGHT");
        bindKey(inputMap, actionMap, KeyEvent.VK_DOWN, "DOWN");
        bindKey(inputMap, actionMap, KeyEvent.VK_A, "ROT_CCW");
        bindKey(inputMap, actionMap, KeyEvent.VK_S, "ROT_CW");
    }

    private void bindKey(InputMap inputMap, ActionMap actionMap, int keyCode, String command) {
        String actionName = "send_" + command;
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0), actionName);
        actionMap.put(actionName, new AbstractAction() {
            
            public void actionPerformed(ActionEvent e) {
                sendKey(command);
            }
        });
    }

    public void setNetworkClient(TCPClient50 tcpClient) {
        this.tcpClient = tcpClient;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
        repaint();
    }

    public void setInfo(String info) {
        this.info = info == null ? "" : info;
        repaint();
    }

    public void applyState(String message) {
        String[] parts = message.split("\\|", 9);
        if (parts.length < 9 || !"STATE".equals(parts[0])) {
            return;
        }

        int rows = parseInt(parts[1], boardHeight);
        int cols = parseInt(parts[2], boardWidth);
        currentPlayerId = parseInt(parts[3], 0);
        gameOver = Boolean.parseBoolean(parts[4]);
        winnerId = parseInt(parts[5], 0);
        parseScores(parts[6]);
        parseGrid(parts[7], rows, cols);
        info = parts[8];
        repaint();
    }

    private void parseGrid(String encoded, int rows, int cols) {
        if (rows != boardHeight || cols != boardWidth) {
            return;
        }
        if (encoded.length() < rows * cols) {
            return;
        }
        int k = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = decodeCell(encoded.charAt(k++));
            }
        }
    }

    private void parseScores(String text) {
        scores.clear();
        if (text == null || text.trim().isEmpty()) return;
        String[] pairs = text.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                scores.put(parseInt(kv[0], 0), parseInt(kv[1], 0));
            }
        }
    }

    private int decodeCell(char ch) {
        if (ch == '.') return 0;
        if (ch >= '0' && ch <= '9') return ch - '0';
        if (ch >= 'A' && ch <= 'Z') return 10 + (ch - 'A');
        return 0;
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        for (int r = 0; r < boardHeight; r++) {
            for (int c = 0; c < boardWidth; c++) {
                int value = grid[r][c];
                if (value != 0) {
                    g.setColor(colorFor(value));
                    g.fillRect(c * blockSize, r * blockSize, blockSize, blockSize);
                }
            }
        }
        //Color.DARK_GRAY    
        g.setColor(Color.WHITE );
        for (int r = 0; r <= boardHeight; r++) {
            g.drawLine(0, r * blockSize, boardWidth * blockSize, r * blockSize);
        }
        for (int c = 0; c <= boardWidth; c++) {
            g.drawLine(c * blockSize, 0, c * blockSize, boardHeight * blockSize);
        }

        int xPanel = boardWidth * blockSize + 15;
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Tu jugador: J" + playerId, xPanel, 30);
        g.drawString("Turno: J" + currentPlayerId, xPanel, 55);

        if (controlesImage != null) {
            int imgWidth = 180;
            int imgHeight = 135;
            g.drawImage(controlesImage, xPanel, 75, imgWidth, imgHeight, null);
        }

        int y = 240;

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Puntajes", xPanel, y);

       
        y += 25;
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        for (Map.Entry<Integer, Integer> e : scores.entrySet()) {
            g.setColor(colorFor(e.getKey()));
            g.fillRect(xPanel, y - 12, 12, 12);
            g.setColor(Color.WHITE);
            g.drawString("J" + e.getKey() + ": " + e.getValue(), xPanel + 18, y);
            y += 22;
        }

        y += 20;
        g.setColor(Color.WHITE);
        drawWrapped(g, info, xPanel, y, 180, 18);

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.WHITE);
            String text = winnerId == 0 ? "FIN DEL JUEGO" : "GANÓ J" + winnerId;
            g.drawString(text, 35, Math.max(30, boardHeight * blockSize / 2));
        }
    }

    private void drawWrapped(Graphics g, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null) return;
        String[] words = text.split("\\s+");
        String line = "";
        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(test) > maxWidth) {
                g.drawString(line, x, y);
                y += lineHeight;
                line = word;
            } else {
                line = test;
            }
        }
        if (!line.isEmpty()) {
            g.drawString(line, x, y);
        }
    }

    private Color colorFor(int player) {
        if (player <= 0) return Color.BLACK;
        return colors[(player - 1) % colors.length];
    }

    private void sendKey(String key) {
        if (tcpClient != null) {
            tcpClient.sendMessage("KEY|" + key);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                sendKey("LEFT");
                break;
            case KeyEvent.VK_RIGHT:
                sendKey("RIGHT");
                break;
            case KeyEvent.VK_DOWN:
                sendKey("DOWN");
                break;
            case KeyEvent.VK_A:
                sendKey("ROT_CCW");
                break;
            case KeyEvent.VK_S:
                sendKey("ROT_CW");
                break;
            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public int getBoardHeight() {
        return boardHeight;
    }

    public int getBoardWidth() {
        return boardWidth;
    }
}
