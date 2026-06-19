package com.example.cliente50

import android.net.Uri

object QrConnectionParser {

    data class ConnectionData(
        val ip: String,
        val port: Int,
        val cloneRequested: Boolean = false,
        val cloneName: String = ""
    )

    fun parse(rawValue: String?): ConnectionData? {
        val value = rawValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        parseDogMsgUri(value)?.let { return it }
        parsePlainIpPort(value)?.let { return it }

        return null
    }

    private fun parseDogMsgUri(value: String): ConnectionData? {
        return try {
            val uri = Uri.parse(value)

            if (uri.scheme?.lowercase() != "dogmsg") {
                return null
            }

            val ip = uri.host?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val port = uri.port.takeIf { it in 1..65535 } ?: return null
            val clone = uri.getQueryParameter("clone") == "1" ||
                    uri.getQueryParameter("mode")?.lowercase() == "clone"
            val cloneName = uri.getQueryParameter("name")
                ?: uri.getQueryParameter("nombre")
                ?: uri.getQueryParameter("cloneName")
                ?: ""

            ConnectionData(ip, port, clone, cloneName.trim())
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePlainIpPort(value: String): ConnectionData? {
        val regex = Regex("""(?:dogmsg://)?([A-Za-z0-9._-]+):(\d{1,5})(?:.*(?:clone=1|mode=clone))?""")
        val match = regex.find(value) ?: return null
        val ip = match.groupValues[1].trim()
        val port = match.groupValues[2].toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        val clone = value.contains("clone=1", ignoreCase = true) || value.contains("mode=clone", ignoreCase = true)

        val cloneName = extraerParametro(value, "name")
            ?: extraerParametro(value, "nombre")
            ?: extraerParametro(value, "cloneName")
            ?: ""

        return ConnectionData(ip, port, clone, cloneName.trim())
    }


    private fun extraerParametro(value: String, key: String): String? {
        val regex = Regex("""[?&]$key=([^&]+)""", RegexOption.IGNORE_CASE)
        val raw = regex.find(value)?.groupValues?.getOrNull(1) ?: return null
        return try {
            java.net.URLDecoder.decode(raw, "UTF-8")
        } catch (_: Exception) {
            raw
        }
    }
}