/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package happensbefore;

class DatosCompartidos {
    private int valor = 0;

    public synchronized void escribir(int nuevoValor) {
        valor = nuevoValor;
        System.out.println("Escrito: " + nuevoValor);
    }

    public synchronized int leer() {
        System.out.println("Leído: " + valor);
        return valor;
    }
}

class Escritor extends Thread {
    private DatosCompartidos datos;

    public Escritor(DatosCompartidos d) {
        datos = d;
    }

    public void run() {
        datos.escribir(42);
    }
}

class Lector extends Thread {
    private DatosCompartidos datos;

    public Lector(DatosCompartidos d) {
        datos = d;
    }

    public void run() {
        datos.leer();
    }
}

public class HappensBefore {
    public static void main(String[] args) {
        DatosCompartidos datos = new DatosCompartidos();

        Escritor escritor = new Escritor(datos);
        Lector lector = new Lector(datos);

        escritor.start();
        try {
            escritor.join(); // asegura que la escritura happens-before la lectura
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        lector.start();
    }
}
