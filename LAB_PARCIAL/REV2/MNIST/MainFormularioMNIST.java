package MNIST;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainFormularioMNIST extends JFrame {

    private static final String MODEL_PATH = "modelo_mnist.bin";

    private JLabel imageLabel;
    private JLabel pathLabel;
    private JLabel resultadoLabel;
    private JTextArea probabilidadesArea;

    private BufferedImage currentImage;
    private File selectedFile;

    private MLP modeloIA;

    public MainFormularioMNIST() {
        setTitle("Formulario MNIST - Clasificador IA");
        setSize(680, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        cargarModeloIA();
        crearInterfaz();
    }

    private void cargarModeloIA() {
        try {
            modeloIA = new MLP();
            modeloIA.loadWeights(MODEL_PATH);

            System.out.println("Modelo IA cargado desde: " + MODEL_PATH);

        } catch (Exception e) {
            modeloIA = null;

            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo cargar el modelo IA.\n\n"
                            + "Verifica que exista el archivo:\n"
                            + MODEL_PATH + "\n\n"
                            + "Error:\n" + e.getMessage(),
                    "Error al cargar modelo",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void crearInterfaz() {
        JLabel titleLabel = new JLabel("Clasificador de imágenes MNIST", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        imageLabel = new JLabel("Ninguna imagen cargada", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(460, 360));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.WHITE);
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

        centerPanel.add(imageLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));

        pathLabel = new JLabel("Archivo: ninguno", SwingConstants.CENTER);
        pathLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        resultadoLabel = new JLabel("Predicción: pendiente", SwingConstants.CENTER);
        resultadoLabel.setFont(resultadoLabel.getFont().deriveFont(Font.BOLD, 22f));
        resultadoLabel.setOpaque(true);
        resultadoLabel.setBackground(new Color(240, 240, 240));
        resultadoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        probabilidadesArea = new JTextArea(8, 30);
        probabilidadesArea.setEditable(false);
        probabilidadesArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        probabilidadesArea.setText("Probabilidades por clase: pendiente");

        JScrollPane scrollProbabilidades = new JScrollPane(probabilidadesArea);

        infoPanel.add(pathLabel, BorderLayout.NORTH);
        infoPanel.add(resultadoLabel, BorderLayout.CENTER);
        infoPanel.add(scrollProbabilidades, BorderLayout.SOUTH);

        centerPanel.add(infoPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        JButton btnAbrir = new JButton("Abrir imagen");
        JButton btnLimpiar = new JButton("Limpiar pantalla");
        JButton btnEnviar = new JButton("Enviar a modelo IA");
        JButton btnMenu = new JButton("Volver al menú principal");
        JButton btnSalir = new JButton("Salir del modelo");

        btnAbrir.addActionListener(e -> abrirImagen());
        btnLimpiar.addActionListener(e -> limpiarPantalla());
        btnEnviar.addActionListener(e -> enviarAlModeloIA());
        btnMenu.addActionListener(e -> volverAlMenuPrincipal());
        btnSalir.addActionListener(e -> salirDelModelo());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        buttonPanel.add(btnAbrir);
        buttonPanel.add(btnLimpiar);
        buttonPanel.add(btnEnviar);
        buttonPanel.add(btnMenu);
        buttonPanel.add(btnSalir);
        buttonPanel.add(new JLabel(""));

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void abrirImagen() {
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setDialogTitle("Seleccionar imagen MNIST");
        fileChooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Imágenes PNG, JPG, JPEG",
                        "png", "jpg", "jpeg"
                )
        );

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                selectedFile = fileChooser.getSelectedFile();
                currentImage = ImageIO.read(selectedFile);

                if (currentImage == null) {
                    JOptionPane.showMessageDialog(
                            this,
                            "El archivo seleccionado no es una imagen válida.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                mostrarImagen(currentImage);

                pathLabel.setText("Archivo: " + selectedFile.getName());
                resultadoLabel.setText("Predicción: pendiente");
                probabilidadesArea.setText("Probabilidades por clase: pendiente");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Error al abrir la imagen:\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void mostrarImagen(BufferedImage image) {
        int labelWidth = imageLabel.getWidth();
        int labelHeight = imageLabel.getHeight();

        if (labelWidth <= 0) {
            labelWidth = 460;
        }

        if (labelHeight <= 0) {
            labelHeight = 360;
        }

        Image scaledImage = image.getScaledInstance(
                labelWidth,
                labelHeight,
                Image.SCALE_SMOOTH
        );

        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setText("");
    }

    private void limpiarPantalla() {
        currentImage = null;
        selectedFile = null;

        imageLabel.setIcon(null);
        imageLabel.setText("Ninguna imagen cargada");

        pathLabel.setText("Archivo: ninguno");
        resultadoLabel.setText("Predicción: pendiente");
        probabilidadesArea.setText("Probabilidades por clase: pendiente");
    }

    private void enviarAlModeloIA() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Primero debes cargar una imagen.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (modeloIA == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "El modelo IA no está cargado.\n"
                            + "Verifica que exista el archivo modelo_mnist.bin.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            double[] entradaMNIST = convertirImagenAEntradaMNIST(currentImage);

            long inicio = System.nanoTime();

            int prediccion = modeloIA.predict(entradaMNIST);
            double[] probabilidades = modeloIA.predictProbs(entradaMNIST);

            long fin = System.nanoTime();

            double tiempoMs = (fin - inicio) / 1_000_000.0;

            resultadoLabel.setText("Predicción del modelo IA: " + prediccion);

            StringBuilder sb = new StringBuilder();

            sb.append("Probabilidades por clase:\n\n");

            for (int i = 0; i < probabilidades.length; i++) {
                sb.append(String.format("Clase %d : %.6f%n", i, probabilidades[i]));
            }

            sb.append(String.format("%nTiempo de predicción: %.6f ms%n", tiempoMs));

            probabilidadesArea.setText(sb.toString());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error durante la predicción:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private double[] convertirImagenAEntradaMNIST(BufferedImage image) {
        BufferedImage resized = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g = resized.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        g.drawImage(image, 0, 0, 28, 28, null);
        g.dispose();

        double[] entrada = new double[28 * 28];

        for (int y = 0; y < 28; y++) {
            for (int x = 0; x < 28; x++) {
                int rgb = resized.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int gr = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int gray = (r + gr + b) / 3;

                entrada[y * 28 + x] = gray / 255.0;
            }
        }

        return entrada;
    }

    private void salirDelModelo() {
        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas salir del modelo?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION
        );

        if (opcion == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    private void volverAlMenuPrincipal() {
        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas volver al menú principal?",
                "Volver al menú principal",
                JOptionPane.YES_NO_OPTION
        );

        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        boolean menuAbierto = abrirClaseMenu("MNIST.MainMenu");

        if (!menuAbierto) {
            menuAbierto = abrirClaseMenu("MNIST.MenuPrincipal");
        }

        if (menuAbierto) {
            dispose();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "No se encontró una clase de menú principal.\n\n"
                            + "Crea una clase llamada:\n"
                            + "MNIST.MainMenu\n\n"
                            + "o una clase llamada:\n"
                            + "MNIST.MenuPrincipal",
                    "Menú principal no encontrado",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private boolean abrirClaseMenu(String nombreClase) {
        try {
            Class<?> menuClass = Class.forName(nombreClase);

            menuClass.getMethod("main", String[].class)
                    .invoke(null, (Object) new String[0]);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFormularioMNIST ventana = new MainFormularioMNIST();
            ventana.setVisible(true);
        });
    }
}