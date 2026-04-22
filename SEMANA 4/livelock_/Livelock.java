/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package livelock_;

class Recurso {
    private String nombre;
    private boolean enUso = false;

    public Recurso(String n) {
        nombre = n;
    }

    public synchronized boolean intentarUsar() {
        if (!enUso) {
            enUso = true;
            return true;
        }
        return false;
    }

    public synchronized void liberar() {
        enUso = false;
    }

    public String getNombre() {
        return nombre;
    }
}

class Trabajador extends Thread {
    private String nombre;
    private Recurso recurso1;
    private Recurso recurso2;

    public Trabajador(String n, Recurso r1, Recurso r2) {
        nombre = n;
        recurso1 = r1;
        recurso2 = r2;
    }

    public void run() {
        while (true) {
            // Intenta usar el primer recurso
            
            if (recurso1.intentarUsar()) {
                System.out.println(nombre + " obtuvo " + recurso1.getNombre());

                // Si no puede obtener el segundo recurso, libera el primero
                if (!recurso2.intentarUsar()) {
                    System.out.println(nombre + " no pudo obtener " + recurso2.getNombre() + ", libera " + recurso1.getNombre());
                    recurso1.liberar();
                    try {
                        Thread.sleep(100); // espera un poco antes de intentar de nuevo
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Si logra ambos recursos, termina
                    System.out.println(nombre + " obtuvo ambos recursos y terminó.");
                    recurso2.liberar();
                    recurso1.liberar();
                    break;
                }
            }
        }
    }
}

public class Livelock {
    public static void main(String[] args) {
        Recurso recursoA = new Recurso("Recurso A");
        Recurso recursoB = new Recurso("Recurso B");

        Trabajador t1 = new Trabajador("Trabajador 1", recursoA, recursoB);
        Trabajador t2 = new Trabajador("Trabajador 2", recursoB, recursoA);

        t1.start();
        t2.start();
    }
}
