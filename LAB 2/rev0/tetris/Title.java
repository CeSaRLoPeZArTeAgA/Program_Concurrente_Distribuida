package tetris;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.Timer;

//representa la pantalla de inicio del juego, con instrucciones y un boton para iniciar el juego
public class Title extends JPanel implements KeyListener {

	private static final long serialVersionUID = 1L;
	private BufferedImage instructions;
	private WindowGame window;
	private BufferedImage[] playButton = new BufferedImage[2];
	private Timer timer;
	
	
	public Title(WindowGame window){
				//carga la imagen de instrucciones y un mensaje para iniciar el juego
                instructions = ImageLoader.loadImage("/arrow.png");
		timer = new Timer(1000/60, new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
			
		});
		timer.start();
		this.window = window;
	}
	
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		
		g.setColor(Color.BLACK);
		
		g.fillRect(0, 0, WindowGame.WIDTH, WindowGame.HEIGHT);
		
		//dibuja la imagen de instrucciones en la pantalla
		g.drawImage(instructions, WindowGame.WIDTH/2 - instructions.getWidth()/2,
				30 - instructions.getHeight()/2 + 150, null);
		
                g.setColor(Color.WHITE);
		//dibuja en la pantalla un mensaje para iniciar el juego
		g.drawString("Presione space para iniciar juego!", 150, WindowGame.HEIGHT / 2 + 100);
		
		
	}	

	//captura el evento de presionar la tecla espacio para iniciar el juego
    @Override
    public void keyTyped(KeyEvent e) {
        if(e.getKeyChar() == KeyEvent.VK_SPACE) {
            window.startTetris();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}
