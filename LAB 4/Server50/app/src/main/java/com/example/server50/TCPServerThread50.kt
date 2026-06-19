package com.example.server50

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

class TCPServerThread50(
    private var client: Socket,
    private val tcpserver: TCPServer50,
    private val clientID: Int,
    private val clientIP: String
) : Runnable {

    @Volatile
    private var running = false

    private var mOut: PrintWriter? = null
    private var inReader: BufferedReader? = null

    companion object {
        private const val TAG = "TCPServerThread50"
    }

    override fun run() {
        running = true

        try {
            mOut = PrintWriter(BufferedWriter(OutputStreamWriter(client.getOutputStream())), true)
            inReader = BufferedReader(InputStreamReader(client.getInputStream()))

            Log.d(TAG, "[Client $clientID] Hilo iniciado")

            while (running) {
                val mensajeCliente = inReader?.readLine() ?: break
                if (mensajeCliente.isNotBlank()) {
                    tcpserver.notificarMensajeCliente(clientID, mensajeCliente)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "[Client $clientID] Error: ${e.message}")
        } finally {
            cerrarCliente()
        }
    }

    fun sendMessage(message: String) {
        try {
            val writer = mOut
            if (writer != null && !writer.checkError()) {
                writer.println(message)
                writer.flush()
                Log.d(TAG, "Mensaje enviado a Client $clientID")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando mensaje a Client $clientID: ${e.message}")
        }
    }

    fun stopClient() {
        running = false
        cerrarSocket()
    }

    private fun cerrarCliente() {
        running = false
        cerrarSocket()
        tcpserver.liberarCliente(clientID)
        tcpserver.notificarDesconexionCliente(clientID)
        Log.d(TAG, "[Client $clientID] Desconectado")
    }

    private fun cerrarSocket() {
        try { inReader?.close() } catch (_: Exception) {}
        try { mOut?.close() } catch (_: Exception) {}
        try { client.close() } catch (e: Exception) {
            Log.e(TAG, "Error cerrando socket de Client $clientID: ${e.message}")
        }
    }
}
