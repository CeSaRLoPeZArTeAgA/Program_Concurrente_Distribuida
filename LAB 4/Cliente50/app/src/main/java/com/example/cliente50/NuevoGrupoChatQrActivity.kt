package com.example.cliente50

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLEncoder

class NuevoGrupoChatQrActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SERVER_IP = "SERVER_IP"
        const val EXTRA_SERVER_PORT = "SERVER_PORT"
        const val EXTRA_CLONE_MODE = "CLONE_MODE"
        const val EXTRA_CLONE_NAME = "CLONE_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        configureSystemBars()
        setContentView(R.layout.activity_nuevo_grupo_chat_qr)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainQr)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val ip = intent.getStringExtra(EXTRA_SERVER_IP)
            ?.takeIf { it.isNotBlank() && it != "IP no disponible" }
            ?: obtenerIpLocal()

        val port = intent.getStringExtra(EXTRA_SERVER_PORT)
            ?.toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: 8189

        val cloneMode = intent.getBooleanExtra(EXTRA_CLONE_MODE, false)
        val cloneName = intent.getStringExtra(EXTRA_CLONE_NAME)?.trim().orEmpty()
        val qrContent = if (cloneMode) {
            val encodedName = URLEncoder.encode(cloneName.ifBlank { "Cliente" }, "UTF-8")
            "dogmsg://$ip:$port?clone=1&name=$encodedName"
        } else {
            "dogmsg://$ip:$port"
        }
        val imgQrServidor = findViewById<ImageView>(R.id.imgQrServidor)
        val txtDatosQr = findViewById<TextView>(R.id.txtDatosQr)
        val txtIndicacionQr = findViewById<TextView>(R.id.txtIndicacionQr)

        txtIndicacionQr.text = if (cloneMode) {
            "SCANEA EL QR PARA CLONAR\nDISPOSITIVO"
        } else {
            "SCANEA EL QR PARA INGRESO A\nGRUPO CHAT"
        }
        imgQrServidor.setImageBitmap(SimpleQrCodeGenerator.createQrBitmap(qrContent, 720))
        txtDatosQr.text = if (cloneMode && cloneName.isNotBlank()) {
            "Datos de conexión: $ip:$port\nNombre clonado: $cloneName"
        } else {
            "Datos de conexión: $ip:$port"
        }

        val btnRegresarPrincipal = findViewById<Button>(R.id.btnRegresarPrincipal)
        btnRegresarPrincipal.setOnClickListener {
            finish()
        }
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

    private fun obtenerIpLocal(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()

            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()

                if (!networkInterface.isUp || networkInterface.isLoopback) {
                    continue
                }

                val direcciones = networkInterface.inetAddresses

                while (direcciones.hasMoreElements()) {
                    val direccion = direcciones.nextElement()

                    if (!direccion.isLoopbackAddress && direccion is Inet4Address) {
                        return direccion.hostAddress ?: "IP no disponible"
                    }
                }
            }
        } catch (_: Exception) {
            // Se mantiene el valor controlado si no se puede leer la IP local.
        }

        return "IP no disponible"
    }
}
