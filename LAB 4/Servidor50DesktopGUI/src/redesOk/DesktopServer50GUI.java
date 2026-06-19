package redesOk;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class DesktopServer50GUI extends JFrame {

    private final JTextField txtIp = new JTextField(15);
    private final JTextField txtPort = new JTextField("8189", 5);
    private final JTextField txtNombre = new JTextField("Servidor desktop", 18);
    private final JTextField txtTiendaIp = new JTextField(15);
    private final JTextField txtTiendaPort = new JTextField("8190", 5);
    private final JTextArea txtMensajes = new JTextArea();
    private final JTextField txtMensaje = new JTextField();
    private final JButton btnIniciar = new JButton("Iniciar Servidor");
    private final JButton btnParar = new JButton("Parar Servidor");
    private final JButton btnEnviar = new JButton("Send");
    private final JButton btnAdjuntar = new JButton("Adjuntar");
    private final JButton btnTienda = new JButton("Tienda Virtual");
    private final JButton btnMenu = new JButton("⋮");

    private TCPServer server;
    private volatile boolean serverRunning = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DesktopServer50GUI::mostrarSplashYEntrar);
    }

    private static void mostrarSplashYEntrar() {
        Color blue = new Color(0x0078C8);
        javax.swing.JWindow splash = new javax.swing.JWindow();
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        splash.setContentPane(root);

        JLabel header = new JLabel("Dog Messenger");
        header.setOpaque(true);
        header.setBackground(blue);
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 24f));
        header.setBorder(new EmptyBorder(16, 18, 16, 18));
        root.add(header, BorderLayout.NORTH);

        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        ImageIcon icon = loadDogIcon(280, 280);
        if (icon != null) {
            imageLabel.setIcon(icon);
        } else {
            imageLabel.setText("Dog Messenger");
            imageLabel.setFont(imageLabel.getFont().deriveFont(Font.BOLD, 28f));
        }
        root.add(imageLabel, BorderLayout.CENTER);

        splash.setSize(390, 620);
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);

        javax.swing.Timer timer = new javax.swing.Timer(4000, e -> {
            splash.dispose();
            DesktopServer50GUI ui = new DesktopServer50GUI();
            ui.setVisible(true);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static ImageIcon loadDogIcon(int width, int height) {
        try {
            java.net.URL url = DesktopServer50GUI.class.getResource("/redesOk/dog_shield_crop.png");
            if (url == null) return null;
            ImageIcon original = new ImageIcon(url);
            java.awt.Image scaled = original.getImage().getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ignored) {
            return null;
        }
    }

    public DesktopServer50GUI() {
        setTitle("Dog Messenger - Servidor Desktop");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(660, 780);
        setMinimumSize(new Dimension(540, 650));
        setLocationRelativeTo(null);

        txtIp.setText(obtenerIpLocal());
        txtTiendaIp.setText(txtIp.getText().trim());
        txtIp.setEditable(true);
        txtIp.setToolTipText("IP local visible para los clientes. Si no coincide con la red Wi-Fi, escríbela manualmente.");

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                detenerServidor();
                dispose();
                System.exit(0);
            }
        });

        buildLayout();
        setupListeners();
        estadoInicial();
        agregarMensaje("Presione Iniciar Servidor para levantar el servidor.");
    }

    private void buildLayout() {
        Color background = new Color(0xFFF7FF);
        Color blue = new Color(0x0078C8);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(18, 20, 18, 20));
        root.setBackground(background);
        setContentPane(root);

        JPanel headerArea = new JPanel(new BorderLayout(8, 8));
        headerArea.setOpaque(false);

        JLabel title = new JLabel("Dog Messenger");
        title.setOpaque(true);
        title.setBackground(blue);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel subtitle = new JLabel("Servidor Desktop", SwingConstants.CENTER);
        subtitle.setOpaque(true);
        subtitle.setBackground(blue);
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.BOLD, 16f));
        subtitle.setBorder(new EmptyBorder(8, 10, 8, 10));

        headerArea.add(title, BorderLayout.NORTH);
        headerArea.add(subtitle, BorderLayout.SOUTH);
        root.add(headerArea, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.anchor = GridBagConstraints.CENTER;

        c.gridy = 0;
        c.gridx = 0;
        formPanel.add(new JLabel("IP:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(txtIp, c);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Port:"), c);
        c.gridx = 3;
        formPanel.add(txtPort, c);
        c.gridx = 4;
        btnMenu.setMargin(new Insets(4, 8, 4, 8));
        formPanel.add(btnMenu, c);

        c.gridy = 1;
        c.gridx = 0;
        formPanel.add(new JLabel("Nombre:"), c);
        c.gridx = 1;
        c.gridwidth = 4;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(txtNombre, c);
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;

        c.gridy = 2;
        c.gridx = 0;
        formPanel.add(new JLabel("Tienda IP:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(txtTiendaIp, c);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Tienda Port:"), c);
        c.gridx = 3;
        formPanel.add(txtTiendaPort, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        buttons.setOpaque(false);
        styleButton(btnIniciar, blue);
        styleButton(btnParar, blue);
        styleButton(btnEnviar, blue);
        styleButton(btnAdjuntar, blue);
        styleButton(btnTienda, blue);
        buttons.add(btnIniciar);
        buttons.add(btnParar);
        buttons.add(btnTienda);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(formPanel, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.SOUTH);
        center.add(top, BorderLayout.NORTH);

        txtMensajes.setEditable(false);
        txtMensajes.setLineWrap(true);
        txtMensajes.setWrapStyleWord(true);
        txtMensajes.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        txtMensajes.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(txtMensajes);
        center.add(scroll, BorderLayout.CENTER);

        JPanel sendPanel = new JPanel(new BorderLayout(10, 0));
        sendPanel.setOpaque(false);
        txtMensaje.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        sendPanel.add(txtMensaje, BorderLayout.CENTER);

        JPanel sendButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sendButtons.setOpaque(false);
        sendButtons.add(btnAdjuntar);
        sendButtons.add(btnEnviar);
        sendPanel.add(sendButtons, BorderLayout.EAST);
        center.add(sendPanel, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);
    }

    private void styleButton(JButton button, Color blue) {
        button.setBackground(blue);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
    }

    private void setupListeners() {
        btnIniciar.addActionListener(e -> iniciarServidor());
        btnParar.addActionListener(e -> detenerServidor());
        btnEnviar.addActionListener(e -> servidorEnviaTexto());
        btnAdjuntar.addActionListener(e -> seleccionarArchivo());
        btnTienda.addActionListener(e -> abrirTiendaVirtual());
        txtMensaje.addActionListener(e -> servidorEnviaTexto());
        btnMenu.addActionListener(e -> mostrarOverflowMenu());
    }

    private void mostrarOverflowMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem unirseGrupo = new JMenuItem("Unirse a Grupo de Chat");
        unirseGrupo.setEnabled(serverRunning);
        unirseGrupo.addActionListener(e -> mostrarQrUnirseGrupo());
        menu.add(unirseGrupo);
        menu.show(btnMenu, 0, btnMenu.getHeight());
    }

    private void mostrarQrUnirseGrupo() {
        if (!serverRunning) {
            JOptionPane.showMessageDialog(this, "Primero inicia el servidor.");
            return;
        }
        int port = leerPuerto();
        if (port < 0) {
            JOptionPane.showMessageDialog(this, "Ingrese un puerto válido entre 1 y 65535.");
            return;
        }
        String ip = txtIp.getText().trim();
        mostrarQrDialog("Unirse a Grupo de Chat", "ESCANEA EL QR PARA INGRESO A GRUPO CHAT", ip, port);
    }

    private void mostrarQrDialog(String titulo, String indicacion, String ip, int port) {
        String qrContent = "dogmsg://" + ip + ":" + port;
        BufferedImage image = SimpleQrCodeGenerator.createQrImage(qrContent, 360);

        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel header = new JLabel("Dog Messenger");
        header.setOpaque(true);
        header.setBackground(new Color(0x0078C8));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 24f));
        header.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(8, 16));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel label = new JLabel("<html><b>" + indicacion + "</b><br><br>Datos: " + ip + ":" + port + "</html>");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        body.add(label, BorderLayout.NORTH);

        JLabel qr = new JLabel(new ImageIcon(image));
        qr.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(qr, BorderLayout.CENTER);

        card.add(body, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, card, titulo, JOptionPane.PLAIN_MESSAGE);
    }

    private void iniciarServidor() {
        if (serverRunning) {
            agregarMensaje("El servidor ya está iniciado.");
            return;
        }
        int port = leerPuerto();
        if (port < 0) {
            JOptionPane.showMessageDialog(this, "Ingrese un puerto válido entre 1 y 65535.");
            return;
        }

        server = new TCPServer(port, new TCPServer.Listener() {
            @Override
            public void onInfo(String text) {
                SwingUtilities.invokeLater(() -> agregarMensaje(text));
            }

            @Override
            public void onRawMessageFromClient(String raw) {
                SwingUtilities.invokeLater(() -> procesarMensajeEntrante(raw));
            }

            @Override
            public void onStopped() {
                SwingUtilities.invokeLater(() -> {
                    serverRunning = false;
                    estadoInicial();
                });
            }
        });

        serverRunning = true;
        estadoServidorActivo();
        agregarMensaje("Iniciando servidor en " + txtIp.getText().trim() + ":" + port + "...");
        server.start();
    }

    private void detenerServidor() {
        if (server != null) {
            server.stopServer();
            server = null;
        }
        serverRunning = false;
        estadoInicial();
    }

    private void servidorEnviaTexto() {
        String message = txtMensaje.getText().trim();
        if (message.isEmpty()) return;
        if (!serverRunning || server == null) {
            JOptionPane.showMessageDialog(this, "Primero inicia el servidor.");
            return;
        }
        String raw = ChatProtocol.encodeMessage(nombreActual(), message);
        server.broadcastRaw(raw);
        agregarMensaje(ChatProtocol.display(raw));
        txtMensaje.setText("");
    }

    private void seleccionarArchivo() {
        if (!serverRunning || server == null) {
            JOptionPane.showMessageDialog(this, "Primero inicia el servidor.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo adjunto");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null || mimeType.isBlank()) mimeType = "application/octet-stream";
            String raw = ChatProtocol.encodeFile(nombreActual(), file.getName(), mimeType, bytes);
            server.broadcastRaw(raw);
            agregarMensaje(ChatProtocol.display(raw));
            agregarMensaje("Archivo enviado: " + file.getName());
        } catch (Exception ex) {
            agregarMensaje("Error enviando archivo: " + ex.getMessage());
        }
    }

    private void procesarMensajeEntrante(String raw) {
        ChatProtocol.ChatPacket packet = ChatProtocol.parse(raw);
        String visible = ChatProtocol.display(raw);
        if (!visible.isBlank()) agregarMensaje(visible);
        if ("conectado".equalsIgnoreCase(packet.text)) enviarConfiguracionTienda();
        if (packet.isFile()) guardarArchivoRecibido(packet);
    }

    private void enviarConfiguracionTienda() {
        if (server == null || !serverRunning) return;
        String ip = txtTiendaIp.getText().trim().isBlank() ? txtIp.getText().trim() : txtTiendaIp.getText().trim();
        int port = leerTiendaPort();
        if (!ip.isBlank() && port > 0) server.broadcastRaw(ChatProtocol.encodeStoreConfig(ip, port));
    }

    private void abrirTiendaVirtual() {
        String ipTienda = txtTiendaIp.getText().trim().isBlank() ? txtIp.getText().trim() : txtTiendaIp.getText().trim();
        int portTienda = leerTiendaPort();
        if (ipTienda.isBlank() || portTienda < 0) {
            JOptionPane.showMessageDialog(this, "Ingrese IP y puerto de Server50Tienda.");
            return;
        }
        boolean estabaActivo = serverRunning;
        detenerServidor();
        TiendaClientWindow tienda = new TiendaClientWindow(this, ipTienda, portTienda, nombreActual(), () -> {
            if (estabaActivo) iniciarServidor();
        });
        tienda.setVisible(true);
    }

    private void guardarArchivoRecibido(ChatProtocol.ChatPacket packet) {
        try {
            File dir = new File(System.getProperty("user.home"), "DogMessengerRecibidos");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, packet.safeFileName());
            Files.write(out.toPath(), packet.bytes);
            agregarMensaje("Archivo guardado en: " + out.getAbsolutePath());
        } catch (Exception ex) {
            agregarMensaje("No se pudo guardar el archivo recibido: " + ex.getMessage());
        }
    }

    private void estadoInicial() {
        btnIniciar.setEnabled(true);
        btnParar.setEnabled(false);
        btnEnviar.setEnabled(false);
        btnAdjuntar.setEnabled(false);
        txtPort.setEditable(true);
    }

    private void estadoServidorActivo() {
        btnIniciar.setEnabled(false);
        btnParar.setEnabled(true);
        btnEnviar.setEnabled(true);
        btnAdjuntar.setEnabled(true);
        txtPort.setEditable(false);
    }

    private int leerPuerto() {
        try {
            int port = Integer.parseInt(txtPort.getText().trim());
            return (port >= 1 && port <= 65535) ? port : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int leerTiendaPort() {
        try {
            int port = Integer.parseInt(txtTiendaPort.getText().trim());
            return (port >= 1 && port <= 65535) ? port : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String nombreActual() {
        String value = txtNombre.getText().trim();
        return value.isEmpty() ? "Servidor desktop" : value;
    }

    private void agregarMensaje(String text) {
        txtMensajes.append(text + "\n");
        txtMensajes.setCaretPosition(txtMensajes.getDocument().getLength());
    }

    private String obtenerIpLocal() {
        String bestIp = "127.0.0.1";
        int bestScore = Integer.MIN_VALUE;

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;

                String adapterName = ((ni.getName() == null ? "" : ni.getName()) + " " +
                        (ni.getDisplayName() == null ? "" : ni.getDisplayName())).toLowerCase(Locale.ROOT);

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLoopbackAddress() || !(address instanceof Inet4Address)) continue;

                    String ip = address.getHostAddress();
                    int score = 0;

                    if (ip.startsWith("192.168.")) score += 300;
                    if (ip.startsWith("10.")) score += 80;
                    if (ip.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) score += 70;
                    if (ip.startsWith("169.254.")) score -= 200;

                    // Penaliza adaptadores virtuales que normalmente no son alcanzables desde el celular.
                    if (ip.startsWith("192.168.56.")) score -= 250; // VirtualBox Host-Only
                    if (ip.startsWith("172.30.")) score -= 200;     // WSL/Hyper-V frecuente

                    if (adapterName.contains("wi-fi") || adapterName.contains("wifi") ||
                            adapterName.contains("wlan") || adapterName.contains("wireless")) score += 120;
                    if (adapterName.contains("ethernet")) score += 20;

                    if (adapterName.contains("virtual") || adapterName.contains("virtualbox") ||
                            adapterName.contains("vbox") || adapterName.contains("vmware") ||
                            adapterName.contains("hyper-v") || adapterName.contains("wsl") ||
                            adapterName.contains("vethernet") || adapterName.contains("docker") ||
                            adapterName.contains("bluetooth") || adapterName.contains("tap") ||
                            adapterName.contains("tunnel")) score -= 300;

                    try { if (ni.isVirtual()) score -= 200; } catch (Exception ignored) {}
                    try { if (ni.isPointToPoint()) score -= 100; } catch (Exception ignored) {}

                    if (score > bestScore) {
                        bestScore = score;
                        bestIp = ip;
                    }
                }
            }
        } catch (Exception ignored) {}

        return bestIp;
    }

    private static class TCPServer {
        interface Listener {
            void onInfo(String text);
            void onRawMessageFromClient(String raw);
            void onStopped();
        }

        private final int port;
        private final Listener listener;
        private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean running;
        private ServerSocket serverSocket;
        private Thread serverThread;
        private int nextClientId = 1;

        TCPServer(int port, Listener listener) {
            this.port = port;
            this.listener = listener;
        }

        void start() {
            serverThread = new Thread(this::run, "TCPServer50-Desktop");
            serverThread.setDaemon(true);
            serverThread.start();
        }

        private void run() {
            running = true;
            try (ServerSocket ss = new ServerSocket(port)) {
                serverSocket = ss;
                info("Servidor iniciado.");
                while (running) {
                    Socket socket = ss.accept();
                    int id;
                    synchronized (this) { id = nextClientId++; }
                    ClientHandler handler = new ClientHandler(id, socket, this);
                    clients.add(handler);
                    Thread thread = new Thread(handler, "ClientHandler-" + id);
                    thread.setDaemon(true);
                    thread.start();
                }
            } catch (SocketException e) {
                if (running) info("Error de socket: " + e.getMessage());
            } catch (Exception e) {
                if (running) info("Error del servidor: " + e.getMessage());
            } finally {
                running = false;
                closeAllClients();
                if (listener != null) listener.onStopped();
            }
        }

        void broadcastRaw(String raw) {
            List<ClientHandler> snapshot;
            synchronized (clients) { snapshot = new ArrayList<>(clients); }
            for (ClientHandler client : snapshot) {
                client.sendRaw(raw);
            }
        }

        void onClientRawMessage(ClientHandler sender, String raw) {
            if (listener != null) listener.onRawMessageFromClient(raw);
            broadcastRaw(raw);
        }

        void removeClient(ClientHandler client) {
            clients.remove(client);
        }

        void stopServer() {
            running = false;
            try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
            closeAllClients();
        }

        private void closeAllClients() {
            List<ClientHandler> snapshot;
            synchronized (clients) { snapshot = new ArrayList<>(clients); }
            for (ClientHandler client : snapshot) client.stopClient();
            clients.clear();
        }

        private void info(String text) {
            if (listener != null) listener.onInfo(text);
        }
    }

    private static class ClientHandler implements Runnable {
        private final int clientId;
        private final Socket socket;
        private final TCPServer server;
        private volatile boolean running;
        private PrintWriter out;
        private BufferedReader in;

        ClientHandler(int clientId, Socket socket, TCPServer server) {
            this.clientId = clientId;
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            running = true;
            try {
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while (running && (line = in.readLine()) != null) {
                    if (!line.isBlank()) server.onClientRawMessage(this, line);
                }
            } catch (Exception ignored) {
            } finally {
                stopClient();
                server.removeClient(this);
            }
        }

        void sendRaw(String raw) {
            try {
                PrintWriter writer = out;
                if (writer != null && !writer.checkError()) {
                    writer.println(raw);
                    writer.flush();
                }
            } catch (Exception ignored) {}
        }

        void stopClient() {
            running = false;
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private static class ChatProtocol {
        static String encodeStoreConfig(String ip, int port) {
            return "STORECFG|" + enc(ip == null ? "" : ip.trim()) + "|" + port;
        }

        static class ChatPacket {
            final String type;
            final String sender;
            final String text;
            final String fileName;
            final String mimeType;
            final byte[] bytes;

            ChatPacket(String type, String sender, String text, String fileName, String mimeType, byte[] bytes) {
                this.type = type;
                this.sender = sender;
                this.text = text;
                this.fileName = fileName;
                this.mimeType = mimeType;
                this.bytes = bytes;
            }

            boolean isFile() {
                return "FILE".equals(type) && bytes != null;
            }

            String safeFileName() {
                String value = fileName == null || fileName.isBlank() ? "archivo_adjunto" : fileName;
                return value.replaceAll("[\\\\/:*?\"<>|]", "_");
            }
        }

        static String encodeMessage(String sender, String text) {
            return "MSG|" + enc(blank(sender, "Sin nombre")) + "|" + enc(text == null ? "" : text);
        }

        static String encodeFile(String sender, String fileName, String mimeType, byte[] bytes) {
            return "FILE|" + enc(blank(sender, "Sin nombre")) + "|" + enc(blank(fileName, "archivo")) + "|" + enc(blank(mimeType, "application/octet-stream")) + "|" + Base64.getEncoder().encodeToString(bytes);
        }

        static ChatPacket parse(String raw) {
            try {
                String[] parts = raw.split("\\|", 5);
                if (parts.length >= 3 && "MSG".equals(parts[0])) {
                    return new ChatPacket("MSG", dec(parts[1]), dec(parts[2]), "", "", null);
                }
                if (parts.length >= 5 && "FILE".equals(parts[0])) {
                    return new ChatPacket("FILE", dec(parts[1]), "", dec(parts[2]), dec(parts[3]), Base64.getDecoder().decode(parts[4]));
                }
                if (parts.length >= 3 && "STORECFG".equals(parts[0])) {
                    return new ChatPacket("STORECFG", "Sistema", "", "", "", null);
                }
            } catch (Exception ignored) {}
            return new ChatPacket("MSG", "Sistema", raw, "", "", null);
        }

        static String display(String raw) {
            ChatPacket packet = parse(raw);
            String sender = bracketName(packet.sender);
            if ("STORECFG".equals(packet.type)) return "";
            if (packet.isFile()) {
                return sender + " [Archivo adjunto] " + packet.fileName + " (" + formatBytes(packet.bytes.length) + ")";
            }
            String text = packet.text == null ? "" : packet.text.trim();
            if ("conectado".equalsIgnoreCase(text)) return sender + " conectado";
            if ("desconectado".equalsIgnoreCase(text)) return sender + " desconectado";
            return sender + ": " + packet.text;
        }

        private static String bracketName(String name) {
            String clean = name == null || name.isBlank() ? "Sin nombre" : name.trim();
            return clean.startsWith("[") && clean.endsWith("]") ? clean : "[" + clean + "]";
        }

        private static String blank(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }

        private static String formatBytes(int bytes) {
            if (bytes < 1024) return bytes + " B";
            double kb = bytes / 1024.0;
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
            double mb = kb / 1024.0;
            return String.format(Locale.US, "%.1f MB", mb);
        }

        private static String enc(String value) {
            try { return URLEncoder.encode(value, "UTF-8"); } catch (Exception e) { return value; }
        }

        private static String dec(String value) {
            try { return URLDecoder.decode(value, "UTF-8"); } catch (Exception e) { return value; }
        }
    }

    private static class SimpleQrCodeGenerator {
        private static final int VERSION = 3;
        private static final int QR_SIZE = 21 + 4 * (VERSION - 1);
        private static final int DATA_CODEWORDS = 55;
        private static final int ECC_CODEWORDS = 15;
        private static final int BORDER_MODULES = 4;

        static BufferedImage createQrImage(String text, int bitmapSize) {
            String safeText = text;
            byte[] bytes = safeText.getBytes(StandardCharsets.ISO_8859_1);
            if (bytes.length > 53) {
                safeText = safeText.substring(0, Math.min(safeText.length(), 53));
            }

            boolean[][] modules = encode(safeText);
            int totalModules = QR_SIZE + BORDER_MODULES * 2;
            int scale = Math.max(1, bitmapSize / totalModules);
            int finalSize = totalModules * scale;

            BufferedImage image = new BufferedImage(finalSize, finalSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, finalSize, finalSize);
            g.setColor(Color.BLACK);

            for (int y = 0; y < QR_SIZE; y++) {
                for (int x = 0; x < QR_SIZE; x++) {
                    if (modules[y][x]) {
                        int left = (x + BORDER_MODULES) * scale;
                        int top = (y + BORDER_MODULES) * scale;
                        g.fillRect(left, top, scale, scale);
                    }
                }
            }
            g.dispose();
            return image;
        }

        private static boolean[][] encode(String text) {
            boolean[][] modules = new boolean[QR_SIZE][QR_SIZE];
            boolean[][] isFunction = new boolean[QR_SIZE][QR_SIZE];

            drawFinderPattern(modules, isFunction, 3, 3);
            drawFinderPattern(modules, isFunction, QR_SIZE - 4, 3);
            drawFinderPattern(modules, isFunction, 3, QR_SIZE - 4);

            for (int i = 0; i < QR_SIZE; i++) {
                if (!isFunction[6][i]) setModule(modules, isFunction, i, 6, i % 2 == 0, true);
                if (!isFunction[i][6]) setModule(modules, isFunction, 6, i, i % 2 == 0, true);
            }

            drawAlignmentPattern(modules, isFunction, 22, 22);
            drawFormatBits(modules, isFunction);

            int[] codewords = createCodewords(text);
            int bitIndex = 0;
            int right = QR_SIZE - 1;

            while (right >= 1) {
                if (right == 6) right--;
                boolean upward = (((right + 1) & 2) == 0);

                for (int vertical = 0; vertical < QR_SIZE; vertical++) {
                    int y = upward ? QR_SIZE - 1 - vertical : vertical;
                    for (int j = 0; j <= 1; j++) {
                        int x = right - j;
                        if (!isFunction[y][x] && bitIndex < codewords.length * 8) {
                            modules[y][x] = (((codewords[bitIndex / 8] >>> (7 - bitIndex % 8)) & 1) != 0);
                            bitIndex++;
                        }
                    }
                }
                right -= 2;
            }

            applyMask0(modules, isFunction);
            return modules;
        }

        private static void setModule(boolean[][] modules, boolean[][] isFunction, int x, int y, boolean value, boolean function) {
            modules[y][x] = value;
            if (function) isFunction[y][x] = true;
        }

        private static void drawFinderPattern(boolean[][] modules, boolean[][] isFunction, int centerX, int centerY) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dx = -4; dx <= 4; dx++) {
                    int x = centerX + dx;
                    int y = centerY + dy;
                    if (x >= 0 && x < QR_SIZE && y >= 0 && y < QR_SIZE) {
                        int distance = Math.max(Math.abs(dx), Math.abs(dy));
                        setModule(modules, isFunction, x, y, distance != 2 && distance != 4, true);
                    }
                }
            }
        }

        private static void drawAlignmentPattern(boolean[][] modules, boolean[][] isFunction, int centerX, int centerY) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    int x = centerX + dx;
                    int y = centerY + dy;
                    int distance = Math.max(Math.abs(dx), Math.abs(dy));
                    setModule(modules, isFunction, x, y, distance != 1, true);
                }
            }
        }

        private static int[] createCodewords(String text) {
            byte[] textBytes = text.getBytes(StandardCharsets.ISO_8859_1);
            List<Integer> bits = new ArrayList<>();

            appendBits(bits, 0x4, 4);
            appendBits(bits, textBytes.length, 8);

            for (byte b : textBytes) {
                appendBits(bits, b & 0xFF, 8);
            }

            int capacityBits = DATA_CODEWORDS * 8;
            for (int i = 0; i < Math.min(4, capacityBits - bits.size()); i++) bits.add(0);
            while (bits.size() % 8 != 0) bits.add(0);

            List<Integer> dataCodewords = new ArrayList<>();
            for (int i = 0; i < bits.size(); i += 8) {
                int value = 0;
                for (int j = 0; j < 8; j++) value = (value << 1) | bits.get(i + j);
                dataCodewords.add(value);
            }

            int[] padBytes = {0xEC, 0x11};
            int padIndex = 0;
            while (dataCodewords.size() < DATA_CODEWORDS) {
                dataCodewords.add(padBytes[padIndex % 2]);
                padIndex++;
            }

            int[] data = dataCodewords.stream().mapToInt(Integer::intValue).toArray();
            int[] ecc = reedSolomonRemainder(data, reedSolomonDivisor(ECC_CODEWORDS));
            int[] result = new int[data.length + ecc.length];
            System.arraycopy(data, 0, result, 0, data.length);
            System.arraycopy(ecc, 0, result, data.length, ecc.length);
            return result;
        }

        private static void appendBits(List<Integer> bits, int value, int count) {
            for (int i = count - 1; i >= 0; i--) {
                bits.add((value >>> i) & 1);
            }
        }

        private static void drawFormatBits(boolean[][] modules, boolean[][] isFunction) {
            int bits = formatBits(0);

            for (int i = 0; i <= 5; i++) setFunction(modules, isFunction, 8, i, getBit(bits, i));
            setFunction(modules, isFunction, 8, 7, getBit(bits, 6));
            setFunction(modules, isFunction, 8, 8, getBit(bits, 7));
            setFunction(modules, isFunction, 7, 8, getBit(bits, 8));
            for (int i = 9; i <= 14; i++) setFunction(modules, isFunction, 14 - i, 8, getBit(bits, i));
            for (int i = 0; i <= 7; i++) setFunction(modules, isFunction, QR_SIZE - 1 - i, 8, getBit(bits, i));
            for (int i = 8; i <= 14; i++) setFunction(modules, isFunction, 8, QR_SIZE - 15 + i, getBit(bits, i));
            setFunction(modules, isFunction, 8, QR_SIZE - 8, true);
        }

        private static boolean getBit(int value, int index) {
            return ((value >>> index) & 1) != 0;
        }

        private static void setFunction(boolean[][] modules, boolean[][] isFunction, int x, int y, boolean value) {
            modules[y][x] = value;
            isFunction[y][x] = true;
        }

        private static int formatBits(int mask) {
            int data = (1 << 3) | mask;
            int remainder = data;
            for (int i = 0; i < 10; i++) {
                remainder <<= 1;
                if ((remainder & (1 << 10)) != 0) remainder ^= 0x537;
            }
            return ((data << 10) | (remainder & 0x3FF)) ^ 0x5412;
        }

        private static void applyMask0(boolean[][] modules, boolean[][] isFunction) {
            for (int y = 0; y < QR_SIZE; y++) {
                for (int x = 0; x < QR_SIZE; x++) {
                    if (!isFunction[y][x] && (x + y) % 2 == 0) {
                        modules[y][x] = !modules[y][x];
                    }
                }
            }
        }

        private static int[] reedSolomonDivisor(int degree) {
            int[] result = new int[degree];
            result[degree - 1] = 1;
            int root = 1;
            for (int i = 0; i < degree; i++) {
                for (int j = 0; j < degree; j++) {
                    result[j] = gfMultiply(result[j], root);
                    if (j + 1 < degree) result[j] ^= result[j + 1];
                }
                root = gfMultiply(root, 0x02);
            }
            return result;
        }

        private static int[] reedSolomonRemainder(int[] data, int[] divisor) {
            int[] result = new int[divisor.length];
            for (int b : data) {
                int factor = b ^ result[0];
                for (int i = 0; i < result.length - 1; i++) result[i] = result[i + 1];
                result[result.length - 1] = 0;
                for (int i = 0; i < result.length; i++) result[i] ^= gfMultiply(divisor[i], factor);
            }
            return result;
        }

        private static int gfMultiply(int xInput, int yInput) {
            int x = xInput;
            int y = yInput;
            int result = 0;
            while (y != 0) {
                if ((y & 1) != 0) result ^= x;
                x <<= 1;
                if ((x & 0x100) != 0) x ^= 0x11D;
                y >>>= 1;
            }
            return result & 0xFF;
        }
    }
}
