/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bloqueointrínseco_;

/**
 *
 * @author Yupp
 */
class Contador {
    private int valor = 0;

    // Método sincronizado: requiere el bloqueo intrínseco del objeto Contador
    public synchronized void incrementar() {
        valor++;
        System.out.println(Thread.currentThread().getName() + " incrementó a " + valor);
    }

    public synchronized int obtenerValor() {
        return valor;
    }
}

class HiloIncrementador extends Thread {
    private Contador contador;

    public HiloIncrementador(Contador c, String nombre) {
        super(nombre);
        contador = c;
    }

    public void run() {
        for (int i = 0; i < 5; i++) {
            contador.incrementar();
            try {
                Thread.sleep(100); // simula trabajo
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

public class BloqueoIntrinseco {
    public static void main(String[] args) {
        Contador contador = new Contador();

        HiloIncrementador t1 = new HiloIncrementador(contador, "Hilo 1");
        HiloIncrementador t2 = new HiloIncrementador(contador, "Hilo 2");

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Valor final: " + contador.obtenerValor());
    }

}
