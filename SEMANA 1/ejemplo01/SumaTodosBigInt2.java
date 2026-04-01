package ejemplo;
import java.math.BigInteger;

public class SumaTodosBigInt2 {

    public BigInteger sum[];

    public static void main(String args[]) {
        SumaTodosBigInt2 obj = new SumaTodosBigInt2();

        int N = 1000000000;

        for (int H = 1; H <= 20; H++) {
            double tiempo = obj.ejecutar(N, H);
            System.out.println("Hilos: " + H + "  Tiempo (s): " + tiempo);
        }
    }

    double ejecutar(int N, int H) {
        sum = new BigInteger[H];
        Thread todos[] = new Thread[H];

        int d = N / H;

        long tInicio = System.nanoTime();

        for (int i = 0; i < H - 1; i++) {
            todos[i] = new tarea0101((i * d + 1), (i * d + d), i);
            todos[i].start();
        }

        todos[H - 1] = new tarea0101((d * (H - 1) + 1), N, H - 1);
        todos[H - 1].start();

        for (int i = 0; i < H; i++) {
            try {
                todos[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < H; i++) {
            total = total.add(sum[i]);
        }

        long tFin = System.nanoTime();

        return (tFin - tInicio) / 1e9;
    }

    public class tarea0101 extends Thread {
        int min, max, id;

        tarea0101(int min_, int max_, int id_) {
            min = min_;
            max = max_;
            id = id_;
        }

        public void run() {
            BigInteger suma = BigInteger.ZERO;

            for (long i = min; i <= max; i++) {
                suma = suma.add(BigInteger.valueOf(i));
            }

            sum[id] = suma;
        }
    }
}
