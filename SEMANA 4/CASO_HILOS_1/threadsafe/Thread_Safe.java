/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package threadsafe;

/**
 *
 * @author Yupp
 */
class ContadorSeguro {
    private int valor = 0;

    // Métodos sincronizados: garantizan thread-safety
    public synchronized void incrementar() {
        valor++;
    }

    public synchronized int obtenerValor() {
        return valor;
    }
}

class HiloIncrementador extends Thread {
    private ContadorSeguro contador;

    public HiloIncrementador(ContadorSeguro c, String nombre) {
        super(nombre);
        contador = c;
    }

    public void run() {
        for (int i = 0; i < 1000; i++) {
            contador.incrementar();
        }
        System.out.println(getName() + " terminó.");
    }
}

public class Thread_Safe {
    public static void main(String[] args) {
        ContadorSeguro contador = new ContadorSeguro();

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

        // Siempre será 2000 gracias a la seguridad de hilos
        System.out.println("Valor final: " + contador.obtenerValor());
    }
}
