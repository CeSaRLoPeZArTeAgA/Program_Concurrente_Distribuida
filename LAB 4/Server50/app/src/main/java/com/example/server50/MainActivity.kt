package com.example.server50

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private var mTcpServer: TCPServer50? = null
    private var serverThread: Thread? = null

    private lateinit var tvMessages: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var etMessage: EditText
    private lateinit var etPorts: EditText
    private lateinit var etNombre: EditText
    private lateinit var etTiendaIp: EditText
    private lateinit var etTiendaPort: EditText
    private lateinit var tvIpAddress: TextView

    private lateinit var btnIniciar: Button
    private lateinit var btnSend: Button
    private lateinit var btnStop: Button
    private lateinit var btnAttach: Button
    private lateinit var btnTiendaVirtual: Button
    private lateinit var btnOverflowMenu: ImageButton

    private val messages = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var servidorActivo = false

    @Volatile
    private var cerrandoServidor = false

    companion object {
        private const val TAG = "Servidor50"
        private const val REQUEST_PICK_FILE = 5001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        configureSystemBars()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = maxOf(systemBars.bottom, ime.bottom)

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

        initViews()

        val ipLocal = obtenerIpLocal()
        tvIpAddress.text = ipLocal
        etTiendaIp.setText(ipLocal)

        addMessageToUI("Presione Iniciar Ser para levantar el servidor.")
    }

    private fun configureSystemBars() {
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    private fun initViews() {
        tvMessages = findViewById(R.id.tvMessages)
        scrollView = findViewById(R.id.scrollview)
        etMessage = findViewById(R.id.etMessage)
        etPorts = findViewById(R.id.etPort_)
        etNombre = findViewById(R.id.etNombre)
        etTiendaIp = findViewById(R.id.etTiendaIp)
        etTiendaPort = findViewById(R.id.etTiendaPort)
        tvIpAddress = findViewById(R.id.tvIpAddress)

        btnIniciar = findViewById(R.id.btnIniciar)
        btnSend = findViewById(R.id.btnSend)
        btnStop = findViewById(R.id.btnStop)
        btnAttach = findViewById(R.id.btnAttach)
        btnTiendaVirtual = findViewById(R.id.btnTiendaVirtual)
        btnOverflowMenu = findViewById(R.id.btnOverflowMenu)

        btnIniciar.setOnClickListener { iniciarServidor() }

        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                servidorEnviaTexto(message)
                etMessage.text.clear()
            } else {
                addMessageToUI("No se puede enviar un mensaje vacío.")
            }
        }

        btnAttach.setOnClickListener { seleccionarArchivo() }
        btnTiendaVirtual.setOnClickListener { abrirTiendaVirtual() }
        btnStop.setOnClickListener { detenerServidor() }
        btnOverflowMenu.setOnClickListener { mostrarOverflowMenu() }
    }

    private fun mostrarOverflowMenu() {
        val popupMenu = PopupMenu(this, btnOverflowMenu)
        val itemUnirse = popupMenu.menu.add("Unirse a Grupo de Chat")
        itemUnirse.isEnabled = servidorActivo

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Unirse a Grupo de Chat" -> {
                    abrirPantallaNuevoGrupoChat()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun abrirPantallaNuevoGrupoChat() {
        val puertoTexto = etPorts.text.toString().trim()
        val puerto = puertoTexto.toIntOrNull()

        if (puerto == null || puerto !in 1..65535) {
            Toast.makeText(this, "Puerto inválido. Ingrese un puerto entre 1 y 65535.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, NuevoGrupoChatQrActivity::class.java).apply {
            putExtra(NuevoGrupoChatQrActivity.EXTRA_SERVER_IP, tvIpAddress.text.toString())
            putExtra(NuevoGrupoChatQrActivity.EXTRA_SERVER_PORT, puerto.toString())
        }
        startActivity(intent)
    }

    private fun iniciarServidor() {
        if (servidorActivo) {
            Toast.makeText(this, "El servidor ya está iniciado", Toast.LENGTH_SHORT).show()
            return
        }

        val puertoTexto = etPorts.text.toString().trim()
        val puerto = puertoTexto.toIntOrNull()

        if (puerto == null || puerto !in 1..65535) {
            Toast.makeText(this, "Puerto inválido", Toast.LENGTH_SHORT).show()
            return
        }

        servidorActivo = true
        cerrandoServidor = false

        serverThread = Thread {
            try {
                mTcpServer = TCPServer50(
                    puerto,
                    object : TCPServer50.OnMessageReceived {
                        override fun messageReceived(message: String) {
                            handler.post { servidorRecibe(message) }
                        }
                    }
                )

                Log.d(TAG, "Servidor iniciado, IP: ${tvIpAddress.text}, Puerto: $puerto")
                mTcpServer?.run()

            } catch (e: Exception) {
                Log.e(TAG, "Error general del servidor: ${e.message}", e)
                handler.post { addMessageToUI("Error del servidor: ${e.message}") }
            } finally {
                servidorActivo = false
                handler.post { }
            }
        }

        serverThread?.start()
    }

    private fun servidorRecibe(raw: String) {
        val packet = ChatProtocol.parse(raw)
        val visible = ChatProtocol.display(raw)
        if (visible.isNotBlank()) addMessageToUI(visible)

        if (packet.text.equals("conectado", ignoreCase = true)) {
            enviarConfiguracionTienda()
        }

        if (ChatProtocol.isFile(packet)) {
            guardarArchivoRecibido(packet)
        }

        Log.d(TAG, "Servidor recibió: $raw")
    }

    private fun enviarConfiguracionTienda() {
        val ipTienda = etTiendaIp.text.toString().trim().ifBlank { tvIpAddress.text.toString().trim() }
        val portTienda = etTiendaPort.text.toString().trim().toIntOrNull() ?: 8190
        if (ipTienda.isNotBlank() && portTienda in 1..65535) {
            mTcpServer?.sendMessageTCPServer(ChatProtocol.encodeStoreConfig(ipTienda, portTienda))
        }
    }

    private fun abrirTiendaVirtual() {
        val ipTienda = etTiendaIp.text.toString().trim().ifBlank { tvIpAddress.text.toString().trim() }
        val portTienda = etTiendaPort.text.toString().trim().toIntOrNull() ?: 8190
        if (ipTienda.isBlank() || portTienda !in 1..65535) {
            Toast.makeText(this, "Ingrese IP y puerto de Server50Tienda", Toast.LENGTH_LONG).show()
            return
        }
        detenerServidor()
        val intent = Intent(this, TiendaVirtualActivity::class.java).apply {
            putExtra(TiendaVirtualActivity.EXTRA_TIENDA_IP, ipTienda)
            putExtra(TiendaVirtualActivity.EXTRA_TIENDA_PORT, portTienda)
            putExtra(TiendaVirtualActivity.EXTRA_PREV_IP, tvIpAddress.text.toString().trim())
            putExtra(TiendaVirtualActivity.EXTRA_PREV_PORT, etPorts.text.toString().trim().toIntOrNull() ?: 8189)
            putExtra(TiendaVirtualActivity.EXTRA_NOMBRE, nombreActual())
            putExtra(TiendaVirtualActivity.EXTRA_RECONNECT_PREVIOUS, false)
        }
        startActivity(intent)
    }

    private fun servidorEnviaTexto(texto: String) {
        if (!servidorActivo || mTcpServer == null) {
            Toast.makeText(this, "Primero inicia el servidor", Toast.LENGTH_SHORT).show()
            return
        }

        val raw = ChatProtocol.encodeMessage(nombreActual(), texto)
        mTcpServer?.sendMessageTCPServer(raw)
        addMessageToUI(ChatProtocol.display(raw))
        Log.d(TAG, "Servidor envió: $raw")
    }

    private fun servidorEnviaArchivo(fileName: String, mimeType: String, bytes: ByteArray) {
        if (!servidorActivo || mTcpServer == null) {
            Toast.makeText(this, "Primero inicia el servidor", Toast.LENGTH_SHORT).show()
            return
        }

        val raw = ChatProtocol.encodeFile(nombreActual(), fileName, mimeType, bytes)
        mTcpServer?.sendMessageTCPServer(raw)
        addMessageToUI(ChatProtocol.display(raw))
    }

    private fun seleccionarArchivo() {
        if (!servidorActivo || mTcpServer == null) {
            Toast.makeText(this, "Primero inicia el servidor", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_FILE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            enviarArchivoDesdeUri(uri)
        }
    }

    private fun enviarArchivoDesdeUri(uri: Uri) {
        try {
            val fileName = obtenerNombreArchivo(uri)
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }

            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this, "No se pudo leer el archivo", Toast.LENGTH_SHORT).show()
                return
            }

            servidorEnviaArchivo(fileName, mimeType, bytes)
            Toast.makeText(this, "Archivo enviado: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            addMessageToUI("Error enviando archivo: ${e.message}")
        }
    }

    private fun detenerServidor() {
        if (!servidorActivo && mTcpServer == null) {
            Toast.makeText(this, "El servidor ya está detenido", Toast.LENGTH_SHORT).show()
            return
        }

        if (cerrandoServidor) {
            Toast.makeText(this, "El servidor ya se está deteniendo", Toast.LENGTH_SHORT).show()
            return
        }

        cerrandoServidor = true

        Thread {
            try {
                mTcpServer?.stop()
                mTcpServer = null
                servidorActivo = false
                Log.d(TAG, "Servidor detenido")

                handler.post {
                    cerrandoServidor = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al detener servidor: ${e.message}", e)
                handler.post {
                    addMessageToUI("Error al detener servidor: ${e.message}")
                    cerrandoServidor = false
                }
            }
        }.start()
    }

    private fun nombreActual(): String = etNombre.text.toString().trim().ifBlank { "Servidor" }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var result = "archivo_adjunto"
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                result = it.getString(nameIndex) ?: result
            }
        }
        return result
    }

    private fun guardarArchivoRecibido(packet: ChatProtocol.ChatPacket) {
        val bytes = packet.bytes ?: return
        val safeName = packet.fileName.replace(Regex("[\\/:*?\"<>|]"), "_").ifBlank { "archivo_adjunto" }
        val mimeType = packet.mimeType.ifBlank { "application/octet-stream" }

        try {
            val rutaVisible = guardarEnDescargasPublicas(safeName, mimeType, bytes)
            addMessageToUI("Archivo guardado en: $rutaVisible")
        } catch (e: Exception) {
            try {
                val rutaPrivada = guardarEnCarpetaPrivada(safeName, bytes)
                addMessageToUI("No se pudo guardar en Descargas públicas. Archivo guardado en carpeta privada: $rutaPrivada")
            } catch (e2: Exception) {
                addMessageToUI("No se pudo guardar el archivo recibido: ${e2.message}")
            }
        }
    }

    private fun guardarEnDescargasPublicas(fileName: String, mimeType: String, bytes: ByteArray): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = Environment.DIRECTORY_DOWNLOADS + "/DogMessengerRecibidos"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("No se pudo crear el archivo en Descargas")

            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
                output.flush()
            } ?: throw IllegalStateException("No se pudo abrir el archivo para escritura")

            val finishValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            contentResolver.update(uri, finishValues, null, null)

            "Descargas/DogMessengerRecibidos/$fileName"
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "DogMessengerRecibidos"
            )
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { it.write(bytes) }
            outFile.absolutePath
        }
    }

    private fun guardarEnCarpetaPrivada(fileName: String, bytes: ByteArray): String {
        val baseDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
        val dir = File(baseDir, "DogMessengerRecibidos")
        if (!dir.exists()) dir.mkdirs()
        val outFile = File(dir, fileName)
        FileOutputStream(outFile).use { it.write(bytes) }
        return outFile.absolutePath
    }

    private fun obtenerIpLocal(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val direcciones = networkInterface.inetAddresses
                while (direcciones.hasMoreElements()) {
                    val direccion = direcciones.nextElement()
                    if (!direccion.isLoopbackAddress && direccion is Inet4Address) {
                        return direccion.hostAddress ?: "IP no disponible"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo IP: ${e.message}", e)
        }
        return "IP no disponible"
    }

    private fun addMessageToUI(message: String) {
        handler.post {
            messages.append(message).append("\n")
            tvMessages.text = messages.toString()
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    override fun onDestroy() {
        try {
            mTcpServer?.stop()
            mTcpServer = null
            servidorActivo = false
        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroy: ${e.message}", e)
        }
        super.onDestroy()
    }
}
