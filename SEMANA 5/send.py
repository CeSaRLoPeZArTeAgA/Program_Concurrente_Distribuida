#!/usr/bin/env python
import pika

credentials = pika.PlainCredentials('admin', 'admin')
connection = pika.BlockingConnection(
    pika.ConnectionParameters(host='192.168.1.118', credentials=credentials, virtual_host='aaa'))
channel = connection.channel()

QUEUE_NAME = 'hello78_quorum'

try:
    channel.queue_declare(queue=QUEUE_NAME, durable=True, arguments={'x-queue-type': 'quorum'})
except pika.exceptions.ChannelClosedByBroker as e:
    print("ERROR: la cola existe con parámetros diferentes:", e)
    print("Opciones: usar otro nombre de cola, borrar la cola existente o quitar 'x-queue-type'.")
    connection.close()
    raise

channel.basic_publish(exchange='', routing_key=QUEUE_NAME, body='Profe mi punto adicional, PD:cesar lopez!')
print(" [x] Sent 'Mensaje Completado!'")
connection.close()