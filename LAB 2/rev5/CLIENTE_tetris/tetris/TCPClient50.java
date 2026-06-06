package tetris;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;


//Cliente TCP encargado de comunicarse con el servidor del juego
//Esta clase no dibuja nada en pantalla. Su función es abrir un socket,
//enviar mensajes al servidor y recibir mensajes continuamente en un hilo
public class TCPClient50 {

    //puerto por defecto usado si el usuario no ingresa otro
    public static final int DEFAULT_SERVER_PORT = 4444;

    //direcciosn IP o nombre del servidor
    private final String serverIp;

    //puerto TCP del servidor
    private final int serverPort;

    //objeto que recibe eventos de conexion, mensajes, errores y desconexion
    private final OnMessageReceived messageListener;

    //indica si el ciclo de lectura del cliente debe seguir ejecutandose
    private volatile boolean running = false;

    //flujo de salida para enviar texto al servidor
    private PrintWriter out;

    //flujo de entrada para leer texto enviado por el servidor
    private BufferedReader in;

    //socket que representa la conexion TCP con el servidor
    private Socket socket;

    
    //constructor del cliente TCP
    public TCPClient50(String ip, int port, OnMessageReceived listener) {
        this.serverIp = ip;
        this.serverPort = port;
        this.messageListener = listener;
    }

    
    //envia un mensaje de texto al servidor
    //el protocolo usado por el juego envía comandos como:
    //JOIN|nombre, KEY|LEFT, KEY|ROT_CW, etc
    public void sendMessage(String message) {
        // se verifica que el flujo exista y que no tenga errores antes de escribir
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    
    // Detiene el cliente cerrando el socket
    // al cerrar el socket, el ciclo de lectura se interrumpe y se ejecuta el
    // bloque finally del método run()
    public void stopClient() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
            // Se ignora porque el cliente ya está cerrando la conexión.
        }
    }

    //abre la conexión con el servidor y queda escuchando mensajes
    //este metodo normalmente se ejecuta dentro de un hilo independiente para no
    //bloquear la interfaz grafica swing.
    public void run() {
        running = true;
        try {
            //resuelve la IP o nombre del servidor
            InetAddress serverAddr = InetAddress.getByName(serverIp);

            //abre el socket TCP hacia el servidor
            socket = new Socket(serverAddr, serverPort);

            //prepara el flujo de salida para enviar lineas de texto
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            //prepara el flujo de entrada para recibir lineas de texto
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //avisa a la interfaz que la conexion fue exitosa
            if (messageListener != null) {
                messageListener.connected();
            }

            //lee mensajes del servidor mientras la conexión este activa
            String serverMessage;
            while (running && (serverMessage = in.readLine()) != null) {
                if (messageListener != null) {
                    messageListener.messageReceived(serverMessage);
                }
            }
        } catch (Exception e) {
            //informa el error a la interfaz grafica o lo imprime por consola
            if (messageListener != null) {
                messageListener.error("Error TCP cliente conectando a " + serverIp + ":" + serverPort + " -> " + e.getMessage());
            } else {
                System.out.println("Error TCP cliente conectando a " + serverIp + ":" + serverPort + " -> " + e.getMessage());
            }
        } finally {
            //se asegura de marcar el cliente como detenido y cerrar el socket
            running = false;
            try {
                if (socket != null) socket.close();
            } catch (Exception ignored) {
                //se ignoran errores durante el cierre
            }

            //notifica a la interfaz que la conexión termino
            if (messageListener != null) {
                messageListener.disconnected();
            }
        }
    }

    //interfaz de callbacks para comunicar eventos TCP hacia otras clases
    //WindowGame implementa esta interfaz mediante una clase anonima para
    //reaccionar cuando llegan mensajes del servidor
    public interface OnMessageReceived {

        //Se ejecuta cada vez que llega un mensaje del servidor
        void messageReceived(String message);

        //se ejecuta cuando el cliente logró conectarse
        default void connected() {}

        //se ejecuta cuando la conexión se cierra
        default void disconnected() {}

        //se ejecuta cuando ocurre un error de conexion o comunicacion
        default void error(String message) {}
    }
}
