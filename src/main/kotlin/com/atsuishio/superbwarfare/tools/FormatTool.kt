package com.atsuishio.superbwarfare.tools

import java.text.DecimalFormat
import java.util.*

fun String.camelToSnake() = FormatTool.camelToSnake(this)

object FormatTool {
    @JvmField
    val DECIMAL_FORMAT_0 = DecimalFormat("##")

    @JvmField
    val DECIMAL_FORMAT_1 = DecimalFormat("##.#")

    @JvmField
    val DECIMAL_FORMAT_2 = DecimalFormat("##.##")

    @JvmField
    val DECIMAL_FORMAT_1Z = DecimalFormat("##.0")

    @JvmField
    val DECIMAL_FORMAT_1ZZ = DecimalFormat("#0.0")

    @JvmField
    val DECIMAL_FORMAT_2ZZZ = DecimalFormat("#0.00")

    @JvmStatic
    @JvmOverloads
    fun format0D(num: Double, str: String = "") = DECIMAL_FORMAT_0.format(num) + str

    @JvmStatic
    @JvmOverloads
    fun format1D(num: Double, str: String = "") = DECIMAL_FORMAT_1.format(num) + str

    @JvmStatic
    @JvmOverloads
    fun format2D(num: Double, str: String = "") = DECIMAL_FORMAT_2.format(num) + str

    @JvmStatic
    @JvmOverloads
    fun format1DZ(num: Double, str: String = "") = DECIMAL_FORMAT_1Z.format(num) + str

    @JvmStatic
    @JvmOverloads
    fun format1DZZ(num: Double, str: String = "") = DECIMAL_FORMAT_1ZZ.format(num) + str

    fun camelToSnake(camel: String): String {
        if (camel.isEmpty()) return camel
        val result = StringBuilder()
        result.append(camel[0].lowercase(Locale.ROOT))
        for (i in 1 until camel.length) {
            val ch = camel[i]
            if (ch.isUpperCase()) {
                result.append('_')
                result.append(ch.lowercase(Locale.ROOT))
            } else {
                result.append(ch)
            }
        }
        return result.toString()
    }
}
