package tetris;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient50 {
    public static final int DEFAULT_SERVER_PORT = 4444;

    private final String serverIp;
    private final int serverPort;
    private final OnMessageReceived messageListener;
    private volatile boolean running = false;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    public TCPClient50(String ip, int port, OnMessageReceived listener) {
        this.serverIp = ip;
        this.serverPort = port;
        this.messageListener = listener;
    }

    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    public void stopClient() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
    }

    public void run() {
        running = true;
        try {
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            socket = new Socket(serverAddr, serverPort);
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            if (messageListener != null) {
                messageListener.connected();
            }

            String serverMessage;
            while (running && (serverMessage = in.readLine()) != null) {
                if (messageListener != null) {
                    messageListener.messageReceived(serverMessage);
                }
            }
        } catch (Exception e) {
            if (messageListener != null) {
                messageListener.error("Error TCP cliente conectando a " + serverIp + ":" + serverPort + " -> " + e.getMessage());
            } else {
                System.out.println("Error TCP cliente conectando a " + serverIp + ":" + serverPort + " -> " + e.getMessage());
            }
        } finally {
            running = false;
            try {
                if (socket != null) socket.close();
            } catch (Exception ignored) {
            }
            if (messageListener != null) {
                messageListener.disconnected();
            }
        }
    }

    public interface OnMessageReceived {
        void messageReceived(String message);
        default void connected() {}
        default void disconnected() {}
        default void error(String message) {}
    }
}
