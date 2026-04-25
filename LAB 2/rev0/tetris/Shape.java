package tetris;

import java.awt.Color;
import java.awt.Graphics;

public class Shape {

    private Color color;

    private int x, y;

    private long time, lastTime;

    private int normal = 600, fast = 50;

    private int delay;

    private int[][] coords;

    private int[][] reference;

    private int deltaX;

    private Board board;

    private boolean collision = false, moveX = false;

    private int timePassedFromCollision = -1;

    //repressenta una pieza individual del juego, con su forma, color y posicion en el tablero
    public Shape(int[][] coords, Board board, Color color) {
        this.coords = coords;
        this.board = board;
        this.color = color;
        deltaX = 0;
        x = Math.max(0, (board.getBoardWidth() - coords[0].length) / 2);
        y = 0;
        delay = normal;
        time = 0;
        lastTime = System.currentTimeMillis();
        reference = new int[coords.length][coords[0].length];

        System.arraycopy(coords, 0, reference, 0, coords.length);

    }

    long deltaTime;

    public void update() {
        moveX = true;
        deltaTime = System.currentTimeMillis() - lastTime;
        time += deltaTime;
        lastTime = System.currentTimeMillis();

        if (collision && timePassedFromCollision > 500) {
            for (int row = 0; row < coords.length; row++) {
                for (int col = 0; col < coords[0].length; col++) {
                    if (coords[row][col] != 0) {
                        board.getBoard()[y + row][x + col] = color;
                    }
                }
            }
            checkLine();
            board.addScore();
            board.setCurrentShape();
            timePassedFromCollision = -1;
        }

        // check moving horizontal
        if (!(x + deltaX + coords[0].length > board.getBoardWidth()) && !(x + deltaX < 0)) {
            for (int row = 0; row < coords.length; row++) {
                for (int col = 0; col < coords[row].length; col++) {
                    if (coords[row][col] != 0) {
                        if (board.getBoard()[y + row][x + deltaX + col] != null) {
                            moveX = false;
                        }

                    }
                }
            }

            if (moveX) {
                x += deltaX;
            }

        }

        // Check position + height(number of row) of shape
        if (timePassedFromCollision == -1) {
            if (!(y + 1 + coords.length > board.getBoardHeight())) {
            
                for (int row = 0; row < coords.length; row++) {
                    for (int col = 0; col < coords[row].length; col++) {
                        if (coords[row][col] != 0) {

                            if (board.getBoard()[y + 1 + row][x + col] != null) {
                                collision();
                            }
                        }
                    }
                }
                if (time > delay) {
                    y++;
                    time = 0;
                }
            } else {
                collision();
            }
        } else {
            timePassedFromCollision += deltaTime;
        }

        deltaX = 0;
    }

    private void collision() {
        collision = true;
        timePassedFromCollision = 0;
    }

    public void render(Graphics g) {

        g.setColor(color);
        for (int row = 0; row < coords.length; row++) {
            for (int col = 0; col < coords[0].length; col++) {
                if (coords[row][col] != 0) {
                   g.fillRect(
                                (col + x) * Board.blockSize,
                                (row + y) * Board.blockSize,
                                Board.blockSize,
                                Board.blockSize
                            );
                }
            }
        }
    }

    private void checkLine() {
        int size = board.getBoard().length - 1;

        for (int i = board.getBoard().length - 1; i > 0; i--) {
            int count = 0;
            for (int j = 0; j < board.getBoard()[0].length; j++) {
                if (board.getBoard()[i][j] != null) {
                    count++;
                }

                board.getBoard()[size][j] = board.getBoard()[i][j];
            }
            if (count < board.getBoard()[0].length) {
                size--;
            }
        }
    }

    //funcion que rotacion anti-horaria de las piezas
    public void rotateShapeCounterClockwise() {

        int[][] rotatedShape = null;

        rotatedShape = transposeMatrix(coords);

        rotatedShape = reverseRows(rotatedShape);

        if ((x + rotatedShape[0].length > board.getBoardWidth()) || (y + rotatedShape.length > board.getBoardHeight())) {
            return;
        }

        for (int row = 0; row < rotatedShape.length; row++) {
            for (int col = 0; col < rotatedShape[row].length; col++) {
                if (rotatedShape[row][col] != 0) {
                    if (board.getBoard()[y + row][x + col] != null) {
                        return;
                    }
                }
            }
        }
        coords = rotatedShape;
    }

    //funcion que rotacion horaria de las piezas
    public void rotateShapeClockwise() {

        int[][] rotatedShape = null;

        rotatedShape = transposeMatrix(coords);

        rotatedShape = reverseColumns(rotatedShape);

        if ((x + rotatedShape[0].length > board.getBoardWidth()) || 
            (y + rotatedShape.length > board.getBoardHeight())) {
            return;
        }

        for (int row = 0; row < rotatedShape.length; row++) {
            for (int col = 0; col < rotatedShape[row].length; col++) {
                if (rotatedShape[row][col] != 0) {
                    if (board.getBoard()[y + row][x + col] != null) {
                        return;
                    }
                }
            }
        }

        coords = rotatedShape;
    }


    private int[][] transposeMatrix(int[][] matrix) {
        int[][] temp = new int[matrix[0].length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                temp[j][i] = matrix[i][j];
            }
        }
        return temp;
    }

    //funcion que invierte las columnas de la matriz, para rotacion horaria
    private int[][] reverseColumns(int[][] matrix) {

        for (int row = 0; row < matrix.length; row++) {
            int left = 0;
            int right = matrix[row].length - 1;

            while (left < right) {
                int temp = matrix[row][left];
                matrix[row][left] = matrix[row][right];
                matrix[row][right] = temp;

                left++;
                right--;
            }
        }

        return matrix;
    }

    private int[][] reverseRows(int[][] matrix) {

        int middle = matrix.length / 2;

        for (int i = 0; i < middle; i++) {
            int[] temp = matrix[i];

            matrix[i] = matrix[matrix.length - i - 1];
            matrix[matrix.length - i - 1] = temp;
        }

        return matrix;

    }

    public Color getColor() {
        return color;
    }

    public void setDeltaX(int deltaX) {
        this.deltaX = deltaX;
    }

    public void speedUp() {
        delay = fast;
    }

    public void speedDown() {
        delay = normal;
    }

    public int[][] getCoords() {
        return coords;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
