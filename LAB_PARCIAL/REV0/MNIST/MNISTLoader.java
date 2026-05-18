package MNIST;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class MNISTLoader {

    private static class ImageData {
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

    public static MNISTDataset load(String imagesPath, String labelsPath, int limit) throws IOException {
        ImageData imageData = readImages(imagesPath, limit);
        int[] labels = readLabels(labelsPath, imageData.count);

        if (imageData.count != labels.length) {
            throw new IOException("El número de imágenes y etiquetas no coincide.");
        }

        return new MNISTDataset(imageData.images, labels, imageData.rows, imageData.cols);
    }

    private static ImageData readImages(String path, int limit) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(openFile(path)))) {

            int magicNumber = dis.readInt();
            if (magicNumber != 2051) {
                throw new IOException("Archivo de imágenes inválido. Magic number esperado: 2051, obtenido: " + magicNumber);
            }

            int numberOfImages = dis.readInt();
            int rows = dis.readInt();
            int cols = dis.readInt();

            int count = (limit <= 0) ? numberOfImages : Math.min(limit, numberOfImages);
            int[][] images = new int[count][rows * cols];

            for (int i = 0; i < count; i++) {
                for (int j = 0; j < rows * cols; j++) {
                    images[i][j] = dis.readUnsignedByte();
                }
            }

            return new ImageData(images, rows, cols, count);
        }
    }

    private static int[] readLabels(String path, int limit) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(openFile(path)))) {

            int magicNumber = dis.readInt();
            if (magicNumber != 2049) {
                throw new IOException("Archivo de etiquetas inválido. Magic number esperado: 2049, obtenido: " + magicNumber);
            }

            int numberOfLabels = dis.readInt();
            int count = (limit <= 0) ? numberOfLabels : Math.min(limit, numberOfLabels);
            int[] labels = new int[count];

            for (int i = 0; i < count; i++) {
                labels[i] = dis.readUnsignedByte();
            }

            return labels;
        }
    }

    private static InputStream openFile(String path) throws IOException {
        InputStream fis = new FileInputStream(path);
        if (path.endsWith(".gz")) {
            return new GZIPInputStream(fis);
        }
        return fis;
    }
}
