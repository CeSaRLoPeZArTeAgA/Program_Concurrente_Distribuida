package redesOk;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

// SERVIDOR MICRO CHUNK
// Toda la logica de IA esta aqui:
// - Carga modelo_mnist.bin
// - Carga word2vec_model.bin
// - Atiende varios clientes simultaneamente usando TCPServer50 + TCPServerThread50
// - Cada cliente tiene su propio hilo y recibe solo su respuesta
public class Servidor50 extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final String MNIST_MODEL_PATH = "modelo_mnist.bin";
    private static final String WORD2VEC_MODEL_PATH = "word2vec_model.bin";

    private final int port;
    private TCPServer50 mTcpServer;

    private MLP modeloMNIST;
    private Word2VecModel modeloWord2Vec;

    private JTextArea logArea;
    private JLabel estadoLabel;
    private JLabel clientesLabel;

    public static void main(String[] args) {
        int port = TCPServer50.DEFAULT_PORT;

        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Puerto invalido. Usando puerto por defecto: " + TCPServer50.DEFAULT_PORT);
            }
        }

        final int finalPort = port;
        SwingUtilities.invokeLater(() -> {
            Servidor50 server = new Servidor50(finalPort);
            server.setVisible(true);
            server.iniciar();
        });
    }

    public Servidor50(int port) {
        this.port = port;
        crearInterfaz();
    }

    private void crearInterfaz() {
        setTitle("Servidor MICRO CHUNK - IA centralizada - Puerto " + port);
        setSize(850, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel titulo = new JLabel("SERVIDOR MICRO CHUNK - MNIST + Word2Vec", JLabel.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        add(titulo, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Logs del servidor"));
        add(scroll, BorderLayout.CENTER);

        JPanel panelEstado = new JPanel(new GridLayout(1, 3, 10, 10));
        panelEstado.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        estadoLabel = new JLabel("Estado: iniciando");
        clientesLabel = new JLabel("Clientes conectados: 0");
        JLabel puertoLabel = new JLabel("Puerto: " + port);
        panelEstado.add(estadoLabel);
        panelEstado.add(clientesLabel);
        panelEstado.add(puertoLabel);
        add(panelEstado, BorderLayout.SOUTH);

        JButton btnLimpiar = new JButton("Limpiar logs");
        JButton btnDetener = new JButton("Detener servidor");
        JPanel panelBotones = new JPanel(new GridLayout(1, 2, 10, 10));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15));
        panelBotones.add(btnLimpiar);
        panelBotones.add(btnDetener);

        btnLimpiar.addActionListener(e -> logArea.setText(""));
        btnDetener.addActionListener(e -> detenerServidor());

        add(panelBotones, BorderLayout.EAST);
    }

    public void iniciar() {
        cargarModelosIA();

        mTcpServer = new TCPServer50(port, new TCPServer50.OnMessageReceived() {
            @Override
            public void messageReceived(int clientId, String message) {
                ServidorRecibe(clientId, message);
            }

            @Override
            public void clientConnected(int clientId) {
                SwingUtilities.invokeLater(() -> {
                    log("C" + clientId + " conectado.");
                    actualizarClientes();
                });
                mTcpServer.sendToClient(clientId, "WELCOME|" + clientId + "|Conectado al servidor MICRO CHUNK");
            }

            @Override
            public void clientDisconnected(int clientId) {
                SwingUtilities.invokeLater(() -> {
                    log("C" + clientId + " desconectado.");
                    actualizarClientes();
                });
            }

            @Override
            public void serverLog(String message) {
                SwingUtilities.invokeLater(() -> log(message));
            }
        });

        Thread serverThread = new Thread(() -> mTcpServer.run(), "Servidor-TCP-MicroChunk");
        serverThread.start();

        estadoLabel.setText("Estado: escuchando");
        log("Servidor iniciado en puerto " + port + ".");
        log("Protocolo disponible: CLIENT_HELLO, MNIST_PREDICT, W2V_ANALOGY, W2V_NEAREST.");
    }

    private void cargarModelosIA() {
        try {
            modeloMNIST = new MLP();
            modeloMNIST.loadWeights(MNIST_MODEL_PATH);
            log("Modelo MNIST cargado desde: " + MNIST_MODEL_PATH);
        } catch (Exception e) {
            modeloMNIST = null;
            log("ERROR cargando modelo MNIST: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "No se pudo cargar " + MNIST_MODEL_PATH + "\n" + e.getMessage(),
                    "Error MNIST",
                    JOptionPane.ERROR_MESSAGE);
        }

        try {
            modeloWord2Vec = Word2VecModel.load(WORD2VEC_MODEL_PATH);
            log("Modelo Word2Vec cargado desde: " + WORD2VEC_MODEL_PATH
                    + " | vocabulario = " + modeloWord2Vec.getVocabularySize()
                    + " | dimension = " + modeloWord2Vec.getDimension());
        } catch (Exception e) {
            modeloWord2Vec = null;
            log("ERROR cargando modelo Word2Vec: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "No se pudo cargar " + WORD2VEC_MODEL_PATH + "\n" + e.getMessage(),
                    "Error Word2Vec",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    //recibe mensajes de cualquier cliente
    void ServidorRecibe(int clientId, String llego) {
        log("C" + clientId + " -> " + limitar(llego, 180));

        try {
            String[] parts = llego.split("\\|", 6);
            String command = parts[0].trim();

            switch (command) {
                case "CLIENT_HELLO":
                    procesarHello(clientId, parts);
                    break;

                case "MNIST_PREDICT":
                    procesarMNIST(clientId, parts);
                    break;

                case "W2V_ANALOGY":
                    procesarAnalogia(clientId, parts);
                    break;

                case "W2V_NEAREST":
                    procesarCercanas(clientId, parts);
                    break;

                default:
                    enviarError(clientId, "Comando no reconocido: " + command);
                    break;
            }
        } catch (Exception e) {
            enviarError(clientId, e.getMessage());
        }
    }

    void ServidorEnvia(String envia) {
        if (mTcpServer != null) {
            mTcpServer.sendMessageTCPServer(envia);
        }
    }

    private void procesarHello(int clientId, String[] parts) {
        String nombre = parts.length >= 2 ? safe(parts[1]) : "Cliente " + clientId;
        log("C" + clientId + " se identifico como: " + nombre);
        mTcpServer.sendToClient(clientId, "INFO|Servidor registro a C" + clientId + " como " + nombre);
    }

    private void procesarMNIST(int clientId, String[] parts) {
        if (modeloMNIST == null) {
            enviarError(clientId, "El modelo MNIST no esta cargado en el servidor.");
            return;
        }
        if (parts.length < 2) {
            enviarError(clientId, "Formato MNIST invalido. Use MNIST_PREDICT|x1,x2,...,x784");
            return;
        }

        double[] x = parseVector(parts[1], 784);

        long inicio = System.nanoTime();
        int prediccion;
        double[] probs;

        synchronized (modeloMNIST) {
            prediccion = modeloMNIST.predict(x);
            probs = modeloMNIST.predictProbs(x);
        }

        long fin = System.nanoTime();
        double tiempoMs = (fin - inicio) / 1_000_000.0;

        String response = "MNIST_RESULT|" + prediccion + "|" + joinDouble(probs) + "|" + String.format(java.util.Locale.US, "%.6f", tiempoMs);
        mTcpServer.sendToClient(clientId, response);
        log("C" + clientId + " <- MNIST_RESULT prediccion=" + prediccion + " tiempo=" + String.format(java.util.Locale.US, "%.6f", tiempoMs) + " ms");
    }

    private void procesarAnalogia(int clientId, String[] parts) {
        if (modeloWord2Vec == null) {
            enviarError(clientId, "El modelo Word2Vec no esta cargado en el servidor.");
            return;
        }
        if (parts.length < 4) {
            enviarError(clientId, "Formato W2V_ANALOGY invalido. Use W2V_ANALOGY|A|B|C|topK");
            return;
        }

        String a = parts[1].trim();
        String b = parts[2].trim();
        String c = parts[3].trim();
        int topK = 10;
        if (parts.length >= 5) {
            topK = parseInt(parts[4], 10);
        }
        topK = Math.max(1, Math.min(30, topK));

        long inicio = System.nanoTime();
        List<Word2VecModel.SimilarWord> resultados;

        synchronized (modeloWord2Vec) {
            resultados = modeloWord2Vec.analogy(a, b, c, topK);
        }

        long fin = System.nanoTime();
        double tiempoMs = (fin - inicio) / 1_000_000.0;

        String response = "W2V_RESULT|" + safe(a + " - " + b + " + " + c) + "|" + encodeSimilarWords(resultados)
                + "|" + String.format(java.util.Locale.US, "%.6f", tiempoMs);
        mTcpServer.sendToClient(clientId, response);
        log("C" + clientId + " <- W2V_RESULT analogia=" + a + "-" + b + "+" + c + " tiempo=" + String.format(java.util.Locale.US, "%.6f", tiempoMs) + " ms");
    }

    private void procesarCercanas(int clientId, String[] parts) {
        if (modeloWord2Vec == null) {
            enviarError(clientId, "El modelo Word2Vec no esta cargado en el servidor.");
            return;
        }
        if (parts.length < 2) {
            enviarError(clientId, "Formato W2V_NEAREST invalido. Use W2V_NEAREST|palabra|topK");
            return;
        }

        String palabra = parts[1].trim();
        int topK = parts.length >= 3 ? parseInt(parts[2], 10) : 10;
        topK = Math.max(1, Math.min(30, topK));

        long inicio = System.nanoTime();
        List<Word2VecModel.SimilarWord> resultados;

        synchronized (modeloWord2Vec) {
            resultados = modeloWord2Vec.nearestWords(palabra, topK);
        }

        long fin = System.nanoTime();
        double tiempoMs = (fin - inicio) / 1_000_000.0;

        String response = "W2V_NEAREST_RESULT|" + safe(palabra) + "|" + encodeSimilarWords(resultados)
                + "|" + String.format(java.util.Locale.US, "%.6f", tiempoMs);
        mTcpServer.sendToClient(clientId, response);
        log("C" + clientId + " <- W2V_NEAREST_RESULT palabra=" + palabra + " tiempo=" + String.format(java.util.Locale.US, "%.6f", tiempoMs) + " ms");
    }

    private double[] parseVector(String csv, int expectedSize) {
        String[] values = csv.split(",");
        if (values.length != expectedSize) {
            throw new IllegalArgumentException("Vector invalido. Se esperaban " + expectedSize + " valores y llegaron " + values.length);
        }

        double[] x = new double[expectedSize];
        for (int i = 0; i < expectedSize; i++) {
            x[i] = Double.parseDouble(values[i]);
        }
        return x;
    }

    private String joinDouble(double[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(java.util.Locale.US, "%.10f", values[i]));
        }
        return sb.toString();
    }

    private String encodeSimilarWords(List<Word2VecModel.SimilarWord> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            Word2VecModel.SimilarWord item = items.get(i);
            if (i > 0) sb.append(',');
            sb.append(safe(item.getWord()))
                    .append(':')
                    .append(String.format(java.util.Locale.US, "%.10f", item.getSimilarity()));
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

    private void enviarError(int clientId, String message) {
        String clean = safe(message);
        log("C" + clientId + " <- ERROR " + clean);
        if (mTcpServer != null) {
            mTcpServer.sendToClient(clientId, "ERROR|" + clean);
        }
    }

    private String safe(String text) {
        if (text == null) return "";
        return text.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private String limitar(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max) + " ...";
    }

    private void log(String text) {
        if (logArea == null) {
            System.out.println(text);
            return;
        }
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        logArea.append("[" + time + "] " + text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void actualizarClientes() {
        if (clientesLabel != null && mTcpServer != null) {
            clientesLabel.setText("Clientes conectados: " + mTcpServer.getClientCount());
        }
    }

    private void detenerServidor() {
        int opcion = JOptionPane.showConfirmDialog(this,
                "Deseas detener el servidor?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (opcion == JOptionPane.YES_OPTION) {
            if (mTcpServer != null) {
                mTcpServer.stopServer();
            }
            estadoLabel.setText("Estado: detenido");
            log("Servidor detenido.");
        }
    }
}
