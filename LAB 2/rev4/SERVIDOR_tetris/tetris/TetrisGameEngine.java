package tetris;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;


public class TetrisGameEngine {
    private final int rows;
    private final int cols;
    private final int[][] board;
    private final LinkedHashMap<Integer, String> players = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, Integer> scores = new LinkedHashMap<>();
    private final Random random = new Random();

    private final int[][][] shapes = new int[][][] {
        {{1, 1}},
        {{1, 1, 1}, {0, 1, 0}},
        {{1, 1}, {1, 0}},
        {{1, 1}, {0, 1}},
        {{1}},
        {{1, 1, 1}},
        {{1, 1}, {1, 1}}
    };

    private boolean started = false;
    private boolean gameOver = false;
    private int winnerId = 0;
    private String message = "Esperando jugadores...";

    private int currentPlayerIndex = 0;
    private int activePlayerId = 0;
    private int[][] activeShape;
    private int activeX;
    private int activeY;

    public TetrisGameEngine(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.board = new int[rows][cols];
    }

    public synchronized void addPlayer(int playerId, String name) {
        if (!players.containsKey(playerId)) {
            players.put(playerId, name == null || name.trim().isEmpty() ? "Jugador " + playerId : name.trim());
            scores.put(playerId, 0);
        }
    }

    public synchronized boolean hasPlayer(int playerId) {
        return players.containsKey(playerId);
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public synchronized void startGame() {
        if (players.isEmpty()) {
            message = "No hay jugadores conectados.";
            return;
        }
        clearBoard();
        for (Integer id : scores.keySet()) {
            scores.put(id, 0);
        }
        started = true;
        gameOver = false;
        winnerId = 0;
        currentPlayerIndex = 0;
        message = "Inicio del juego. Orden: " + orderText();
        spawnForCurrentPlayer();
    }

    public synchronized void tick() {
        if (!started || gameOver || activeShape == null) {
            return;
        }
        if (canPlace(activeShape, activeX, activeY + 1)) {
            activeY++;
        } else {
            lockActiveShape();
            int cleared = clearLines();
            if (cleared > 0) {
                scores.put(activePlayerId, scores.getOrDefault(activePlayerId, 0) + cleared);
                message = "Jugador " + activePlayerId + " completó " + cleared + " línea(s).";
            }
            advanceTurn();
            spawnForCurrentPlayer();
        }
    }

    public synchronized void handleKey(int playerId, String key) {
        if (!started || gameOver || activeShape == null) {
            return;
        }
        if (playerId != activePlayerId) {
            return;
        }
        switch (key) {
            case "LEFT":
                if (canPlace(activeShape, activeX - 1, activeY)) activeX--;
                break;
            case "RIGHT":
                if (canPlace(activeShape, activeX + 1, activeY)) activeX++;
                break;
            case "DOWN":
                if (canPlace(activeShape, activeX, activeY + 1)) activeY++;
                break;
            case "ROT_CW": {
                int[][] rotated = Shape.rotateClockwise(activeShape);
                if (canPlace(rotated, activeX, activeY)) activeShape = rotated;
                break;
            }
            case "ROT_CCW": {
                int[][] rotated = Shape.rotateCounterClockwise(activeShape);
                if (canPlace(rotated, activeX, activeY)) activeShape = rotated;
                break;
            }
            default:
                break;
        }
    }

    public synchronized String serializeState() {
        int[][] view = buildViewBoard();
        StringBuilder grid = new StringBuilder(rows * cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid.append(encodeCell(view[r][c]));
            }
        }

        return "STATE|" + rows + "|" + cols + "|" + activePlayerId + "|" + gameOver + "|" + winnerId
                + "|" + scoresText() + "|" + grid + "|" + safe(message);
    }

    public synchronized String startMessageForClient(int playerId) {
        return "START|" + rows + "|" + cols + "|" + playerId + "|" + safe(orderText());
    }

    public synchronized String orderText() {
        StringBuilder sb = new StringBuilder();
        int k = 1;
        for (Map.Entry<Integer, String> e : players.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(k).append("° J").append(e.getKey()).append("=").append(e.getValue());
            k++;
        }
        return sb.toString();
    }

    private void spawnForCurrentPlayer() {
        Integer[] ids = players.keySet().toArray(new Integer[0]);
        if (ids.length == 0) {
            gameOver = true;
            message = "No hay jugadores.";
            return;
        }
        activePlayerId = ids[currentPlayerIndex % ids.length];
        activeShape = Shape.copy(shapes[random.nextInt(shapes.length)]);
        activeX = Math.max(0, (cols - activeShape[0].length) / 2);
        activeY = 0;

        if (!canPlace(activeShape, activeX, activeY)) {
            gameOver = true;
            winnerId = computeWinner();
            if (winnerId == 0) {
                message = "Fin del juego: el tablero se llenó sin ganador por puntos.";
            } else {
                message = "Fin del juego. Ganador: Jugador " + winnerId + " con " + scores.get(winnerId) + " punto(s).";
            }
        } else {
            message = "Turno del Jugador " + activePlayerId + ".";
        }
    }

    private void advanceTurn() {
        if (!players.isEmpty()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
    }

    private boolean canPlace(int[][] shape, int x, int y) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;
                int br = y + r;
                int bc = x + c;
                if (br < 0 || br >= rows || bc < 0 || bc >= cols) return false;
                if (board[br][bc] != 0) return false;
            }
        }
        return true;
    }

    private void lockActiveShape() {
        for (int r = 0; r < activeShape.length; r++) {
            for (int c = 0; c < activeShape[r].length; c++) {
                if (activeShape[r][c] != 0) {
                    int br = activeY + r;
                    int bc = activeX + c;
                    if (br >= 0 && br < rows && bc >= 0 && bc < cols) {
                        board[br][bc] = activePlayerId;
                    }
                }
            }
        }
    }

    private int clearLines() {
        int cleared = 0;
        int write = rows - 1;
        for (int read = rows - 1; read >= 0; read--) {
            boolean full = true;
            for (int c = 0; c < cols; c++) {
                if (board[read][c] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cleared++;
            } else {
                if (write != read) {
                    board[write] = Arrays.copyOf(board[read], cols);
                }
                write--;
            }
        }
        while (write >= 0) {
            Arrays.fill(board[write], 0);
            write--;
        }
        return cleared;
    }

    private int[][] buildViewBoard() {
        int[][] view = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            view[r] = Arrays.copyOf(board[r], cols);
        }
        if (activeShape != null && !gameOver) {
            for (int r = 0; r < activeShape.length; r++) {
                for (int c = 0; c < activeShape[r].length; c++) {
                    if (activeShape[r][c] != 0) {
                        int br = activeY + r;
                        int bc = activeX + c;
                        if (br >= 0 && br < rows && bc >= 0 && bc < cols) {
                            view[br][bc] = activePlayerId;
                        }
                    }
                }
            }
        }
        return view;
    }

    private int computeWinner() {
        int bestId = 0;
        int bestScore = 0;
        boolean tie = false;
        for (Map.Entry<Integer, Integer> e : scores.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                bestId = e.getKey();
                tie = false;
            } else if (e.getValue() == bestScore && bestScore > 0) {
                tie = true;
            }
        }
        return tie ? 0 : bestId;
    }

    private void clearBoard() {
        for (int r = 0; r < rows; r++) {
            Arrays.fill(board[r], 0);
        }
    }

    private String scoresText() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : scores.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        return sb.toString();
    }

    private char encodeCell(int value) {
        if (value <= 0) return '.';
        if (value < 10) return (char) ('0' + value);
        return (char) ('A' + (value - 10));
    }

    private String safe(String text) {
        if (text == null) return "";
        return text.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
