//package pkg03prusoc;

|// Importa la clase InputStream del paquete java.io.
// InputStream representa un flujo de entrada de bytes.
// En este programa se usa para leer los datos que llegan desde el servidor.
import java.io.InputStream;

// Importa la clase Socket del paquete java.net.
// Socket permite crear una conexión cliente-servidor mediante red.
import java.net.Socket;

// Importa la clase Scanner del paquete java.util.
// Scanner facilita la lectura de datos desde distintas fuentes,
// por ejemplo teclado, archivos o un flujo de entrada.
import java.util.Scanner;

// Este programa actuará como cliente,
// es decir, se conectará a un servidor para recibir información.
public class PruebaSocket {    

// Método principal del programa. La ejecución empieza aquí.
// args es un arreglo de cadenas que puede contener argumentos enviados desde la línea de comandos.
    public static void main(String[] args) {

        // Se inicia un bloque try para manejar posibles errores,
        // por ejemplo:
        // - que no exista conexión a internet,
        // - que el servidor no responda,
        // - que el puerto esté cerrado,
        // - o que ocurra un problema al leer los datos.
        try {
            // Crea un socket cliente y trata de conectarse al servidor:
            // "time-A.timefreq.bldrdoc.gov", usando el puerto 13.
            //
            // El puerto 13 corresponde históricamente al servicio "Daytime",
            // que devuelve la fecha y hora del servidor en texto plano.
            //
            // Cuando esta línea se ejecuta correctamente:
            // 1. se resuelve el nombre del servidor a una dirección IP,
            // 2. se establece la conexión TCP,
            // 3. y el objeto s representa esa conexión abierta.
            Socket s = new Socket("time-A.timefreq.bldrdoc.gov",13);
            
            // Hace lo mismo que la anterior, pero en vez de usar el nombre del servidor, usa directamente su dirección IP.
            // Puede servir si el DNS falla o si se quiere probar la conexión de forma directa.
            // Socket s = new Socket("132.163.96.1",13);

            // Segundo bloque try.
            // Se usa junto con finally para asegurarse de que el socket
            // se cierre siempre, incluso si ocurre un error mientras se leen datos.
            try {
                // Obtiene el flujo de entrada del socket.
                // Ese flujo contiene los bytes que el servidor envía al cliente.
                //
                // En otras palabras:
                // - el servidor escribe datos,
                // - este cliente los recibe por la red,
                // - y getInputStream() permite acceder a ellos.
                InputStream secuenciaDeEntrada = s.getInputStream();

                // Crea un objeto Scanner asociado al flujo de entrada.
                // Scanner permitirá leer el contenido recibido de forma más cómoda,
                // en este caso línea por línea.
                Scanner in = new Scanner(secuenciaDeEntrada);
                
                //imprime un mensaje para indicar que se va a mostrar la información recibida del servidor.
                System.out.print("Lectura del servidor de IP 132.163.96.1, en el puerto 13:");

                // Bucle while:
                // se ejecuta mientras todavía haya otra línea disponible
                // en los datos enviados por el servidor.
                //
                // hasNextLine() devuelve true si existe una línea más
                // que puede ser leída.
                while (in.hasNextLine()) {

                    // Lee una línea completa del flujo de entrada
                    // y la guarda en la variable line.
                    //
                    // Por ejemplo, si el servidor envía:
                    // "57942 26-04-09 04:10:15 50 0 0 478.1 UTC(NIST) *"
                    // esa línea se almacena aquí como String.
                    String line = in.nextLine();

                    // Imprime en pantalla la línea leída.
                    // System.out.println muestra el texto en la consola
                    // y luego hace un salto de línea.
                    System.out.println(line);                    
                }

            } finally{

                // Este bloque finally se ejecuta siempre,
                // ocurra o no ocurra una excepción dentro del try interno.
                //
                // Aquí se cierra el socket para liberar recursos de red.
                // Esto es importante porque una conexión abierta consume recursos
                // del sistema operativo y del programa.
                s.close();
            }

        } catch (Exception e) {

            // Si ocurre cualquier excepción en el bloque try externo,
            // se captura aquí.
            //
            // Exception e contiene información sobre el error ocurrido.
            // printStackTrace() muestra en consola:
            // - el tipo de error,
            // - el mensaje,
            // - y la secuencia de llamadas donde ocurrió.
            //
            // Esto ayuda mucho para depuración.
            e.printStackTrace();
        }        
    }
}