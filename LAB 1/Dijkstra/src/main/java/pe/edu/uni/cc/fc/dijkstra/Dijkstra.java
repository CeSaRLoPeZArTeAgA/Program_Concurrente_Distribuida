/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package pe.edu.uni.cc.fc.dijkstra;

/**
 *
 * @author Usuario
 */
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dijkstra {
    public int[] distancia;
    private List<List<Arista>> grafo;

    public Dijkstra(List<List<Arista>> grafo, int numNodos) {
        this.grafo = grafo;
        this.distancia = new int[numNodos];
    }

    public void calcParalelo(int origen, int h) {
        int n = grafo.size();
        boolean[] visitado = new boolean[n];

        // 1. Inicialización
        for (int i = 0; i < n; i++) {
            distancia[i] = Integer.MAX_VALUE;
        }
        distancia[origen] = 0;

        // Creamos el Pool de hilos
        ExecutorService executor = Executors.newFixedThreadPool(h);

        // Bucle principal de Dijkstra
        for (int c = 0; c < n - 1; c++) {
            
            // FASE SECUENCIAL: Buscar el nodo con la distancia mínima
            int minimo = Integer.MAX_VALUE;
            int minpos = -1;
            for (int k = 0; k < n; k++) {
                if (!visitado[k] && distancia[k] <= minimo) {
                    minimo = distancia[k];
                    minpos = k;
                }
            }

            // Si ya no hay nodos alcanzables, terminamos
            if (minimo == Integer.MAX_VALUE || minpos == -1) break;
            visitado[minpos] = true;

            // FASE PARALELA: Relajación de los vecinos del nodo seleccionado
            List<Arista> vecinos = grafo.get(minpos);
            int numVecinos = vecinos.size();
            
            if (numVecinos > 0) {
                CountDownLatch latch = new CountDownLatch(h);
                int tamanoBloque = (int) Math.ceil((double) numVecinos / h);

                for (int t = 0; t < h; t++) {
                    final int inicio = t * tamanoBloque;
                    final int fin = Math.min(inicio + tamanoBloque, numVecinos);
                    final int actualMinPos = minpos;

                    executor.submit(() -> {
                        try {
                            for (int i = inicio; i < fin; i++) {
                                Arista arista = vecinos.get(i);
                                int v = arista.destino;
                                int peso = arista.peso;

                                if (!visitado[v] && distancia[actualMinPos] != Integer.MAX_VALUE) {
                                    if (distancia[actualMinPos] + peso < distancia[v]) {
                                        distancia[v] = distancia[actualMinPos] + peso;
                                    }
                                }
                            }
                        } finally {
                            latch.countDown(); // Aseguramos que el hilo siempre avise que terminó
                        }
                    });
                }

                // El hilo principal espera a que los trabajadores terminen de evaluar vecinos
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        executor.shutdown(); // Importante apagar el pool al terminar
    }
}