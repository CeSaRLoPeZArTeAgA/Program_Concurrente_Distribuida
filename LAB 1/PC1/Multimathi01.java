
public class Multimathi01 {
    int L = 200;
    public int[][] C;

    public static void main(String[] args) {
        new Multimathi01().inicio();
    }

    void inicio() {
        C = new int[L][L];

        // generacion de la matriz C con enteros aleatorios entre 0 y 10
        for (int i = 0; i < L; i++) {
            for (int j = 0; j < L; j++) {
                C[i][j] = (int) (Math.random() * 11);
            }
        }

        // arreglos de una posicion para poder modificarlos
        final int[] sumaColumnasTotal = {0};
        final int[] sumaFilasTotal = {0};
        final int[] sumaDiagonalesTotal = {0};

        // hilo para sumar columnas
        Thread hiloSumaColumnas = new Thread(() -> {
            for (int j = 0; j < L; j++) {
                int sumaColumna = 0;
                for (int i = 0; i < L; i++) {
                    sumaColumna += C[i][j];
                }
                sumaColumnasTotal[0] += sumaColumna;
            }
        });

        // hilo para sumar filas
        Thread hiloSumaFilas = new Thread(() -> {
            for (int i = 0; i < L; i++) {
                int sumaFila = 0;
                for (int j = 0; j < L; j++) {
                    sumaFila += C[i][j];
                }
                sumaFilasTotal[0] += sumaFila;
            }
        });

        // hilo para sumar todas las diagonales paralelas a la diagonal principal
        Thread hiloSumaDiagonales = new Thread(() -> {
            int totalDiag = 0;

            // diagonal principal y diagonales superiores
            for (int k = 0; k < L; k++) {
                int sumaDiagonal = 0;
                for (int i = 0, j = k; j < L; i++, j++) {
                    sumaDiagonal += C[i][j];
                }
                totalDiag += sumaDiagonal;
            }

            // diagonales inferiores
            for (int k = 1; k < L; k++) {
                int sumaDiagonal = 0;
                for (int i = k, j = 0; i < L; i++, j++) {
                    sumaDiagonal += C[i][j];
                }
                totalDiag += sumaDiagonal;
            }

            sumaDiagonalesTotal[0] = totalDiag;
        });

        // iniciar hilos
        hiloSumaColumnas.start();
        hiloSumaFilas.start();
        hiloSumaDiagonales.start();

        // esperar que terminen
        try {
            hiloSumaColumnas.join();
            hiloSumaFilas.join();
            hiloSumaDiagonales.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // imprimir resultados
        System.out.println();
        System.out.println("Suma total de las columnas: " + sumaColumnasTotal[0]);
        System.out.println("Suma total de las filas: " + sumaFilasTotal[0]);
        System.out.println("Suma total de las diagonales paralelas: " + sumaDiagonalesTotal[0]);
    }
}
