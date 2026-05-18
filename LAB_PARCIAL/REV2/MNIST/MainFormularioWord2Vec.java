package MNIST;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class MainFormularioWord2Vec extends JFrame {

    private static final String MODEL_PATH = "word2vec_model.bin";

    private Word2VecModel modelo;

    private JTextField palabraAField;
    private JTextField palabraBField;
    private JTextField palabraCField;

    private JTextArea resultadoArea;
    private JLabel estadoLabel;

    public MainFormularioWord2Vec() {
        setTitle("Formulario Word2Vec - Analogias");
        setSize(840, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        cargarModelo();
        crearInterfaz();
    }

    private void cargarModelo() {
        try {
            modelo = Word2VecModel.load(MODEL_PATH);

            System.out.println("Modelo Word2Vec cargado desde: " + MODEL_PATH);

        } catch (Exception e) {
            modelo = null;

            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo cargar el modelo Word2Vec.\n\n"
                            + "Verifica que exista el archivo:\n"
                            + MODEL_PATH + "\n\n"
                            + "Primero entrena con:\n"
                            + "java -cp out MNIST.MainEntrenarWord2Vec\n\n"
                            + "Error:\n" + e.getMessage(),
                    "Error al cargar modelo",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void crearInterfaz() {
        JLabel titulo = new JLabel("Modelo Word2Vec - Analogías", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 26));
        titulo.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        add(titulo, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new BorderLayout(10, 10));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        JPanel panelAnalogia = new JPanel(new BorderLayout(10, 10));
        panelAnalogia.setBorder(BorderFactory.createTitledBorder("Analogía clásica: A - B + C"));

        JPanel panelCampos = new JPanel(new GridLayout(2, 3, 10, 8));

        JLabel labelA = new JLabel("A", SwingConstants.CENTER);
        JLabel labelB = new JLabel("B", SwingConstants.CENTER);
        JLabel labelC = new JLabel("C", SwingConstants.CENTER);

        labelA.setFont(new Font("Arial", Font.BOLD, 15));
        labelB.setFont(new Font("Arial", Font.BOLD, 15));
        labelC.setFont(new Font("Arial", Font.BOLD, 15));

        palabraAField = new JTextField();
        palabraBField = new JTextField();
        palabraCField = new JTextField();

        palabraAField.setFont(new Font("Arial", Font.PLAIN, 16));
        palabraBField.setFont(new Font("Arial", Font.PLAIN, 16));
        palabraCField.setFont(new Font("Arial", Font.PLAIN, 16));

        panelCampos.add(labelA);
        panelCampos.add(labelB);
        panelCampos.add(labelC);

        panelCampos.add(palabraAField);
        panelCampos.add(palabraBField);
        panelCampos.add(palabraCField);

        JLabel ejemploLabel = new JLabel("Ejemplo: rey - hombre + mujer ≈ reina", SwingConstants.CENTER);
        ejemploLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        ejemploLabel.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));

        JButton btnResolver = new JButton("Resolver analogía");
        btnResolver.setFont(new Font("Arial", Font.BOLD, 14));
        btnResolver.addActionListener(e -> resolverAnalogia());

        panelAnalogia.add(panelCampos, BorderLayout.CENTER);
        panelAnalogia.add(btnResolver, BorderLayout.EAST);
        panelAnalogia.add(ejemploLabel, BorderLayout.SOUTH);

        panelCentro.add(panelAnalogia, BorderLayout.NORTH);

        resultadoArea = new JTextArea();
        resultadoArea.setEditable(false);
        resultadoArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        resultadoArea.setText("Resultado pendiente.");

        JScrollPane scroll = new JScrollPane(resultadoArea);
        panelCentro.add(scroll, BorderLayout.CENTER);

        estadoLabel = new JLabel("Estado: esperando analogía", SwingConstants.CENTER);
        estadoLabel.setOpaque(true);
        estadoLabel.setBackground(new Color(240, 240, 240));
        estadoLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        panelCentro.add(estadoLabel, BorderLayout.SOUTH);

        add(panelCentro, BorderLayout.CENTER);

        JButton btnLimpiar = new JButton("Limpiar");
        JButton btnMenu = new JButton("Volver al menú principal");
        JButton btnSalir = new JButton("Salir del modelo");

        btnLimpiar.addActionListener(e -> limpiar());
        btnMenu.addActionListener(e -> volverAlMenuPrincipal());
        btnSalir.addActionListener(e -> salirDelModelo());

        JPanel panelBotones = new JPanel(new GridLayout(1, 3, 10, 10));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(10, 25, 20, 25));

        panelBotones.add(btnLimpiar);
        panelBotones.add(btnMenu);
        panelBotones.add(btnSalir);

        add(panelBotones, BorderLayout.SOUTH);
    }

    private void resolverAnalogia() {
        if (modelo == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "El modelo Word2Vec no está cargado.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String a = palabraAField.getText().trim();
        String b = palabraBField.getText().trim();
        String c = palabraCField.getText().trim();

        if (a.isEmpty() || b.isEmpty() || c.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Debes ingresar las tres palabras: A, B y C.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            long inicio = System.nanoTime();

            List<Word2VecModel.SimilarWord> resultados = modelo.analogy(a, b, c, 10);

            long fin = System.nanoTime();
            double tiempoMs = (fin - inicio) / 1_000_000.0;

            StringBuilder sb = new StringBuilder();

            sb.append("Analogía calculada:\n\n");
            sb.append(a).append(" - ").append(b).append(" + ").append(c).append(" ≈ ?\n\n");

            if (resultados.isEmpty()) {
                sb.append("No se encontraron resultados.\n");
            } else {
                Word2VecModel.SimilarWord mejor = resultados.get(0);

                sb.append("Resultado principal:\n\n");
                sb.append("    ").append(mejor.getWord()).append("\n\n");

                sb.append("Ranking de resultados:\n\n");

                for (int i = 0; i < resultados.size(); i++) {
                    Word2VecModel.SimilarWord item = resultados.get(i);

                    sb.append(String.format(
                            "%2d. %-20s similitud = %.6f%n",
                            i + 1,
                            item.getWord(),
                            item.getSimilarity()
                    ));
                }
            }

            sb.append(String.format("%nTiempo de cálculo: %.6f ms%n", tiempoMs));

            resultadoArea.setText(sb.toString());
            estadoLabel.setText("Estado: analogía resuelta");

        } catch (Exception e) {
            resultadoArea.setText(
                    "No se pudo resolver la analogía.\n\n"
                            + "Operación solicitada:\n\n"
                            + a + " - " + b + " + " + c + " ≈ ?\n\n"
                            + "Error:\n"
                            + e.getMessage() + "\n\n"
                            + "Verifica que las tres palabras existan en el vocabulario del modelo."
            );

            estadoLabel.setText("Estado: error en analogía");
        }
    }

    private void limpiar() {
        palabraAField.setText("");
        palabraBField.setText("");
        palabraCField.setText("");

        resultadoArea.setText("Resultado pendiente.");
        estadoLabel.setText("Estado: esperando analogía");
    }

    private void volverAlMenuPrincipal() {
        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas volver al menú principal?",
                "Volver al menú principal",
                JOptionPane.YES_NO_OPTION
        );

        if (opcion == JOptionPane.YES_OPTION) {
            MainMenu menu = new MainMenu();
            menu.setVisible(true);
            dispose();
        }
    }

    private void salirDelModelo() {
        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas salir del modelo de texto?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION
        );

        if (opcion == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFormularioWord2Vec ventana = new MainFormularioWord2Vec();
            ventana.setVisible(true);
        });
    }
}