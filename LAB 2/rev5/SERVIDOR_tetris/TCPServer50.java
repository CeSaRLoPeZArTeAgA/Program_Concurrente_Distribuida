package tetris;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

//servidor TCP encargado de aceptar conexiones de clientes
public class TCPServer50 {
    //puerto usado si el usuario no especifica otro
    public static final int DEFAULT_PORT = 4444;

    //puerto donde escuchara el servidor
    private final int serverPort;

    //objeto que recibira las notificaciones de conexion, mensajes y desconexión
    private final OnMessageReceived messageListener;

    
    //mapa de clientes conectados
    //la clave es el id del cliente y el valor es el hilo que maneja a ese cliente
    private final Map<Integer, TCPServerThread50> clients = new LinkedHashMap<>();

    //controla si el servidor debe seguir aceptando conexiones
    private volatile boolean running = false;

    //socket principal del servidor. Escucha conexiones entrantes
    private ServerSocket serverSocket;

    // Id que se asignará al siguiente cliente conectado
    private int nextClientId = 1;

    //constructor que usa el puerto por defecto 
    public TCPServer50(OnMessageReceived messageListener) {
        this(DEFAULT_PORT, messageListener);
    }

    //constructor que permite especificar el puerto
    public TCPServer50(int serverPort, OnMessageReceived messageListener) {
        this.serverPort = serverPort;
        this.messageListener = messageListener;
    }

    
    //inicia el servidor TCP
    public void run() {
        running = true;
        try {
            //se abre el socket servidor en el puerto configurado
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Servidor TCP escuchando en el puerto " + serverPort);

            //mientras running sea true, el servidor acepta nuevos clientes
            while (running) {
                //accept() se bloquea hasta que un cliente se conecte
                Socket socket = serverSocket.accept();
                int clientId;
                TCPServerThread50 worker;

                //la asignacion de id y modificacion del mapa se hace de forma sincronizada
                synchronized (this) {
                    clientId = nextClientId++;
                    worker = new TCPServerThread50(socket, this, clientId);
                    clients.put(clientId, worker);
                }

                //cada cliente se atiende en su propio hilo
                new Thread(worker, "Cliente-" + clientId).start();
                System.out.println("Nuevo jugador conectado: J" + clientId);
            }
        } catch (Exception e) {
            // si el error ocurre mientras el servidor seguía activo, se informa
            if (running) {
                System.out.println("Error en servidor TCP: " + e.getMessage());
            }
        } finally {
            //pase lo que pase, al final se limpian conexiones y recursos
            stopServer();
        }
    }

    //Envía un mensaje a todos los clientes conectados
    public synchronized void sendMessageTCPServer(String message) {
        for (TCPServerThread50 client : clients.values()) {
            client.sendMessage(message);
        }
    }

    //envia un mensaje a un cliente especifico
    public synchronized void sendToClient(int clientId, String message) {
        TCPServerThread50 client = clients.get(clientId);
        if (client != null) {
            client.sendMessage(message);
        }
    }

    //Devuelve la cantidad de clientes actualmente conectados   
    public synchronized int getClientCount() {
        return clients.size();
    }

    // Verifica si existe un cliente conectado con determinado id
    public synchronized boolean hasClient(int clientId) {
        return clients.containsKey(clientId);
    }

    //elimina un cliente del mapa y notifica la desconexión
    public synchronized void removeClient(int clientId) {
        clients.remove(clientId);
        if (messageListener != null) {
            messageListener.clientDisconnected(clientId);
        }
    }

    //notifica que un cliente ya tiene sus flujos de entrada/salida preparados   
    void notifyClientReady(int clientId) {
        if (messageListener != null) {
            messageListener.clientConnected(clientId);
        }
    }

    //notifica que llego un mensaje desde un cliente.
    void notifyMessageReceived(int clientId, String message) {
        if (messageListener != null) {
            messageListener.messageReceived(clientId, message);
        }
    }

    //detiene el servidor y cierra todas las conexiones activas.
    public synchronized void stopServer() {
        running = false;

        //se detiene cada cliente conectado
        for (TCPServerThread50 client : clients.values()) {
            client.stopClient();
        }
        clients.clear();

        //se cierra el socket principal del servidor
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
            //se ignora el error porque el servidor ya se esta cerrando
        }
    }

    // Interfaz de callbacks usada para comunicar eventos TCP a otra clase
    // En este proyecto, Servidor50 implementa estas acciones mediante una clase anonima   
    public interface OnMessageReceived {
        //se ejecuta cuando llega un mensaje desde un cliente
        void messageReceived(int clientId, String message);

        //se ejecuta cuando un cliente termino de conectarse
        default void clientConnected(int clientId) {}

        //se ejecuta cuando un cliente se desconecta
        default void clientDisconnected(int clientId) {}
    }
}
