package redesOk;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

// formulario cliente MNIST, solo convierte la imagen a 784 valores y envia al servidor
public class MainFormularioMNISTCliente extends JFrame {
    private static final long serialVersionUID = 1L;

    private final TCPClient50 tcpClient;
    private final MainMenuCliente menu;

    private JLabel imageLabel;
    private JLabel pathLabel;
    private JLabel resultadoLabel;
    private JTextArea probabilidadesArea;

    private BufferedImage currentImage;
    private File selectedFile;

    private final TCPClient50.OnMessageReceived listener;

    public MainFormularioMNISTCliente(TCPClient50 tcpClient, MainMenuCliente menu) {
        this.tcpClient = tcpClient;
        this.menu = menu;

        setTitle("Cliente MNIST - prediccion remota en servidor");
        setSize(700, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        listener = new TCPClient50.OnMessageReceived() {
            @Override
            public void messageReceived(String message) {
                if (message.startsWith("MNIST_RESULT|") || message.startsWith("ERROR|")) {
                    SwingUtilities.invokeLater(() -> procesarRespuestaServidor(message));
                }
            }
        };
        tcpClient.addListener(listener);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                tcpClient.removeListener(listener);
                menu.volverAMostrar();
            }
        });

        crearInterfaz();
    }

    private void crearInterfaz() {
        JLabel titleLabel = new JLabel("Clasificador MNIST - Cliente", SwingConstants.CENTER);
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

        resultadoLabel = new JLabel("Prediccion: pendiente", SwingConstants.CENTER);
        resultadoLabel.setFont(resultadoLabel.getFont().deriveFont(Font.BOLD, 22f));
        resultadoLabel.setOpaque(true);
        resultadoLabel.setBackground(new Color(240, 240, 240));
        resultadoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        probabilidadesArea = new JTextArea(8, 30);
        probabilidadesArea.setEditable(false);
        probabilidadesArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        probabilidadesArea.setText("Probabilidades por clase: pendiente");

        infoPanel.add(pathLabel, BorderLayout.NORTH);
        infoPanel.add(resultadoLabel, BorderLayout.CENTER);
        infoPanel.add(new JScrollPane(probabilidadesArea), BorderLayout.SOUTH);

        centerPanel.add(infoPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        JButton btnAbrir = new JButton("Abrir imagen");
        JButton btnLimpiar = new JButton("Limpiar pantalla");
        JButton btnEnviar = new JButton("Enviar al servidor IA");
        JButton btnMenu = new JButton("Volver al menu principal");
        JButton btnSalir = new JButton("Salir");

        btnAbrir.addActionListener(e -> abrirImagen());
        btnLimpiar.addActionListener(e -> limpiarPantalla());
        btnEnviar.addActionListener(e -> enviarAlServidorIA());
        btnMenu.addActionListener(e -> dispose());
        btnSalir.addActionListener(e -> salirDelSistema());

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
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imagenes PNG, JPG, JPEG", "png", "jpg", "jpeg"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                selectedFile = fileChooser.getSelectedFile();
                currentImage = ImageIO.read(selectedFile);

                if (currentImage == null) {
                    JOptionPane.showMessageDialog(this,
                            "El archivo seleccionado no es una imagen valida.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                mostrarImagen(currentImage);
                pathLabel.setText("Archivo: " + selectedFile.getName());
                resultadoLabel.setText("Prediccion: pendiente");
                probabilidadesArea.setText("Probabilidades por clase: pendiente");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error al abrir la imagen:\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void mostrarImagen(BufferedImage image) {
        int labelWidth = imageLabel.getWidth() > 0 ? imageLabel.getWidth() : 460;
        int labelHeight = imageLabel.getHeight() > 0 ? imageLabel.getHeight() : 360;

        Image scaledImage = image.getScaledInstance(labelWidth, labelHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setText("");
    }

    private void limpiarPantalla() {
        currentImage = null;
        selectedFile = null;
        imageLabel.setIcon(null);
        imageLabel.setText("Ninguna imagen cargada");
        pathLabel.setText("Archivo: ninguno");
        resultadoLabel.setText("Prediccion: pendiente");
        probabilidadesArea.setText("Probabilidades por clase: pendiente");
    }

    private void enviarAlServidorIA() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this,
                    "Primero debes cargar una imagen.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!tcpClient.isConnected()) {
            JOptionPane.showMessageDialog(this,
                    "No hay conexion activa con el servidor.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        double[] entradaMNIST = convertirImagenAEntradaMNIST(currentImage);
        String mensaje = "MNIST_PREDICT|" + vectorToCsv(entradaMNIST);

        resultadoLabel.setText("Prediccion: consultando servidor...");
        probabilidadesArea.setText("Esperando respuesta del servidor IA...");
        tcpClient.sendMessage(mensaje);
    }

    private void procesarRespuestaServidor(String message) {
        if (message.startsWith("ERROR|")) {
            String[] parts = message.split("\\|", 2);
            String error = parts.length >= 2 ? parts[1] : "Error desconocido";
            probabilidadesArea.setText("Error recibido del servidor:\n" + error);
            resultadoLabel.setText("Prediccion: error");
            return;
        }

        String[] parts = message.split("\\|", 4);
        if (parts.length < 4) {
            return;
        }

        int prediccion = parseInt(parts[1], -1);
        String[] probs = parts[2].split(",");
        String tiempoMs = parts[3];

        resultadoLabel.setText("Prediccion del servidor IA: " + prediccion);

        StringBuilder sb = new StringBuilder();
        sb.append("Respuesta recibida desde el servidor.\n\n");
        sb.append("Probabilidades por clase:\n\n");

        for (int i = 0; i < probs.length; i++) {
            sb.append(String.format("Clase %d : %s%n", i, probs[i]));
        }

        sb.append("\nTiempo en servidor: ").append(tiempoMs).append(" ms\n");
        probabilidadesArea.setText(sb.toString());
    }

    private double[] convertirImagenAEntradaMNIST(BufferedImage image) {
        BufferedImage resized = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
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

    private String vectorToCsv(double[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(java.util.Locale.US, "%.8f", values[i]));
        }
        return sb.toString();
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void salirDelSistema() {
        int opcion = JOptionPane.showConfirmDialog(this,
                "Deseas cerrar el cliente?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION);

        if (opcion == JOptionPane.YES_OPTION) {
            tcpClient.stopClient();
            dispose();
            System.exit(0);
        }
    }
}
