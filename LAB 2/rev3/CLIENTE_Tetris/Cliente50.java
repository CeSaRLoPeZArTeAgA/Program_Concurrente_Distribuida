package tetris;

import javax.swing.JOptionPane;

public class Cliente50 {
    public static void main(String[] args) {
        String ip = JOptionPane.showInputDialog(null, "IP del servidor:", "127.0.0.1");
        if (ip == null || ip.trim().isEmpty()) {
            ip = "127.0.0.1";
        }

        String name = JOptionPane.showInputDialog(null, "Nombre del jugador:", "Jugador");
        if (name == null || name.trim().isEmpty()) {
            name = "Jugador";
        }

        new WindowGame(ip.trim(), name.trim());
    }
}
