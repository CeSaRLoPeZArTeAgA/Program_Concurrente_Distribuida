import java.util.Random;

public class PerceptronParalelo {

    static class Resultado {
        double[] dw;
        double du;

        Resultado(int n) {
            dw = new double[n];
            du = 0.0;
        }
    }

    static class Worker extends Thread {

        double[][] X;
        double[] T;
        double[] w;
        double u;
        double e;
        Resultado res;

        Worker(double[][] X, double[] T, double[] w, double u, double e) {
            this.X = X;
            this.T = T;
            this.w = w;
            this.u = u;
            this.e = e;
            this.res = new Resultado(w.length);
        }

        @Override
        public void run() {
            for (int i = 0; i < X.length; i++) {

                double y = dot(X[i], w) - u;
                double Y = (y >= 0) ? 1.0 : -1.0;

                if (Y != T[i]) {
                    for (int j = 0; j < w.length; j++) {
                        res.dw[j] += 2.0 * e * T[i] * X[i][j];
                    }
                    res.du += 2.0 * e * T[i] * (-1);
                }
            }
        }
    }

    // ---------------- CLASE PRINCIPAL ----------------

    static class Neurona {

        double e = 0.5;
        double[] w = new double[2];
        double u;

        public Neurona() {
            Random rand = new Random();
            for (int i = 0; i < w.length; i++)
                w[i] = rand.nextDouble();
            u = rand.nextDouble();
        }

        public void entrenar(double[][] X, double[] T) throws InterruptedException {

            int numThreads = 4;
            int chunk = X.length / numThreads;

            Worker[] workers = new Worker[numThreads];

            // Crear hilos
            for (int i = 0; i < numThreads; i++) {
                int start = i * chunk;
                int end = (i == numThreads - 1) ? X.length : (i + 1) * chunk;

                double[][] Xi = slice(X, start, end);
                double[] Ti = slice(T, start, end);

                workers[i] = new Worker(Xi, Ti, w, u, e);
                workers[i].start();
            }

            // Esperar y acumular resultados
            for (Worker wkr : workers) {
                wkr.join();

                for (int j = 0; j < w.length; j++) {
                    w[j] += wkr.res.dw[j];
                }
                u += wkr.res.du;
            }

            System.out.println("Entrenamiento paralelo terminado");
        }

        public double mapear(double[] Xi) {
            double y = dot(Xi, w) - u;
            return (y >= 0) ? 1.0 : -1.0;
        }
    }

    // ---------------- FUNCIONES AUXILIARES ----------------

    static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++)
            s += a[i] * b[i];
        return s;
    }

    static double[][] slice(double[][] X, int start, int end) {
        double[][] r = new double[end - start][X[0].length];
        for (int i = start; i < end; i++)
            r[i - start] = X[i].clone();
        return r;
    }

    static double[] slice(double[] T, int start, int end) {
        double[] r = new double[end - start];
        for (int i = start; i < end; i++)
            r[i - start] = T[i];
        return r;
    }

    // ---------------- MAIN ----------------

    public static void main(String[] args) throws InterruptedException {

        double[][] X = {
                {1.0, 1.0},
                {1.0, -1.0},
                {-1.0, 1.0},
                {-1.0, -1.0}
        };

        double[] T = {1.0, 1.0, 1.0, -1.0};

        Neurona n = new Neurona();
        n.entrenar(X, T);

        System.out.println("----- mapeo ----------");
        for (int i = 0; i < X.length; i++) {
            System.out.println(
                X[i][0] + " " + X[i][1] + " -> " + n.mapear(X[i])
            );
        }
    }
}