package com.example.server50

import android.util.Log
import java.net.ServerSocket
import java.net.Socket

class TCPServer50(
    private val portIp__: Int,
    private val messageListener: OnMessageReceived?
) {

    @Volatile
    private var running = false

    private var serverSocket: ServerSocket? = null
    private val maxClientes = 10
    private val sendclis = arrayOfNulls<TCPServerThread50>(maxClientes + 1)

    companion object {
        const val SERVERPORT = 8189
        private const val TAG = "TCPServer50"
    }

    fun getMessageListener(): OnMessageReceived? = messageListener

    fun run() {
        running = true

        try {
            Log.d(TAG, "TCP Server S: Connecting...")
            serverSocket = ServerSocket(portIp__)

            while (running) {
                val client: Socket = serverSocket!!.accept()
                val idDisponible = obtenerIdDisponible()

                if (idDisponible == -1) {
                    Log.e(TAG, "Servidor lleno. Cliente rechazado.")
                    try {
                        client.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cerrando cliente rechazado: ${e.message}")
                    }
                    continue
                }

                val ipCliente = client.inetAddress.hostAddress ?: "IP desconocida"
                val hiloCliente = TCPServerThread50(
                    client = client,
                    tcpserver = this,
                    clientID = idDisponible,
                    clientIP = ipCliente
                )

                registrarCliente(idDisponible, hiloCliente)
                Thread(hiloCliente).start()

                Log.d(TAG, "Cliente conectado en slot $idDisponible")
            }

        } catch (e: Exception) {
            if (running) {
                Log.e(TAG, "Error en servidor: ${e.message}", e)
            } else {
                Log.d(TAG, "Servidor detenido correctamente.")
            }
        } finally {
            cerrarTodosLosClientes()
            try {
                serverSocket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error cerrando ServerSocket: ${e.message}")
            }
            serverSocket = null
            running = false
        }
    }

    @Synchronized
    private fun obtenerIdDisponible(): Int {
        for (i in 1..maxClientes) {
            if (sendclis[i] == null) return i
        }
        return -1
    }

    @Synchronized
    private fun registrarCliente(id: Int, cliente: TCPServerThread50) {
        sendclis[id] = cliente
    }

    @Synchronized
    fun liberarCliente(id: Int) {
        if (id in 1..maxClientes) {
            sendclis[id] = null
            Log.d(TAG, "ID liberado: Client $id")
        }
    }

    fun notificarMensajeCliente(clientID: Int, message: String) {
        Log.d(TAG, "Mensaje recibido de Client $clientID: $message")
        notifyUI(message)
        broadcastRaw(message)
    }

    fun notificarDesconexionCliente(clientID: Int) {
        Log.d(TAG, "Cliente desconectado en slot $clientID")
    }

    fun sendMessageTCPServer(message: String) {
        broadcastRaw(message)
    }

    @Synchronized
    private fun broadcastRaw(message: String) {
        for (i in 1..maxClientes) {
            val cliente = sendclis[i]
            if (cliente != null) {
                Thread {
                    cliente.sendMessage(message)
                    Log.d(TAG, "Mensaje reenviado a Client $i")
                }.start()
            }
        }
    }

    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando servidor: ${e.message}")
        }
        cerrarTodosLosClientes()
    }

    @Synchronized
    private fun cerrarTodosLosClientes() {
        for (i in 1..maxClientes) {
            sendclis[i]?.stopClient()
            sendclis[i] = null
        }
    }

    private fun notifyUI(message: String) {
        messageListener?.messageReceived(message)
    }

    interface OnMessageReceived {
        fun messageReceived(message: String)
    }
}
