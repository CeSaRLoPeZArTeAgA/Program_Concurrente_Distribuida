GENERAR_MODELO_WORD2VEC
-------------------------
hay que ubicarse dentro la carpeta GENERAR_MODELO_WORD2VEC

Compilar:

    javac -encoding UTF-8 -d out generar/word2vec/*.java

Ejecutar entrenamiento:

    java -cp out generar.word2vec.MainEntrenarWord2Vec

El archivo generado se copia/guarda en:

    salida_modelo/word2vec_model.bin
