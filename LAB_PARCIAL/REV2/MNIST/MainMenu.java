package MNIST;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class MainMenu extends JFrame {

    private JRadioButton rbImagen;
    private JRadioButton rbTexto;

    public MainMenu() {
        setTitle("Menú Principal - MICRO CHUNCK");
        setSize(330, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        crearInterfaz();
    }

    private void crearInterfaz() {
        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(Color.WHITE);
        panelPrincipal.setBorder(BorderFactory.createLineBorder(Color.BLACK, 4));

        // titulo
        JLabel titulo = new JLabel("M I C R O   C H U N C K", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 20));
        titulo.setBorder(BorderFactory.createEmptyBorder(70, 10, 30, 10));

        panelPrincipal.add(titulo, BorderLayout.NORTH);

        // opciones centrales
        JPanel panelOpciones = new JPanel();
        panelOpciones.setBackground(Color.WHITE);
        panelOpciones.setLayout(new GridLayout(4, 1, 0, 15));
        panelOpciones.setBorder(BorderFactory.createEmptyBorder(120, 35, 80, 25));

        rbImagen = new JRadioButton("Modelo de procesamiento de Imagenes");
        rbTexto = new JRadioButton("Modelo de procesamiento de texto");

        configurarRadioButton(rbImagen);
        configurarRadioButton(rbTexto);

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbImagen);
        grupo.add(rbTexto);

        JPanel filaImagen = new JPanel(new BorderLayout());
        filaImagen.setBackground(Color.WHITE);
        filaImagen.add(rbImagen, BorderLayout.CENTER);

        JPanel filaTexto = new JPanel(new BorderLayout());
        filaTexto.setBackground(Color.WHITE);
        filaTexto.add(rbTexto, BorderLayout.CENTER);

        panelOpciones.add(Box.createVerticalStrut(10));
        panelOpciones.add(filaImagen);
        panelOpciones.add(filaTexto);
        panelOpciones.add(Box.createVerticalStrut(10));

        panelPrincipal.add(panelOpciones, BorderLayout.CENTER);

        
        // botones interiores
        JButton btnEntrar = new JButton("Entrar a IA");
        JButton btnSalir = new JButton("Salir del Sistema");

        configurarBoton(btnEntrar);
        configurarBoton(btnSalir);

        btnEntrar.addActionListener(e -> entrarAlModelo());
        btnSalir.addActionListener(e -> salirDelSistema());

        JPanel panelBotones = new JPanel(new GridLayout(1, 2, 35, 0));
        panelBotones.setBackground(Color.WHITE);
        panelBotones.setBorder(BorderFactory.createEmptyBorder(10, 30, 25, 30));

        panelBotones.add(btnEntrar);
        panelBotones.add(btnSalir);

        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    private void configurarRadioButton(JRadioButton radioButton) {
        radioButton.setBackground(Color.WHITE);
        radioButton.setForeground(Color.BLACK);
        radioButton.setFont(new Font("Serif", Font.BOLD, 13));
        radioButton.setFocusPainted(false);
    }

    private void configurarBoton(JButton boton) {
        boton.setBackground(Color.WHITE);
        boton.setForeground(Color.BLACK);
        boton.setFont(new Font("Serif", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        boton.setPreferredSize(new Dimension(110, 38));
    }

    private void entrarAlModelo() {
        if (rbImagen.isSelected()) {
            MainFormularioMNIST formulario = new MainFormularioMNIST();
            formulario.setVisible(true);
            dispose();
            return;
        }

       if (rbTexto.isSelected()) {
            MainFormularioWord2Vec formularioTexto = new MainFormularioWord2Vec();
            formularioTexto.setVisible(true);
            dispose();
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Selecciona un modelo antes de continuar.",
                "Aviso",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void salirDelSistema() {
        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas salir del sistema?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION
        );

        if (opcion == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainMenu menu = new MainMenu();
            menu.setVisible(true);
        });
    }
}
