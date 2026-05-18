package generar.mnist;

import java.nio.file.Files;
import java.nio.file.Paths;

public class MainEntrenarGuardar {

    public static void main(String[] args) {

        String dataDir = "datos_mnist";
        String modelPath = "salida_modelo/modelo_mnist.bin";

        if (args.length >= 1) {
            dataDir = args[0];
        }

        if (args.length >= 2) {
            modelPath = args[1];
        }

        String trainImages = dataDir + "/train-images-idx3-ubyte.gz";
        String trainLabels = dataDir + "/train-labels-idx1-ubyte.gz";

        String testImages = dataDir + "/t10k-images-idx3-ubyte.gz";
        String testLabels = dataDir + "/t10k-labels-idx1-ubyte.gz";

        int epochs = 5;
        double lr = 0.01;

        try {
            Files.createDirectories(Paths.get("salida_modelo"));

            MNISTDataset train = MNISTLoader.load(trainImages, trainLabels, 60000);
            MNISTDataset test = MNISTLoader.load(testImages, testLabels, 10000);

            MLP net = new MLP();

            long inicioTotal = System.nanoTime();

            for (int epoch = 1; epoch <= epochs; epoch++) {
                double lossSum = 0.0;

                long inicioEpoch = System.nanoTime();

                for (int i = 0; i < train.getSize(); i++) {
                    double[] x = MLP.normalize(train.getImage(i));
                    int y = train.getLabel(i);

                    lossSum += net.trainSample(x, y, lr);

                    if ((i + 1) % 5000 == 0) {
                        System.out.println(
                                "Epoch " + epoch +
                                " | muestra " + (i + 1) +
                                " | loss promedio parcial = " + (lossSum / (i + 1))
                        );
                    }
                }

                long finEpoch = System.nanoTime();
                double tiempoEpoch = (finEpoch - inicioEpoch) / 1_000_000_000.0;

                double trainAcc = net.accuracy(train);
                double testAcc = net.accuracy(test);

                System.out.println("====================================");
                System.out.println("Epoch " + epoch + " terminada");
                System.out.println("Loss promedio = " + (lossSum / train.getSize()));
                System.out.println("Accuracy train = " + trainAcc + "%");
                System.out.println("Accuracy test  = " + testAcc + "%");
                System.out.printf("Tiempo epoch   = %.3f segundos%n", tiempoEpoch);
                System.out.println("====================================");

                net.saveWeights(modelPath);
                System.out.println("Pesos guardados en: " + modelPath);
            }

            long finTotal = System.nanoTime();
            double tiempoTotal = (finTotal - inicioTotal) / 1_000_000_000.0;

            System.out.println();
            System.out.println("========= ENTRENAMIENTO FINALIZADO =========");
            System.out.printf("Tiempo total: %.3f segundos%n", tiempoTotal);
            System.out.println("Modelo final guardado en: " + modelPath);
            System.out.println("============================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
