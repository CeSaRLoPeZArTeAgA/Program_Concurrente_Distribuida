package MNIST;

import java.util.Random;

public class MLP_HILOS {

    public static final int INPUT = 784;
    public static final int H1 = 100;
    public static final int H2 = 50;
    public static final int OUTPUT = 10;

    private final double[][] W1 = new double[H1][INPUT];
    private final double[] b1 = new double[H1];

    private final double[][] W2 = new double[H2][H1];
    private final double[] b2 = new double[H2];

    private final double[][] W3 = new double[OUTPUT][H2];
    private final double[] b3 = new double[OUTPUT];

    public MLP_HILOS() {
        initWeights();
    }

    private void initWeights() {
        Random rnd = new Random();

        double std1 = Math.sqrt(2.0 / INPUT);
        double std2 = Math.sqrt(2.0 / H1);
        double std3 = Math.sqrt(2.0 / H2);

        for (int i = 0; i < H1; i++) {
            for (int j = 0; j < INPUT; j++) {
                W1[i][j] = rnd.nextGaussian() * std1;
            }
            b1[i] = 0.0;
        }

        for (int i = 0; i < H2; i++) {
            for (int j = 0; j < H1; j++) {
                W2[i][j] = rnd.nextGaussian() * std2;
            }
            b2[i] = 0.0;
        }

        for (int i = 0; i < OUTPUT; i++) {
            for (int j = 0; j < H2; j++) {
                W3[i][j] = rnd.nextGaussian() * std3;
            }
            b3[i] = 0.0;
        }
    }

    public static class BatchGradients {
        public double loss = 0.0;
        public int count = 0;

        public double[][] dW1 = new double[H1][INPUT];
        public double[] db1 = new double[H1];

        public double[][] dW2 = new double[H2][H1];
        public double[] db2 = new double[H2];

        public double[][] dW3 = new double[OUTPUT][H2];
        public double[] db3 = new double[OUTPUT];

        public void addInPlace(BatchGradients other) {
            this.loss += other.loss;
            this.count += other.count;

            for (int i = 0; i < H1; i++) {
                this.db1[i] += other.db1[i];
                for (int j = 0; j < INPUT; j++) {
                    this.dW1[i][j] += other.dW1[i][j];
                }
            }

            for (int i = 0; i < H2; i++) {
                this.db2[i] += other.db2[i];
                for (int j = 0; j < H1; j++) {
                    this.dW2[i][j] += other.dW2[i][j];
                }
            }

            for (int i = 0; i < OUTPUT; i++) {
                this.db3[i] += other.db3[i];
                for (int j = 0; j < H2; j++) {
                    this.dW3[i][j] += other.dW3[i][j];
                }
            }
        }
    }

    private static class ForwardCache {
        double[] z1 = new double[H1];
        double[] a1 = new double[H1];

        double[] z2 = new double[H2];
        double[] a2 = new double[H2];

        double[] z3 = new double[OUTPUT];
        double[] probs = new double[OUTPUT];
    }

    public BatchGradients createEmptyGradients() {
        return new BatchGradients();
    }

    public BatchGradients computeMiniBatchGradients(MNISTDataset data, int[] order, int from, int to) {
        BatchGradients g = new BatchGradients();

        for (int p = from; p < to; p++) {
            int idx = order[p];
            double[] x = normalizeGeneric(data.getImage(idx));
            int y = data.getLabel(idx);
            accumulateSampleGradients(x, y, g);
        }

        return g;
    }

    public void applyGradients(BatchGradients g, double lr) {
        if (g.count == 0) {
            return;
        }

        double scale = lr / g.count;

        for (int i = 0; i < H1; i++) {
            b1[i] -= scale * g.db1[i];
            for (int j = 0; j < INPUT; j++) {
                W1[i][j] -= scale * g.dW1[i][j];
            }
        }

        for (int i = 0; i < H2; i++) {
            b2[i] -= scale * g.db2[i];
            for (int j = 0; j < H1; j++) {
                W2[i][j] -= scale * g.dW2[i][j];
            }
        }

        for (int i = 0; i < OUTPUT; i++) {
            b3[i] -= scale * g.db3[i];
            for (int j = 0; j < H2; j++) {
                W3[i][j] -= scale * g.dW3[i][j];
            }
        }
    }

    public double trainSample(double[] x, int y, double lr) {
        BatchGradients g = new BatchGradients();
        accumulateSampleGradients(x, y, g);
        applyGradients(g, lr);
        return g.loss;
    }

    private void accumulateSampleGradients(double[] x, int y, BatchGradients g) {
        ForwardCache fc = forward(x);

        g.loss += -Math.log(fc.probs[y] + 1e-12);
        g.count++;

        double[] delta3 = new double[OUTPUT];
        for (int i = 0; i < OUTPUT; i++) {
            delta3[i] = fc.probs[i] - (i == y ? 1.0 : 0.0);
        }

        for (int i = 0; i < OUTPUT; i++) {
            g.db3[i] += delta3[i];
            for (int j = 0; j < H2; j++) {
                g.dW3[i][j] += delta3[i] * fc.a2[j];
            }
        }

        double[] delta2 = new double[H2];
        for (int j = 0; j < H2; j++) {
            double sum = 0.0;
            for (int i = 0; i < OUTPUT; i++) {
                sum += W3[i][j] * delta3[i];
            }
            delta2[j] = sum * dSigmoidFromActivation(fc.a2[j]);
        }

        for (int i = 0; i < H2; i++) {
            g.db2[i] += delta2[i];
            for (int j = 0; j < H1; j++) {
                g.dW2[i][j] += delta2[i] * fc.a1[j];
            }
        }

        double[] delta1 = new double[H1];
        for (int j = 0; j < H1; j++) {
            double sum = 0.0;
            for (int i = 0; i < H2; i++) {
                sum += W2[i][j] * delta2[i];
            }
            delta1[j] = sum * dSigmoidFromActivation(fc.a1[j]);
        }

        for (int i = 0; i < H1; i++) {
            g.db1[i] += delta1[i];
            for (int j = 0; j < INPUT; j++) {
                g.dW1[i][j] += delta1[i] * x[j];
            }
        }
    }

    private ForwardCache forward(double[] x) {
        ForwardCache fc = new ForwardCache();

        for (int i = 0; i < H1; i++) {
            double sum = b1[i];
            for (int j = 0; j < INPUT; j++) {
                sum += W1[i][j] * x[j];
            }
            fc.z1[i] = sum;
            fc.a1[i] = sigmoid(sum);
        }

        for (int i = 0; i < H2; i++) {
            double sum = b2[i];
            for (int j = 0; j < H1; j++) {
                sum += W2[i][j] * fc.a1[j];
            }
            fc.z2[i] = sum;
            fc.a2[i] = sigmoid(sum);
        }

        for (int i = 0; i < OUTPUT; i++) {
            double sum = b3[i];
            for (int j = 0; j < H2; j++) {
                sum += W3[i][j] * fc.a2[j];
            }
            fc.z3[i] = sum;
        }

        fc.probs = softmax(fc.z3);
        return fc;
    }

    public int predict(double[] x) {
        double[] probs = predictProbs(x);
        return argmax(probs);
    }

    public double[] predictProbs(double[] x) {
        ForwardCache fc = forward(x);
        return fc.probs;
    }

    public double accuracy(MNISTDataset data) {
        int correct = 0;

        for (int i = 0; i < data.getSize(); i++) {
            double[] x = normalizeGeneric(data.getImage(i));
            int y = data.getLabel(i);
            int pred = predict(x);
            if (pred == y) {
                correct++;
            }
        }

        return 100.0 * correct / data.getSize();
    }

    private int argmax(double[] v) {
        int idx = 0;
        for (int i = 1; i < v.length; i++) {
            if (v[i] > v[idx]) {
                idx = i;
            }
        }
        return idx;
    }

    private double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private double dSigmoidFromActivation(double a) {
        return a * (1.0 - a);
    }

    private double[] softmax(double[] z) {
        double max = z[0];
        for (int i = 1; i < z.length; i++) {
            if (z[i] > max) {
                max = z[i];
            }
        }

        double[] out = new double[z.length];
        double sum = 0.0;

        for (int i = 0; i < z.length; i++) {
            out[i] = Math.exp(z[i] - max);
            sum += out[i];
        }

        for (int i = 0; i < z.length; i++) {
            out[i] /= sum;
        }

        return out;
    }

    public static double[] normalize(int[] image) {
        double[] x = new double[image.length];
        for (int i = 0; i < image.length; i++) {
            x[i] = (image[i] & 0xFF) / 255.0;
        }
        return x;
    }

    public static double[] normalize(byte[] image) {
        double[] x = new double[image.length];
        for (int i = 0; i < image.length; i++) {
            x[i] = (image[i] & 0xFF) / 255.0;
        }
        return x;
    }

    public static double[] normalize(double[] image) {
        double[] x = new double[image.length];
        for (int i = 0; i < image.length; i++) {
            x[i] = image[i];
        }
        return x;
    }

    public static double[] normalizeGeneric(Object image) {
        if (image instanceof int[]) {
            return normalize((int[]) image);
        }
        if (image instanceof byte[]) {
            return normalize((byte[]) image);
        }
        if (image instanceof double[]) {
            return normalize((double[]) image);
        }
        throw new IllegalArgumentException("Tipo de imagen no soportado: " +
                (image == null ? "null" : image.getClass().getName()));
    }
}
