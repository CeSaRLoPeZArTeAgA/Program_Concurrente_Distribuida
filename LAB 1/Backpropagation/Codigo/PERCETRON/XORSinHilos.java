import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public class XORSinHilos {

    // función de activación
    public static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    // derivada de la sigmoide, recibiendo la salida activada
    public static double dSigmoid(double x) {
        return x * (1.0 - x);
    }

    // producto punto
    public static double dot(double[] a, double[] b) {
        double suma = 0.0;
        for (int i = 0; i < a.length; i++) {
            suma += a[i] * b[i];
        }
        return suma;
    }

    // aleatorio uniforme en [-1,1]
    public static double randomUniform(Random rnd) {
        return -1.0 + 2.0 * rnd.nextDouble();
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        long inicioTotal = System.nanoTime();

        Random rnd = new Random(42);

        // Entradas y salidas XOR
        double[][] X = {
            {0.0, 0.0},
            {0.0, 1.0},
            {1.0, 0.0},
            {1.0, 1.0}
        };

        double[] y = {0.0, 1.0, 1.0, 0.0};

        double lr = 0.5;
        int epochs = 8000;

        // W1: capa oculta (2 neuronas, 2 entradas + bias)
        double[][] W1 = new double[2][3];

        // W2: capa salida (1 neurona, 2 entradas ocultas + bias)
        double[][] W2 = new double[1][3];

        // inicialización
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                W1[i][j] = randomUniform(rnd);
            }
        }

        for (int j = 0; j < 3; j++) {
            W2[0][j] = randomUniform(rnd);
        }

        long inicioEntrenamiento = System.nanoTime();

        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalError = 0.0;

            for (int i = 0; i < X.length; i++) {
                double[] entrada = X[i];
                double objetivo = y[i];

                // ===== propagación hacia adelante =====

                double[] salidaOculta = new double[2];
                for (int j = 0; j < 2; j++) {
                    double suma = 0.0;
                    for (int k = 0; k < 2; k++) {
                        suma += entrada[k] * W1[j][k];
                    }
                    suma += W1[j][2]; // bias
                    salidaOculta[j] = sigmoid(suma);
                }

                double sumaSalida = 0.0;
                for (int j = 0; j < 2; j++) {
                    sumaSalida += salidaOculta[j] * W2[0][j];
                }
                sumaSalida += W2[0][2]; // bias

                double salidaFinal = sigmoid(sumaSalida);

                // ===== error =====
                double error = objetivo - salidaFinal;
                totalError += Math.abs(error);

                // ===== retropropagación =====
                double dOut = error * dSigmoid(salidaFinal);

                double[] dH = new double[2];
                for (int j = 0; j < 2; j++) {
                    dH[j] = dSigmoid(salidaOculta[j]) * (dOut * W2[0][j]);
                }

                // ===== actualización de pesos salida =====
                for (int j = 0; j < 2; j++) {
                    W2[0][j] += lr * dOut * salidaOculta[j];
                }
                W2[0][2] += lr * dOut; // bias salida

                // ===== actualización de pesos ocultos =====
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 2; k++) {
                        W1[j][k] += lr * dH[j] * entrada[k];
                    }
                    W1[j][2] += lr * dH[j]; // bias oculto
                }
            }

            if (epoch % 500 == 0) {
                System.out.printf("Iteración %d: Error medio = %.5f%n", epoch, totalError / 4.0);
            }
        }

        long finEntrenamiento = System.nanoTime();

        // ===== prueba final =====
        System.out.println("\n===== PRUEBA XOR =====");
        for (double[] entrada : X) {
            double[] salidaOculta = new double[2];

            for (int j = 0; j < 2; j++) {
                double suma = dot(Arrays.copyOfRange(W1[j], 0, 2), entrada) + W1[j][2];
                salidaOculta[j] = sigmoid(suma);
            }

            double salidaFinal = sigmoid(dot(Arrays.copyOfRange(W2[0], 0, 2), salidaOculta) + W2[0][2]);

            System.out.printf("%s -> %.4f -> %d%n",
                    Arrays.toString(entrada),
                    salidaFinal,
                    (salidaFinal > 0.5 ? 1 : 0));
        }

        long finTotal = System.nanoTime();

        double tiempoEntrenamientoMs = (finEntrenamiento - inicioEntrenamiento) / 1_000_000.0;
        double tiempoEntrenamientoSeg = (finEntrenamiento - inicioEntrenamiento) / 1_000_000_000.0;

        double tiempoTotalMs = (finTotal - inicioTotal) / 1_000_000.0;
        double tiempoTotalSeg = (finTotal - inicioTotal) / 1_000_000_000.0;

        System.out.printf("%nTiempo de entrenamiento: %.3f ms%n", tiempoEntrenamientoMs);
        System.out.printf("Tiempo de entrenamiento: %.6f s%n", tiempoEntrenamientoSeg);

        System.out.printf("%nTiempo total del programa: %.3f ms%n", tiempoTotalMs);
        System.out.printf("Tiempo total del programa: %.6f s%n", tiempoTotalSeg);
    }
}