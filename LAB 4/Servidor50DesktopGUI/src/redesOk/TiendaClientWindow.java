package redesOk;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Locale;

public class TiendaClientWindow extends JFrame {
    private final String tiendaIp;
    private final int tiendaPort;
    private final String nombre;
    private final Runnable onVolverChatAnterior;

    private final JTextArea txtHistorial = new JTextArea();
    private final JTextField txtConsulta = new JTextField();
    private final JButton btnEnviar = new JButton("Send");
    private final JButton btnVolver = new JButton("Volver a chat anterior");
    private final JButton btnCerrar = new JButton("Cerrar aplicación");
    private final JLabel lblDatos = new JLabel();

    private StoreClient client;

    public TiendaClientWindow(Window owner, String tiendaIp, int tiendaPort, String nombre, Runnable onVolverChatAnterior) {
        this.tiendaIp = tiendaIp == null || tiendaIp.isBlank() ? "127.0.0.1" : tiendaIp.trim();
        this.tiendaPort = tiendaPort > 0 ? tiendaPort : 8190;
        this.nombre = nombre == null || nombre.isBlank() ? "Cliente" : nombre.trim();
        this.onVolverChatAnterior = onVolverChatAnterior;

        setTitle("Dog Messenger - Tienda Virtual");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(640, 720);
        setLocationRelativeTo(owner);
        buildLayout();
        setupListeners();
        conectar();
    }

    private void buildLayout() {
        Color blue = new Color(0x0078C8);
        Color background = new Color(0xFFF7FF);
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(background);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setOpaque(false);
        JLabel title = new JLabel("Dog Messenger");
        title.setOpaque(true);
        title.setBackground(blue);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel sub = new JLabel("Tienda Virtual", SwingConstants.CENTER);
        sub.setOpaque(true);
        sub.setBackground(blue);
        sub.setForeground(Color.WHITE);
        sub.setFont(sub.getFont().deriveFont(Font.BOLD, 16f));
        sub.setBorder(new EmptyBorder(8, 10, 8, 10));
        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        lblDatos.setText("Server50Tienda: " + tiendaIp + ":" + tiendaPort);
        lblDatos.setFont(lblDatos.getFont().deriveFont(Font.BOLD, 14f));
        center.add(lblDatos, BorderLayout.NORTH);

        txtHistorial.setEditable(false);
        txtHistorial.setLineWrap(true);
        txtHistorial.setWrapStyleWord(true);
        txtHistorial.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        center.add(new JScrollPane(txtHistorial), BorderLayout.CENTER);

        JPanel send = new JPanel(new BorderLayout(8, 0));
        send.setOpaque(false);
        send.add(txtConsulta, BorderLayout.CENTER);
        styleButton(btnEnviar, blue);
        send.add(btnEnviar, BorderLayout.EAST);
        center.add(send, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottom.setOpaque(false);
        styleButton(btnVolver, blue);
        styleButton(btnCerrar, blue);
        bottom.add(btnVolver);
        bottom.add(btnCerrar);
        root.add(bottom, BorderLayout.SOUTH);
    }

    private void styleButton(JButton button, Color blue) {
        button.setBackground(blue);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
    }

    private void setupListeners() {
        btnEnviar.addActionListener(e -> enviarConsulta());
        txtConsulta.addActionListener(e -> enviarConsulta());
        btnVolver.addActionListener(e -> volverChatAnterior());
        btnCerrar.addActionListener(e -> cerrarAplicacion());
    }

    private void conectar() {
        agregar("Conectando a Server50Tienda " + tiendaIp + ":" + tiendaPort + "...");
        client = new StoreClient(tiendaIp, tiendaPort, new StoreClient.Listener() {
            @Override public void onConnected() {
                SwingUtilities.invokeLater(() -> {
                    agregar("Conectado a Tienda Virtual.");
                    if (client != null) client.send(ChatProtocol.encodeMessage(nombre, "conectado"));
                });
            }
            @Override public void onMessage(String raw) {
                SwingUtilities.invokeLater(() -> {
                    String text = ChatProtocol.display(raw);
                    if (!text.isBlank()) agregar(text);
                });
            }
            @Override public void onError(String error) {
                SwingUtilities.invokeLater(() -> agregar("Error de Tienda Virtual: " + error));
            }
            @Override public void onDisconnected() {
                SwingUtilities.invokeLater(() -> agregar("Comunicación con Tienda Virtual cerrada."));
            }
        });
        client.start();
    }

    private void enviarConsulta() {
        String consulta = txtConsulta.getText().trim();
        if (consulta.isEmpty()) return;
        if (client == null) {
            JOptionPane.showMessageDialog(this, "No estás conectado a Server50Tienda.");
            return;
        }
        client.send(ChatProtocol.encodeMessage(nombre, consulta));
        agregar("[" + nombre + "]: " + consulta);
        txtConsulta.setText("");
    }

    private void volverChatAnterior() {
        cerrarCliente();
        dispose();
        if (onVolverChatAnterior != null) onVolverChatAnterior.run();
    }

    private void cerrarAplicacion() {
        cerrarCliente();
        dispose();
        System.exit(0);
    }

    private void cerrarCliente() {
        if (client != null) {
            client.send(ChatProtocol.encodeMessage(nombre, "desconectado"));
            client.stopClient();
            client = null;
        }
    }

    private void agregar(String text) {
        txtHistorial.append(text + "\n");
        txtHistorial.setCaretPosition(txtHistorial.getDocument().getLength());
    }

    private static class StoreClient {
        interface Listener {
            void onConnected();
            void onMessage(String raw);
            void onError(String error);
            void onDisconnected();
        }
        private final String host;
        private final int port;
        private final Listener listener;
        private volatile boolean running;
        private Socket socket;
        private PrintWriter out;
        StoreClient(String host, int port, Listener listener) {
            this.host = host;
            this.port = port;
            this.listener = listener;
        }
        void start() {
            Thread t = new Thread(() -> {
                running = true;
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), 5000);
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    if (listener != null) listener.onConnected();
                    String line;
                    while (running && (line = in.readLine()) != null) {
                        if (listener != null && !line.isBlank()) listener.onMessage(line);
                    }
                } catch (Exception e) {
                    if (listener != null && running) listener.onError(e.getMessage());
                } finally {
                    stopClient();
                    if (listener != null) listener.onDisconnected();
                }
            }, "TiendaClient");
            t.setDaemon(true);
            t.start();
        }
        void send(String raw) {
            PrintWriter w = out;
            if (w != null && !w.checkError()) {
                w.println(raw);
                w.flush();
            }
        }
        void stopClient() {
            running = false;
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        }
    }

    private static class ChatProtocol {
        static String encodeMessage(String sender, String text) {
            return "MSG|" + enc(blank(sender, "Sin nombre")) + "|" + enc(text == null ? "" : text);
        }
        static String display(String raw) {
            try {
                String[] p = raw.split("\\|", 5);
                if (p.length >= 3 && "MSG".equals(p[0])) {
                    String sender = bracket(dec(p[1]));
                    String text = dec(p[2]);
                    if ("conectado".equalsIgnoreCase(text)) return sender + " conectado";
                    if ("desconectado".equalsIgnoreCase(text)) return sender + " desconectado";
                    return sender + ": " + text;
                }
                if (p.length >= 5 && "FILE".equals(p[0])) {
                    return bracket(dec(p[1])) + " [Archivo adjunto] " + dec(p[2]);
                }
                if (p.length >= 3 && "STORECFG".equals(p[0])) return "";
            } catch (Exception ignored) {}
            return raw;
        }
        private static String bracket(String name) {
            String clean = name == null || name.isBlank() ? "Sin nombre" : name.trim();
            return clean.startsWith("[") && clean.endsWith("]") ? clean : "[" + clean + "]";
        }
        private static String blank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
        private static String enc(String value) { try { return URLEncoder.encode(value, "UTF-8"); } catch (Exception e) { return value; } }
        private static String dec(String value) { try { return URLDecoder.decode(value, "UTF-8"); } catch (Exception e) { return value; } }
    }
}
