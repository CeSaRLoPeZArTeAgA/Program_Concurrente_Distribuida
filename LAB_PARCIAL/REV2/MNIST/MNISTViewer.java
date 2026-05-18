package MNIST;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class MNISTViewer {

    public static BufferedImage toBufferedImage(int[] imageData, int rows, int cols) {
        BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int pixel = imageData[i * cols + j];
                raster.setSample(j, i, 0, pixel);
            }
        }

        return img;
    }

    public static void showSample(MNISTDataset dataset, int index, int scale) {
        if (index < 0 || index >= dataset.getSize()) {
            throw new IllegalArgumentException(
                "Índice fuera de rango. Debe estar entre 0 y " + (dataset.getSize() - 1)
            );
        }

        int[] imageData = dataset.getImage(index);
        int label = dataset.getLabel(index);

        BufferedImage img = toBufferedImage(imageData, dataset.getRows(), dataset.getCols());

        Image scaled = img.getScaledInstance(
                dataset.getCols() * scale,
                dataset.getRows() * scale,
                Image.SCALE_FAST
        );

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("MNIST - muestra " + index + " - etiqueta = " + label);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JLabel imageLabel = new JLabel(new ImageIcon(scaled));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel textLabel = new JLabel("Muestra: " + index + " | Etiqueta: " + label);
            textLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(textLabel, BorderLayout.NORTH);
            panel.add(imageLabel, BorderLayout.CENTER);

            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void showSamples(MNISTDataset dataset, int[] indices, int scale, int columnas) {
        if (indices == null || indices.length == 0) {
            throw new IllegalArgumentException("Debes proporcionar al menos un índice.");
        }

        if (columnas <= 0) {
            throw new IllegalArgumentException("El número de columnas debe ser positivo.");
        }

        for (int idx : indices) {
            if (idx < 0 || idx >= dataset.getSize()) {
                throw new IllegalArgumentException(
                    "Índice fuera de rango: " + idx +
                    ". Debe estar entre 0 y " + (dataset.getSize() - 1)
                );
            }
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("MNIST - múltiples muestras");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            int n = indices.length;
            int filas = (int) Math.ceil((double) n / columnas);

            JPanel panelPrincipal = new JPanel(new GridLayout(filas, columnas, 10, 10));

            for (int idx : indices) {
                int[] imageData = dataset.getImage(idx);
                int label = dataset.getLabel(idx);

                BufferedImage img = toBufferedImage(imageData, dataset.getRows(), dataset.getCols());

                Image scaled = img.getScaledInstance(
                        dataset.getCols() * scale,
                        dataset.getRows() * scale,
                        Image.SCALE_FAST
                );

                JLabel imageLabel = new JLabel(new ImageIcon(scaled));
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

                JLabel textLabel = new JLabel(
                        "Muestra: " + idx + " | Etiqueta: " + label,
                        SwingConstants.CENTER
                );

                JPanel panelIndividual = new JPanel(new BorderLayout());
                panelIndividual.add(textLabel, BorderLayout.NORTH);
                panelIndividual.add(imageLabel, BorderLayout.CENTER);

                panelPrincipal.add(panelIndividual);
            }

            JScrollPane scrollPane = new JScrollPane(panelPrincipal);
            frame.add(scrollPane);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static BufferedImage buildSamplesCollage(MNISTDataset dataset, int[] indices, int scale, int columnas) {
        if (indices == null || indices.length == 0) {
            throw new IllegalArgumentException("Debes proporcionar al menos un índice.");
        }

        if (columnas <= 0) {
            throw new IllegalArgumentException("El número de columnas debe ser positivo.");
        }

        for (int idx : indices) {
            if (idx < 0 || idx >= dataset.getSize()) {
                throw new IllegalArgumentException(
                    "Índice fuera de rango: " + idx +
                    ". Debe estar entre 0 y " + (dataset.getSize() - 1)
                );
            }
        }

        int n = indices.length;
        int filas = (int) Math.ceil((double) n / columnas);

        int imgW = dataset.getCols() * scale;
        int imgH = dataset.getRows() * scale;

        int textH = 22;
        int margen = 10;
        int celdaW = imgW + 2 * margen;
        int celdaH = imgH + textH + 2 * margen;

        int anchoTotal = columnas * celdaW;
        int altoTotal = filas * celdaH;

        BufferedImage collage = new BufferedImage(anchoTotal, altoTotal, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = collage.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, anchoTotal, altoTotal);
        g.setColor(java.awt.Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 14));

        for (int k = 0; k < n; k++) {
            int idx = indices[k];
            int label = dataset.getLabel(idx);

            int fila = k / columnas;
            int col = k % columnas;

            int x0 = col * celdaW;
            int y0 = fila * celdaH;

            BufferedImage base = toBufferedImage(dataset.getImage(idx), dataset.getRows(), dataset.getCols());

            Image scaled = base.getScaledInstance(imgW, imgH, Image.SCALE_FAST);

            g.drawString("Muestra: " + idx + " | Etiqueta: " + label, x0 + margen, y0 + 16);
            g.drawImage(scaled, x0 + margen, y0 + textH, null);
            g.drawRect(x0 + margen, y0 + textH, imgW, imgH);
        }

        g.dispose();
        return collage;
    }

    public static void showCollage(BufferedImage collage, String titulo) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(titulo);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JLabel label = new JLabel(new ImageIcon(collage));
            JScrollPane scrollPane = new JScrollPane(label);

            frame.add(scrollPane);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void printSampleInfo(MNISTDataset dataset, int index, int firstPixels) {
        if (index < 0 || index >= dataset.getSize()) {
            throw new IllegalArgumentException(
                "Índice fuera de rango. Debe estar entre 0 y " + (dataset.getSize() - 1)
            );
        }

        int[] image = dataset.getImage(index);

        System.out.println("=== MUESTRA " + index + " ===");
        System.out.println("Etiqueta: " + dataset.getLabel(index));
        System.out.println("Dimensión: " + dataset.getRows() + " x " + dataset.getCols());

        System.out.print("Primeros " + firstPixels + " píxeles: ");
        for (int i = 0; i < Math.min(firstPixels, image.length); i++) {
            System.out.print(image[i] + " ");
        }
        System.out.println();
    }
}