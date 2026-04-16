/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.edu.uni.cc.fc.dijkstra;

/**
 *
 * @author Usuario
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GrafoLoader {

    public static List<List<Arista>> cargarGrafoSNAP(String rutaArchivo, int numNodos) {
        // Inicializar la lista de adyacencia
        List<List<Arista>> grafo = new ArrayList<>(numNodos);
        for (int i = 0; i < numNodos; i++) {
            grafo.add(new ArrayList<>());
        }

        System.out.println("Iniciando lectura del archivo de texto (Filtrando a " + numNodos + " nodos)...");
        long inicioLectura = System.currentTimeMillis();

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int aristasCargadas = 0;
            
            while ((linea = br.readLine()) != null) {
                if (linea.startsWith("#")) continue; // Ignorar cabeceras
                
                String[] partes = linea.trim().split("\\s+");
                if (partes.length >= 2) {
                    int origen = Integer.parseInt(partes[0]);
                    int destino = Integer.parseInt(partes[1]);
                    
                    // --- FILTRO CRUCIAL ---
                    // Solo guardamos la conexión si ambos nodos pertenecen a nuestra porción de 15,000 nodos
                    if (origen < numNodos && destino < numNodos) {
                        int pesoAleatorio = (int)(Math.random() * 100) + 1; // Peso entre 1 y 100
                        grafo.get(origen).add(new Arista(destino, pesoAleatorio));
                        aristasCargadas++;
                    }
                }
            }
            long finLectura = System.currentTimeMillis();
            System.out.println("¡Lectura completada! " + aristasCargadas + " aristas válidas cargadas en " + (finLectura - inicioLectura) + " ms.");
            
        } catch (IOException e) {
            System.err.println("ERROR: No se encontró el archivo '" + rutaArchivo + "'.");
            System.err.println("Asegúrate de pegarlo en la carpeta raíz de tu proyecto NetBeans.");
        }
        return grafo;
    }
}