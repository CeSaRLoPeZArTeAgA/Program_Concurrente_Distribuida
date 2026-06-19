package com.example.cliente50

import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder

object ChatProtocol {
    private const val TYPE_MSG = "MSG"
    private const val TYPE_FILE = "FILE"
    private const val TYPE_STORECFG = "STORECFG"
    private const val SEP = "|"

    data class ChatPacket(
        val type: String,
        val sender: String,
        val text: String = "",
        val fileName: String = "",
        val mimeType: String = "application/octet-stream",
        val bytes: ByteArray? = null
    )

    fun encodeMessage(sender: String, text: String): String {
        return TYPE_MSG + SEP + enc(sender.ifBlank { "Sin nombre" }) + SEP + enc(text)
    }

    fun encodeFile(sender: String, fileName: String, mimeType: String, bytes: ByteArray): String {
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return TYPE_FILE + SEP + enc(sender.ifBlank { "Sin nombre" }) + SEP + enc(fileName.ifBlank { "archivo" }) + SEP + enc(mimeType.ifBlank { "application/octet-stream" }) + SEP + base64
    }

    fun encodeStoreConfig(ip: String, port: Int): String {
        return TYPE_STORECFG + SEP + enc(ip.trim()) + SEP + port.toString()
    }

    fun parseStoreConfig(raw: String): Pair<String, Int>? {
        return try {
            val parts = raw.split(SEP, limit = 3)
            if (parts.size == 3 && parts[0] == TYPE_STORECFG) {
                val ip = dec(parts[1]).trim()
                val port = parts[2].trim().toIntOrNull() ?: return null
                if (ip.isNotBlank() && port in 1..65535) Pair(ip, port) else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun parse(raw: String): ChatPacket {
        return try {
            val parts = raw.split(SEP, limit = 5)
            when {
                parts.size >= 3 && parts[0] == TYPE_MSG -> ChatPacket(
                    type = TYPE_MSG,
                    sender = dec(parts[1]),
                    text = dec(parts[2])
                )

                parts.size >= 5 && parts[0] == TYPE_FILE -> ChatPacket(
                    type = TYPE_FILE,
                    sender = dec(parts[1]),
                    fileName = dec(parts[2]),
                    mimeType = dec(parts[3]),
                    bytes = Base64.decode(parts[4], Base64.NO_WRAP)
                )

                parts.size >= 3 && parts[0] == TYPE_STORECFG -> ChatPacket(
                    type = TYPE_STORECFG,
                    sender = "Sistema",
                    text = "Configuración Tienda Virtual recibida"
                )

                else -> ChatPacket(type = TYPE_MSG, sender = "Sistema", text = raw)
            }
        } catch (e: Exception) {
            ChatPacket(type = TYPE_MSG, sender = "Sistema", text = raw)
        }
    }

    fun display(raw: String): String {
        val packet = parse(raw)
        val sender = bracketName(packet.sender)
        return when (packet.type) {
            TYPE_STORECFG -> ""
            TYPE_FILE -> {
                val size = packet.bytes?.size ?: 0
                "$sender [Archivo adjunto] ${packet.fileName} (${formatBytes(size)})"
            }
            else -> {
                val text = packet.text.trim()
                when (text.lowercase()) {
                    "conectado" -> "$sender conectado"
                    "desconectado" -> "$sender desconectado"
                    else -> "$sender: ${packet.text}"
                }
            }
        }
    }

    private fun bracketName(name: String): String {
        val clean = name.trim().ifBlank { "Sin nombre" }
        return if (clean.startsWith("[") && clean.endsWith("]")) clean else "[$clean]"
    }

    fun isFile(packet: ChatPacket): Boolean = packet.type == TYPE_FILE && packet.bytes != null

    private fun formatBytes(bytes: Int): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.1f MB", mb)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun dec(value: String): String = URLDecoder.decode(value, "UTF-8")
}
