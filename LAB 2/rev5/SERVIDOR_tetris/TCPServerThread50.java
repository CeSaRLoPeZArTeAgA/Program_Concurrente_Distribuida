package tetris;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

//hilo encargado de atender a un cliente TCP especifico
public class TCPServerThread50 implements Runnable {
    // Socket de conexión con el cliente
    private final Socket client;

    //referencia al servidor principal para notificar mensajes y desconexiones
    private final TCPServer50 tcpServer;

    //identificador asignado al cliente
    private final int clientID;

    //controla si el hilo debe seguir leyendo mensajes del cliente
    private volatile boolean running = true;

    //flujo de salida para enviar texto al cliente
    private PrintWriter out;

    //flujo de entrada para leer texto enviado por el cliente
    private BufferedReader in;

    
    //constructor del hilo de atencion de cliente
    public TCPServerThread50(Socket client, TCPServer50 tcpServer, int clientID) {
        this.client = client;
        this.tcpServer = tcpServer;
        this.clientID = clientID;
    }
    
    // metodo que se ejecuta cuando inicia el hilo. lee mensajes del cliente mientras la conexion este activa 
    @Override
    public void run() {
        try {
            // out permite enviar lineas de texto al cliente
            // el true activa autoFlush para enviar automaticamente despues de println
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);

            // in permite leer líneas de texto enviadas por el cliente
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            //se informa al servidor principal que el cliente esta listo
            tcpServer.notifyClientReady(clientID);

            // bucle de lectura: readLine() espera hasta que llegue una linea o se cierre la conexion
            String message;
            while (running && (message = in.readLine()) != null) {
                tcpServer.notifyMessageReceived(clientID, message);
            }
        } catch (Exception e) {
            System.out.println("Cliente " + clientID + " desconectado: " + e.getMessage());
        } finally {
            // al finalizar, se marca como detenido, se elimina del servidor y se cierra el socket
            running = false;
            tcpServer.removeClient(clientID);
            try {
                client.close();
            } catch (Exception ignored) {
                // se ignora porque la conexion ya está en proceso de cierre
            }
        }
    }

    //envia un mensaje de texto al cliente atendido por este hilo     
    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    //detiene este cliente cerrando su socket. al cerrarse el socket, readLine() deja de bloquearse y el hilo termina     
    public void stopClient() {
        running = false;
        try {
            client.close();
        } catch (Exception ignored) {
            // se ignora porque el objetivo es cerrar la conexion
        }
    }
}
