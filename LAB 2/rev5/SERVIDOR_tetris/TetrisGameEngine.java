package tetris;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;


// motor autoritativo del juego tetris
public class TetrisGameEngine {
    //numero de filas del tablero
    private final int rows;

    //numero de columnas del tablero
    private final int cols;

    // tablero fijo del juego
    // 0 representa celda vacia
    // Un número positivo representa el id del jugador que dejo esa celda ocupada
    private final int[][] board;

    //jugadores registrados: id del jugador -> nombre
    private final LinkedHashMap<Integer, String> players = new LinkedHashMap<>();

    //puntajes: id del jugador -> líneas completadas
    private final LinkedHashMap<Integer, Integer> scores = new LinkedHashMap<>();

    //generador aleatorio usado para escoger la siguiente pieza
    private final Random random = new Random();

    //catalogo de piezas disponibles,cada pieza es una matriz de 0/1
    private final int[][][] shapes = new int[][][] {
        {{1, 1}},
        {{1, 1, 1}, {0, 1, 0}},
        {{1, 1}, {1, 0}},
        {{1, 1}, {0, 1}},
        {{1}},
        {{1, 1, 1}},
        {{1, 1}, {1, 1}}
    };

    //indica si la partida ya empezo
    private boolean started = false;

    //indica si la partida termino
    private boolean gameOver = false;

    //Id del ganador,si vale 0, no hay ganador o hubo empate
    private int winnerId = 0;

    //mensaje informativo que se envia junto con el estado del juego
    private String message = "Esperando jugadores...";

    //indice del jugador activo dentro del orden de insercion de players
    private int currentPlayerIndex = 0;

    //Id del jugador que controla la pieza activa
    private int activePlayerId = 0;

    //matriz de la pieza que esta cayendo actualmente
    private int[][] activeShape;

    //posicion horizontal de la esquina superior izquierda de la pieza activa
    private int activeX;

    //posicion vertical de la esquina superior izquierda de la pieza activa
    private int activeY;

    //crea el motor del juego con un tablero vacio    
    public TetrisGameEngine(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.board = new int[rows][cols];
    }


    //registra un jugador en la partida, si el jugador ya existe, no lo vuelve a insertar 
    public synchronized void addPlayer(int playerId, String name) {
        if (!players.containsKey(playerId)) {
            players.put(playerId, name == null || name.trim().isEmpty() ? "Jugador " + playerId : name.trim());
            scores.put(playerId, 0);
        }
    }

    //verifica si un id corresponde a un jugador registrado    
    public synchronized boolean hasPlayer(int playerId) {
        return players.containsKey(playerId);
    }

    //devuelve la cantidad de jugadores registrados
    public synchronized int getPlayerCount() {
        return players.size();
    }

    //inicia o reinicia la partida
    //limpia el tablero, reinicia puntajes, estados y genera la primera pieza
    public synchronized void startGame() {
        if (players.isEmpty()) {
            message = "No hay jugadores conectados.";
            return;
        }

        //se limpia el tablero y se reinician puntajes
        clearBoard();
        for (Integer id : scores.keySet()) {
            scores.put(id, 0);
        }

        //se reinician las banderas principales del juego
        started = true;
        gameOver = false;
        winnerId = 0;
        currentPlayerIndex = 0;
        message = "Inicio del juego. Orden: " + orderText();

        //se genera la primera pieza para el primer jugador
        spawnForCurrentPlayer();
    }

    //avanza automaticamente un paso temporal del juego
    public synchronized void tick() {
        if (!started || gameOver || activeShape == null) {
            return;
        }

        if (canPlace(activeShape, activeX, activeY + 1)) {
            //la pieza cae una fila.
            activeY++;
        } else {
            // aa pieza llego al fondo o choco con bloques existentes
            lockActiveShape();

            //se limpian lineas completas y se suman puntos al jugador activo
            int cleared = clearLines();
            if (cleared > 0) {
                scores.put(activePlayerId, scores.getOrDefault(activePlayerId, 0) + cleared);
                message = "Jugador " + activePlayerId + " completó " + cleared + " línea(s).";
            }

            //cambia el turno y aparece una nueva pieza
            advanceTurn();
            spawnForCurrentPlayer();
        }
    }

    
    //procesa una tecla enviada por un cliente
    //solo el jugador activo puede mover o rotar la pieza
    public synchronized void handleKey(int playerId, String key) {
        if (!started || gameOver || activeShape == null) {
            return;
        }

        //si no es el turno del jugador, se ignora la accion
        if (playerId != activePlayerId) {
            return;
        }

        switch (key) {
            case "LEFT":
                //mueve la pieza una columna a la izquierda si no colisiona
                if (canPlace(activeShape, activeX - 1, activeY)) activeX--;
                break;
            case "RIGHT":
                //mueve la pieza una columna a la derecha si no colisiona
                if (canPlace(activeShape, activeX + 1, activeY)) activeX++;
                break;
            case "DOWN":
                //baja la pieza una fila si la posición es valida
                if (canPlace(activeShape, activeX, activeY + 1)) activeY++;
                break;
            case "ROT_CW": {
                //rota en sentido horario y aplica la rotacion solo si no colisiona
                int[][] rotated = Shape.rotateClockwise(activeShape);
                if (canPlace(rotated, activeX, activeY)) activeShape = rotated;
                break;
            }
            case "ROT_CCW": {
                //rota en sentido antihorario y aplica la rotacion solo si no colisiona
                int[][] rotated = Shape.rotateCounterClockwise(activeShape);
                if (canPlace(rotated, activeX, activeY)) activeShape = rotated;
                break;
            }
            default:
                //cualquier tecla desconocida se ignora
                break;
        }
    }

    
    //convierte el estado actual del juego en una cadena de texto para enviarla por TCP
    public synchronized String serializeState() {
        int[][] view = buildViewBoard();
        StringBuilder grid = new StringBuilder(rows * cols);

        //se codifica el tablero como una cadena lineal de caracteres
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid.append(encodeCell(view[r][c]));
            }
        }

        return "STATE|" + rows + "|" + cols + "|" + activePlayerId + "|" + gameOver + "|" + winnerId
                + "|" + scoresText() + "|" + grid + "|" + safe(message);
    }

    
    //construye el mensaje START para un cliente especifico. 
    public synchronized String startMessageForClient(int playerId) {
        return "START|" + rows + "|" + cols + "|" + playerId + "|" + safe(orderText());
    }

    //genera un texto con el orden de jugadores    
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

    //genera una pieza aleatoria para el jugador activo
    //si la pieza no puede aparecer en la parte superior, termina el juego    
    private void spawnForCurrentPlayer() {
        Integer[] ids = players.keySet().toArray(new Integer[0]);
        if (ids.length == 0) {
            gameOver = true;
            message = "No hay jugadores.";
            return;
        }

        //selecciona el jugador activo según el indice de turno
        activePlayerId = ids[currentPlayerIndex % ids.length];

        //copia una pieza aleatoria para evitar modificar el catalogo original
        activeShape = Shape.copy(shapes[random.nextInt(shapes.length)]);

        //posiciona la pieza horizontalmente al centro y verticalmente arriba
        activeX = Math.max(0, (cols - activeShape[0].length) / 2);
        activeY = 0;

        //si la nueva pieza no entra, el tablero esta lleno y el juego termina
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

    
    //avanza el indice al siguiente jugador
    //se usa modulo para regresar al primer jugador despues del ultimo
    private void advanceTurn() {
        if (!players.isEmpty()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
    }

    //verifica si una pieza puede colocarse en una posicion dada
    private boolean canPlace(int[][] shape, int x, int y) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;

                int br = y + r;
                int bc = x + c;

                // verifica limites del tablero
                if (br < 0 || br >= rows || bc < 0 || bc >= cols) return false;

                //verifica colision con bloques ya fijados
                if (board[br][bc] != 0) return false;
            }
        }
        return true;
    }

    //fija la pieza activa al tablero
    //las celdas ocupadas por la pieza se marcan con el id del jugador activo
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

    //elimina las filas completas del tablero
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
                //la fila esta completa y no se copiaa
                cleared++;
            } else {
                //la fila no completa baja hasta la posicion writea
                if (write != read) {
                    board[write] = Arrays.copyOf(board[read], cols);
                }
                write--;
            }
        }

        //las filas superiores que quedan libres se rellenan con 0
        while (write >= 0) {
            Arrays.fill(board[write], 0);
            write--;
        }
        return cleared;
    }

    //construye una vista del tablero incluyendo la pieza activa 
    private int[][] buildViewBoard() {
        int[][] view = new int[rows][cols];

        //copia el tablero fijo
        for (int r = 0; r < rows; r++) {
            view[r] = Arrays.copyOf(board[r], cols);
        }

        //superpone la pieza activa si el juego sigue en curso
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

    //calcula el ganador segun el mayor puntaje
    //si hay empate en el mayor puntaje positivo, retorna 0
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

    //limpia todo el tablero, colocando 0 en cada celda
    private void clearBoard() {
        for (int r = 0; r < rows; r++) {
            Arrays.fill(board[r], 0);
        }
    }

    //convierte el mapa de puntajes en una cadena compacta
    private String scoresText() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : scores.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        return sb.toString();
    }

    //codifica una celda del tablero como un caracter
    private char encodeCell(int value) {
        if (value <= 0) return '.';
        if (value < 10) return (char) ('0' + value);
        return (char) ('A' + (value - 10));
    }

    //limpia textos antes de enviarlos por el protocolo TCP
    private String safe(String text) {
        if (text == null) return "";
        return text.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
