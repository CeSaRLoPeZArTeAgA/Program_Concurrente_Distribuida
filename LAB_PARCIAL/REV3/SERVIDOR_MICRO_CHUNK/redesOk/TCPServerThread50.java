package redesOk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

// hilo individual para cada cliente conectado al servidor
public class TCPServerThread50 implements Runnable {
    private final Socket client;
    private final TCPServer50 tcpServer;
    private final int clientId;

    private volatile boolean running = true;
    private PrintWriter out;
    private BufferedReader in;

    public TCPServerThread50(Socket client, TCPServer50 tcpServer, int clientId) {
        this.client = client;
        this.tcpServer = tcpServer;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),
                    true
            );
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            tcpServer.notifyClientReady(clientId);

            String message;
            while (running && (message = in.readLine()) != null) {
                tcpServer.notifyMessageReceived(clientId, message);
            }
        } catch (Exception e) {
            System.out.println("Cliente C" + clientId + " desconectado: " + e.getMessage());
        } finally {
            running = false;
            tcpServer.removeClient(clientId);
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
