package tetris;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.Toolkit;

public class WindowGame {
    
    public static int WIDTH = 445;//ancho
    public static int HEIGHT = 629;//alto
 
    private Board board;
    private Title title;
    private JFrame window;

    public WindowGame() {

        int filas = pedirEntero("Ingrese número de filas del tablero:", 20, 10, 40);
        int columnas = pedirEntero("Ingrese número de columnas del tablero:", 10, 6, 30);

        // Obtener tamaño de la pantalla
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();

        // Espacio extra para botones, puntaje y bordes
        int margenAncho = 170;
        int margenAlto = 100;

        // Calculamos qué tamaño máximo puede tener cada bloque
        int blockPorAlto = (pantalla.height - margenAlto) / filas;
        int blockPorAncho = (pantalla.width - margenAncho) / columnas;

        // Elegimos el menor para que entre en pantalla
        Board.blockSize = Math.min(30, Math.min(blockPorAlto, blockPorAncho));

        // Evitamos bloques demasiado pequeños
        Board.blockSize = Math.max(12, Board.blockSize);

        WIDTH = columnas * Board.blockSize + 145;
        HEIGHT = filas * Board.blockSize + 29;

        window = new JFrame("Tetris CLA - rev1");
        window.setSize(WIDTH, HEIGHT);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.setResizable(false);

        //creacion del tablero y titulo
        board = new Board(filas, columnas);
        title = new Title(this);

        window.addKeyListener(board);
        window.addKeyListener(title);

        window.add(title);

        window.setVisible(true);
    }

      private int pedirEntero(String mensaje, int valorDefault, int minimo, int maximo) {
        while (true) {
            String entrada = JOptionPane.showInputDialog(
                    null,
                    mensaje + "\nValor permitido: " + minimo + " a " + maximo,
                    valorDefault
            );

            if (entrada == null) {
                return valorDefault;
            }

            try {
                int valor = Integer.parseInt(entrada.trim());

                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }

                JOptionPane.showMessageDialog(
                        null,
                        "Ingrese un número entre " + minimo + " y " + maximo
                );

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Ingrese un número entero válido."
                );
            }
        }
    }

    //aqui empieza el juego tetris
    public void startTetris() {
        window.remove(title);
        window.addMouseMotionListener(board);
        window.addMouseListener(board);
        window.add(board);
        board.startGame();
        window.revalidate();
        window.repaint();//NUEVO
    }

    public static void main(String[] args) {
        new WindowGame();
    }

}
