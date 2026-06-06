import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class PrestamosSwingClient extends JFrame {
    JTextField host = new JTextField("192.168.0.137", 14);
    JTextField port = new JTextField("8080", 5);
    JTextField idUsuario = new JTextField("1", 8);
    JTextArea contenido = new JTextArea("Solicito un prestamo de 5500 soles, tengo sueldo estable", 5, 50);
    JTextArea out = new JTextArea(15, 80);
    HttpClient client = HttpClient.newHttpClient();
    public PrestamosSwingClient() {
        super("Cliente Java Swing - Prestamos Distribuidos v8");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel top = new JPanel(); top.add(new JLabel("IP PC1")); top.add(host); top.add(new JLabel("Puerto")); top.add(port);
        JButton test = new JButton("Probar conexion"); test.addActionListener(e -> get("/api/health")); top.add(test);
        JPanel form = new JPanel(new BorderLayout()); JPanel inputs = new JPanel(); inputs.add(new JLabel("ID usuario")); inputs.add(idUsuario); form.add(inputs, BorderLayout.NORTH); form.add(new JScrollPane(contenido), BorderLayout.CENTER);
        JButton send = new JButton("Enviar solicitud"); send.addActionListener(e -> postSolicitud()); form.add(send, BorderLayout.SOUTH);
        out.setEditable(false);
        add(top, BorderLayout.NORTH); add(form, BorderLayout.CENTER); add(new JScrollPane(out), BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(null);
    }
    String base(){return "http://" + host.getText().trim() + ":" + port.getText().trim();}
    void get(String path){new Thread(() -> {try{HttpRequest req=HttpRequest.newBuilder(URI.create(base()+path)).GET().build(); HttpResponse<String> r=client.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); out.setText(r.body());}catch(Exception ex){out.setText("ERROR: "+ex);}}).start();}
    void postSolicitud(){new Thread(() -> {try{String body="{\"id_usuario\":"+Integer.parseInt(idUsuario.getText().trim())+",\"canal\":\"WhatsApp\",\"tipo\":\"solicitud\",\"contenido\":\""+contenido.getText().replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ")+"\"}"; HttpRequest req=HttpRequest.newBuilder(URI.create(base()+"/api/solicitud")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(); HttpResponse<String> r=client.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); out.setText(r.body());}catch(Exception ex){out.setText("ERROR: "+ex);}}).start();}
    public static void main(String[] args){SwingUtilities.invokeLater(() -> new PrestamosSwingClient().setVisible(true));}
}
