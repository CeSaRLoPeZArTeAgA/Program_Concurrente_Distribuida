/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package starvation;

/**
 *
 * @author Yupp
 */
class RecursoCompartido {
    public synchronized void usar(String nombreHilo) {
        System.out.println(nombreHilo + " está usando el recurso.");
        try {
            Thread.sleep(500); // simula trabajo
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(nombreHilo + " terminó de usar el recurso.");
    }
}

class HiloTrabajador extends Thread {
    private RecursoCompartido recurso;

    public HiloTrabajador(RecursoCompartido r, String nombre) {
        super(nombre);
        recurso = r;
    }

    public void run() {
        while (true) {
            recurso.usar(getName());
            try {
                Thread.sleep(100); // espera antes de intentar de nuevo
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

public class Starvation {
    public static void main(String[] args) {
        RecursoCompartido recurso = new RecursoCompartido();

        // Crear varios hilos con distintas prioridades
        HiloTrabajador hilo1 = new HiloTrabajador(recurso, "Hilo 1 (Alta prioridad)");
        HiloTrabajador hilo2 = new HiloTrabajador(recurso, "Hilo 2 (Media prioridad)");
        HiloTrabajador hilo3 = new HiloTrabajador(recurso, "Hilo 3 (Baja prioridad)");

        hilo1.setPriority(Thread.MAX_PRIORITY); // prioridad alta
        hilo2.setPriority(Thread.NORM_PRIORITY); // prioridad normal
        hilo3.setPriority(Thread.MIN_PRIORITY); // prioridad baja

        hilo1.start();
        hilo2.start();
        hilo3.start();
    }
}
