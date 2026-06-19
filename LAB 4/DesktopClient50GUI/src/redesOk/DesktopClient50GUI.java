package redesOk;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class DesktopClient50GUI extends JFrame {

    private final JTextField txtIp = new JTextField("192.168.0.163", 12);
    private final JTextField txtPort = new JTextField("8189", 5);
    private final JTextField txtNombre = new JTextField("Cliente desktop", 14);
    private final JTextArea txtMensajes = new JTextArea();
    private final JTextField txtMensaje = new JTextField();
    private final JButton btnConectar = new JButton("Conectar");
    private final JButton btnDesconectar = new JButton("Desconectar");
    private final JButton btnEnviar = new JButton("Send");
    private final JButton btnAdjuntar = new JButton("Adjuntar");
    private final JButton btnTienda = new JButton("Tienda Virtual");
    private final JButton btnMenu = new JButton("⋮");

    private TCPClient client;
    private volatile boolean connected = false;
    private volatile boolean silentJoinOnNextConnect = false;
    private volatile boolean silentSession = false;
    private volatile String tiendaIp = "";
    private volatile int tiendaPort = 8190;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DesktopClient50GUI::mostrarSplashYEntrar);
    }

    private static void mostrarSplashYEntrar() {
        Color blue = new Color(0x0078C8);
        JWindow splash = new JWindow();
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
            DesktopClient50GUI ui = new DesktopClient50GUI();
            ui.setVisible(true);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static ImageIcon loadDogIcon(int width, int height) {
        try {
            java.net.URL url = DesktopClient50GUI.class.getResource("/redesOk/dog_shield_crop.png");
            if (url == null) return null;
            ImageIcon original = new ImageIcon(url);
            Image scaled = original.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ignored) {
            return null;
        }
    }

    public DesktopClient50GUI() {
        setTitle("Dog Messenger - Cliente Desktop");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(620, 760);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(520, 650));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                desconectar();
                dispose();
                System.exit(0);
            }
        });

        buildLayout();
        setupListeners();
        estadoInicial();
        agregarMensaje("Ingrese nombre, IP y puerto del servidor para conectarse.");
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
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel subtitle = new JLabel("Cliente Desktop", SwingConstants.CENTER);
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
        c.insets = new Insets(4, 4, 4, 4);
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

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        buttons.setOpaque(false);
        styleButton(btnConectar, blue);
        styleButton(btnDesconectar, blue);
        styleButton(btnEnviar, blue);
        styleButton(btnAdjuntar, blue);
        styleButton(btnTienda, blue);
        buttons.add(btnConectar);
        buttons.add(btnDesconectar);
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
        btnConectar.addActionListener(e -> conectar());
        btnDesconectar.addActionListener(e -> desconectar());
        btnEnviar.addActionListener(e -> enviarMensaje());
        btnAdjuntar.addActionListener(e -> seleccionarArchivo());
        btnTienda.addActionListener(e -> abrirTiendaVirtual());
        txtMensaje.addActionListener(e -> enviarMensaje());
        btnMenu.addActionListener(e -> mostrarOverflowMenu());
    }

    private void mostrarOverflowMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem unirseGrupo = new JMenuItem("Unirse a Grupo de Chat");
        unirseGrupo.setEnabled(connected);
        unirseGrupo.addActionListener(e -> mostrarQrUnirseGrupo());

        JMenuItem vincular = new JMenuItem("Vincular Dispositivo");
        vincular.setEnabled(!connected);
        vincular.addActionListener(e -> mostrarDialogoEscanearQr(false, "Vincular dispositivo"));

        JMenuItem scanClonar = new JMenuItem("Scanear p/clonar Dispositivo");
        scanClonar.setEnabled(!connected);
        scanClonar.addActionListener(e -> mostrarDialogoEscanearQr(true, "Scanear p/clonar Dispositivo"));

        JMenuItem qrClonar = new JMenuItem("QR p/clonar Dispositivo");
        qrClonar.setEnabled(connected);
        qrClonar.addActionListener(e -> mostrarQrClonarDispositivo());

        menu.add(unirseGrupo);
        menu.add(vincular);
        menu.add(scanClonar);
        menu.add(qrClonar);
        menu.show(btnMenu, 0, btnMenu.getHeight());
    }

    private void mostrarQrUnirseGrupo() {
        if (!connected) {
            JOptionPane.showMessageDialog(this, "Primero conecta el cliente al servidor.");
            return;
        }
        String ip = txtIp.getText().trim();
        int port = leerPuerto();
        if (ip.isEmpty() || port < 0) {
            JOptionPane.showMessageDialog(this, "Ingrese una IP y un puerto válido.");
            return;
        }
        mostrarQrDialog("Unirse a Grupo de Chat", "Escanea el QR para ingresar al grupo chat", ip, port, false);
    }

    private void mostrarQrClonarDispositivo() {
        if (!connected) {
            JOptionPane.showMessageDialog(this, "Conéctate al servidor para generar el QR de clonación.");
            return;
        }
        String ip = txtIp.getText().trim();
        int port = leerPuerto();
        if (ip.isEmpty() || port < 0) {
            JOptionPane.showMessageDialog(this, "Ingrese una IP y un puerto válido.");
            return;
        }
        mostrarQrDialog("QR p/clonar Dispositivo", "Escanea el QR para clonar este cliente desktop", ip, port, true);
    }

    private void mostrarDialogoEscanearQr(boolean requireClone, String titulo) {
        if (connected) {
            JOptionPane.showMessageDialog(this, "Desconecta este cliente antes de vincular o clonar.");
            return;
        }

        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            JOptionPane.showMessageDialog(this, "No se detectó cámara interna. Se abrirá ingreso manual del QR.");
            mostrarDialogoEscanearQrManual(requireClone, titulo);
            return;
        }

        JDialog dialog = new JDialog(this, titulo, true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(720, 620);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.setBackground(new Color(0xFFF7FF));
        dialog.setContentPane(root);

        JLabel header = new JLabel("Dog Messenger");
        header.setOpaque(true);
        header.setBackground(new Color(0x0078C8));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 24f));
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        root.add(header, BorderLayout.NORTH);

        JLabel preview = new JLabel("Inicializando cámara interna...", SwingConstants.CENTER);
        preview.setOpaque(true);
        preview.setBackground(Color.BLACK);
        preview.setForeground(Color.WHITE);
        preview.setPreferredSize(new Dimension(640, 420));
        root.add(preview, BorderLayout.CENTER);

        JLabel instruction = new JLabel("Coloca el QR frente a la cámara. La conexión será automática al leerlo.");
        JButton btnManual = new JButton("Ingresar QR manual");
        JButton btnCancelar = new JButton("Cancelar");

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);
        bottom.add(instruction, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(btnManual);
        actions.add(btnCancelar);
        bottom.add(actions, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        final javax.swing.Timer[] timerRef = new javax.swing.Timer[1];
        final AtomicBoolean busy = new AtomicBoolean(false);
        final AtomicBoolean closed = new AtomicBoolean(false);

        Runnable cleanup = () -> {
            closed.set(true);
            if (timerRef[0] != null) timerRef[0].stop();
            try {
                if (webcam.isOpen()) webcam.close();
            } catch (Exception ignored) {}
        };

        btnCancelar.addActionListener(e -> {
            cleanup.run();
            dialog.dispose();
        });

        btnManual.addActionListener(e -> {
            cleanup.run();
            dialog.dispose();
            mostrarDialogoEscanearQrManual(requireClone, titulo);
        });

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cleanup.run();
            }
        });

        try {
            try {
                webcam.setViewSize(WebcamResolution.VGA.getSize());
            } catch (Exception ignored) {}
            webcam.open();
        } catch (Exception ex) {
            cleanup.run();
            JOptionPane.showMessageDialog(this, "No se pudo abrir la cámara interna: " + ex.getMessage() + "\nSe abrirá ingreso manual del QR.");
            mostrarDialogoEscanearQrManual(requireClone, titulo);
            return;
        }

        timerRef[0] = new javax.swing.Timer(140, e -> {
            if (closed.get() || busy.getAndSet(true)) return;
            Thread worker = new Thread(() -> {
                BufferedImage image = null;
                String decoded = null;
                try {
                    image = webcam.getImage();
                    decoded = decodeQrFromImage(image);
                } catch (Exception ignored) {
                    // Mientras no se detecte QR, se sigue mostrando la cámara.
                }

                BufferedImage finalImage = image;
                String finalDecoded = decoded;
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (finalImage != null && !closed.get()) {
                            preview.setIcon(new ImageIcon(scaleImage(finalImage, 640, 420)));
                            preview.setText("");
                        }
                        if (finalDecoded != null && !closed.get()) {
                            cleanup.run();
                            dialog.dispose();
                            procesarQrEscaneado(finalDecoded, requireClone);
                        }
                    } finally {
                        busy.set(false);
                    }
                });
            }, "DogMessenger-QR-Camera-Scanner");
            worker.setDaemon(true);
            worker.start();
        });
        timerRef[0].start();

        dialog.setVisible(true);
    }

    private void mostrarDialogoEscanearQrManual(boolean requireClone, String titulo) {
        JTextField qrField = new JTextField(38);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("Pegue el contenido del QR:"), BorderLayout.NORTH);
        panel.add(qrField, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        procesarQrEscaneado(qrField.getText(), requireClone);
    }

    private void procesarQrEscaneado(String qrText, boolean requireClone) {
        QrData data = parseQrData(qrText);
        if (data == null || (requireClone && !data.clone)) {
            JOptionPane.showMessageDialog(this, requireClone ? "QR inválido para clonar dispositivo." : "QR inválido para Dog Messenger.");
            return;
        }

        txtIp.setText(data.ip);
        txtPort.setText(String.valueOf(data.port));
        if (data.clone && data.cloneName != null && !data.cloneName.isBlank()) {
            txtNombre.setText(data.cloneName);
        }
        conectar(data.clone);
    }

    private static String decodeQrFromImage(BufferedImage image) {
        if (image == null) return null;
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap);
            return result == null ? null : result.getText();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Image scaleImage(BufferedImage image, int maxWidth, int maxHeight) {
        int width = image.getWidth();
        int height = image.getHeight();
        double factor = Math.min((double) maxWidth / width, (double) maxHeight / height);
        int newWidth = Math.max(1, (int) Math.round(width * factor));
        int newHeight = Math.max(1, (int) Math.round(height * factor));
        return image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }

    private void mostrarQrDialog(String titulo, String indicacion, String ip, int port, boolean cloneMode) {
        String cloneName = nombreActual();
        String qrContent = "dogmsg://" + ip + ":" + port + (cloneMode ? "?clone=1&name=" + urlEncode(cloneName) : "");
        BufferedImage image = SimpleQrCodeGenerator.createQrImage(qrContent, 360);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("Dog Messenger");
        header.setOpaque(true);
        header.setBackground(new Color(0x0078C8));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 22f));
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        panel.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(8, 8));
        String datos = "Datos: " + ip + ":" + port + (cloneMode ? "<br>Nombre clonado: " + escapeHtml(cloneName) : "");
        JLabel label = new JLabel("<html><b>" + indicacion + "</b><br>" + datos + "</html>");
        body.add(label, BorderLayout.NORTH);
        JLabel qr = new JLabel(new ImageIcon(image));
        qr.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(qr, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, panel, titulo, JOptionPane.PLAIN_MESSAGE);
    }

    private void conectar() {
        conectar(false);
    }

    private void conectar(boolean silentJoin) {
        if (connected) {
            JOptionPane.showMessageDialog(this, "Ya estás conectado.");
            return;
        }

        String ip = txtIp.getText().trim();
        int port = leerPuerto();
        if (ip.isEmpty() || port < 0) {
            JOptionPane.showMessageDialog(this, "Ingrese IP y puerto válidos.");
            return;
        }

        String errorRedLocal = validarConexionRedLocal(ip);
        if (errorRedLocal != null) {
            agregarMensaje(errorRedLocal);
            JOptionPane.showMessageDialog(this, errorRedLocal);
            return;
        }

        btnConectar.setEnabled(false);
        btnDesconectar.setEnabled(true);

        silentJoinOnNextConnect = silentJoin;

        client = new TCPClient(ip, port, new TCPClient.Listener() {
            @Override
            public void onConnected() {
                SwingUtilities.invokeLater(() -> {
                    connected = true;
                    btnEnviar.setEnabled(true);
                    btnAdjuntar.setEnabled(true);
                    btnConectar.setEnabled(false);
                    btnDesconectar.setEnabled(true);
                    silentSession = silentJoinOnNextConnect;
                    if (!silentSession && client != null) {
                        client.sendMessage(ChatProtocol.encodeMessage(nombreActual(), "conectado"));
                    }
                    silentJoinOnNextConnect = false;
                });
            }

            @Override
            public void onMessage(String message) {
                SwingUtilities.invokeLater(() -> procesarMensajeRecibido(message));
            }

            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    connected = false;
                    btnEnviar.setEnabled(false);
                    btnAdjuntar.setEnabled(false);
                    btnConectar.setEnabled(true);
                    btnDesconectar.setEnabled(false);
                    silentJoinOnNextConnect = false;
                    silentSession = false;
                    agregarMensaje("Error: " + error);
                });
            }

            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(() -> {
                    connected = false;
                    btnEnviar.setEnabled(false);
                    btnAdjuntar.setEnabled(false);
                    btnConectar.setEnabled(true);
                    btnDesconectar.setEnabled(false);
                    silentJoinOnNextConnect = false;
                });
            }
        });

        client.start();
    }

    private void desconectar() {
        if (client != null) {
            if (connected && !silentSession) client.sendMessage(ChatProtocol.encodeMessage(nombreActual(), "desconectado"));
            client.stopClient();
            client = null;
        }
        connected = false;
        silentSession = false;
        estadoInicial();
    }

    private void enviarMensaje() {
        String message = txtMensaje.getText().trim();
        if (message.isEmpty()) return;
        if (!connected || client == null) {
            JOptionPane.showMessageDialog(this, "No estás conectado al servidor.");
            return;
        }
        client.sendMessage(ChatProtocol.encodeMessage(nombreActual(), message));
        txtMensaje.setText("");
    }

    private void seleccionarArchivo() {
        if (!connected || client == null) {
            JOptionPane.showMessageDialog(this, "Primero conecta el cliente al servidor.");
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
            client.sendMessage(ChatProtocol.encodeFile(nombreActual(), file.getName(), mimeType, bytes));
            agregarMensaje("Archivo enviado: " + file.getName());
        } catch (Exception ex) {
            agregarMensaje("Error enviando archivo: " + ex.getMessage());
        }
    }

    private void procesarMensajeRecibido(String raw) {
        ChatProtocol.StoreConfig store = ChatProtocol.parseStoreConfig(raw);
        if (store != null) {
            tiendaIp = store.ip;
            tiendaPort = store.port;
            return;
        }
        ChatProtocol.ChatPacket packet = ChatProtocol.parse(raw);
        String visible = ChatProtocol.display(raw);
        if (!visible.isBlank()) agregarMensaje(visible);
        if (packet.isFile()) guardarArchivoRecibido(packet);
    }

    private void abrirTiendaVirtual() {
        String prevIp = txtIp.getText().trim();
        int prevPort = leerPuerto();
        if (prevPort < 0) prevPort = 8189;
        String ipTienda = tiendaIp == null || tiendaIp.isBlank() ? prevIp : tiendaIp;
        int portTienda = tiendaPort > 0 ? tiendaPort : 8190;
        if (ipTienda == null || ipTienda.isBlank()) {
            JOptionPane.showMessageDialog(this, "No se recibió IP de Server50Tienda.");
            return;
        }
        boolean estabaConectado = connected;
        desconectar();
        final String finalPrevIp = prevIp;
        final int finalPrevPort = prevPort;
        TiendaClientWindow tienda = new TiendaClientWindow(this, ipTienda, portTienda, nombreActual(), () -> {
            txtIp.setText(finalPrevIp);
            txtPort.setText(String.valueOf(finalPrevPort));
            if (estabaConectado) conectar(false);
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
        btnEnviar.setEnabled(false);
        btnAdjuntar.setEnabled(false);
        btnConectar.setEnabled(true);
        btnDesconectar.setEnabled(false);
    }

    private int leerPuerto() {
        try {
            int port = Integer.parseInt(txtPort.getText().trim());
            return (port >= 1 && port <= 65535) ? port : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String nombreActual() {
        String value = txtNombre.getText().trim();
        return value.isEmpty() ? "Cliente desktop" : value;
    }

    private void agregarMensaje(String text) {
        txtMensajes.append(text + "\n");
        txtMensajes.setCaretPosition(txtMensajes.getDocument().getLength());
    }



    private String validarConexionRedLocal(String serverIp) {
        String ipServidor = serverIp == null ? "" : serverIp.trim();

        if (!esIpv4(ipServidor) || !esIpPrivadaIpv4(ipServidor)) {
            return "La IP del servidor debe ser una IP de red local válida. Ejemplo: 192.168.0.164";
        }

        List<String> ipsLocales = obtenerIpsLocalesPrivadas();
        if (ipsLocales.isEmpty()) {
            return "Esta PC no tiene una IP de red local activa.";
        }

        for (String ipLocal : ipsLocales) {
            if (estaEnMismaRedLocal(ipLocal, ipServidor)) {
                return null;
            }
        }

        return "Esta PC no está en la misma red local del servidor. PC: " + ipsLocales + " / Servidor: " + ipServidor;
    }

    private List<String> obtenerIpsLocalesPrivadas() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLoopbackAddress() || !(address instanceof Inet4Address)) continue;
                    String ip = address.getHostAddress();
                    if (esIpPrivadaIpv4(ip) && !ip.startsWith("169.254.")) {
                        result.add(ip);
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private boolean estaEnMismaRedLocal(String ipA, String ipB) {
        String[] a = ipA.split("\\.");
        String[] b = ipB.split("\\.");
        if (a.length != 4 || b.length != 4) return false;
        // Para este proyecto se usa red local simple /24: 192.168.0.x, 192.168.1.x, etc.
        return a[0].equals(b[0]) && a[1].equals(b[1]) && a[2].equals(b[2]);
    }

    private boolean esIpv4(String ip) {
        String[] p = ip.split("\\.");
        if (p.length != 4) return false;
        for (String part : p) {
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private boolean esIpPrivadaIpv4(String ip) {
        String[] p = ip.split("\\.");
        if (p.length != 4) return false;
        try {
            int a = Integer.parseInt(p[0]);
            int b = Integer.parseInt(p[1]);
            return a == 10 || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String obtenerIpLocal() {        String bestIp = txtIp.getText().trim().isEmpty() ? "127.0.0.1" : txtIp.getText().trim();
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
                    if (ip.startsWith("192.168.56.")) score -= 250;
                    if (ip.startsWith("172.30.")) score -= 200;

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


    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }

    private static String getQueryParam(String queryOrRaw, String key) {
        if (queryOrRaw == null || key == null) return "";
        String[] parts = queryOrRaw.split("&");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String k = part.substring(0, idx);
            if (k.equalsIgnoreCase(key)) {
                return urlDecode(part.substring(idx + 1));
            }
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[?&]" + java.util.regex.Pattern.quote(key) + "=([^&]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(queryOrRaw);
        if (matcher.find()) return urlDecode(matcher.group(1));
        return "";
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static class QrData {
        final String ip;
        final int port;
        final boolean clone;
        final String cloneName;

        QrData(String ip, int port, boolean clone, String cloneName) {
            this.ip = ip;
            this.port = port;
            this.clone = clone;
            this.cloneName = cloneName == null ? "" : cloneName.trim();
        }
    }

    private static QrData parseQrData(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;
        try {
            if (value.toLowerCase(Locale.ROOT).startsWith("dogmsg://")) {
                URI uri = new URI(value);
                String host = uri.getHost();
                int port = uri.getPort();
                boolean clone = uri.getQuery() != null &&
                        (uri.getQuery().toLowerCase(Locale.ROOT).contains("clone=1") ||
                                uri.getQuery().toLowerCase(Locale.ROOT).contains("mode=clone"));
                if (host != null && !host.isBlank() && port >= 1 && port <= 65535) {
                    String cloneName = getQueryParam(uri.getQuery(), "name");
                    if (cloneName == null || cloneName.isBlank()) cloneName = getQueryParam(uri.getQuery(), "nombre");
                    if (cloneName == null || cloneName.isBlank()) cloneName = getQueryParam(uri.getQuery(), "cloneName");
                    return new QrData(host, port, clone, cloneName);
                }
            }

            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?:dogmsg://)?([A-Za-z0-9._-]+):(\\d{1,5}).*")
                    .matcher(value);
            if (matcher.matches()) {
                String host = matcher.group(1);
                int port = Integer.parseInt(matcher.group(2));
                boolean clone = value.toLowerCase(Locale.ROOT).contains("clone=1") ||
                        value.toLowerCase(Locale.ROOT).contains("mode=clone");
                if (port >= 1 && port <= 65535) {
                    String cloneName = getQueryParam(value, "name");
                    if (cloneName == null || cloneName.isBlank()) cloneName = getQueryParam(value, "nombre");
                    if (cloneName == null || cloneName.isBlank()) cloneName = getQueryParam(value, "cloneName");
                    return new QrData(host, port, clone, cloneName);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static class TCPClient {
        interface Listener {
            void onConnected();
            void onMessage(String message);
            void onError(String error);
            void onDisconnected();
        }

        private final String host;
        private final int port;
        private final Listener listener;
        private volatile boolean running;
        private Socket socket;
        private PrintWriter out;

        TCPClient(String host, int port, Listener listener) {
            this.host = host;
            this.port = port;
            this.listener = listener;
        }

        void start() {
            Thread thread = new Thread(this::run, "TCPClient50-Desktop");
            thread.setDaemon(true);
            thread.start();
        }

        void sendMessage(String message) {
            PrintWriter writer = out;
            if (writer != null && !writer.checkError()) {
                writer.println(message);
                writer.flush();
            }
        }

        void stopClient() {
            running = false;
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        }

        private void run() {
            running = true;
            try (Socket s = new Socket()) {
                socket = s;
                s.connect(new InetSocketAddress(host, port), 5000);
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
                listener.onConnected();

                try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                    String line;
                    while (running && (line = in.readLine()) != null) {
                        listener.onMessage(line);
                    }
                }
            } catch (Exception e) {
                if (running) listener.onError(e.getMessage() == null ? e.toString() : e.getMessage());
            } finally {
                running = false;
                listener.onDisconnected();
            }
        }
    }

    private static class ChatProtocol {
        static class StoreConfig {
            final String ip;
            final int port;
            StoreConfig(String ip, int port) { this.ip = ip; this.port = port; }
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

        static StoreConfig parseStoreConfig(String raw) {
            try {
                String[] parts = raw.split("\\|", 3);
                if (parts.length == 3 && "STORECFG".equals(parts[0])) {
                    String ip = dec(parts[1]).trim();
                    int port = Integer.parseInt(parts[2].trim());
                    if (!ip.isBlank() && port >= 1 && port <= 65535) return new StoreConfig(ip, port);
                }
            } catch (Exception ignored) {}
            return null;
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
