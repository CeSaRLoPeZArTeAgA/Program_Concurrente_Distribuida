import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RedNeuronalBatchParalelo {

    static final Random rnd = new Random(42);

    // activaciones
    public static double relu(double x) {
        return Math.max(0.0, x);
    }

    public static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    // genera matriz aleatoria
    public static double[][] randomMatrix(int rows, int cols, double min, double max) {
        double[][] A = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                A[i][j] = min + (max - min) * rnd.nextDouble();
            }
        }
        return A;
    }

    // genera vector aleatorio
    public static double[] randomVector(int n, double min, double max) {
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            v[i] = min + (max - min) * rnd.nextDouble();
        }
        return v;
    }

    // forward de UNA muestra:
    // x -> capa oculta -> salida
    public static double forwardUnaMuestra(
            double[] x,
            double[][] W1, double[] b1,
            double[][] W2, double[] b2
    ) {
        int hiddenSize = W1.length;
        int outputSize = W2.length;

        double[] h = new double[hiddenSize];

        // capa oculta
        for (int j = 0; j < hiddenSize; j++) {
            double suma = b1[j];
            for (int k = 0; k < x.length; k++) {
                suma += W1[j][k] * x[k];
            }
            h[j] = relu(suma);
        }

        // capa de salida
        // aquí asumimos outputSize = 1 para simplificar
        double out = b2[0];
        for (int j = 0; j < hiddenSize; j++) {
            out += W2[0][j] * h[j];
        }

        return sigmoid(out);
    }

    // inferencia secuencial sobre todo el batch
    public static double[] inferenciaSecuencial(
            double[][] X,
            double[][] W1, double[] b1,
            double[][] W2, double[] b2
    ) {
        double[] y = new double[X.length];
        for (int i = 0; i < X.length; i++) {
            y[i] = forwardUnaMuestra(X[i], W1, b1, W2, b2);
        }
        return y;
    }

    // inferencia multihilo sobre todo el batch
    public static double[] inferenciaMultihilo(
            double[][] X,
            double[][] W1, double[] b1,
            double[][] W2, double[] b2,
            int numThreads
    ) throws InterruptedException {

        double[] y = new double[X.length];
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        int n = X.length;
        int block = (n + numThreads - 1) / numThreads;

        for (int t = 0; t < numThreads; t++) {
            final int inicio = t * block;
            final int fin = Math.min(inicio + block, n);

            if (inicio >= fin) break;

            pool.execute(() -> {
                for (int i = inicio; i < fin; i++) {
                    y[i] = forwardUnaMuestra(X[i], W1, b1, W2, b2);
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.HOURS);

        return y;
    }

    public static void main(String[] args) throws InterruptedException {

        // dimensiones de una red más realista que XOR
        int numMuestras = 20000;   // batch grande
        int inputSize   = 256;
        int hiddenSize  = 512;
        int outputSize  = 1;

        int numThreads = Runtime.getRuntime().availableProcessors();

        System.out.println("Muestras      = " + numMuestras);
        System.out.println("Input size    = " + inputSize);
        System.out.println("Hidden size   = " + hiddenSize);
        System.out.println("Threads       = " + numThreads);

        // datos aleatorios de entrada
        double[][] X = randomMatrix(numMuestras, inputSize, -1.0, 1.0);

        // pesos y sesgos
        double[][] W1 = randomMatrix(hiddenSize, inputSize, -0.5, 0.5);
        double[]   b1 = randomVector(hiddenSize, -0.1, 0.1);

        double[][] W2 = randomMatrix(outputSize, hiddenSize, -0.5, 0.5);
        double[]   b2 = randomVector(outputSize, -0.1, 0.1);

        // secuencial
        long t1 = System.nanoTime();
        double[] ys = inferenciaSecuencial(X, W1, b1, W2, b2);
        long t2 = System.nanoTime();

        // multihilo
        long t3 = System.nanoTime();
        double[] yp = inferenciaMultihilo(X, W1, b1, W2, b2, numThreads);
        long t4 = System.nanoTime();

        double tiempoSecMs = (t2 - t1) / 1_000_000.0;
        double tiempoParMs = (t4 - t3) / 1_000_000.0;

        // pequeña verificación
        double maxDiff = 0.0;
        for (int i = 0; i < numMuestras; i++) {
            maxDiff = Math.max(maxDiff, Math.abs(ys[i] - yp[i]));
        }

        System.out.printf("%nTiempo secuencial : %.3f ms%n", tiempoSecMs);
        System.out.printf("Tiempo multihilo  : %.3f ms%n", tiempoParMs);
        System.out.printf("Aceleración       : %.3f veces%n", tiempoSecMs / tiempoParMs);
        System.out.printf("Diferencia máxima : %.12f%n", maxDiff);

        System.out.println("\nPrimeras 10 salidas:");
        for (int i = 0; i < 10; i++) {
            System.out.printf("y[%d] = %.6f%n", i, yp[i]);
        }
    }
}