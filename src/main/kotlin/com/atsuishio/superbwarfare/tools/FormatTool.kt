package com.atsuishio.superbwarfare.tools

import java.text.DecimalFormat
import java.util.*

/**
 * Extension function to convert camelCase string into snake_case.
 */
fun String.camelToSnake() = FormatTool.camelToSnake(this)

/**
 * Utility class for decimal formatting and string modifications.
 *
 * @author atsuishio
 * @since 0.8.9.1
 */
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

    /**
     * Converts camelCase string to snake_case format safely.
     * Use lowercaseChar() for efficient char mutations.
     */
    fun camelToSnake(camel: String): String {
        if (camel.isEmpty()) return camel
        val result = StringBuilder()
        result.append(camel[0].lowercaseChar())
        for (i in 1 until camel.length) {
            val ch = camel[i]
            if (ch.isUpperCase()) {
                result.append('_')
                result.append(ch.lowercaseChar())
            } else {
                result.append(ch)
            }
        }
        return result.toString()
    }
}