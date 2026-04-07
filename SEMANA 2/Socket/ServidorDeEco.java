package pkg03serveco01;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
public class ServidorDeEco {
    public static void main(String[] args) {
        try{
            ServerSocket s = new ServerSocket(8189);
            Socket entrante = s.accept();
            try {
                InputStream secuenciaDeEntrada = entrante.getInputStream();
                OutputStream secuenciaDeSalida = entrante.getOutputStream();
                Scanner in = new Scanner(secuenciaDeEntrada);
                PrintWriter out = new PrintWriter(secuenciaDeSalida,true);
                out.println("¡Hola! Escriba ADIOS para salir.");
                boolean terminado = false;
                Scanner sc = new Scanner(System.in);//Leer de Sistema (teclado)
                while (!terminado && ( in.hasNextLine() || sc.hasNextLine() ) ){
                    if(in.hasNextLine()){
                        String linea = in.nextLine();//Leer de la RED
                        System.out.println("recibi:"+linea);
                        out.println("Eco:"+linea);//Escribir en la RED
                        if(linea.trim().equals("ADIOS"))
                            terminado = true;
                    }
                    if(sc.hasNextLine()){
                        String lineaout = sc.nextLine();//Leer de Sistema (teclado)
                        System.out.println("li:"+lineaout);//Escribir en Sistema(pantalla)
                        out.println("Eco:"+lineaout);//Escribir en la RED
                    }
                }                        
            }finally{
                entrante.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
//panel programas /activar o desactivar las caracteristicas de windows /cliente telnet /aceptar
//telnet 127.0.0.1 8189

//telnet 10.10.0.199 8189