//package pkg03serveco01;

// Importa la clase IOException del paquete java.io.
// IOException representa errores de entrada/salida, por ejemplo al leer o escribir en sockets, archivos o flujos.
import java.io.IOException;

// Importa la clase InputStream.
// InputStream permite leer datos binarios o texto como una secuencia de bytes.
import java.io.InputStream;

// Importa la clase OutputStream.
// OutputStream permite enviar o escribir datos como una secuencia de bytes.
import java.io.OutputStream;

// Importa la clase PrintWriter.
// PrintWriter permite escribir texto de forma cómoda sobre un flujo de salida.
// Aquí se usará para enviar texto al cliente por la red.
import java.io.PrintWriter;

// Importa la clase ServerSocket.
// ServerSocket permite crear un servidor que escucha conexiones
// entrantes en un puerto específico.
import java.net.ServerSocket;

// Importa la clase Socket.
// Socket representa una conexión de red entre dos extremos: entre el servidor y el cliente.
import java.net.Socket;

// Importa todas las clases del paquete java.util.
// Aquí se usa principalmente Scanner.
import java.util.*;

// Define la clase pública llamada ServidorDeEco.
// Un "servidor de eco" recibe mensajes y los devuelve al cliente.
public class ServidorDeEco {

    // Método principal del programa. Aquí comienza la ejecución del servidor.
    public static void main(String[] args) {

        // Inicia un bloque try para capturar errores de entrada/salida,
        // como problemas al abrir el puerto o al comunicarse con el cliente.
        try{

            // Crea un ServerSocket en el puerto 8189.
            // Esto significa que el programa se pone a la escucha esperando conexiones de clientes en ese puerto.
            //
            // Mientras este objeto exista y el puerto esté libre, el servidor estará disponible para aceptar conexiones.
            ServerSocket s = new ServerSocket(8189);

            // accept() bloquea la ejecución hasta que algún cliente
            // se conecte al servidor en el puerto 8189.
            //
            // Cuando un cliente se conecta:
            // - accept() retorna un objeto Socket
            // - ese Socket representa la conexión particular con ese cliente
            //
            // Importante:
            // ServerSocket sirve para escuchar,
            // Socket sirve para comunicarse con un cliente específico.
            Socket entrante = s.accept();

            // Segundo bloque try.
            // Aquí se manejará la comunicación con el cliente.
            // Se usa junto con finally para asegurar que el socket del cliente se cierre al terminar.
            try {

                // Obtiene el flujo de entrada del socket del cliente.
                // Este flujo contiene lo que el cliente envía al servidor.
                InputStream secuenciaDeEntrada = entrante.getInputStream();

                // Obtiene el flujo de salida del socket del cliente.
                // Este flujo se usa para enviar información desde el servidor al cliente.
                OutputStream secuenciaDeSalida = entrante.getOutputStream();

                // Crea un Scanner para leer del flujo de entrada de la red.
                // Es decir, permite leer fácilmente el texto que manda el cliente.
                Scanner in = new Scanner(secuenciaDeEntrada);

                // Crea un PrintWriter para escribir al flujo de salida de la red.
                // El segundo parámetro true activa el "auto-flush", es decir, cada vez que se use println(), el texto se envía inmediatamente.
                PrintWriter out = new PrintWriter(secuenciaDeSalida,true);

                // Envía al cliente un mensaje de bienvenida.
                // El cliente verá este texto al conectarse.
                out.println("¡Hola! Escriba ADIOS para salir.");

                // Variable booleana de control.
                // Indica si la conversación debe terminar.
                // Al inicio vale false, por lo que el bucle continuará.
                boolean terminado = false;

                // Crea un Scanner para leer desde el teclado del servidor,
                // es decir, desde la entrada estándar del sistema.
                //
                // System.in representa lo que el usuario escribe en consola.
                Scanner sc = new Scanner(System.in);//Leer de Sistema (teclado)

                // Bucle principal de comunicación.
                //
                // Condición:
                // - mientras NO se haya terminado
                // - y exista una línea disponible ya sea desde la red o desde el teclado
                //
                // !terminado:
                //     el servidor sigue funcionando
                //
                // (in.hasNextLine() || sc.hasNextLine()):
                //     sigue si hay algo que leer del cliente o del teclado local
                while (!terminado && ( in.hasNextLine() || sc.hasNextLine() ) ){

                    // Verifica si hay una línea disponible proveniente del cliente.
                    if(in.hasNextLine()){

                        // Lee una línea enviada por el cliente a través de la red.
                        // Esa línea se guarda en la variable linea.
                        String linea = in.nextLine();//Leer de la RED

                        // Muestra en la consola del servidor el mensaje recibido.
                        // Esto sirve para monitorear lo que está mandando el cliente.
                        System.out.println("recibi:"+linea);

                        // Envía de vuelta al cliente el mismo mensaje,
                        // anteponiendo la palabra "Eco:".
                        //
                        // si el cliente envía: hola
                        // el servidor responde: Eco:hola
                        out.println("Eco:"+linea);//Escribir en la RED

                        // trim() elimina espacios al inicio y al final de la cadena.
                        // Luego se compara con "ADIOS".
                        //
                        // Si el cliente escribe ADIOS,
                        // la variable terminado pasa a true
                        // y el bucle dejará de ejecutarse.
                        if(linea.trim().equals("ADIOS"))
                            terminado = true;
                    }

                    // Verifica si hay una línea disponible desde el teclado del servidor.
                    if(sc.hasNextLine()){

                        // Lee una línea escrita por el usuario en la consola del servidor.
                        String lineaout = sc.nextLine();//Leer de Sistema (teclado)

                        // Imprime en pantalla del servidor el texto escrito localmente.
                        System.out.println("li:"+lineaout);//Escribir en Sistema(pantalla)

                        // Envía también ese texto al cliente.
                        // De esta forma el servidor no solo responde al cliente, sino que además puede mandar mensajes manualmente desde su consola.
                        out.println("Eco:"+lineaout);//Escribir en la RED
                    }
                }

            }finally{

                // Este bloque finally se ejecuta siempre, ocurra o no un error dentro del try interno.
                //
                // Cierra el socket conectado al cliente, liberando los recursos asociados a esa conexión.
                entrante.close();
            }

        }catch(IOException e){

            // Captura errores de entrada/salida,
            // por ejemplo:
            // - si el puerto 8189 ya está ocupado,
            // - si falla la red,
            // - si hay error al leer o escribir en el socket.
            //
            // printStackTrace() imprime el detalle completo del error.
            e.printStackTrace();
        }
    }
}

//panel programas /activar o desactivar las caracteristicas de windows /cliente telnet /aceptar
//telnet 127.0.0.1 8189

//para acceder desde terminal,se usa el comando telnet seguido de la dirección IP del servidor y el puerto en el que está escuchando.
// telnet IP puerto
//
// ejemplo: 
// telnet 10.10.0.199 8189