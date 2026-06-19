package com.example.cliente50

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.MediaStore
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

    companion object {
        const val EXTRA_QR_IP = "QR_IP"
        const val EXTRA_QR_PORT = "QR_PORT"
        const val EXTRA_QR_AUTO_CONNECT = "QR_AUTO_CONNECT"
        const val EXTRA_QR_SILENT_JOIN = "QR_SILENT_JOIN"
        const val EXTRA_QR_CLONE_NAME = "QR_CLONE_NAME"
        private const val REQUEST_PICK_FILE = 6001
    }

    private lateinit var etMensaje: EditText
    private lateinit var etNombre: EditText
    private lateinit var btnEnviar: Button
    private lateinit var btnConectar: Button
    private lateinit var btnDesconectar: Button
    private lateinit var btnAdjuntar: Button
    private lateinit var btnTiendaVirtual: Button
    private lateinit var btnOverflowMenu: ImageButton
    private lateinit var tvMensajes: TextView

    private lateinit var etPorts: EditText
    private lateinit var etIp: EditText
    private lateinit var scrollView: ScrollView

    private var mTcpClient: TCPClient50? = null
    private var isConnected = false
    private var silentJoinOnNextConnect = false
    private var silentSession = false
    private var tiendaIp: String = ""
    private var tiendaPort: Int = 8190

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
        setupListeners()
        estadoInicial()
        agregarMensaje("Ingrese nombre, IP y puerto del servidor para conectarse.")
        procesarIntentDeQr(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        procesarIntentDeQr(intent)
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
        etMensaje = findViewById(R.id.etMensaje_)
        etNombre = findViewById(R.id.etNombre)
        btnEnviar = findViewById(R.id.btnEnviar_)
        btnConectar = findViewById(R.id.btnConectar_)
        btnDesconectar = findViewById(R.id.btnDesconectar_)
        btnAdjuntar = findViewById(R.id.btnAdjuntar_)
        btnTiendaVirtual = findViewById(R.id.btnTiendaVirtual)
        btnOverflowMenu = findViewById(R.id.btnOverflowMenu)

        tvMensajes = findViewById(R.id.tvMensajes_)
        etPorts = findViewById(R.id.etPort_)
        etIp = findViewById(R.id.etIP_)
        scrollView = findViewById(R.id.scrollView_)
    }

    private fun setupListeners() {
        btnConectar.setOnClickListener { conectarAlServidor() }
        btnDesconectar.setOnClickListener { desconectarDelServidor() }
        btnEnviar.setOnClickListener { enviarMensaje() }
        btnAdjuntar.setOnClickListener { seleccionarArchivo() }
        btnTiendaVirtual.setOnClickListener { abrirTiendaVirtual() }
        btnOverflowMenu.setOnClickListener { mostrarOverflowMenu() }
    }

    private fun mostrarOverflowMenu() {
        val popupMenu = PopupMenu(this, btnOverflowMenu)

        val itemUnirse = popupMenu.menu.add("Unirse a Grupo de Chat")
        itemUnirse.isEnabled = isConnected

        val itemVincular = popupMenu.menu.add("Vincular Dispositivo")
        itemVincular.isEnabled = !isConnected

        val itemScanClonar = popupMenu.menu.add("Scanear p/clonar Dispositivo")
        itemScanClonar.isEnabled = !isConnected

        val itemQrClonar = popupMenu.menu.add("QR p/clonar Dispositivo")
        itemQrClonar.isEnabled = isConnected

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Unirse a Grupo de Chat" -> {
                    unirseGrupoChat()
                    true
                }
                "Vincular Dispositivo" -> {
                    vincularDispositivo()
                    true
                }
                "Scanear p/clonar Dispositivo" -> {
                    scanearParaClonarDispositivo()
                    true
                }
                "QR p/clonar Dispositivo" -> {
                    mostrarQrParaClonarDispositivo()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun unirseGrupoChat() {
        if (!isConnected) {
            Toast.makeText(this, "Primero conecta el cliente al servidor", Toast.LENGTH_SHORT).show()
            return
        }

        val serverIp = etIp.text.toString().trim()
        val serverPort = etPorts.text.toString().trim()

        val intent = Intent(this, NuevoGrupoChatQrActivity::class.java).apply {
            putExtra(NuevoGrupoChatQrActivity.EXTRA_SERVER_IP, serverIp)
            putExtra(NuevoGrupoChatQrActivity.EXTRA_SERVER_PORT, serverPort)
            putExtra(NuevoGrupoChatQrActivity.EXTRA_CLONE_MODE, false)
        }
        startActivity(intent)
    }

    private fun scanearParaClonarDispositivo() {
        if (isConnected) {
            Toast.makeText(this, "Desconéctate para clonar en este dispositivo", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, ScanQrActivity::class.java).apply {
            putExtra(ScanQrActivity.EXTRA_REQUIRE_CLONE_QR, true)
        }
        startActivity(intent)
    }

    private fun mostrarQrParaClonarDispositivo() {
        if (!isConnected) {
            Toast.makeText(this, "Conéctate al servidor para generar el QR de clonación", Toast.LENGTH_SHORT).show()
            return
        }

        val serverIp = etIp.text.toString().trim()
        val serverPort = etPorts.text.toString().trim()

        val intent = Intent(this, NuevoGrupoChatQrActivity::class.java).apply {
            putExtra(NuevoGrupoChatQrActivity.EXTRA_SERVER_IP, serverIp)
            putExtra(NuevoGrupoChatQrActivity.EXTRA_SERVER_PORT, serverPort)
            putExtra(NuevoGrupoChatQrActivity.EXTRA_CLONE_MODE, true)
            putExtra(NuevoGrupoChatQrActivity.EXTRA_CLONE_NAME, nombreActual())
        }
        startActivity(intent)
    }

    private fun vincularDispositivo() {
        startActivity(Intent(this, VincularDispositivoActivity::class.java))
    }

    private fun procesarIntentDeQr(intent: Intent?) {
        val ip = intent?.getStringExtra(EXTRA_QR_IP)?.trim().orEmpty()
        val port = intent?.getIntExtra(EXTRA_QR_PORT, -1) ?: -1
        val autoConnect = intent?.getBooleanExtra(EXTRA_QR_AUTO_CONNECT, false) ?: false
        val silentJoin = intent?.getBooleanExtra(EXTRA_QR_SILENT_JOIN, false) ?: false
        val cloneName = intent?.getStringExtra(EXTRA_QR_CLONE_NAME)?.trim().orEmpty()

        if (ip.isBlank() || port !in 1..65535) return

        etIp.setText(ip)
        etPorts.setText(port.toString())

        // Si el QR es para clonar, este dispositivo debe adoptar el nombre
        // del cliente que generó el QR. Así el servidor y los chats ven el
        // mismo usuario en la nueva conexión clonada.
        if (silentJoin && cloneName.isNotBlank()) {
            etNombre.setText(cloneName)
        }

        silentJoinOnNextConnect = silentJoin

        Toast.makeText(this, "Datos QR cargados", Toast.LENGTH_SHORT).show()

        if (autoConnect && !isConnected) conectarAlServidor()
    }

    private fun estadoInicial() {
        isConnected = false
        habilitarEnvio(false)
        btnConectar.isEnabled = true
        btnDesconectar.isEnabled = false
    }

    private fun conectarAlServidor() {
        val serverIp = etIp.text.toString().trim()
        val puertoTexto = etPorts.text.toString().trim()

        if (serverIp.isEmpty()) {
            Toast.makeText(this, "Ingresa la IP del servidor", Toast.LENGTH_SHORT).show()
            return
        }
        if (puertoTexto.isEmpty()) {
            Toast.makeText(this, "Ingresa el puerto del servidor", Toast.LENGTH_SHORT).show()
            return
        }

        val puerto = try {
            puertoTexto.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Puerto inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (puerto !in 1..65535) {
            Toast.makeText(this, "Puerto inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val errorRedLocal = validarConexionRedLocal(serverIp)
        if (errorRedLocal != null) {
            agregarMensaje(errorRedLocal)
            Toast.makeText(this, errorRedLocal, Toast.LENGTH_LONG).show()
            return
        }

        if (isConnected) {
            Toast.makeText(this, "Ya estás conectado al servidor", Toast.LENGTH_SHORT).show()
            return
        }

        habilitarEnvio(false)
        btnConectar.isEnabled = false
        btnDesconectar.isEnabled = true

        mTcpClient = TCPClient50(
            serverIp,
            puerto,
            object : TCPClient50.OnTCPClientListener {
                override fun onConnected() {
                    isConnected = true
                    habilitarEnvio(true)
                    btnConectar.isEnabled = false
                    btnDesconectar.isEnabled = true

                    silentSession = silentJoinOnNextConnect
                    if (!silentSession) {
                        mTcpClient?.sendMessage(ChatProtocol.encodeMessage(nombreActual(), "conectado"))
                    }
                    silentJoinOnNextConnect = false
                    Toast.makeText(this@MainActivity, if (silentSession) "Clon conectado al servidor" else "Conectado al servidor", Toast.LENGTH_SHORT).show()
                }

                override fun onConnectionError(error: String) {
                    isConnected = false
                    mTcpClient = null
                    habilitarEnvio(false)
                    btnConectar.isEnabled = true
                    btnDesconectar.isEnabled = false

                    silentJoinOnNextConnect = false
                    silentSession = false
                    agregarMensaje("Error de conexión: $error")
                    Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                }

                override fun onDisconnected(manual: Boolean) {
                    val estabaConectado = isConnected
                    isConnected = false
                    mTcpClient = null
                    habilitarEnvio(false)
                    btnConectar.isEnabled = true
                    btnDesconectar.isEnabled = false

                    if (!manual && estabaConectado) agregarMensaje("Servidor desconectado")
                    silentJoinOnNextConnect = false
                    silentSession = false
                }

                override fun messageReceived(message: String) {
                    procesarMensajeRecibido(message)
                }
            }
        )

        mTcpClient?.run()
    }

    private fun desconectarDelServidor() {
        if (mTcpClient != null) {
            if (isConnected && !silentSession) {
                mTcpClient?.sendMessage(ChatProtocol.encodeMessage(nombreActual(), "desconectado"))
            }
            mTcpClient?.stopClient()
            mTcpClient = null
        }

        isConnected = false
        habilitarEnvio(false)
        btnConectar.isEnabled = true
        btnDesconectar.isEnabled = false
        silentJoinOnNextConnect = false
        silentSession = false
    }

    private fun enviarMensaje() {
        val mensaje = etMensaje.text.toString().trim()
        if (mensaje.isEmpty()) {
            Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isConnected || mTcpClient == null) {
            Toast.makeText(this, "No estás conectado al servidor", Toast.LENGTH_SHORT).show()
            return
        }

        mTcpClient?.sendMessage(ChatProtocol.encodeMessage(nombreActual(), mensaje))
        etMensaje.text.clear()
    }

    private fun seleccionarArchivo() {
        if (!isConnected || mTcpClient == null) {
            Toast.makeText(this, "Primero conéctate al servidor", Toast.LENGTH_SHORT).show()
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

            mTcpClient?.sendMessage(ChatProtocol.encodeFile(nombreActual(), fileName, mimeType, bytes))
            Toast.makeText(this, "Archivo enviado: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            agregarMensaje("Error enviando archivo: ${e.message}")
        }
    }

    private fun procesarMensajeRecibido(raw: String) {
        val tienda = ChatProtocol.parseStoreConfig(raw)
        if (tienda != null) {
            tiendaIp = tienda.first
            tiendaPort = tienda.second
            return
        }
        val packet = ChatProtocol.parse(raw)
        val visible = ChatProtocol.display(raw)
        if (visible.isNotBlank()) agregarMensaje(visible)
        if (ChatProtocol.isFile(packet)) guardarArchivoRecibido(packet)
    }

    private fun abrirTiendaVirtual() {
        val prevIp = etIp.text.toString().trim()
        val prevPort = etPorts.text.toString().trim().toIntOrNull() ?: 8189
        val ipTienda = tiendaIp.ifBlank { prevIp }
        val portTienda = if (tiendaPort in 1..65535) tiendaPort else 8190

        if (ipTienda.isBlank()) {
            Toast.makeText(this, "No se recibió IP de Server50Tienda", Toast.LENGTH_LONG).show()
            return
        }

        val estabaConectado = isConnected
        desconectarDelServidor()

        val intent = Intent(this, TiendaVirtualActivity::class.java).apply {
            putExtra(TiendaVirtualActivity.EXTRA_TIENDA_IP, ipTienda)
            putExtra(TiendaVirtualActivity.EXTRA_TIENDA_PORT, portTienda)
            putExtra(TiendaVirtualActivity.EXTRA_PREV_IP, prevIp)
            putExtra(TiendaVirtualActivity.EXTRA_PREV_PORT, prevPort)
            putExtra(TiendaVirtualActivity.EXTRA_NOMBRE, nombreActual())
            putExtra(TiendaVirtualActivity.EXTRA_RECONNECT_PREVIOUS, estabaConectado)
        }
        startActivity(intent)
    }


    private fun validarConexionRedLocal(serverIp: String): String? {
        val ipServidor = serverIp.trim()

        if (!esIpv4(ipServidor) || !esIpPrivadaIpv4(ipServidor)) {
            return "La IP del servidor debe ser una IP de red local válida. Ejemplo: 192.168.0.164"
        }

        val ipsLocales = obtenerIpsLocalesPrivadas()
        if (ipsLocales.isEmpty()) {
            return "Este móvil no tiene una IP de red local activa. Conéctalo al mismo Wi-Fi que el servidor."
        }

        val mismaRed = ipsLocales.any { estaEnMismaRedLocal(it, ipServidor) }
        if (!mismaRed) {
            return "El móvil no está en la misma red local. Móvil: ${ipsLocales.joinToString()} / Servidor: $ipServidor"
        }

        return null
    }

    private fun obtenerIpsLocalesPrivadas(): List<String> {
        val result = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (!ni.isUp || ni.isLoopback) continue

                val addresses = ni.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val ip = address.hostAddress?.trim().orEmpty()
                        if (esIpPrivadaIpv4(ip) && !ip.startsWith("169.254.")) {
                            result.add(ip)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result.distinct()
    }

    private fun estaEnMismaRedLocal(ipA: String, ipB: String): Boolean {
        val a = ipA.split(".")
        val b = ipB.split(".")
        if (a.size != 4 || b.size != 4) return false
        // Para este proyecto se usa red local simple /24: 192.168.0.x, 192.168.1.x, etc.
        return a[0] == b[0] && a[1] == b[1] && a[2] == b[2]
    }

    private fun esIpv4(ip: String): Boolean {
        val p = ip.split(".")
        return p.size == 4 && p.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }
    }

    private fun esIpPrivadaIpv4(ip: String): Boolean {
        val p = ip.split(".")
        if (p.size != 4) return false
        val a = p[0].toIntOrNull() ?: return false
        val b = p[1].toIntOrNull() ?: return false
        return a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168)
    }

    private fun nombreActual(): String = etNombre.text.toString().trim().ifBlank { "Cliente móvil" }

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
            agregarMensaje("Archivo guardado en: $rutaVisible")
        } catch (e: Exception) {
            try {
                val rutaPrivada = guardarEnCarpetaPrivada(safeName, bytes)
                agregarMensaje("No se pudo guardar en Descargas públicas. Archivo guardado en carpeta privada: $rutaPrivada")
            } catch (e2: Exception) {
                agregarMensaje("No se pudo guardar el archivo recibido: ${e2.message}")
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

    private fun agregarMensaje(mensaje: String) {
        tvMensajes.append("$mensaje\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun habilitarEnvio(habilitado: Boolean) {
        btnEnviar.isEnabled = habilitado
        btnAdjuntar.isEnabled = habilitado
        etMensaje.isEnabled = habilitado
    }

    override fun onDestroy() {
        if (isConnected && !silentSession) {
            mTcpClient?.sendMessage(ChatProtocol.encodeMessage(nombreActual(), "desconectado"))
        }
        mTcpClient?.stopClient()
        mTcpClient = null
        super.onDestroy()
    }
}
