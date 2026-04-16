/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.edu.uni.cc.fc.dijkstra;


import java.util.List;

public class Dijkstra_hilos {

    public static void main(String args[]) {
        int nucleosLogicos = Runtime.getRuntime().availableProcessors();
        System.out.println("==================================================");
        System.out.println("INFO: El procesador tiene " + nucleosLogicos + " hilos logicos.");
        System.out.println("==================================================\n");

        // IMPORTANTE: Asegúrate de que el archivo se llame exactamente así
        String rutaArchivo = "roadNet-CA.txt"; 
        int NUM_NODOS = 15000; // Capacidad suficiente para el ID más alto de SNAP (1,965,206)
        int origen = 0; 

        // 1. Cargar el grafo desde el TXT
        List<List<Arista>> grafo = GrafoLoader.cargarGrafoSNAP(rutaArchivo, NUM_NODOS);
        
        // 2. Instanciar nuestro algoritmo
        Dijkstra d = new Dijkstra(grafo, NUM_NODOS);

        // 3. Calentamiento de la JVM (Para que los resultados de la prueba sean reales)
        System.out.println("\nCalentando la JVM ejecutando con 1 hilo...");
        d.calcParalelo(origen, 1);
        System.out.println("Calentamiento terminado.");

        // 4. PRUEBA DE RENDIMIENTO (Benchmarking)
        System.out.println("\n=== INICIANDO PRUEBAS PARA LA GRAFICA DEL INFORME ===");
        System.out.println("Hilos\tTiempo (ms)\tSpeedup");
        
        // Probaremos hasta la cantidad de núcleos de tu PC + 2 extras para forzar el límite
        int limitePrueba = nucleosLogicos + 2; 
        long tiempoBase = 0;

        for (int h = 1; h <= limitePrueba; h++) {
            
            long tiempoInicio = System.currentTimeMillis();
            
            d.calcParalelo(origen, h);
            
            long tiempoFin = System.currentTimeMillis();
            long tiempoTotal = tiempoFin - tiempoInicio;
            
            if (h == 1) {
                tiempoBase = tiempoTotal;
            }
            
            // Calculamos el Speedup. Usamos Math.max(1, tiempoTotal) para evitar división por cero.
            double speedup = (double) tiempoBase / Math.max(1, tiempoTotal);
            
            System.out.printf("%d\t%d\t\t%.2f\n", h, tiempoTotal, speedup);
        }
        
        System.out.println("\n=== BUSQUEDA FINALIZADA ===");
    }
}