package redesOk;

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

public class MainMenuCliente extends JFrame {
    private static final long serialVersionUID = 1L;

    private final TCPClient50 tcpClient;
    private final String clientName;
    private final String serverIp;
    private final int serverPort;

    private JRadioButton rbImagen;
    private JRadioButton rbTexto;

    public MainMenuCliente(TCPClient50 tcpClient, String clientName, String serverIp, int serverPort) {
        this.tcpClient = tcpClient;
        this.clientName = clientName;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        setTitle("Cliente MICRO CHUNK - " + clientName + " - " + serverIp + ":" + serverPort);
        setSize(380, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        crearInterfaz();
    }

    private void crearInterfaz() {
        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(Color.WHITE);
        panelPrincipal.setBorder(BorderFactory.createLineBorder(Color.BLACK, 4));

        JLabel titulo = new JLabel("M I C R O   C H U N K", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 20));
        titulo.setBorder(BorderFactory.createEmptyBorder(55, 10, 20, 10));
        panelPrincipal.add(titulo, BorderLayout.NORTH);

        JPanel panelOpciones = new JPanel();
        panelOpciones.setBackground(Color.WHITE);
        panelOpciones.setLayout(new GridLayout(5, 1, 0, 15));
        panelOpciones.setBorder(BorderFactory.createEmptyBorder(90, 35, 70, 25));

        JLabel estado = new JLabel("Servidor: " + serverIp + ":" + serverPort, SwingConstants.CENTER);
        estado.setFont(new Font("Serif", Font.BOLD, 13));

        rbImagen = new JRadioButton("Modelo de procesamiento de imagenes");
        rbTexto = new JRadioButton("Modelo de procesamiento de texto");

        configurarRadioButton(rbImagen);
        configurarRadioButton(rbTexto);

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbImagen);
        grupo.add(rbTexto);

        panelOpciones.add(estado);
        panelOpciones.add(Box.createVerticalStrut(10));
        panelOpciones.add(rbImagen);
        panelOpciones.add(rbTexto);
        panelOpciones.add(Box.createVerticalStrut(10));

        panelPrincipal.add(panelOpciones, BorderLayout.CENTER);

        JButton btnEntrar = new JButton("Entrar a IA");
        JButton btnSalir = new JButton("Salir");

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
        boton.setPreferredSize(new Dimension(120, 38));
    }

    private void entrarAlModelo() {
        if (!tcpClient.isConnected()) {
            JOptionPane.showMessageDialog(this,
                    "Todavia no hay conexion activa con el servidor.",
                    "Conexion no lista",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (rbImagen.isSelected()) {
            MainFormularioMNISTCliente formulario = new MainFormularioMNISTCliente(tcpClient, this);
            formulario.setVisible(true);
            setVisible(false);
            return;
        }

        if (rbTexto.isSelected()) {
            MainFormularioWord2VecCliente formularioTexto = new MainFormularioWord2VecCliente(tcpClient, this);
            formularioTexto.setVisible(true);
            setVisible(false);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Selecciona un modelo antes de continuar.",
                "Aviso",
                JOptionPane.WARNING_MESSAGE);
    }

    public void volverAMostrar() {
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void salirDelSistema() {
        int opcion = JOptionPane.showConfirmDialog(this,
                "Deseas salir del sistema?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION);

        if (opcion == JOptionPane.YES_OPTION) {
            tcpClient.stopClient();
            dispose();
            System.exit(0);
        }
    }
}
