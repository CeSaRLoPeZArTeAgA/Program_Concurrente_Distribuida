GENERAR MODELO MNIST
-----------------------
hay que ubicarse dentro la carpeta GENERAR_MODELO_MNIST

Compilar:

    javac -encoding UTF-8 -d out generar/mnist/*.java

Ejecutar entrenamiento:

    java -cp out generar.mnist.MainEntrenarGuardar


El archivo generado se copia/guarda en:

    salida_modelo/modelo_mnist.bin
