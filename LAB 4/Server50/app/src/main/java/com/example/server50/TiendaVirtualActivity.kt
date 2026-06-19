package com.example.server50

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TiendaVirtualActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TIENDA_IP = "TIENDA_IP"
        const val EXTRA_TIENDA_PORT = "TIENDA_PORT"
        const val EXTRA_PREV_IP = "PREV_IP"
        const val EXTRA_PREV_PORT = "PREV_PORT"
        const val EXTRA_NOMBRE = "NOMBRE"
        const val EXTRA_RECONNECT_PREVIOUS = "RECONNECT_PREVIOUS"
    }

    private lateinit var tvDatos: TextView
    private lateinit var tvMensajes: TextView
    private lateinit var etConsulta: EditText
    private lateinit var scroll: ScrollView
    private lateinit var btnEnviar: Button
    private lateinit var btnVolver: Button
    private lateinit var btnCerrar: Button

    private var tiendaClient: TCPClient50? = null
    private val historial = StringBuilder()

    private var tiendaIp = ""
    private var tiendaPort = 8190
    private var prevIp = ""
    private var prevPort = 8189
    private var nombre = "Cliente"
    private var reconnectPrevious = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()
        setContentView(R.layout.activity_tienda_virtual)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left + 16, systemBars.top + 16, systemBars.right + 16, maxOf(systemBars.bottom, ime.bottom) + 16)
            insets
        }

        tvDatos = findViewById(R.id.tvDatosTienda)
        tvMensajes = findViewById(R.id.tvMensajesTienda)
        etConsulta = findViewById(R.id.etConsultaTienda)
        scroll = findViewById(R.id.scrollTienda)
        btnEnviar = findViewById(R.id.btnEnviarTienda)
        btnVolver = findViewById(R.id.btnVolverChat)
        btnCerrar = findViewById(R.id.btnCerrarApp)

        tiendaIp = intent.getStringExtra(EXTRA_TIENDA_IP)?.trim().orEmpty()
        tiendaPort = intent.getIntExtra(EXTRA_TIENDA_PORT, 8190)
        prevIp = intent.getStringExtra(EXTRA_PREV_IP)?.trim().orEmpty()
        prevPort = intent.getIntExtra(EXTRA_PREV_PORT, 8189)
        nombre = intent.getStringExtra(EXTRA_NOMBRE)?.trim().orEmpty().ifBlank { "Cliente" }
        reconnectPrevious = intent.getBooleanExtra(EXTRA_RECONNECT_PREVIOUS, true)

        if (tiendaIp.isBlank()) tiendaIp = prevIp
        if (tiendaPort !in 1..65535) tiendaPort = 8190

        tvDatos.text = "Server50Tienda: $tiendaIp:$tiendaPort"
        btnEnviar.setOnClickListener { enviarConsulta() }
        etConsulta.setOnEditorActionListener { _, _, _ -> enviarConsulta(); true }
        btnVolver.setOnClickListener { volverAlChatAnterior() }
        btnCerrar.setOnClickListener { cerrarAplicacion() }

        conectarATienda()
    }

    private fun configureSystemBars() {
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.systemUiVisibility = flags
        }
    }

    private fun conectarATienda() {
        if (tiendaIp.isBlank()) {
            agregar("No se recibió IP de Server50Tienda. Verifique el campo Tienda IP en Server50.")
            return
        }
        agregar("Conectando a Server50Tienda $tiendaIp:$tiendaPort...")
        tiendaClient = TCPClient50(tiendaIp, tiendaPort, object : TCPClient50.OnTCPClientListener {
            override fun onConnected() {
                agregar("Conectado a Tienda Virtual.")
                tiendaClient?.sendMessage(ChatProtocol.encodeMessage(nombre, "conectado"))
            }

            override fun onConnectionError(error: String) {
                agregar("Error de conexión con Server50Tienda: $error")
            }

            override fun onDisconnected(manual: Boolean) {
                agregar("Comunicación con Tienda Virtual cerrada.")
            }

            override fun messageReceived(message: String) {
                val show = ChatProtocol.display(message)
                if (show.isNotBlank()) agregar(show)
            }
        })
        tiendaClient?.run()
    }

    private fun enviarConsulta() {
        val consulta = etConsulta.text.toString().trim()
        if (consulta.isBlank()) return
        val c = tiendaClient
        if (c == null) {
            Toast.makeText(this, "No estás conectado a Server50Tienda", Toast.LENGTH_SHORT).show()
            return
        }
        c.sendMessage(ChatProtocol.encodeMessage(nombre, consulta))
        agregar("[$nombre]: $consulta")
        etConsulta.text.clear()
    }

    private fun volverAlChatAnterior() {
        tiendaClient?.sendMessage(ChatProtocol.encodeMessage(nombre, "desconectado"))
        tiendaClient?.stopClient()
        tiendaClient = null

        val intent = Intent(this, MainActivity::class.java).apply {
            // En la app servidor se vuelve a la pantalla principal del servidor.
            // El operador vuelve a presionar Iniciar Servidor si desea levantar el chat principal.
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun cerrarAplicacion() {
        tiendaClient?.sendMessage(ChatProtocol.encodeMessage(nombre, "desconectado"))
        tiendaClient?.stopClient()
        tiendaClient = null
        finishAffinity()
    }

    private fun agregar(texto: String) {
        runOnUiThread {
            historial.append(texto).append('\n')
            tvMensajes.text = historial.toString()
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    override fun onDestroy() {
        tiendaClient?.stopClient()
        tiendaClient = null
        super.onDestroy()
    }
}
