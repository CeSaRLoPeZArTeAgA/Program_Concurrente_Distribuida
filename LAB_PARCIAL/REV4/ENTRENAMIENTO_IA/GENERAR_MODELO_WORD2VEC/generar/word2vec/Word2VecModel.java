package generar.word2vec;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.text.Normalizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Word2VecModel {

    private static final String MAGIC = "WORD2VEC_SKIPGRAM_V1";

    private static final Set<String> STOP_WORDS = new HashSet<>(
            Arrays.asList(
                    "como", "es", "a", "de", "del", "la", "el", "los", "las",
                    "un", "una", "unos", "unas", "y", "o", "en", "por", "con",
                    "para", "se", "su", "sus", "que", "al", "lo", "le", "les",
                    "mi", "mis", "tu", "tus", "este", "esta", "esto", "estos",
                    "estas", "ser", "tiene", "tienen", "tengo", "tendra"
            )
    );

    private int dimension;

    private Map<String, Integer> wordToId;
    private List<String> idToWord;

    private double[][] inputEmbeddings;
    private double[][] outputEmbeddings;

    private Random rnd = new Random(42);

    public Word2VecModel() {
        this.wordToId = new LinkedHashMap<>();
        this.idToWord = new ArrayList<>();
    }

    public static Word2VecModel trainFromFile(
            String corpusPath,
            int dimension,
            int windowSize,
            int epochs,
            double learningRate,
            int negativeSamples,
            int minCount
    ) throws IOException {

        String text = Files.readString(Paths.get(corpusPath), StandardCharsets.UTF_8);
        List<String> tokens = tokenize(text);

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("El corpus está vacío o no contiene palabras válidas.");
        }

        Word2VecModel model = new Word2VecModel();
        model.dimension = dimension;

        model.buildVocabulary(tokens, minCount);

        if (model.getVocabularySize() < 2) {
            throw new IllegalArgumentException("El vocabulario debe tener al menos 2 palabras.");
        }

        model.initializeWeights();

        List<Integer> corpusIds = model.tokensToIds(tokens);

        if (corpusIds.size() < 2) {
            throw new IllegalArgumentException("El corpus tiene muy pocas palabras luego del filtrado.");
        }

        System.out.println("========================================");
        System.out.println("Entrenamiento Word2Vec Skip-gram");
        System.out.println("Vocabulario       : " + model.getVocabularySize());
        System.out.println("Tokens usados     : " + corpusIds.size());
        System.out.println("Dimension vector  : " + dimension);
        System.out.println("Ventana contexto  : " + windowSize);
        System.out.println("Epocas            : " + epochs);
        System.out.println("Negative samples  : " + negativeSamples);
        System.out.println("========================================");

        for (int epoch = 1; epoch <= epochs; epoch++) {
            double loss = 0.0;
            int positivePairs = 0;

            long inicioEpoch = System.nanoTime();

            for (int i = 0; i < corpusIds.size(); i++) {
                int centerId = corpusIds.get(i);

                //int start = Math.max(0, i - windowSize);
                //int end = Math.min(corpusIds.size() - 1, i + windowSize);
                int dynamicWindow = 1 + model.rnd.nextInt(windowSize);
                int start = Math.max(0, i - dynamicWindow);
                int end = Math.min(corpusIds.size() - 1, i + dynamicWindow);

                for (int j = start; j <= end; j++) {
                    if (j == i) {
                        continue;
                    }

                    int contextId = corpusIds.get(j);

                    loss += model.trainPair(centerId, contextId, 1, learningRate);
                    positivePairs++;

                    for (int k = 0; k < negativeSamples; k++) {
                        int negativeId = model.randomNegative(contextId);
                        loss += model.trainPair(centerId, negativeId, 0, learningRate);
                    }
                }
            }

            long finEpoch = System.nanoTime();

            double avgLoss = loss / Math.max(1, positivePairs);
            double tiempoSeg = (finEpoch - inicioEpoch) / 1_000_000_000.0;

            System.out.printf(
                    "Epoch %d/%d | pares positivos = %d | loss promedio = %.6f | tiempo = %.3f s%n",
                    epoch,
                    epochs,
                    positivePairs,
                    avgLoss,
                    tiempoSeg
            );
        }

        return model;
    }

    private void buildVocabulary(List<String> tokens, int minCount) {
        Map<String, Integer> freq = new HashMap<>();

        for (String token : tokens) {
            freq.put(token, freq.getOrDefault(token, 0) + 1);
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freq.entrySet());

        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        wordToId.clear();
        idToWord.clear();

        for (Map.Entry<String, Integer> entry : entries) {
            String word = entry.getKey();

            if (entry.getValue() >= minCount && !isStopWord(word)) {
                int id = idToWord.size();
                wordToId.put(word, id);
                idToWord.add(word);
            }
        }
    }

    private List<Integer> tokensToIds(List<String> tokens) {
        List<Integer> ids = new ArrayList<>();

        for (String token : tokens) {
            Integer id = wordToId.get(token);

            if (id != null) {
                ids.add(id);
            }
        }

        return ids;
    }

    private void initializeWeights() {
        int vocabSize = getVocabularySize();

        inputEmbeddings = new double[vocabSize][dimension];
        outputEmbeddings = new double[vocabSize][dimension];

        double scale = 0.5 / dimension;

        for (int i = 0; i < vocabSize; i++) {
            for (int j = 0; j < dimension; j++) {
                inputEmbeddings[i][j] = (rnd.nextDouble() - 0.5) * scale;
                outputEmbeddings[i][j] = 0.0;
            }
        }
    }

    private double trainPair(int centerId, int targetId, int label, double learningRate) {
        double[] vCenter = inputEmbeddings[centerId];
        double[] vTarget = outputEmbeddings[targetId];

        double score = dot(vCenter, vTarget);
        double prediction = sigmoid(score);

        double gradient = learningRate * (label - prediction);

        for (int d = 0; d < dimension; d++) {
            double oldCenter = vCenter[d];
            double oldTarget = vTarget[d];

            vCenter[d] += gradient * oldTarget;
            vTarget[d] += gradient * oldCenter;
        }

        if (label == 1) {
            return -Math.log(prediction + 1e-12);
        } else {
            return -Math.log(1.0 - prediction + 1e-12);
        }
    }

    private int randomNegative(int positiveId) {
        int vocabSize = getVocabularySize();

        int id;

        do {
            id = rnd.nextInt(vocabSize);
        } while (id == positiveId);

        return id;
    }

    public List<SimilarWord> nearestWords(String word, int topK) {
        String normalized = normalizeWord(word);

        Integer id = wordToId.get(normalized);

        if (id == null) {
            return Collections.emptyList();
        }

        double[] query = normalizeVector(inputEmbeddings[id]);

        List<SimilarWord> result = new ArrayList<>();

        for (int i = 0; i < idToWord.size(); i++) {
            if (i == id) {
                continue;
            }

            String candidate = idToWord.get(i);

            if (isStopWord(candidate) || candidate.length() <= 2) {
                continue;
            }

            double sim = cosine(query, inputEmbeddings[i]);
            result.add(new SimilarWord(candidate, sim));
        }

        result.sort(Comparator.comparingDouble(SimilarWord::getSimilarity).reversed());

        if (result.size() > topK) {
            return new ArrayList<>(result.subList(0, topK));
        }

        return result;
    }

    public List<SimilarWord> analogy(String wordA, String wordB, String wordC, int topK) {
        String a = normalizeWord(wordA);
        String b = normalizeWord(wordB);
        String c = normalizeWord(wordC);

        Integer idA = wordToId.get(a);
        Integer idB = wordToId.get(b);
        Integer idC = wordToId.get(c);

        if (idA == null) {
            throw new IllegalArgumentException("La palabra no existe en el vocabulario: " + wordA);
        }

        if (idB == null) {
            throw new IllegalArgumentException("La palabra no existe en el vocabulario: " + wordB);
        }

        if (idC == null) {
            throw new IllegalArgumentException("La palabra no existe en el vocabulario: " + wordC);
        }

        double[] va = normalizeVector(inputEmbeddings[idA]);
        double[] vb = normalizeVector(inputEmbeddings[idB]);
        double[] vc = normalizeVector(inputEmbeddings[idC]);

        double[] target = new double[dimension];

        for (int d = 0; d < dimension; d++) {
            target[d] = va[d] - vb[d] + vc[d];
        }

        target = normalizeVector(target);

        List<SimilarWord> result = new ArrayList<>();

        for (int i = 0; i < idToWord.size(); i++) {
            String candidate = idToWord.get(i);

            if (i == idA || i == idB || i == idC) {
                continue;
            }

            if (isStopWord(candidate)) {
                continue;
            }

            if (candidate.length() <= 2) {
                continue;
            }

            double sim = cosine(target, inputEmbeddings[i]);
            result.add(new SimilarWord(candidate, sim));
        }

        result.sort(Comparator.comparingDouble(SimilarWord::getSimilarity).reversed());

        if (result.size() > topK) {
            return new ArrayList<>(result.subList(0, topK));
        }

        return result;
    }

    public boolean containsWord(String word) {
        return wordToId.containsKey(normalizeWord(word));
    }

    public int getVocabularySize() {
        return idToWord.size();
    }

    public int getDimension() {
        return dimension;
    }

    public void save(String path) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(path)))) {

            out.writeUTF(MAGIC);

            out.writeInt(dimension);
            out.writeInt(getVocabularySize());

            for (String word : idToWord) {
                out.writeUTF(word);
            }

            writeMatrix(out, inputEmbeddings);
            writeMatrix(out, outputEmbeddings);
        }
    }

    public static Word2VecModel load(String path) throws IOException {
        Word2VecModel model = new Word2VecModel();

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(path)))) {

            String magic = in.readUTF();

            if (!MAGIC.equals(magic)) {
                throw new IOException("Archivo Word2Vec inválido o incompatible.");
            }

            model.dimension = in.readInt();

            int vocabSize = in.readInt();

            model.wordToId = new LinkedHashMap<>();
            model.idToWord = new ArrayList<>();

            for (int i = 0; i < vocabSize; i++) {
                String word = in.readUTF();
                model.wordToId.put(word, i);
                model.idToWord.add(word);
            }

            model.inputEmbeddings = readMatrix(in);
            model.outputEmbeddings = readMatrix(in);
        }

        return model;
    }

    private static void writeMatrix(DataOutputStream out, double[][] matrix) throws IOException {
        out.writeInt(matrix.length);
        out.writeInt(matrix[0].length);

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                out.writeDouble(matrix[i][j]);
            }
        }
    }

    private static double[][] readMatrix(DataInputStream in) throws IOException {
        int rows = in.readInt();
        int cols = in.readInt();

        double[][] matrix = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = in.readDouble();
            }
        }

        return matrix;
    }

    private static double dot(double[] a, double[] b) {
        double sum = 0.0;

        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }

        return sum;
    }

    private static double norm(double[] a) {
        double sum = 0.0;

        for (double x : a) {
            sum += x * x;
        }

        return Math.sqrt(sum);
    }

    private static double cosine(double[] a, double[] b) {
        double na = norm(a);
        double nb = norm(b);

        if (na == 0.0 || nb == 0.0) {
            return 0.0;
        }

        return dot(a, b) / (na * nb);
    }

    private static double[] normalizeVector(double[] vector) {
        double n = norm(vector);

        double[] out = new double[vector.length];

        if (n == 0.0) {
            return out;
        }

        for (int i = 0; i < vector.length; i++) {
            out[i] = vector[i] / n;
        }

        return out;
    }

    private static double sigmoid(double x) {
        if (x > 20) {
            return 1.0;
        }

        if (x < -20) {
            return 0.0;
        }

        return 1.0 / (1.0 + Math.exp(-x));
    }

    public static List<String> tokenize(String text) {
        String normalized = text.toLowerCase();

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");

        normalized = normalized.replaceAll("[^a-zA-Z0-9ñÑ]+", " ");

        String[] parts = normalized.trim().split("\\s+");

        List<String> tokens = new ArrayList<>();

        for (String part : parts) {
            String token = normalizeWord(part);

            if (!token.isEmpty() && !isStopWord(token)) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private static String normalizeWord(String word) {
        if (word == null) {
            return "";
        }

        String w = word.trim().toLowerCase();

        w = Normalizer.normalize(w, Normalizer.Form.NFD);
        w = w.replaceAll("\\p{M}", "");

        w = w.replaceAll("[^a-zA-Z0-9ñÑ]", "");

        return w;
    }

    private static boolean isStopWord(String word) {
        if (word == null) {
            return true;
        }

        return STOP_WORDS.contains(normalizeWord(word));
    }

    public static class SimilarWord {
        private final String word;
        private final double similarity;

        public SimilarWord(String word, double similarity) {
            this.word = word;
            this.similarity = similarity;
        }

        public String getWord() {
            return word;
        }

        public double getSimilarity() {
            return similarity;
        }
    }
}
