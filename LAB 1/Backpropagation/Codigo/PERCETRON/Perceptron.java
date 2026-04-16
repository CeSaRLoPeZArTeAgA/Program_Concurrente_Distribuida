import java.util.Random;   // Permite generar números aleatorios para inicializar los pesos
import java.util.Arrays;   // Permite usar Arrays.toString para imprimir arreglos

// Definición de la clase Perceptron
public class Perceptron {

    // Arreglo que almacena los pesos del perceptrón:
    // pesos[i] corresponde al peso asociado a la entrada x[i]
    private double[] pesos;

    // Sesgo (bias) del perceptrón, equivalente al término independiente b
    private double sesgo;

    // Tasa de aprendizaje η, controla el tamaño de las actualizaciones
    private double tasaAprendizaje;

    // Constructor de la clase
    // numeroEntradas: cantidad de variables de entrada del perceptrón
    // tasaAprendizaje: valor positivo usado en la regla de actualización
    public Perceptron(int numeroEntradas, double tasaAprendizaje) {

        // Verifica que el número de entradas sea válido
        if (numeroEntradas <= 0) {
            throw new IllegalArgumentException("El numero de entradas debe ser positivo.");
        }

        // Verifica que la tasa de aprendizaje sea válida
        if (tasaAprendizaje <= 0) {
            throw new IllegalArgumentException("La tasa de aprendizaje debe ser positiva.");
        }

        // Crea el arreglo de pesos con tamaño igual al número de entradas
        this.pesos = new double[numeroEntradas];

        // Guarda la tasa de aprendizaje en el atributo de la clase
        this.tasaAprendizaje = tasaAprendizaje;

        // Inicializa pesos y sesgo con valores aleatorios pequeños
        inicializarPesos();
    }

    // Método privado para inicializar pesos y sesgo
    // Se usa al construir el objeto
    private void inicializarPesos() {

        // Crea un generador de números aleatorios
        Random rand = new Random();

        // Recorre cada posición del arreglo de pesos
        for (int i = 0; i < pesos.length; i++) {

            // Asigna un valor aleatorio entre -0.5 y 0.5
            // Esto evita comenzar con todos los pesos iguales
            pesos[i] = rand.nextDouble() - 0.5;
        }

        // Inicializa también el sesgo con un valor aleatorio entre -0.5 y 0.5
        sesgo = rand.nextDouble() - 0.5;
    }

    // Método que calcula la suma ponderada:
    // z = w·x + b
    public double sumaPonderada(double[] entradas) {

        // Verifica que el vector de entrada tenga la dimensión correcta
        verificarDimension(entradas);

        // Empieza la suma desde el sesgo
        double suma = sesgo;

        // Agrega cada producto peso[i] * entrada[i]
        for (int i = 0; i < pesos.length; i++) {
            suma += pesos[i] * entradas[i];
        }

        // Devuelve el valor z
        return suma;
    }

    // Método de activación
    // Aplica función escalón:
    // devuelve 1 si z >= 0, y 0 en caso contrario
    public int activar(double[] entradas) {
        return sumaPonderada(entradas) >= 0 ? 1 : 0;
    }

    // Entrena el perceptrón con una sola muestra
    // entradas: vector x
    // salidaEsperada: etiqueta real y, que debe ser 0 o 1
    public void entrenarUnaMuestra(double[] entradas, int salidaEsperada) {

        // Verifica que la dimensión del vector de entrada sea correcta
        verificarDimension(entradas);

        // Verifica que la salida esperada sea válida
        verificarSalida(salidaEsperada);

        // Calcula la salida predicha por el perceptrón para esa entrada
        int salidaPredicha = activar(entradas);

        // Calcula el error:
        // error = y - yHat
        int error = salidaEsperada - salidaPredicha;

        // Actualiza cada peso según la regla del perceptrón:
        // w_i <- w_i + eta * error * x_i
        for (int i = 0; i < pesos.length; i++) {
            pesos[i] += tasaAprendizaje * error * entradas[i];
        }

        // Actualiza el sesgo:
        // b <- b + eta * error
        sesgo += tasaAprendizaje * error;
    }

    // Entrena el perceptrón usando varias muestras durante varias épocas
    // X: matriz de entradas, donde cada fila es un ejemplo
    // y: vector de etiquetas esperadas
    // epocas: número de veces que se recorrerán todos los datos
    public void entrenar(double[][] X, int[] y, int epocas) {

        // Verifica que X e y no sean null
        if (X == null || y == null) {
            throw new IllegalArgumentException("Los datos no pueden ser null.");
        }

        // Verifica que el número de ejemplos coincida con el número de etiquetas
        if (X.length != y.length) {
            throw new IllegalArgumentException("La cantidad de filas de X debe coincidir con el tamaño de y.");
        }

        // Verifica que el número de épocas sea válido
        if (epocas <= 0) {
            throw new IllegalArgumentException("El numero de epocas debe ser positivo.");
        }

        // Repite el proceso de entrenamiento durante varias épocas
        for (int epoca = 0; epoca < epocas; epoca++) {

            // Contador de errores en la época actual
            int errores = 0;

            // Recorre todas las muestras del conjunto de entrenamiento
            for (int i = 0; i < X.length; i++) {

                // Calcula la salida antes de entrenar con esta muestra
                int salidaAntes = activar(X[i]);

                // Ajusta pesos y sesgo usando esta muestra
                entrenarUnaMuestra(X[i], y[i]);

                // Calcula la salida después de actualizar
                int salidaDespues = activar(X[i]);

                // Cuenta error si la clasificación no coincide con la salida esperada
                if (salidaAntes != y[i] || salidaDespues != y[i]) {
                    errores++;
                }
            }

            // Imprime el resumen de la época actual
            System.out.println("Epoca " + (epoca + 1) + " completada. Errores: " + errores);

            // Si ya no hubo errores, se detiene antes
            if (errores == 0) {
                System.out.println("Entrenamiento convergio en la epoca " + (epoca + 1));
                break;
            }
        }
    }

    // Método privado que verifica si el vector de entrada tiene la dimensión correcta
    private void verificarDimension(double[] entradas) {

        // Verifica que no sea null
        if (entradas == null) {
            throw new IllegalArgumentException("El vector de entradas no puede ser null.");
        }

        // Verifica que la longitud coincida con la cantidad de pesos
        if (entradas.length != pesos.length) {
            throw new IllegalArgumentException(
                "Dimension incorrecta: se esperaban " + pesos.length +
                " entradas, pero se recibieron " + entradas.length
            );
        }
    }

    // Método privado que verifica que la salida sea binaria: 0 o 1
    private void verificarSalida(int salida) {
        if (salida != 0 && salida != 1) {
            throw new IllegalArgumentException("La salida esperada debe ser 0 o 1.");
        }
    }

    // Devuelve una copia del arreglo de pesos
    // Se devuelve copia para evitar modificar el arreglo original desde fuera
    public double[] getPesos() {
        return Arrays.copyOf(pesos, pesos.length);
    }

    // Devuelve el valor actual del sesgo
    public double getSesgo() {
        return sesgo;
    }

    // Sobrescribe toString para imprimir el objeto de forma legible
    @Override
    public String toString() {
        return "Perceptron{" +
                "pesos=" + Arrays.toString(pesos) +
                ", sesgo=" + sesgo +
                ", tasaAprendizaje=" + tasaAprendizaje +
                '}';
    }
}