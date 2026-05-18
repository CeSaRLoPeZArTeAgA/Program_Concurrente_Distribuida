package MNIST;

import java.util.Random;

public class MainUsarModelo {

    public static void main(String[] args) {

        String testImages = "t10k-images-idx3-ubyte.gz";
        String testLabels = "t10k-labels-idx1-ubyte.gz";

        String modelPath = "modelo_mnist.bin";

        try {
            MNISTDataset test = MNISTLoader.load(testImages, testLabels, 10000);

            MLP net = new MLP();
            net.loadWeights(modelPath);

            System.out.println("Modelo cargado correctamente desde: " + modelPath);

            int indice;

            if (args.length >= 1) {
                indice = Integer.parseInt(args[0]);
            } else {
                Random rnd = new Random();
                indice = rnd.nextInt(test.getSize());
            }

            if (indice < 0 || indice >= test.getSize()) {
                throw new IllegalArgumentException(
                        "Índice fuera de rango. Debe estar entre 0 y " + (test.getSize() - 1)
                );
            }

            double[] xTest = MLP.normalize(test.getImage(indice));
            int etiquetaReal = test.getLabel(indice);

            long inicioPred = System.nanoTime();

            int prediccion = net.predict(xTest);
            double[] probs = net.predictProbs(xTest);

            long finPred = System.nanoTime();
            double tiempoPredMs = (finPred - inicioPred) / 1_000_000.0;

            System.out.println("=========== PREDICCION SIN ENTRENAR ===========");
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
