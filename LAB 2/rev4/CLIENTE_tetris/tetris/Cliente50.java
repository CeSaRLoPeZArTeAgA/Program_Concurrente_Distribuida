package tetris;

import javax.swing.JOptionPane;

public class Cliente50 {
    public static void main(String[] args) {
        String ip = pedirTexto("IP del servidor:", "127.0.0.1");
        int port = pedirPuerto("Puerto del servidor:", TCPClient50.DEFAULT_SERVER_PORT);
        String name = pedirTexto("Nombre del jugador:", "Jugador");

        new WindowGame(ip.trim(), port, name.trim());
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

            JOptionPane.showMessageDialog(
                    null,
                    "Puerto inválido. Ingrese un número entre 1 y 65535.",
                    "Error de puerto",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
