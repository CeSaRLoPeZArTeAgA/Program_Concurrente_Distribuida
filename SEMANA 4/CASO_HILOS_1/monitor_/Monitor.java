/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package monitor_;

class Contador {
    private int valor = 0;

    // Método sincronizado: solo un hilo puede ejecutarlo a la vez
    public synchronized void incrementar() {
        valor++;
    }

    public synchronized int obtenerValor() {
        return valor;
    }
}

// Clase que representa un hilo que incrementa el contador
class HiloIncrementador extends Thread {
    private Contador contador;

    public HiloIncrementador(Contador c) {
        contador = c;
    }

    public void run() {
        for (int i = 0; i < 1000; i++) {
            contador.incrementar();
        }
    }
}

public class Monitor {
    public static void main(String[] args) {
        Contador contador = new Contador();

        // Crear dos hilos que comparten el mismo recurso
        HiloIncrementador t1 = new HiloIncrementador(contador);
        HiloIncrementador t2 = new HiloIncrementador(contador);

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
