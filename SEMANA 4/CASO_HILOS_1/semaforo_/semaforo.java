package semaforo_;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class semaforo {
    public static void main(String[] args) {
        // Semáforo con 3 permisos (permite 3 hilos concurrentes)
        Semaphore parking = new Semaphore(3);
        
        // Crear 10 hilos (autos) que intentan estacionar
        for (int i = 1; i <= 10; i++) {
            new Thread(new Auto(parking, "Auto-" + i)).start();
        }
    }
}

class Auto implements Runnable {
    private final Semaphore parking;
    private final String nombre;
    
    public Auto(Semaphore parking, String nombre) {
        this.parking = parking;
        this.nombre = nombre;
    }
    
    @Override
    public void run() {
        try {
            System.out.println(nombre + " quiere estacionar...");
            
            // Adquirir un permiso (bloquea si no hay disponibles)
            parking.acquire();
            
            System.out.println(nombre + " ESTACIONÓ. Espacios restantes: " 
                               + parking.availablePermits());
            
            // Simular tiempo estacionado
            Thread.sleep(2000);
            
            System.out.println(nombre + " se va. Espacios restantes: " 
                               + (parking.availablePermits() + 1));
            
            // Liberar el permiso
            parking.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}