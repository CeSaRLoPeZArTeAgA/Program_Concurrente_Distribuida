package tetris;

import java.util.Arrays;

/**
 * Representa una forma/tetromino de Tetris Mix.
 * Esta versión no dibuja; solo guarda y rota matrices de 0/1.
 */
public class Shape {
    private int[][] coords;

    public Shape(int[][] coords) {
        this.coords = copy(coords);
    }

    public int[][] getCoords() {
        return copy(coords);
    }

    public void rotateClockwise() {
        coords = rotateClockwise(coords);
    }

    public void rotateCounterClockwise() {
        coords = rotateCounterClockwise(coords);
    }

    public static int[][] rotateClockwise(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] result = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result[c][rows - 1 - r] = matrix[r][c];
            }
        }
        return result;
    }

    public static int[][] rotateCounterClockwise(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] result = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result[cols - 1 - c][r] = matrix[r][c];
            }
        }
        return result;
    }

    public static int[][] copy(int[][] matrix) {
        int[][] result = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            result[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }
        return result;
    }
}
