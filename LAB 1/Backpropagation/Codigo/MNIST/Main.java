package MNIST;

import java.awt.image.BufferedImage;

public class Main {

    public static void main(String[] args) {
        String imagesPath = "D:/CICLO 2026-1 UNI/Program_Concurrente_Distribuida/LAB 1/Backpropagation/Codigo/MNIST/train-images-idx3-ubyte.gz";
        String labelsPath = "D:/CICLO 2026-1 UNI/Program_Concurrente_Distribuida/LAB 1/Backpropagation/Codigo/MNIST/train-labels-idx1-ubyte.gz";

        int[] indices;

        if (args.length > 0) {
            indices = new int[args.length];
            for (int i = 0; i < args.length; i++) {
                indices[i] = Integer.parseInt(args[i]);
            }
        } else {
            // Serie por defecto
            indices = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        }

        int maxIndex = maximo(indices);
        int limite = maxIndex + 1;

        int escala = 10;
        int columnas = 5;

        try {
            MNISTDataset dataset = MNISTLoader.load(imagesPath, labelsPath, limite);

            System.out.println("Dataset cargado correctamente.");
            System.out.println("Cantidad cargada: " + dataset.getSize());
            System.out.println("Filas: " + dataset.getRows());
            System.out.println("Columnas: " + dataset.getCols());
            System.out.println();

            System.out.print("Muestras solicitadas: ");
            for (int idx : indices) {
                System.out.print(idx + " ");
            }
            System.out.println("\n");

            for (int idx : indices) {
                MNISTViewer.printSampleInfo(dataset, idx, 10);
                System.out.println();
            }

            BufferedImage collage = MNISTViewer.buildSamplesCollage(dataset, indices, escala, columnas);
            MNISTViewer.showCollage(collage, "Collage de muestras MNIST");

            // Si prefieres paneles individuales en una sola ventana:
            // MNISTViewer.showSamples(dataset, indices, escala, columnas);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int maximo(int[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException("El arreglo de índices no puede estar vacío.");
        }

        int max = arr[0];
        for (int x : arr) {
            if (x > max) {
                max = x;
            }
        }
        return max;
    }
}


//datos de entrenamiento 60,0000
//datos de prueba 10,000
//tamaño de cada imagen 28x28

//COMPILACION PREVIAMENTE
// javac -d "D:\CICLO 2026-1 UNI\Program_Concurrente_Distribuida\LAB 1\Backpropagation\Codigo" `
//"D:\CICLO 2026-1 UNI\Program_Concurrente_Distribuida\LAB 1\Backpropagation\Codigo\MNIST\Main.java" `
//"D:\CICLO 2026-1 UNI\Program_Concurrente_Distribuida\LAB 1\Backpropagation\Codigo\MNIST\MNISTDataset.java" `
//"D:\CICLO 2026-1 UNI\Program_Concurrente_Distribuida\LAB 1\Backpropagation\Codigo\MNIST\MNISTLoader.java" `
//"D:\CICLO 2026-1 UNI\Program_Concurrente_Distribuida\LAB 1\Backpropagation\Codigo\MNIST\MNISTViewer.java"

//RUN
// java -cp "D:\CICLO 2026-1 UNI\Program_Concurrente_Distribuida\LAB 1\Backpropagation\Codigo" MNIST.Main 0 7 15 25 100 250 3 5 14 1 75

// FC
// 28X28 -> 100 -> 50 -> 10 
