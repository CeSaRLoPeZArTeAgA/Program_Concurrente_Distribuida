package MNIST;

import java.util.Random;

public class MainProbarMuestra {

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

            long inicioTotalEntrenamiento = System.nanoTime();

            for (int epoch = 1; epoch <= epochs; epoch++) {
                double lossSum = 0.0;

                long inicioEpoch = System.nanoTime();

                for (int i = 0; i < train.getSize(); i++) {
                    double[] x = MLP.normalize(train.getImage(i));
                    int y = train.getLabel(i);

                    lossSum += net.trainSample(x, y, lr);

                    if ((i + 1) % 5000 == 0) {
                        System.out.println("Epoch " + epoch + " | muestra " + (i + 1)
                                + " | loss promedio parcial = " + (lossSum / (i + 1)));
                    }
                }

                long finEpoch = System.nanoTime();
                double tiempoEpochSeg = (finEpoch - inicioEpoch) / 1_000_000_000.0;

                double trainAcc = net.accuracy(train);
                double testAcc = net.accuracy(test);

                System.out.println("====================================");
                System.out.println("Epoch " + epoch + " terminada");
                System.out.println("Loss promedio = " + (lossSum / train.getSize()));
                System.out.println("Accuracy train = " + trainAcc + "%");
                System.out.println("Accuracy test  = " + testAcc + "%");
                System.out.printf("Tiempo epoch   = %.3f segundos%n", tiempoEpochSeg);
                System.out.println("====================================");
            }

            long finTotalEntrenamiento = System.nanoTime();
            double tiempoTotalSeg = (finTotalEntrenamiento - inicioTotalEntrenamiento) / 1_000_000_000.0;
            double tiempoTotalMin = tiempoTotalSeg / 60.0;

            System.out.println();
            System.out.println("========= TIEMPO TOTAL DE ENTRENAMIENTO =========");
            System.out.printf("Tiempo total: %.3f segundos%n", tiempoTotalSeg);
            System.out.printf("Tiempo total: %.3f minutos%n", tiempoTotalMin);
            System.out.println("=================================================");
            System.out.println();

            int indice;
            if (args.length >= 1) {
                indice = Integer.parseInt(args[0]);
            } else {
                Random rnd = new Random();
                indice = rnd.nextInt(test.getSize());
            }

            if (indice < 0 || indice >= test.getSize()) {
                throw new IllegalArgumentException(
                        "Índice fuera de rango. Debe estar entre 0 y " + (test.getSize() - 1));
            }

            double[] xTest = MLP.normalize(test.getImage(indice));
            int etiquetaReal = test.getLabel(indice);

            long inicioPred = System.nanoTime();
            int prediccion = net.predict(xTest);
            double[] probs = net.predictProbs(xTest);
            long finPred = System.nanoTime();

            double tiempoPredMs = (finPred - inicioPred) / 1_000_000.0;

            System.out.println("=========== PRUEBA DE UNA MUESTRA ===========");
            System.out.println("Índice de muestra: " + indice);
            System.out.println("Etiqueta real    : " + etiquetaReal);
            System.out.println("Predicción red   : " + prediccion);
            System.out.println(prediccion == etiquetaReal ? "Resultado        : CORRECTO" : "Resultado        : INCORRECTO");
            System.out.printf("Tiempo predicción: %.6f ms%n", tiempoPredMs);
            System.out.println();

            System.out.println("Probabilidades por clase:");
            for (int k = 0; k < probs.length; k++) {
                System.out.printf("Clase %d: %.6f%n", k, probs[k]);
            }

            System.out.println();
            MNISTViewer.printSampleInfo(test, indice, 20);
            MNISTViewer.showSample(test, indice, 15);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

//hace una red neurola de capa de entrada 28x28 y una capa oculta de 100 y otra de 50 y
// y la capa de salida de 10
//  784 -> 100 -> 50 -> 10

//cd "D:\CICLO 2026-1 UNI\Program_Concurrente_Distribuida\LAB 1\Backpropagation\Codigo"
//javac MNIST\*.java
//java -cp . MNIST.MainProbarMuestra

//java -cp . MNIST.MainProbarMuestra 25
