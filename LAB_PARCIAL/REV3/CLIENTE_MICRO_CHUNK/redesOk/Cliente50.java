package redesOk;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


// munto de entrada del cliente MICRO CHUNK
public class Cliente50 {
    private TCPClient50 mTcpClient;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente50().iniciar());
    }

    void iniciar() {
        String ip = pedirTexto("IP del servidor:", "127.0.0.1");
        int port = pedirPuerto("Puerto del servidor:", TCPClient50.DEFAULT_SERVER_PORT);
        String nombre = pedirTexto("Nombre del cliente:", "Cliente");

        mTcpClient = new TCPClient50(ip, port, new TCPClient50.OnMessageReceived() {
            @Override
            public void connected() {
                ClienteEnvia("CLIENT_HELLO|" + nombre);
            }

            @Override
            public void messageReceived(String message) {
                ClienteRecibe(message);
            }

            @Override
            public void error(String message) {
                JOptionPane.showMessageDialog(null, message, "Error cliente", JOptionPane.ERROR_MESSAGE);
            }
        });

        new Thread(() -> mTcpClient.run(), "Cliente-TCP-MicroChunk").start();

        MainMenuCliente menu = new MainMenuCliente(mTcpClient, nombre, ip, port);
        menu.setVisible(true);
    }

    void ClienteRecibe(String llego) {
        System.out.println("CLIENTE50 recibio: " + llego);
    }

    void ClienteEnvia(String envia) {
        if (mTcpClient != null) {
            mTcpClient.sendMessage(envia);
        }
    }

    private static String pedirTexto(String mensaje, String defecto) {
        String valor = JOptionPane.showInputDialog(null, mensaje, defecto);
        if (valor == null || valor.trim().isEmpty()) {
            return defecto;
        }
        return valor.trim();
    }

    private static int pedirPuerto(String mensaje, int defecto) {
        while (true) {
            String texto = JOptionPane.showInputDialog(null, mensaje, String.valueOf(defecto));
            if (texto == null || texto.trim().isEmpty()) {
                return defecto;
            }

            try {
                int puerto = Integer.parseInt(texto.trim());
                if (puerto >= 1 && puerto <= 65535) {
                    return puerto;
                }
            } catch (NumberFormatException ignored) {
            }

            JOptionPane.showMessageDialog(null,
                    "Puerto invalido. Ingrese un numero entre 1 y 65535.",
                    "Error de puerto",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
