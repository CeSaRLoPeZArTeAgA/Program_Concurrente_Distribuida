package MNIST;

public class MNISTDataset {
    private final int[][] images;
    private final int[] labels;
    private final int rows;
    private final int cols;

    public MNISTDataset(int[][] images, int[] labels, int rows, int cols) {
        this.images = images;
        this.labels = labels;
        this.rows = rows;
        this.cols = cols;
    }

    public int[][] getImages() {
        return images;
    }

    public int[] getLabels() {
        return labels;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getSize() {
        return images.length;
    }

    public int[] getImage(int index) {
        return images[index];
    }

    public int getLabel(int index) {
        return labels[index];
    }
}