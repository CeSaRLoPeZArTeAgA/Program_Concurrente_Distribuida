package redesOk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


//Cliente TCP persistente, mantiene una conexion abierta con el servidor
//y permite que varias ventanas escuchen respuestas sin crear nuevas conexiones
public class TCPClient50 {
    public static final int DEFAULT_SERVER_PORT = 4444;

    private final String serverIp;
    private final int serverPort;
    private final List<OnMessageReceived> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean running = false;
    private volatile boolean connected = false;

    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    public TCPClient50(String ip, OnMessageReceived listener) {
        this(ip, DEFAULT_SERVER_PORT, listener);
    }

    public TCPClient50(String ip, int port, OnMessageReceived listener) {
        this.serverIp = ip;
        this.serverPort = port;
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void addListener(OnMessageReceived listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnMessageReceived listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return connected && out != null && !out.checkError();
    }

    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        } else {
            notifyError("No hay conexion activa con el servidor.");
        }
    }

    public void stopClient() {
        running = false;
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }

    public void run() {
        running = true;

        try {
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            socket = new Socket(serverAddr, serverPort);

            out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                    true
            );
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            connected = true;
            notifyConnected();

            String serverMessage;
            while (running && (serverMessage = in.readLine()) != null) {
                notifyMessage(serverMessage);
            }
        } catch (Exception e) {
            notifyError("Error conectando a " + serverIp + ":" + serverPort + " -> " + e.getMessage());
        } finally {
            running = false;
            connected = false;
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }
            notifyDisconnected();
        }
    }

    private void notifyMessage(String message) {
        for (OnMessageReceived listener : listeners) {
            listener.messageReceived(message);
        }
    }

    private void notifyConnected() {
        for (OnMessageReceived listener : listeners) {
            listener.connected();
        }
    }

    private void notifyDisconnected() {
        for (OnMessageReceived listener : listeners) {
            listener.disconnected();
        }
    }

    private void notifyError(String message) {
        for (OnMessageReceived listener : listeners) {
            listener.error(message);
        }
    }

    public interface OnMessageReceived {
        void messageReceived(String message);

        default void connected() {
        }

        default void disconnected() {
        }

        default void error(String message) {
        }
    }
}
