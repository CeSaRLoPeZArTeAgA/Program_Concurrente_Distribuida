package ejemplo;
import java.math.BigInteger;
public class SumaTodosBigInt {

    public BigInteger sum[] = new BigInteger[40];

    public static void main(String args[]) {
        new SumaTodosBigInt().inicio();
    }

    void inicio() {
        long tInicio = System.nanoTime();  // ← inicio de toma de tiempo
        int N = 1000000000;
        int H = 20 ;
        int d = (int) ((N) / H);
        Thread todos[] = new Thread[40];
        for (int i = 0; i < (H - 1); i++) {
            todos[i] = new tarea0101((i * d + 1), (i * d + d), i);
            todos[i].start();
        }
        //Thread Hilo;
        todos[H - 1] = new tarea0101(((d * (H - 1)) + 1), N, H - 1);
        todos[H - 1].start();

        for (int i = 0; i < H; i++) {
            try {
                todos[i].join();
            } catch (InterruptedException ex) {
                System.out.println("error" + ex);
            }
        }
        BigInteger sumatotal = BigInteger.ZERO;
        for (int i = 0; i < H; i++) {
            sumatotal = sumatotal.add(sum[i]);
        }
        

        long tFin = System.nanoTime();  // ← fin de toma de tiempo

        System.out.println("suma total: " + sumatotal);

        double tiempoSeg = (tFin - tInicio) / 1e9;
        System.out.println("Tiempo total (s): " + tiempoSeg);
    }

    public class tarea0101 extends Thread {

        public int max, min, id;

        tarea0101(int min_, int max_, int id_) {
            max = max_;
            min = min_;
            id = id_;
            System.out.println("id" + id + " min: " + min_ + " max " + max_);
        }

        public void run() {
            BigInteger suma = BigInteger.ZERO;
            for (long i = min; i <= max; i++){
                suma = suma.add(BigInteger.valueOf(i));
//                try {
//                    sleep(1);
//                } catch (InterruptedException ex) {
//                    System.out.println("error " + ex);
//                }
            }
            sum[id] = suma;
            System.out.println("id" + id + "suma:" + suma);
        }
    }
}
