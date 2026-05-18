
OBJETIVO
========
Toda la logica de IA esta en el servidor:
- MNIST: modelo_mnist.bin + MLP.java
- Word2Vec: word2vec_model.bin + Word2VecModel.java

El cliente NO carga modelos. Solo envia requerimientos al servidor y muestra la respuesta.
El servidor acepta multiples clientes simultaneamente usando hilos.

ESTRUCTURA
==========
MICRO_CHUNK_RED_OK_TETRIS_STYLE/

1.- SERVIDOR_MICRO_CHUNK/
   - modelo_mnist.bin
   - word2vec_model.bin
   - redesOk/Servidor50.java
   - redesOk/TCPServer50.java
   - redesOk/TCPServerThread50.java
   - redesOk/MLP.java
   - redesOk/MNISTDataset.java
   - redesOk/Word2VecModel.java

2.- CLIENTE_MICRO_CHUNK/
   - redesOk/Cliente50.java
   - redesOk/TCPClient50.java
   - redesOk/MainMenuCliente.java
   - redesOk/MainFormularioMNISTCliente.java
   - redesOk/MainFormularioWord2VecCliente.java

EJECUTAR SERVIDOR
=================
Abrir terminal dentro de SERVIDOR_MICRO_CHUNK:

javac -encoding UTF-8 -d out redesOk/*.java
java -cp out redesOk.Servidor50 5000

Si no pasas puerto, usa 4444 por defecto:

java -cp out redesOk.Servidor50

EJECUTAR CLIENTE
================
Abrir terminal dentro de CLIENTE_MICRO_CHUNK:

javac -encoding UTF-8 -d out redesOk/*.java
java -cp out redesOk.Cliente50

El cliente pedira:
- IP del servidor
- Puerto del servidor
- Nombre del cliente

Si servidor y cliente estan en la misma computadora:
IP: 127.0.0.1
Puerto: 5000

Si el servidor esta en otra computadora:
IP: usar la IP real del servidor, por ejemplo 192.168.1.119
Puerto: el mismo puerto del servidor, por ejemplo 6666


ARCHIVOS CLAVE
==============
- Servidor50.java: controla modelos IA y respuestas.
- TCPServer50.java: acepta clientes y crea hilos.
- TCPServerThread50.java: hilo individual por cliente.
- Cliente50.java: inicia cliente y conexion TCP.
- TCPClient50.java: mantiene conexion persistente con servidor.
