package MNIST;

public class MainEntrenarWord2Vec {

    public static void main(String[] args) {

        String corpusPath = "corpus_texto.txt";
        String modelPath = "word2vec_model.bin";

        if (args.length >= 1) {
            corpusPath = args[0];
        }

        int dimension = 100;
        int windowSize = 2;
        int epochs = 80;
        double learningRate = 0.025;
        int negativeSamples = 5;
        int minCount = 1;

        try {
            System.out.println("Entrenando modelo Word2Vec...");
            System.out.println("Corpus: " + corpusPath);

            long inicio = System.nanoTime();

            Word2VecModel model = Word2VecModel.trainFromFile(
                    corpusPath,
                    dimension,
                    windowSize,
                    epochs,
                    learningRate,
                    negativeSamples,
                    minCount
            );

            model.save(modelPath);

            long fin = System.nanoTime();
            double tiempoSeg = (fin - inicio) / 1_000_000_000.0;

            System.out.println();
            System.out.println("=====================================");
            System.out.println("Modelo Word2Vec guardado en:");
            System.out.println(modelPath);
            System.out.println("Vocabulario: " + model.getVocabularySize());
            System.out.println("Dimension  : " + model.getDimension());
            System.out.printf("Tiempo total: %.3f segundos%n", tiempoSeg);
            System.out.println("=====================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}