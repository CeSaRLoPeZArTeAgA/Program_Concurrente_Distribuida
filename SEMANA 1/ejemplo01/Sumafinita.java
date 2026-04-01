package ejemplo01;
public class Sumafinita {
    double[] stotal;
    public void inicio(){
        int n = 100000,h = 4, seg = (int)(n/h) ,a ,b;
        trabajo[] htrab = new trabajo[90]; 
        Thread[] t = new Thread[90];
        stotal  = new double[90];
        for (int i = 1; i <= h; i++) {
            a = (i-1)*seg+1;
            b = (i)*seg;
            htrab[i] = new  trabajo(a,b,i); 
            t[i] = new Thread(htrab[i]);
            t[i].start();
        } 
        try {
            for (int i = 1; i <= h; i++) {
                t[i].join();
            }  
        } catch (Exception e) {} 
        double total=0;
        for (int i = 1; i <= h; i++) {
            total = total +  stotal[i];
        }
        System.out.println("TOTAL: "+total);        
    }           
    public class trabajo extends Thread{
        int ini,fin,tmp;
        double sum=0;
        public trabajo(int ini_,int fin_,int tmp_){
            ini=ini_;fin=fin_;tmp=tmp_;}
        public void run(){
            for (int i = ini; i <= fin; i++) {
                for (int j = 0; j < 100000; j++) {
                    sum = sum + Math.sin(i);
                } 
            } 
            stotal[tmp]=sum;
            System.out.println("hilo nro:" + tmp + "  tiene rpta:"+ sum);            
        }            
    }
    public static void main(String[] args){
        new Sumafinita().inicio();
    }
}