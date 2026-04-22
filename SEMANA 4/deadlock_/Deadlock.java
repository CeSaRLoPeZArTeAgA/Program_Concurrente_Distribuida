/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package deadlock_;

class Recurso {
    private String nombre;

    public Recurso(String n) {
        nombre = n;
    }

    public String getNombre() {
        return nombre;
    }
}

class Tarea extends Thread {
    private Recurso recurso1;
    private Recurso recurso2;

    public Tarea(Recurso r1, Recurso r2) {
        recurso1 = r1;
        recurso2 = r2;
    }

    public void run() {
        synchronized (recurso1) {
            System.out.println(getName() + " bloqueó " + recurso1.getNombre());
            try {
                Thread.sleep(100); // simula trabajo
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(getName() + " intenta bloquear " + recurso2.getNombre());
            synchronized (recurso2) {
                System.out.println(getName() + " bloqueó " + recurso2.getNombre());
            }
        }
    }
}

public class Deadlock {
    public static void main(String[] args) {
        Recurso recursoA = new Recurso("Recurso A");
        Recurso recursoB = new Recurso("Recurso B");

        // Dos hilos que intentan bloquear recursos en orden inverso
        Tarea t1 = new Tarea(recursoA, recursoB);
        Tarea t2 = new Tarea(recursoB, recursoA);

        t1.setName("Hilo 1");
        t2.setName("Hilo 2");

        t1.start();
        t2.start();
    }
}
