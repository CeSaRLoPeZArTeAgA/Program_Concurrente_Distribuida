package redesOk;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server50TiendaGUI extends JFrame {
    private final JTextField txtIp = new JTextField(15);
    private final JTextField txtPort = new JTextField("8190", 5);
    private final JTextArea txtHistorial = new JTextArea();
    private final JTextField txtMensaje = new JTextField();
    private final JButton btnIniciar = new JButton("Iniciar Server50Tienda");
    private final JButton btnParar = new JButton("Parar Server50Tienda");
    private final JButton btnEnviar = new JButton("Send");

    private StoreServer server;
    private volatile boolean running = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Server50TiendaGUI().setVisible(true));
    }

    public Server50TiendaGUI() {
        setTitle("Dog Messenger - Server50Tienda");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 760);
        setLocationRelativeTo(null);
        txtIp.setText(obtenerIpLocal());
        txtIp.setEditable(true);
        buildLayout();
        setupListeners();
        estadoInicial();
        agregar("Primero levante este Server50Tienda. Luego levante Server50 y coloque esta IP/puerto como Tienda IP/Tienda Port.");
    }

    private void buildLayout() {
        Color blue = new Color(0x0078C8);
        Color background = new Color(0xFFF7FF);
        JPanel root = new JPanel(new BorderLayout(12, 12));
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
        JLabel sub = new JLabel("Server50Tienda - Ventas Virtual", SwingConstants.CENTER);
        sub.setOpaque(true);
        sub.setBackground(blue);
        sub.setForeground(Color.WHITE);
        sub.setFont(sub.getFont().deriveFont(Font.BOLD, 16f));
        sub.setBorder(new EmptyBorder(8, 10, 8, 10));
        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = 0;
        c.gridx = 0;
        form.add(new JLabel("IP Tienda:"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtIp, c);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Port:"), c);
        c.gridx = 3;
        form.add(txtPort, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        buttons.setOpaque(false);
        style(btnIniciar, blue);
        style(btnParar, blue);
        style(btnEnviar, blue);
        buttons.add(btnIniciar);
        buttons.add(btnParar);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(form, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.SOUTH);
        center.add(top, BorderLayout.NORTH);

        txtHistorial.setEditable(false);
        txtHistorial.setLineWrap(true);
        txtHistorial.setWrapStyleWord(true);
        txtHistorial.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        center.add(new JScrollPane(txtHistorial), BorderLayout.CENTER);

        JPanel send = new JPanel(new BorderLayout(8, 0));
        send.setOpaque(false);
        send.add(txtMensaje, BorderLayout.CENTER);
        send.add(btnEnviar, BorderLayout.EAST);
        center.add(send, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
    }

    private void style(JButton b, Color blue) {
        b.setBackground(blue);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
    }

    private void setupListeners() {
        btnIniciar.addActionListener(e -> iniciar());
        btnParar.addActionListener(e -> parar());
        btnEnviar.addActionListener(e -> enviarGeneral());
        txtMensaje.addActionListener(e -> enviarGeneral());
    }

    private void iniciar() {
        if (running)
            return;
        int port = leerPuerto();
        if (port < 0) {
            JOptionPane.showMessageDialog(this, "Puerto inválido.");
            return;
        }
        server = new StoreServer(port, new StoreServer.Listener() {
            @Override
            public void onInfo(String text) {
                SwingUtilities.invokeLater(() -> agregar(text));
            }

            @Override
            public void onClientQuestion(ClientHandler client, String sender, String question) {
                SwingUtilities.invokeLater(() -> agregar("[" + sender + "]: " + question));
                String response = VentasIA.responder(sender, question);
                client.send(ChatProtocol.encodeMessage("Server50Tienda", response));
                SwingUtilities.invokeLater(() -> agregar("[Server50Tienda -> " + sender + "]: " + response));
            }

            @Override
            public void onStopped() {
                SwingUtilities.invokeLater(() -> estadoInicial());
            }
        });
        running = true;
        estadoActivo();
        agregar("Server50Tienda iniciado en " + txtIp.getText().trim() + ":" + port);
        server.start();
    }

    private void parar() {
        if (server != null)
            server.stopServer();
        server = null;
        running = false;
        estadoInicial();
    }

    private void enviarGeneral() {
        String msg = txtMensaje.getText().trim();
        if (msg.isEmpty())
            return;
        if (server == null || !running) {
            JOptionPane.showMessageDialog(this, "Primero inicia Server50Tienda.");
            return;
        }
        String raw = ChatProtocol.encodeMessage("Server50Tienda", msg);
        server.broadcast(raw);
        agregar("[Server50Tienda]: " + msg);
        txtMensaje.setText("");
    }

    private void estadoInicial() {
        running = false;
        btnIniciar.setEnabled(true);
        btnParar.setEnabled(false);
        btnEnviar.setEnabled(false);
        txtPort.setEditable(true);
    }

    private void estadoActivo() {
        btnIniciar.setEnabled(false);
        btnParar.setEnabled(true);
        btnEnviar.setEnabled(true);
        txtPort.setEditable(false);
    }

    private int leerPuerto() {
        try {
            int p = Integer.parseInt(txtPort.getText().trim());
            return p >= 1 && p <= 65535 ? p : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private void agregar(String text) {
        txtHistorial.append(text + "\n");
        txtHistorial.setCaretPosition(txtHistorial.getDocument().getLength());
    }

    private String obtenerIpLocal() {
        String best = "127.0.0.1";
        int score = Integer.MIN_VALUE;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual())
                    continue;
                String name = (ni.getDisplayName() + " " + ni.getName()).toLowerCase(Locale.ROOT);
                if (name.contains("virtual") || name.contains("vbox") || name.contains("vmware")
                        || name.contains("hyper-v") || name.contains("wsl") || name.contains("docker"))
                    continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        int s = scoreIp(ip, name);
                        if (s > score) {
                            score = s;
                            best = ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return best;
    }

    private int scoreIp(String ip, String interfaceName) {
        int score = 0;
        if (ip.startsWith("192.168."))
            score += 300;
        if (ip.startsWith("10."))
            score += 220;
        if (ip.startsWith("172."))
            score += 180;
        if (interfaceName.contains("wi-fi") || interfaceName.contains("wifi") || interfaceName.contains("wireless"))
            score += 80;
        return score;
    }

    private static class StoreServer {
        interface Listener {
            void onInfo(String text);

            void onClientQuestion(ClientHandler client, String sender, String question);

            void onStopped();
        }

        private final int port;
        private final Listener listener;
        private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
        private ServerSocket serverSocket;
        private volatile boolean running;

        StoreServer(int port, Listener listener) {
            this.port = port;
            this.listener = listener;
        }

        void start() {
            Thread t = new Thread(() -> {
                running = true;
                try {
                    serverSocket = new ServerSocket(port);
                    while (running) {
                        Socket socket = serverSocket.accept();
                        ClientHandler h = new ClientHandler(socket, this);
                        clients.add(h);
                        new Thread(h, "StoreClient").start();
                    }
                } catch (SocketException ignored) {
                } catch (Exception e) {
                    if (listener != null && running)
                        listener.onInfo("Error Server50Tienda: " + e.getMessage());
                } finally {
                    stopServer();
                    if (listener != null)
                        listener.onStopped();
                }
            }, "Server50Tienda");
            t.setDaemon(true);
            t.start();
        }

        void broadcast(String raw) {
            for (ClientHandler c : clients)
                c.send(raw);
        }

        void stopServer() {
            running = false;
            try {
                if (serverSocket != null)
                    serverSocket.close();
            } catch (Exception ignored) {
            }
            for (ClientHandler c : new ArrayList<>(clients))
                c.stop();
            clients.clear();
        }

        void remove(ClientHandler c) {
            clients.remove(c);
        }

        void onRaw(ClientHandler c, String raw) {
            ChatProtocol.Packet p = ChatProtocol.parse(raw);
            if (p.text.equalsIgnoreCase("conectado")) {
                c.send(ChatProtocol.encodeMessage("Server50Tienda",
                        "Bienvenido a la Tienda Virtual. Puede consultar productos, precios, stock, descuentos y formas de pago."));
                if (listener != null)
                    listener.onInfo("[" + p.sender + "] conectado a Tienda Virtual");
                return;
            }
            if (p.text.equalsIgnoreCase("desconectado")) {
                if (listener != null)
                    listener.onInfo("[" + p.sender + "] desconectado de Tienda Virtual");
                return;
            }
            if (listener != null)
                listener.onClientQuestion(c, p.sender, p.text);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final StoreServer server;
        private volatile boolean running;
        private PrintWriter out;

        ClientHandler(Socket socket, StoreServer server) {
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            running = true;
            try {
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while (running && (line = in.readLine()) != null) {
                    if (!line.isBlank())
                        server.onRaw(this, line);
                }
            } catch (Exception ignored) {
            } finally {
                stop();
                server.remove(this);
            }
        }

        void send(String raw) {
            if (out != null && !out.checkError()) {
                out.println(raw);
                out.flush();
            }
        }

        void stop() {
            running = false;
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static class ChatProtocol {
        static class Packet {
            final String sender;
            final String text;

            Packet(String sender, String text) {
                this.sender = sender;
                this.text = text;
            }
        }

        static String encodeMessage(String sender, String text) {
            return "MSG|" + enc(sender) + "|" + enc(text);
        }

        static Packet parse(String raw) {
            try {
                String[] p = raw.split("\\|", 5);
                if (p.length >= 3 && "MSG".equals(p[0]))
                    return new Packet(dec(p[1]), dec(p[2]));
            } catch (Exception ignored) {
            }
            return new Packet("Cliente", raw);
        }

        private static String enc(String value) {
            try {
                return URLEncoder.encode(value == null ? "" : value, "UTF-8");
            } catch (Exception e) {
                return value;
            }
        }

        private static String dec(String value) {
            try {
                return URLDecoder.decode(value == null ? "" : value, "UTF-8");
            } catch (Exception e) {
                return value;
            }
        }
    }
}
