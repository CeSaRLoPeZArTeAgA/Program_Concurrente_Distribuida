import java.math.BigInteger;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class SumaTodosBigInt3 {

    public BigInteger sum[];

    public static void main(String args[]) throws FileNotFoundException {

        SumaTodosBigInt3 obj = new SumaTodosBigInt3();

        int N = 1000000000;
        int repeticiones = 3;

        PrintWriter writer = new PrintWriter(new File("resultados.csv"));
        // Cabecera CSV
        writer.println("H,Tiempo");

        for (int H = 1; H <= 20; H++) {

            // -------------------------
            // Warm-up (no se mide)
            // -------------------------
            obj.ejecutar(N, H);

            double acumulado = 0.0;

            // -------------------------
            // Mediciones reales
            // -------------------------
            for (int r = 0; r < repeticiones; r++) {
                double tiempo = obj.ejecutar(N, H);
                acumulado += tiempo;
            }

            double promedio = acumulado / repeticiones;

            System.out.println("Hilo " + H + ",  Tiempo promedio: " + promedio);
            // Escribir en CSV
            writer.println(H + "," + promedio);
        }
        writer.close();
        System.out.println("Archivo resultados.csv generado");
    }

    double ejecutar(int N, int H) {

        sum = new BigInteger[H];
        Thread todos[] = new Thread[H];

        int d = N / H;

        long tInicio = System.nanoTime();

        // Crear y lanzar hilos
        for (int i = 0; i < H - 1; i++) {
            todos[i] = new tarea0101((i * d + 1), (i * d + d), i);
            todos[i].start();
        }

        // Último hilo toma el resto
        todos[H - 1] = new tarea0101((d * (H - 1) + 1), N, H - 1);
        todos[H - 1].start();

        // Esperar a todos los hilos
        for (int i = 0; i < H; i++) {
            try {
                todos[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Suma total (no afecta el tiempo significativamente)
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