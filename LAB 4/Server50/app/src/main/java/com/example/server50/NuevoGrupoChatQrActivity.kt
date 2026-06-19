package com.example.server50

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.Inet4Address
import java.net.NetworkInterface

class NuevoGrupoChatQrActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SERVER_IP = "SERVER_IP"
        const val EXTRA_SERVER_PORT = "SERVER_PORT"
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

        // Formato simple para que el Client pueda leer IP y puerto:
        // dogmsg://192.168.0.163:8189
        val qrContent = "dogmsg://$ip:$port"
        val imgQrServidor = findViewById<ImageView>(R.id.imgQrServidor)
        imgQrServidor.setImageBitmap(SimpleQrCodeGenerator.createQrBitmap(qrContent, 720))

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
            // Si no se puede obtener la IP, se retorna un texto controlado.
        }

        return "IP no disponible"
    }
}
