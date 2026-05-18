ENTRENAMIENTO DE MODELOS IA
------------------------------

Esta versión corrige la correspondencia entre carpetas y paquetes Java.

1. MNIST

    Carpeta fuente:
        GENERAR_MODELO_MNIST/generar/mnist/

    Package:
        package generar.mnist;

    Clase principal:
        generar.mnist.MainEntrenarGuardar

    Modelo generado:
        GENERAR_MODELO_MNIST/salida_modelo/modelo_mnist.bin

2. Word2Vec

    Carpeta fuente:
        GENERAR_MODELO_WORD2VEC/generar/word2vec/

    Package:
        package generar.word2vec;

    Clase principal:
        generar.word2vec.MainEntrenarWord2Vec

    Modelo generado:
        GENERAR_MODELO_WORD2VEC/salida_modelo/word2vec_model.bin

Después de entrenar, copia los .bin al servidor MICRO CHUNK.
