import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class XORMultihilo {

    // funcion de activacion
    public static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    // derivada de la sigmoide, recibiendo la salida ya activada
    public static double dSigmoid(double x) {
        return x * (1.0 - x);
    }

    // producto punto entre dos vectores
    public static double dot(double[] a, double[] b) {
        double suma = 0.0;
        for (int i = 0; i < a.length; i++) {
            suma += a[i] * b[i];
        }
        return suma;
    }

    // genera un numero aleatorio uniforme en [-1,1]
    public static double randomUniform(Random rnd) {
        return -1.0 + 2.0 * rnd.nextDouble();
    }

    // clase de neurona ejecutada en un hilo
    static class Neurona extends Thread {
        private final String nombre;
        private final double[] entradas;
        private final double[] pesos; // incluye bias al final
        private final Map<String, Double> salidaDict;
        private final Object lock;

        public Neurona(String nombre, double[] entradas, double[] pesos,
                       Map<String, Double> salidaDict, Object lock) {
            this.nombre = nombre;
            this.entradas = entradas;
            this.pesos = pesos;
            this.salidaDict = salidaDict;
            this.lock = lock;
        }

        @Override
        public void run() {
            double suma = 0.0;
            for (int i = 0; i < entradas.length; i++) {
                suma += entradas[i] * pesos[i];
            }
            suma += pesos[pesos.length - 1]; // bias

            double salida = sigmoid(suma);

            synchronized (lock) {
                salidaDict.put(nombre, salida);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Locale.setDefault(Locale.US);

        // ===== TIEMPO TOTAL DEL PROGRAMA =====
        long inicioTotal = System.nanoTime();

        Random rnd = new Random(42);

        // Entradas y salidas del XOR
        double[][] X = {
            {0.0, 0.0},
            {0.0, 1.0},
            {1.0, 0.0},
            {1.0, 1.0}
        };

        double[] y = {0.0, 1.0, 1.0, 0.0};

        double lr = 0.5;
        int epochs = 8000;

        // pesos iniciales: capa oculta (2 neuronas, 2 entradas + bias)
        double[][] W1 = new double[2][3];

        // pesos iniciales: capa de salida (1 neurona, 2 entradas ocultas + bias)
        double[][] W2 = new double[1][3];

        // inicializacion aleatoria uniforme en [-1,1]
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                W1[i][j] = randomUniform(rnd);
            }
        }

        for (int j = 0; j < 3; j++) {
            W2[0][j] = randomUniform(rnd);
        }

        Object lock = new Object();

        // ===== TIEMPO SOLO DEL ENTRENAMIENTO =====
        long inicioEntrenamiento = System.nanoTime();

        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalError = 0.0;

            for (int i = 0; i < X.length; i++) {
                double[] entrada = X[i];
                double objetivo = y[i];

                Map<String, Double> salidas = new HashMap<>();

                // crear hilos para neuronas ocultas
                Neurona[] hilos = new Neurona[2];
                for (int j = 0; j < 2; j++) {
                    hilos[j] = new Neurona("h" + j, entrada, W1[j], salidas, lock);
                    hilos[j].start();
                }

                // esperar que terminen
                for (int j = 0; j < 2; j++) {
                    hilos[j].join();
                }

                // obtener salida oculta
                double[] salidaOculta = {
                    salidas.get("h0"),
                    salidas.get("h1")
                };

                // neurona de salida en hilo
                Neurona outThread = new Neurona("out", salidaOculta, W2[0], salidas, lock);
                outThread.start();
                outThread.join();

                double salidaFinal = salidas.get("out");
                double error = objetivo - salidaFinal;
                totalError += Math.abs(error);

                // retropropagacion
                double dOut = error * dSigmoid(salidaFinal);

                double[] dH = new double[2];
                for (int j = 0; j < 2; j++) {
                    dH[j] = dSigmoid(salidaOculta[j]) * (dOut * W2[0][j]);
                }

                // actualizacion de pesos de salida
                for (int j = 0; j < 2; j++) {
                    W2[0][j] += lr * dOut * salidaOculta[j];
                }
                W2[0][2] += lr * dOut; // bias salida

                // actualizacion de pesos ocultos
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

        // prueba final
        System.out.println("\n===== PRUEBA XOR =====");
        for (double[] entrada : X) {
            double[] salidaOculta = new double[2];

            // forward capa oculta
            for (int j = 0; j < 2; j++) {
                double suma = dot(Arrays.copyOfRange(W1[j], 0, 2), entrada) + W1[j][2];
                salidaOculta[j] = sigmoid(suma);
            }

            // forward capa salida
            double sumaSalida = dot(Arrays.copyOfRange(W2[0], 0, 2), salidaOculta) + W2[0][2];
            double salidaFinal = sigmoid(sumaSalida);

            System.out.printf("%s -> %.4f -> %d%n",
                    Arrays.toString(entrada),
                    salidaFinal,
                    (salidaFinal > 0.5 ? 1 : 0));
        }

        long finTotal = System.nanoTime();

        // conversiones de tiempo
        double tiempoEntrenamientoMs = (finEntrenamiento - inicioEntrenamiento) / 1_000_000.0;
        double tiempoEntrenamientoSeg = (finEntrenamiento - inicioEntrenamiento) / 1_000_000_000.0;

        double tiempoTotalMs = (finTotal - inicioTotal) / 1_000_000.0;
        double tiempoTotalSeg = (finTotal - inicioTotal) / 1_000_000_000.0;

        // impresion en consola
        System.out.printf("%nTiempo de entrenamiento: %.3f ms%n", tiempoEntrenamientoMs);
        System.out.printf("Tiempo de entrenamiento: %.6f s%n", tiempoEntrenamientoSeg);

        System.out.printf("%nTiempo total del programa: %.3f ms%n", tiempoTotalMs);
        System.out.printf("Tiempo total del programa: %.6f s%n", tiempoTotalSeg);
    }
}