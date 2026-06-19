package com.example.server50

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TCPClient50(
    private val serverIp: String,
    private val serverPort: Int,
    private val listener: OnTCPClientListener?
) {
    companion object {
        private const val TAG = "TCPClient50"
        private const val CONNECT_TIMEOUT_MS = 5000
    }

    @Volatile
    private var mRun = false

    @Volatile
    private var manualDisconnect = false

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var input: BufferedReader? = null

    private val sendExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val closeExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun run() {
        Thread {
            mRun = true
            manualDisconnect = false

            try {
                val host = serverIp.trim()
                if (host.isBlank()) {
                    throw IllegalArgumentException("IP del servidor vacía")
                }

                Log.d(TAG, "Conectando SOLO por red local a $host:$serverPort...")

                val nuevoSocket = Socket()
                nuevoSocket.connect(
                    InetSocketAddress(host, serverPort),
                    CONNECT_TIMEOUT_MS
                )

                socket = nuevoSocket

                out = PrintWriter(
                    BufferedWriter(OutputStreamWriter(nuevoSocket.getOutputStream())),
                    true
                )

                input = BufferedReader(
                    InputStreamReader(nuevoSocket.getInputStream())
                )

                Log.d(TAG, "Conexión establecida con $host:$serverPort")

                mainHandler.post {
                    listener?.onConnected()
                }

                while (mRun) {
                    val serverMsg = input?.readLine()

                    if (serverMsg == null) {
                        Log.d(TAG, "Servidor cerró la conexión")
                        break
                    }

                    if (serverMsg.isNotEmpty()) {
                        mainHandler.post {
                            listener?.messageReceived(serverMsg)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al conectar/comunicar: ${e.message}")

                if (!manualDisconnect) {
                    mainHandler.post {
                        listener?.onConnectionError(e.message ?: "Error desconocido")
                    }
                }

            } finally {
                closeResourcesInBackground()

                mainHandler.post {
                    listener?.onDisconnected(manualDisconnect)
                }
            }
        }.start()
    }

    fun sendMessage(message: String) {
        sendExecutor.execute {
            try {
                val writer = out

                if (writer != null && !writer.checkError()) {
                    writer.println(message)
                    writer.flush()
                    Log.d(TAG, "Mensaje enviado: $message")
                } else {
                    Log.e(TAG, "No se puede enviar: out es null o tiene error")

                    if (!manualDisconnect) {
                        mainHandler.post {
                            listener?.onConnectionError("No se puede enviar: conexión no establecida")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al enviar mensaje: ${e.message}")

                if (!manualDisconnect) {
                    mainHandler.post {
                        listener?.onConnectionError(e.message ?: "Error al enviar")
                    }
                }
            }
        }
    }

    fun stopClient() {
        manualDisconnect = true
        mRun = false
        closeResourcesInBackground()
    }

    private fun closeResourcesInBackground() {
        closeExecutor.execute {
            closeResources()
        }
    }

    @Synchronized
    private fun closeResources() {
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando socket: ${e.message}")
        }

        try {
            input?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando input: ${e.message}")
        }

        try {
            out?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando output: ${e.message}")
        }

        socket = null
        input = null
        out = null
    }

    interface OnTCPClientListener {
        fun onConnected()
        fun onConnectionError(error: String)
        fun onDisconnected(manual: Boolean)
        fun messageReceived(message: String)
    }
}
