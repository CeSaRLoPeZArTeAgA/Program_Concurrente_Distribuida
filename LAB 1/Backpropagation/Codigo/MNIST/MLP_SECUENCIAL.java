package MNIST;

import java.util.Random;

public class MLP_SECUENCIAL {
    private final int inputSize = 784;
    private final int hidden1Size = 100;
    private final int hidden2Size = 50;
    private final int outputSize = 10;

    private final double[][] W1 = new double[hidden1Size][inputSize];
    private final double[] b1 = new double[hidden1Size];

    private final double[][] W2 = new double[hidden2Size][hidden1Size];
    private final double[] b2 = new double[hidden2Size];

    private final double[][] W3 = new double[outputSize][hidden2Size];
    private final double[] b3 = new double[outputSize];

    private final Random rnd = new Random(42);

    public MLP_SECUENCIAL() {
        initWeights(W1, inputSize);
        initWeights(W2, hidden1Size);
        initWeights(W3, hidden2Size);
    }

    private void initWeights(double[][] W, int fanIn) {
        double scale = Math.sqrt(2.0 / fanIn); // He initialization aprox
        for (int i = 0; i < W.length; i++) {
            for (int j = 0; j < W[i].length; j++) {
                W[i][j] = rnd.nextGaussian() * scale;
            }
        }
    }

    private double relu(double x) {
        return Math.max(0.0, x);
    }

    private double reluPrime(double x) {
        return x > 0.0 ? 1.0 : 0.0;
    }

    private double[] softmax(double[] z) {
        double max = z[0];
        for (int i = 1; i < z.length; i++) {
            if (z[i] > max) max = z[i];
        }

        double sum = 0.0;
        double[] exp = new double[z.length];
        for (int i = 0; i < z.length; i++) {
            exp[i] = Math.exp(z[i] - max);
            sum += exp[i];
        }

        for (int i = 0; i < z.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    private double[] matVec(double[][] W, double[] x, double[] b) {
        double[] y = new double[W.length];
        for (int i = 0; i < W.length; i++) {
            double sum = b[i];
            for (int j = 0; j < W[i].length; j++) {
                sum += W[i][j] * x[j];
            }
            y[i] = sum;
        }
        return y;
    }

    private double[] applyReLU(double[] z) {
        double[] a = new double[z.length];
        for (int i = 0; i < z.length; i++) {
            a[i] = relu(z[i]);
        }
        return a;
    }

    public int predict(double[] x) {
        double[] z1 = matVec(W1, x, b1);
        double[] a1 = applyReLU(z1);

        double[] z2 = matVec(W2, a1, b2);
        double[] a2 = applyReLU(z2);

        double[] z3 = matVec(W3, a2, b3);
        double[] yhat = softmax(z3);

        int argmax = 0;
        for (int i = 1; i < yhat.length; i++) {
            if (yhat[i] > yhat[argmax]) {
                argmax = i;
            }
        }
        return argmax;
    }

    public double trainSample(double[] x, int label, double lr) {
        // ---------- forward ----------
        double[] z1 = matVec(W1, x, b1);
        double[] a1 = applyReLU(z1);

        double[] z2 = matVec(W2, a1, b2);
        double[] a2 = applyReLU(z2);

        double[] z3 = matVec(W3, a2, b3);
        double[] yhat = softmax(z3);

        // one-hot
        double[] y = new double[outputSize];
        y[label] = 1.0;

        // loss
        double loss = -Math.log(yhat[label] + 1e-12);

        // ---------- backward ----------
        double[] delta3 = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            delta3[i] = yhat[i] - y[i];
        }

        double[] delta2 = new double[hidden2Size];
        for (int i = 0; i < hidden2Size; i++) {
            double sum = 0.0;
            for (int k = 0; k < outputSize; k++) {
                sum += W3[k][i] * delta3[k];
            }
            delta2[i] = sum * reluPrime(z2[i]);
        }

        double[] delta1 = new double[hidden1Size];
        for (int i = 0; i < hidden1Size; i++) {
            double sum = 0.0;
            for (int k = 0; k < hidden2Size; k++) {
                sum += W2[k][i] * delta2[k];
            }
            delta1[i] = sum * reluPrime(z1[i]);
        }

        // ---------- update W3, b3 ----------
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < hidden2Size; j++) {
                W3[i][j] -= lr * delta3[i] * a2[j];
            }
            b3[i] -= lr * delta3[i];
        }

        // ---------- update W2, b2 ----------
        for (int i = 0; i < hidden2Size; i++) {
            for (int j = 0; j < hidden1Size; j++) {
                W2[i][j] -= lr * delta2[i] * a1[j];
            }
            b2[i] -= lr * delta2[i];
        }

        // ---------- update W1, b1 ----------
        for (int i = 0; i < hidden1Size; i++) {
            for (int j = 0; j < inputSize; j++) {
                W1[i][j] -= lr * delta1[i] * x[j];
            }
            b1[i] -= lr * delta1[i];
        }

        return loss;
    }

    public double accuracy(MNISTDataset dataset) {
        int correct = 0;
        for (int i = 0; i < dataset.getSize(); i++) {
            double[] x = normalize(dataset.getImage(i));
            int pred = predict(x);
            if (pred == dataset.getLabel(i)) {
                correct++;
            }
        }
        return 100.0 * correct / dataset.getSize();
    }

    public static double[] normalize(int[] image) {
        double[] x = new double[image.length];
        for (int i = 0; i < image.length; i++) {
            x[i] = image[i] / 255.0;
        }
        return x;
    }

    public double[] predictProbs(double[] x) {
        double[] z1 = matVec(W1, x, b1);
        double[] a1 = applyReLU(z1);

        double[] z2 = matVec(W2, a1, b2);
        double[] a2 = applyReLU(z2);

        double[] z3 = matVec(W3, a2, b3);
        return softmax(z3);
    }
}