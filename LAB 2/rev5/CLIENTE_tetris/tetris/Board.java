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

//Panel grafico del tablero del cliente
//esta clase dibuja el tablero, la cuadricula, los puntajes, el jugador activo,
//la imagen de controles y los mensajes del servidor. Tambien captura teclas y
//las envía al servidor mediante TCPClient50
public class Board extends JPanel implements KeyListener {

    //identificador de serializacion requerido por JPanel
    private static final long serialVersionUID = 1L;

    //tamaño en pixeles de cada celda del tablero. Puede cambiar al redimensionar
    public static int blockSize = 30;

    //imagen opcional con los controles del juego
    private BufferedImage controlesImage;

    //numero de filas del tablero
    private final int boardHeight;

    //numero de columnas del tablero
    private final int boardWidth;

    //matriz local que representa el tablero recibido desde el servidor
    private int[][] grid;

    //mapa jugador -> puntaje. Se mantiene ordenado por insercion
    private final Map<Integer, Integer> scores = new LinkedHashMap<>();

    //ID de este cliente/jugador
    private int playerId = 0;

    //ID del jugador que tiene el turno actual
    private int currentPlayerId = 0;

    //ID del ganador. Vale 0 si no hay ganador o hubo empate
    private int winnerId = 0;

    //indica si la partida termino
    private boolean gameOver = false;

    //texto informativo mostrado en el panel lateral
    private String info = "Conectando al servidor...";

    // Cliente TCP usado para enviar las teclas al servidor
    private TCPClient50 tcpClient;

    //colores usados para pintar piezas de diferentes jugadores
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

    //construye el panel del tablero    
    public Board(int boardHeight, int boardWidth) {
        this.boardHeight = boardHeight;
        this.boardWidth = boardWidth;
        this.grid = new int[boardHeight][boardWidth];

        try {
            //intenta cargar una imagen de controles desde data/controles.png
            controlesImage = ImageLoader.loadImage("/controles.png");
        } catch (Exception e) {
            //si no existe la imagen, el juego sigue funcionando sin mostrarla
            controlesImage = null;
            System.out.println("No se pudo cargar la imagen de controles: " + e.getMessage());
        }

        //permite que el panel reciba foco y eventos de teclado
        setFocusable(true);
        setRequestFocusEnabled(true);

        //instala atajos de teclado mas confiables para componentes Swing
        installKeyBindings();
    }

    //registra las teclas del juego usando InputMap y ActionMap
    private void installKeyBindings() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        bindKey(inputMap, actionMap, KeyEvent.VK_LEFT, "LEFT");
        bindKey(inputMap, actionMap, KeyEvent.VK_RIGHT, "RIGHT");
        bindKey(inputMap, actionMap, KeyEvent.VK_DOWN, "DOWN");
        bindKey(inputMap, actionMap, KeyEvent.VK_A, "ROT_CCW");
        bindKey(inputMap, actionMap, KeyEvent.VK_S, "ROT_CW");
    }

    //asocia una tecla con un comando que sera enviado al servidor
    private void bindKey(InputMap inputMap, ActionMap actionMap, int keyCode, String command) {
        String actionName = "send_" + command;
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0), actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendKey(command);
            }
        });
    }

    //recibe el cliente TCP creado por WindowGame     
    public void setNetworkClient(TCPClient50 tcpClient) {
        this.tcpClient = tcpClient;
    }

    //define el identificador de jugador asignado por el servidor
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
        repaint();
    }

    //actualiza el texto informativo del panel lateral
    public void setInfo(String info) {
        this.info = info == null ? "" : info;
        repaint();
    }

    
    //aplica un estado completo recibido desde el servidor 
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
        //redibuja el panel con el nuevo estado
        repaint();
    }

    //decodifica la cadena compacta del tablero enviada por el servidor 
    private void parseGrid(String encoded, int rows, int cols) {
        //si las dimensiones no coinciden con este panel, se ignora el estado
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

    //decodifica el texto de puntajes enviado por el servidor   
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

    //convierte un carácter del tablero en un número de jugador    
    private int decodeCell(char ch) {
        if (ch == '.') return 0;
        if (ch >= '0' && ch <= '9') return ch - '0';
        if (ch >= 'A' && ch <= 'Z') return 10 + (ch - 'A');
        return 0;
    }

    //convierte texto a entero usando un valor de respaldo si falla
    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    
    //metodo principal de dibujo de Swing
    //Swing llama automaticamente a este metodo cada vez que el panel necesita repintarse
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        //fondo negro de toda el area del panel
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        //dibuja las celdas ocupadas del tablero
        for (int r = 0; r < boardHeight; r++) {
            for (int c = 0; c < boardWidth; c++) {
                int value = grid[r][c];
                if (value != 0) {
                    g.setColor(colorFor(value));
                    g.fillRect(c * blockSize, r * blockSize, blockSize, blockSize);
                }
            }
        }

        //dibuja la cuadricula del tablero
        g.setColor(Color.WHITE);
        for (int r = 0; r <= boardHeight; r++) {
            g.drawLine(0, r * blockSize, boardWidth * blockSize, r * blockSize);
        }
        for (int c = 0; c <= boardWidth; c++) {
            g.drawLine(c * blockSize, 0, c * blockSize, boardHeight * blockSize);
        }

        //coordenada inicial del panel lateral
        int xPanel = boardWidth * blockSize + 15;

        //muestra información del jugador local y del turno actual
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Tu jugador: J" + playerId, xPanel, 30);
        g.drawString("Turno: J" + currentPlayerId, xPanel, 55);

        //dibuja la imagen de controles si fue cargada correctamente
        if (controlesImage != null) {
            int imgWidth = 180;
            int imgHeight = 135;
            g.drawImage(controlesImage, xPanel, 75, imgWidth, imgHeight, null);
        }

        int y = 240;

        //seccion de puntajes
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

        //mensaje informativo del servidor
        y += 20;
        g.setColor(Color.WHITE);
        drawWrapped(g, info, xPanel, y, 180, 18);

        //mensaje grande de fin de juego
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.WHITE);
            String text = winnerId == 0 ? "FIN DEL JUEGO" : "GANÓ J" + winnerId;
            g.drawString(text, 35, Math.max(30, boardHeight * blockSize / 2));
        }
    }

    
    //dibuja texto largo dividiendolo en varias lineas
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

    
    //devuelve el color asociado a un jugador
    private Color colorFor(int player) {
        if (player <= 0) return Color.BLACK;
        return colors[(player - 1) % colors.length];
    }

    
    //envia un comando de tecla al servidor 
    private void sendKey(String key) {
        if (tcpClient != null) {
            tcpClient.sendMessage("KEY|" + key);
        }
    }

    
    //metodo alternativo de captura de teclas por KeyListener
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

    //debe existir por implementar KeyListener
    @Override
    public void keyReleased(KeyEvent e) {
    }

    //debe existir por implementar KeyListener
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
