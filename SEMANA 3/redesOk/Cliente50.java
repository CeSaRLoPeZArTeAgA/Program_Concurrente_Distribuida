package redesOk;

import java.util.Scanner;
import redesOk.TCPClient50;

class Cliente50{
    TCPClient50 mTcpClient;
    Scanner sc;
    public static void main(String[] args)  {
        Cliente50 objcli = new Cliente50();
        objcli.iniciar();
    }
    void iniciar(){
       new Thread(
            new Runnable() {

                @Override
                public void run() {
                    mTcpClient = new TCPClient50("192.168.56.1",
                        new TCPClient50.OnMessageReceived(){
                            @Override
                            public void messageReceived(String message){
                                ClienteRecibe(message);
                            }
                        }
                    );
                    mTcpClient.run();                   
                }
            }
        ).start();
        //---------------------------
       
        String salir = "n";
        sc = new Scanner(System.in);
        System.out.println("Cliente bandera 01");
        while( !salir.equals("s")){
            salir = sc.nextLine();
            ClienteEnvia(salir);
        }
        System.out.println("Cliente bandera 02");
    
    }
    void ClienteRecibe(String llego){
        System.out.println("CLIENTE50, msj input for servidor: " + llego);

        // TAREA: DESDE EL SERVIDOR DE ENVIA EL COMANDO "envia NUM", EL CLIENTE IDENTIFICA
        // LA CABEZERA DEL COMANDO Y INICIA LA ORDEN 66 Y EL COMANDO DE ACTICACION
        if(llego.matches("^envia \\d+$")){
            int numero=Integer.parseInt(llego.substring(6).trim());
            String respuesta="ORDEN 66: "+(numero)+"_Comand_CLA";
            if (mTcpClient!=null) {
                mTcpClient.sendMessage(respuesta);
            }
        }

    }
    void ClienteEnvia(String envia){
        if (mTcpClient != null) {
            mTcpClient.sendMessage(envia);
        }
    }

}
