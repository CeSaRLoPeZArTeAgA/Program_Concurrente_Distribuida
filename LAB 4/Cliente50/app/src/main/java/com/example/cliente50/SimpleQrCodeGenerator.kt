package com.example.cliente50

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Generador QR local para evitar dependencias externas.
 * Usa QR Version 3, nivel de corrección L y modo Byte.
 * Capacidad útil: hasta 53 bytes aproximadamente.
 */
object SimpleQrCodeGenerator {

    private const val VERSION = 3
    private const val QR_SIZE = 21 + 4 * (VERSION - 1)
    private const val DATA_CODEWORDS = 55
    private const val ECC_CODEWORDS = 15
    private const val BORDER_MODULES = 4

    fun createQrBitmap(text: String, bitmapSize: Int): Bitmap {
        val safeText = if (text.toByteArray(Charsets.ISO_8859_1).size <= 53) {
            text
        } else {
            text.take(53)
        }

        val modules = encode(safeText)
        val totalModules = QR_SIZE + BORDER_MODULES * 2
        val scale = maxOf(1, bitmapSize / totalModules)
        val finalSize = totalModules * scale

        val bitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)

        for (y in 0 until QR_SIZE) {
            for (x in 0 until QR_SIZE) {
                if (modules[y][x]) {
                    val left = (x + BORDER_MODULES) * scale
                    val top = (y + BORDER_MODULES) * scale
                    for (yy in top until top + scale) {
                        for (xx in left until left + scale) {
                            bitmap.setPixel(xx, yy, Color.BLACK)
                        }
                    }
                }
            }
        }

        return bitmap
    }

    private fun encode(text: String): Array<BooleanArray> {
        val modules = Array(QR_SIZE) { BooleanArray(QR_SIZE) }
        val isFunction = Array(QR_SIZE) { BooleanArray(QR_SIZE) }

        fun setModule(x: Int, y: Int, value: Boolean, function: Boolean = true) {
            modules[y][x] = value
            if (function) {
                isFunction[y][x] = true
            }
        }

        fun drawFinderPattern(centerX: Int, centerY: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val x = centerX + dx
                    val y = centerY + dy

                    if (x in 0 until QR_SIZE && y in 0 until QR_SIZE) {
                        val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                        setModule(x, y, distance != 2 && distance != 4, true)
                    }
                }
            }
        }

        fun drawAlignmentPattern(centerX: Int, centerY: Int) {
            for (dy in -2..2) {
                for (dx in -2..2) {
                    val x = centerX + dx
                    val y = centerY + dy
                    val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    setModule(x, y, distance != 1, true)
                }
            }
        }

        drawFinderPattern(3, 3)
        drawFinderPattern(QR_SIZE - 4, 3)
        drawFinderPattern(3, QR_SIZE - 4)

        for (i in 0 until QR_SIZE) {
            if (!isFunction[6][i]) {
                setModule(i, 6, i % 2 == 0, true)
            }
            if (!isFunction[i][6]) {
                setModule(6, i, i % 2 == 0, true)
            }
        }

        drawAlignmentPattern(22, 22)
        drawFormatBits(modules, isFunction)

        val codewords = createCodewords(text)
        var bitIndex = 0
        var right = QR_SIZE - 1

        while (right >= 1) {
            if (right == 6) {
                right--
            }

            val upward = ((right + 1) and 2) == 0

            for (vertical in 0 until QR_SIZE) {
                val y = if (upward) QR_SIZE - 1 - vertical else vertical

                for (j in 0..1) {
                    val x = right - j

                    if (!isFunction[y][x] && bitIndex < codewords.size * 8) {
                        modules[y][x] = ((codewords[bitIndex / 8] ushr (7 - bitIndex % 8)) and 1) != 0
                        bitIndex++
                    }
                }
            }

            right -= 2
        }

        applyMask0(modules, isFunction)

        return modules
    }

    private fun createCodewords(text: String): IntArray {
        val bytes = text.toByteArray(Charsets.ISO_8859_1).map { it.toInt() and 0xFF }
        val bits = ArrayList<Int>()

        appendBits(bits, 0x4, 4)              // Byte mode
        appendBits(bits, bytes.size, 8)       // Character count, version 1-9

        for (b in bytes) {
            appendBits(bits, b, 8)
        }

        val capacityBits = DATA_CODEWORDS * 8
        repeat(minOf(4, capacityBits - bits.size)) {
            bits.add(0)
        }

        while (bits.size % 8 != 0) {
            bits.add(0)
        }

        val dataCodewords = ArrayList<Int>()

        for (i in bits.indices step 8) {
            var value = 0
            for (j in 0 until 8) {
                value = (value shl 1) or bits[i + j]
            }
            dataCodewords.add(value)
        }

        var padIndex = 0
        val padBytes = intArrayOf(0xEC, 0x11)

        while (dataCodewords.size < DATA_CODEWORDS) {
            dataCodewords.add(padBytes[padIndex % 2])
            padIndex++
        }

        val ecc = reedSolomonRemainder(dataCodewords.toIntArray(), reedSolomonDivisor(ECC_CODEWORDS))
        return dataCodewords.toIntArray() + ecc
    }

    private fun appendBits(bits: MutableList<Int>, value: Int, count: Int) {
        for (i in count - 1 downTo 0) {
            bits.add((value ushr i) and 1)
        }
    }

    private fun drawFormatBits(modules: Array<BooleanArray>, isFunction: Array<BooleanArray>) {
        val bits = formatBits(mask = 0)

        fun getBit(value: Int, index: Int): Boolean {
            return ((value ushr index) and 1) != 0
        }

        fun setFunction(x: Int, y: Int, value: Boolean) {
            modules[y][x] = value
            isFunction[y][x] = true
        }

        for (i in 0..5) {
            setFunction(8, i, getBit(bits, i))
        }

        setFunction(8, 7, getBit(bits, 6))
        setFunction(8, 8, getBit(bits, 7))
        setFunction(7, 8, getBit(bits, 8))

        for (i in 9..14) {
            setFunction(14 - i, 8, getBit(bits, i))
        }

        for (i in 0..7) {
            setFunction(QR_SIZE - 1 - i, 8, getBit(bits, i))
        }

        for (i in 8..14) {
            setFunction(8, QR_SIZE - 15 + i, getBit(bits, i))
        }

        setFunction(8, QR_SIZE - 8, true)
    }

    private fun formatBits(mask: Int): Int {
        // Nivel L = 01b en los bits de formato QR.
        val data = (1 shl 3) or mask
        var remainder = data

        repeat(10) {
            remainder = remainder shl 1
            if ((remainder and (1 shl 10)) != 0) {
                remainder = remainder xor 0x537
            }
        }

        return ((data shl 10) or (remainder and 0x3FF)) xor 0x5412
    }

    private fun applyMask0(modules: Array<BooleanArray>, isFunction: Array<BooleanArray>) {
        for (y in 0 until QR_SIZE) {
            for (x in 0 until QR_SIZE) {
                if (!isFunction[y][x] && (x + y) % 2 == 0) {
                    modules[y][x] = !modules[y][x]
                }
            }
        }
    }

    private fun reedSolomonDivisor(degree: Int): IntArray {
        val result = IntArray(degree)
        result[degree - 1] = 1
        var root = 1

        repeat(degree) {
            for (j in 0 until degree) {
                result[j] = gfMultiply(result[j], root)
                if (j + 1 < degree) {
                    result[j] = result[j] xor result[j + 1]
                }
            }
            root = gfMultiply(root, 0x02)
        }

        return result
    }

    private fun reedSolomonRemainder(data: IntArray, divisor: IntArray): IntArray {
        val result = IntArray(divisor.size)

        for (b in data) {
            val factor = b xor result[0]

            for (i in 0 until result.size - 1) {
                result[i] = result[i + 1]
            }
            result[result.size - 1] = 0

            for (i in result.indices) {
                result[i] = result[i] xor gfMultiply(divisor[i], factor)
            }
        }

        return result
    }

    private fun gfMultiply(xInput: Int, yInput: Int): Int {
        var x = xInput
        var y = yInput
        var result = 0

        while (y != 0) {
            if ((y and 1) != 0) {
                result = result xor x
            }

            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }

            y = y ushr 1
        }

        return result and 0xFF
    }
}
