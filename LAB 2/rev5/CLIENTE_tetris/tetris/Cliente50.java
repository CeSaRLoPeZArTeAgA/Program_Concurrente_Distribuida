package tetris;

import javax.swing.JOptionPane;


//Clase principal del cliente.
//Su responsabilidad es pedir al usuario los datos necesarios para conectarse
//al servidor: IP, puerto y nombre del jugador. Luego crea una ventana del juego mediante la clase WindowGame

public class Cliente50 {

    
    //punto de entrada del programa cliente
    public static void main(String[] args) {
        //pide la IP del servidor, si el usuario no escribe nada, usa localhost
        String ip = pedirTexto("IP del servidor:", "127.0.0.1");

        //pide el puerto TCP donde escucha el servidor
        int port = pedirPuerto("Puerto del servidor:", TCPClient50.DEFAULT_SERVER_PORT);

        //pide el nombre que se mostrara para este jugador
        String name = pedirTexto("Nombre del jugador:", "Jugador");

        //crea la interfaz grafica del cliente y comienza la conexion al servidor
        new WindowGame(ip.trim(), port, name.trim());
    }

    
    //muestra una ventana para pedir texto al usuario
    private static String pedirTexto(String mensaje, String defecto) {
        String valor = JOptionPane.showInputDialog(null, mensaje, defecto);

        //si el usuario cancela o deja vacio el campo, se devuelve el valor por defecto
        if (valor == null || valor.trim().isEmpty()) {
            return defecto;
        }
        return valor.trim();
    }

    //solicita un puerto valido mediante una ventana de dialogo
    //el metodo repite la pregunta hasta obtener un numero entre 1 y 65535
    private static int pedirPuerto(String mensaje, int defecto) {
        while (true) {
            String texto = JOptionPane.showInputDialog(null, mensaje, String.valueOf(defecto));

            //si el usuario cancela o no escribe nada, se usa el puerto por defecto
            if (texto == null || texto.trim().isEmpty()) {
                return defecto;
            }

            try {
                int puerto = Integer.parseInt(texto.trim());

                //rango valido para un puerto TCP/UDP
                if (puerto >= 1 && puerto <= 65535) {
                    return puerto;
                }
            } catch (NumberFormatException ignored) {
                //si el texto no es numerico, se muestra el mensaje de error de abajo
            }

            JOptionPane.showMessageDialog(
                    null,
                    "Puerto inválido. Ingrese un número entre 1 y 65535.",
                    "Error de puerto",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
