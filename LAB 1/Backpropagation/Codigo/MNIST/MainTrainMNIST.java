package MNIST;

public class MainTrainMNIST {
    public static void main(String[] args) {
        String trainImages = "D:/CICLO 2026-1 UNI/Program_Concurrente_Distribuida/LAB 1/Backpropagation/Codigo/MNIST/train-images-idx3-ubyte.gz";
        String trainLabels = "D:/CICLO 2026-1 UNI/Program_Concurrente_Distribuida/LAB 1/Backpropagation/Codigo/MNIST/train-labels-idx1-ubyte.gz";

        String testImages = "D:/CICLO 2026-1 UNI/Program_Concurrente_Distribuida/LAB 1/Backpropagation/Codigo/MNIST/t10k-images-idx3-ubyte.gz";
        String testLabels = "D:/CICLO 2026-1 UNI/Program_Concurrente_Distribuida/LAB 1/Backpropagation/Codigo/MNIST/t10k-labels-idx1-ubyte.gz";

        try {
            MNISTDataset train = MNISTLoader.load(trainImages, trainLabels, 60000);
            MNISTDataset test = MNISTLoader.load(testImages, testLabels, 10000);

            MLP net = new MLP();

            int epochs = 5;
            double lr = 0.01;

            for (int epoch = 1; epoch <= epochs; epoch++) {
                double lossSum = 0.0;

                for (int i = 0; i < train.getSize(); i++) {
                    double[] x = MLP.normalize(train.getImage(i));
                    int y = train.getLabel(i);

                    lossSum += net.trainSample(x, y, lr);

                    if ((i + 1) % 5000 == 0) {
                        System.out.println("Epoch " + epoch + " | muestra " + (i + 1)
                                + " | loss promedio parcial = " + (lossSum / (i + 1)));
                    }
                }

                double trainAcc = net.accuracy(train);
                double testAcc = net.accuracy(test);

                System.out.println("====================================");
                System.out.println("Epoch " + epoch + " terminada");
                System.out.println("Loss promedio = " + (lossSum / train.getSize()));
                System.out.println("Accuracy train = " + trainAcc + "%");
                System.out.println("Accuracy test  = " + testAcc + "%");
                System.out.println("====================================");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}