package com.example.cliente50

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class ScanQrActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUIRE_CLONE_QR = "REQUIRE_CLONE_QR"
    }

    private lateinit var barcodeScanner: DecoratedBarcodeView
    private var scanningStarted = false
    private var qrProcessed = false

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startQrScan()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        configureSystemBars()
        setContentView(R.layout.activity_scan_qr)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainScanQr)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        barcodeScanner = findViewById(R.id.barcodeScanner)
        barcodeScanner.setStatusText("")

        findViewById<TextView>(R.id.btnVolverScan).setOnClickListener {
            finish()
        }

        findViewById<android.widget.Button>(R.id.btnCancelarScan).setOnClickListener {
            finish()
        }

        requestCameraIfNeeded()
    }

    private fun configureSystemBars() {
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }
    }

    private fun requestCameraIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startQrScan()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startQrScan() {
        if (scanningStarted || qrProcessed) {
            return
        }

        scanningStarted = true
        barcodeScanner.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                val rawValue = result?.text?.trim()
                val data = QrConnectionParser.parse(rawValue)

                val requireClone = intent.getBooleanExtra(EXTRA_REQUIRE_CLONE_QR, false)

                if (data == null || (requireClone && !data.cloneRequested)) {
                    scanningStarted = false
                    Toast.makeText(
                        this@ScanQrActivity,
                        if (requireClone) "QR inválido para clonar dispositivo" else "QR inválido para Dog Messenger",
                        Toast.LENGTH_SHORT
                    ).show()
                    startQrScan()
                    return
                }

                qrProcessed = true
                barcodeScanner.pause()
                enviarDatosAlMain(data.ip, data.port, data.cloneRequested, data.cloneName)
            }
        })
        barcodeScanner.resume()
    }

    private fun enviarDatosAlMain(ip: String, port: Int, silentJoin: Boolean, cloneName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_QR_IP, ip)
            putExtra(MainActivity.EXTRA_QR_PORT, port)
            putExtra(MainActivity.EXTRA_QR_AUTO_CONNECT, true)
            putExtra(MainActivity.EXTRA_QR_SILENT_JOIN, silentJoin)
            putExtra(MainActivity.EXTRA_QR_CLONE_NAME, cloneName)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeScanner.isInitialized && scanningStarted && !qrProcessed) {
            barcodeScanner.resume()
        }
    }

    override fun onPause() {
        if (::barcodeScanner.isInitialized) {
            barcodeScanner.pause()
        }
        super.onPause()
    }
}
