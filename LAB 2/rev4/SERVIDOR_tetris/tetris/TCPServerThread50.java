package tetris;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPServerThread50 implements Runnable {
    private final Socket client;
    private final TCPServer50 tcpServer;
    private final int clientID;
    private volatile boolean running = true;
    private PrintWriter out;
    private BufferedReader in;

    public TCPServerThread50(Socket client, TCPServer50 tcpServer, int clientID) {
        this.client = client;
        this.tcpServer = tcpServer;
        this.clientID = clientID;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            tcpServer.notifyClientReady(clientID);

            String message;
            while (running && (message = in.readLine()) != null) {
                tcpServer.notifyMessageReceived(clientID, message);
            }
        } catch (Exception e) {
            System.out.println("Cliente " + clientID + " desconectado: " + e.getMessage());
        } finally {
            running = false;
            tcpServer.removeClient(clientID);
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
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
            client.close();
        } catch (Exception ignored) {
        }
    }
}
