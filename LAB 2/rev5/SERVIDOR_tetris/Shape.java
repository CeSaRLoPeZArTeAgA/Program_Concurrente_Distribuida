package tetris;

import java.util.Arrays;


//representa una pieza o forma de Tetris mediante una matriz de enteros
public class Shape {
    //matriz que representa la geometria actual de la pieza
    private int[][] coords;

    //crea una nueva pieza copiando la matriz recibida(se copia para evitar que modificaciones externas)
    public Shape(int[][] coords) {
        this.coords = copy(coords);
    }

    //devuelve una copia de la matriz de la pieza
    public int[][] getCoords() {
        return copy(coords);
    }

    //rota la pieza actual 90 grados en sentido horario
    public void rotateClockwise() {
        coords = rotateClockwise(coords);
    }

    //rota la pieza actual 90 grados en sentido antihorario
    public void rotateCounterClockwise() {
        coords = rotateCounterClockwise(coords);
    }

    // rota una matriz 90 grados en sentido horario
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

    //rota una matriz 90 grados en sentido antihorario
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

    //realiza una copia de una matriz bidimensional
    public static int[][] copy(int[][] matrix) {
        int[][] result = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            result[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }
        return result;
    }
}
