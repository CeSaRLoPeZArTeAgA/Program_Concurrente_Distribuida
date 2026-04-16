public class Main {
    public static void main(String[] args) {

        // Conjunto de entrenamiento para la compuerta lógica AND
        // Cada fila representa una entrada de dos variables
        double[][] X = {
            {0, 0},
            {0, 1},
            {1, 0},
            {1, 1}
        };

        // Salidas esperadas de la compuerta AND
        // Solo (1,1) produce 1
        int[] y = {0, 0, 0, 1};

        // Crea un perceptrón con 2 entradas y tasa de aprendizaje 0.1
        Perceptron p = new Perceptron(2, 0.1);

        // Entrena durante hasta 100 épocas
        p.entrenar(X, y, 100);

        // Imprime el modelo final
        System.out.println("\nModelo final:");
        System.out.println(p);

        // Prueba el perceptrón con todas las entradas
        System.out.println("\nPredicciones:");
        for (double[] entrada : X) {
            int salida = p.activar(entrada);
            System.out.println(entrada[0] + ", " + entrada[1] + " -> " + salida);
        }
    }
}
