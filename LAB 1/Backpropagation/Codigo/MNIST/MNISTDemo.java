package MNIST;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class MNISTDemo {

    // Estructura para guardar el dataset cargado
    static class MNISTDataset {
        int[][] images;   // cada imagen se guarda como vector de 784 pixeles
        int[] labels;
        int rows;
        int cols;

        public MNISTDataset(int[][] images, int[] labels, int rows, int cols) {
            this.images = images;
            this.labels = labels;
            this.rows = rows;
            this.cols = cols;
        }
    }

    public static void main(String[] args) {
        
        String imagesPath = "D:/CICLO 2026-1 UNI/Program_Concurrente_Distribuida/LAB 1/Backpropagation/Codigo/MNIST/train-images-idx3-ubyte.gz";
        
        String labelsPath = "D:/CICLO 2026-1 UNI/Program_Concurrente_Distribuida/LAB 1/Backpropagation/Codigo/MNIST/train-labels-idx1-ubyte.gz";

    System.out.println("imagesPath = " + imagesPath);
    System.out.println("labelsPath = " + labelsPath);

        try {
            // Cargar solo algunas muestras para no usar demasiada memoria
            int limite = 10;
            MNISTDataset dataset = loadMNIST(imagesPath, labelsPath, limite);

            System.out.println("=== INFORMACION GENERAL ===");
            System.out.println("Cantidad de imagenes cargadas: " + dataset.images.length);
            System.out.println("Filas por imagen: " + dataset.rows);
            System.out.println("Columnas por imagen: " + dataset.cols);
            System.out.println("Pixeles por imagen: " + (dataset.rows * dataset.cols));
            System.out.println();

            // Imprimir información de las primeras muestras
            for (int i = 0; i < dataset.images.length; i++) {
                System.out.println("=== MUESTRA " + i + " ===");
                System.out.println("Etiqueta: " + dataset.labels[i]);

                System.out.print("Primeros 20 pixeles: ");
                for (int j = 0; j < 20; j++) {
                    System.out.print(dataset.images[i][j] + " ");
                }
                System.out.println("\n");

                System.out.println("Imagen como matriz 28x28:");
                printImageMatrix(dataset.images[i], dataset.rows, dataset.cols);

                System.out.println("\nImagen en ASCII:");
                printImageASCII(dataset.images[i], dataset.rows, dataset.cols);

                System.out.println("\n--------------------------------------------\n");
            }

        } catch (IOException e) {
            System.err.println("Error al leer MNIST: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Carga imágenes y etiquetas
    public static MNISTDataset loadMNIST(String imagesPath, String labelsPath, int limit) throws IOException {
        ImageData imageData = readImages(imagesPath, limit);
        int[] labels = readLabels(labelsPath, imageData.count);

        if (imageData.count != labels.length) {
            throw new IOException("El número de imágenes y etiquetas no coincide.");
        }

        return new MNISTDataset(imageData.images, labels, imageData.rows, imageData.cols);
    }

    // Clase auxiliar para devolver varias cosas al leer imágenes
    static class ImageData {
        int[][] images;
        int rows;
        int cols;
        int count;

        ImageData(int[][] images, int rows, int cols, int count) {
            this.images = images;
            this.rows = rows;
            this.cols = cols;
            this.count = count;
        }
    }

    // Lee el archivo de imágenes IDX
    public static ImageData readImages(String path, int limit) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(openFile(path)))) {

            int magicNumber = dis.readInt();
            if (magicNumber != 2051) {
                throw new IOException("Archivo de imágenes inválido. Magic number esperado: 2051, obtenido: " + magicNumber);
            }

            int numberOfImages = dis.readInt();
            int rows = dis.readInt();
            int cols = dis.readInt();

            int count = Math.min(limit, numberOfImages);
            int[][] images = new int[count][rows * cols];

            for (int i = 0; i < count; i++) {
                for (int j = 0; j < rows * cols; j++) {
                    images[i][j] = dis.readUnsignedByte(); // valor entre 0 y 255
                }
            }

            return new ImageData(images, rows, cols, count);
        }
    }

    // Lee el archivo de etiquetas IDX
    public static int[] readLabels(String path, int limit) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(openFile(path)))) {

            int magicNumber = dis.readInt();
            if (magicNumber != 2049) {
                throw new IOException("Archivo de etiquetas inválido. Magic number esperado: 2049, obtenido: " + magicNumber);
            }

            int numberOfLabels = dis.readInt();
            int count = Math.min(limit, numberOfLabels);
            int[] labels = new int[count];

            for (int i = 0; i < count; i++) {
                labels[i] = dis.readUnsignedByte();
            }

            return labels;
        }
    }

    // Abre archivo normal o .gz
    public static InputStream openFile(String path) throws IOException {
        InputStream fis = new FileInputStream(path);
        if (path.endsWith(".gz")) {
            return new GZIPInputStream(fis);
        }
        return fis;
    }

    // Imprime la imagen como matriz numérica
    public static void printImageMatrix(int[] image, int rows, int cols) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int pixel = image[i * cols + j];
                System.out.printf("%3d ", pixel);
            }
            System.out.println();
        }
    }

    // Imprime la imagen como arte ASCII
    public static void printImageASCII(int[] image, int rows, int cols) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int pixel = image[i * cols + j];
                char c;

                if (pixel == 0) {
                    c = ' ';
                } else if (pixel < 64) {
                    c = '.';
                } else if (pixel < 128) {
                    c = '*';
                } else if (pixel < 192) {
                    c = 'o';
                } else {
                    c = '#';
                }

                System.out.print(c);
            }
            System.out.println();
        }
    }
}