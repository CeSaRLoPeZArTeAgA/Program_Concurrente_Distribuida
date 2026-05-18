package redesOk;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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


// Formulario cliente Word2Vec, solo envia palabras al servidor y muestra la respuesta
public class MainFormularioWord2VecCliente extends JFrame {
    private static final long serialVersionUID = 1L;

    private final TCPClient50 tcpClient;
    private final MainMenuCliente menu;

    private JTextField palabraAField;
    private JTextField palabraBField;
    private JTextField palabraCField;
    private JTextField palabraNearestField;
    private JTextArea resultadoArea;
    private JLabel estadoLabel;

    private final TCPClient50.OnMessageReceived listener;

    public MainFormularioWord2VecCliente(TCPClient50 tcpClient, MainMenuCliente menu) {
        this.tcpClient = tcpClient;
        this.menu = menu;

        setTitle("Cliente Word2Vec - calculo remoto en servidor");
        setSize(880, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        listener = new TCPClient50.OnMessageReceived() {
            @Override
            public void messageReceived(String message) {
                if (message.startsWith("W2V_RESULT|") || message.startsWith("W2V_NEAREST_RESULT|") || message.startsWith("ERROR|")) {
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
        JLabel titulo = new JLabel("Modelo Word2Vec - Cliente", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 26));
        titulo.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        add(titulo, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new BorderLayout(10, 10));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        JPanel panelSuperior = new JPanel(new GridLayout(2, 1, 10, 10));

        JPanel panelAnalogia = new JPanel(new BorderLayout(10, 10));
        panelAnalogia.setBorder(BorderFactory.createTitledBorder("Analogia: A - B + C"));

        JPanel panelCampos = new JPanel(new GridLayout(2, 3, 10, 8));
        panelCampos.add(new JLabel("A", SwingConstants.CENTER));
        panelCampos.add(new JLabel("B", SwingConstants.CENTER));
        panelCampos.add(new JLabel("C", SwingConstants.CENTER));

        palabraAField = new JTextField();
        palabraBField = new JTextField();
        palabraCField = new JTextField();
        palabraAField.setFont(new Font("Arial", Font.PLAIN, 16));
        palabraBField.setFont(new Font("Arial", Font.PLAIN, 16));
        palabraCField.setFont(new Font("Arial", Font.PLAIN, 16));

        panelCampos.add(palabraAField);
        panelCampos.add(palabraBField);
        panelCampos.add(palabraCField);

        JButton btnResolver = new JButton("Enviar analogia al servidor");
        btnResolver.addActionListener(e -> resolverAnalogia());

        JLabel ejemploLabel = new JLabel("Ejemplo: rey - hombre + mujer ≈ reina", SwingConstants.CENTER);
        ejemploLabel.setFont(new Font("Arial", Font.ITALIC, 14));

        panelAnalogia.add(panelCampos, BorderLayout.CENTER);
        panelAnalogia.add(btnResolver, BorderLayout.EAST);
        panelAnalogia.add(ejemploLabel, BorderLayout.SOUTH);

        JPanel panelNearest = new JPanel(new BorderLayout(10, 10));
        panelNearest.setBorder(BorderFactory.createTitledBorder("Palabras mas cercanas"));
        palabraNearestField = new JTextField();
        palabraNearestField.setFont(new Font("Arial", Font.PLAIN, 16));
        JButton btnNearest = new JButton("Buscar cercanas en servidor");
        btnNearest.addActionListener(e -> buscarCercanas());
        panelNearest.add(new JLabel("Palabra: "), BorderLayout.WEST);
        panelNearest.add(palabraNearestField, BorderLayout.CENTER);
        panelNearest.add(btnNearest, BorderLayout.EAST);

        panelSuperior.add(panelAnalogia);
        panelSuperior.add(panelNearest);

        panelCentro.add(panelSuperior, BorderLayout.NORTH);

        resultadoArea = new JTextArea();
        resultadoArea.setEditable(false);
        resultadoArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        resultadoArea.setText("Resultado pendiente.");
        panelCentro.add(new JScrollPane(resultadoArea), BorderLayout.CENTER);

        estadoLabel = new JLabel("Estado: esperando consulta", SwingConstants.CENTER);
        estadoLabel.setOpaque(true);
        estadoLabel.setBackground(new Color(240, 240, 240));
        estadoLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelCentro.add(estadoLabel, BorderLayout.SOUTH);

        add(panelCentro, BorderLayout.CENTER);

        JButton btnLimpiar = new JButton("Limpiar");
        JButton btnMenu = new JButton("Volver al menu principal");
        JButton btnSalir = new JButton("Salir");

        btnLimpiar.addActionListener(e -> limpiar());
        btnMenu.addActionListener(e -> dispose());
        btnSalir.addActionListener(e -> salirDelSistema());

        JPanel panelBotones = new JPanel(new GridLayout(1, 3, 10, 10));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(10, 25, 20, 25));
        panelBotones.add(btnLimpiar);
        panelBotones.add(btnMenu);
        panelBotones.add(btnSalir);

        add(panelBotones, BorderLayout.SOUTH);
    }

    private void resolverAnalogia() {
        if (!tcpClient.isConnected()) {
            JOptionPane.showMessageDialog(this,
                    "No hay conexion activa con el servidor.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String a = palabraAField.getText().trim();
        String b = palabraBField.getText().trim();
        String c = palabraCField.getText().trim();

        if (a.isEmpty() || b.isEmpty() || c.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debes ingresar A, B y C.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        estadoLabel.setText("Estado: consultando analogia en servidor...");
        resultadoArea.setText("Esperando respuesta del servidor Word2Vec...");
        tcpClient.sendMessage("W2V_ANALOGY|" + safe(a) + "|" + safe(b) + "|" + safe(c) + "|10");
    }

    private void buscarCercanas() {
        if (!tcpClient.isConnected()) {
            JOptionPane.showMessageDialog(this,
                    "No hay conexion activa con el servidor.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String palabra = palabraNearestField.getText().trim();
        if (palabra.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debes ingresar una palabra.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        estadoLabel.setText("Estado: buscando palabras cercanas en servidor...");
        resultadoArea.setText("Esperando respuesta del servidor Word2Vec...");
        tcpClient.sendMessage("W2V_NEAREST|" + safe(palabra) + "|10");
    }

    private void procesarRespuestaServidor(String message) {
        if (message.startsWith("ERROR|")) {
            String[] parts = message.split("\\|", 2);
            String error = parts.length >= 2 ? parts[1] : "Error desconocido";
            resultadoArea.setText("Error recibido del servidor:\n\n" + error);
            estadoLabel.setText("Estado: error");
            return;
        }

        String[] parts = message.split("\\|", 4);
        if (parts.length < 4) {
            return;
        }

        String tipo = parts[0];
        String consulta = parts[1];
        String ranking = parts[2];
        String tiempoMs = parts[3];

        StringBuilder sb = new StringBuilder();

        if ("W2V_RESULT".equals(tipo)) {
            sb.append("Analogia calculada en el servidor:\n\n");
            sb.append(consulta).append(" ≈ ?\n\n");
        } else {
            sb.append("Palabras mas cercanas calculadas en el servidor:\n\n");
            sb.append("Palabra consultada: ").append(consulta).append("\n\n");
        }

        if (ranking == null || ranking.trim().isEmpty()) {
            sb.append("No se encontraron resultados.\n");
        } else {
            String[] items = ranking.split(",");
            for (int i = 0; i < items.length; i++) {
                String[] kv = items[i].split(":", 2);
                String palabra = kv.length >= 1 ? kv[0] : "";
                String sim = kv.length >= 2 ? kv[1] : "";
                sb.append(String.format("%2d. %-24s similitud = %s%n", i + 1, palabra, sim));
            }
        }

        sb.append("\nTiempo en servidor: ").append(tiempoMs).append(" ms\n");
        resultadoArea.setText(sb.toString());
        estadoLabel.setText("Estado: respuesta recibida del servidor");
    }

    private String safe(String text) {
        if (text == null) return "";
        return text.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private void limpiar() {
        palabraAField.setText("");
        palabraBField.setText("");
        palabraCField.setText("");
        palabraNearestField.setText("");
        resultadoArea.setText("Resultado pendiente.");
        estadoLabel.setText("Estado: esperando consulta");
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
