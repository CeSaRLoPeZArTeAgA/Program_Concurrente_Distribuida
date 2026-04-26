package tetris;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

public class TCPServer50 {
    public static final int DEFAULT_PORT = 4444;

    private final int serverPort;
    private final OnMessageReceived messageListener;
    private final Map<Integer, TCPServerThread50> clients = new LinkedHashMap<>();
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private int nextClientId = 1;

    public TCPServer50(OnMessageReceived messageListener) {
        this(DEFAULT_PORT, messageListener);
    }

    public TCPServer50(int serverPort, OnMessageReceived messageListener) {
        this.serverPort = serverPort;
        this.messageListener = messageListener;
    }

    public void run() {
        running = true;
        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Servidor TCP escuchando en el puerto " + serverPort);

            while (running) {
                Socket socket = serverSocket.accept();
                int clientId;
                TCPServerThread50 worker;

                synchronized (this) {
                    clientId = nextClientId++;
                    worker = new TCPServerThread50(socket, this, clientId);
                    clients.put(clientId, worker);
                }

                new Thread(worker, "Cliente-" + clientId).start();
                System.out.println("Nuevo jugador conectado: J" + clientId);
            }
        } catch (Exception e) {
            if (running) {
                System.out.println("Error en servidor TCP: " + e.getMessage());
            }
        } finally {
            stopServer();
        }
    }

    public synchronized void sendMessageTCPServer(String message) {
        for (TCPServerThread50 client : clients.values()) {
            client.sendMessage(message);
        }
    }

    public synchronized void sendToClient(int clientId, String message) {
        TCPServerThread50 client = clients.get(clientId);
        if (client != null) {
            client.sendMessage(message);
        }
    }

    public synchronized int getClientCount() {
        return clients.size();
    }

    public synchronized boolean hasClient(int clientId) {
        return clients.containsKey(clientId);
    }

    public synchronized void removeClient(int clientId) {
        clients.remove(clientId);
        if (messageListener != null) {
            messageListener.clientDisconnected(clientId);
        }
    }

    void notifyClientReady(int clientId) {
        if (messageListener != null) {
            messageListener.clientConnected(clientId);
        }
    }

    void notifyMessageReceived(int clientId, String message) {
        if (messageListener != null) {
            messageListener.messageReceived(clientId, message);
        }
    }

    public synchronized void stopServer() {
        running = false;
        for (TCPServerThread50 client : clients.values()) {
            client.stopClient();
        }
        clients.clear();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
    }

    public interface OnMessageReceived {
        void messageReceived(int clientId, String message);
        default void clientConnected(int clientId) {}
        default void clientDisconnected(int clientId) {}
    }
}
