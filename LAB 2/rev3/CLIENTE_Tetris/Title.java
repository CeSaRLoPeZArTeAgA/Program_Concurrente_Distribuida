package tetris;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class Title extends JPanel {
    private static final long serialVersionUID = 1L;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("Tetris Mix Multiplayer", 60, 120);
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.drawString("El servidor controla el tablero compartido.", 60, 160);
        g.drawString("Flechas: mover / bajar", 60, 190);
        g.drawString("A: rotar antihorario", 60, 220);
        g.drawString("S: rotar horario", 60, 250);
    }
}
