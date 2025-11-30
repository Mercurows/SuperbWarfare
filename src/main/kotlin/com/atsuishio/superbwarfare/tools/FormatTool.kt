package com.atsuishio.superbwarfare.tools

import java.text.DecimalFormat

object FormatTool {
    @JvmField
    val DECIMAL_FORMAT_0: DecimalFormat = DecimalFormat("##")

    @JvmField
    val DECIMAL_FORMAT_1: DecimalFormat = DecimalFormat("##.#")

    @JvmField
    val DECIMAL_FORMAT_2: DecimalFormat = DecimalFormat("##.##")

    @JvmField
    val DECIMAL_FORMAT_1Z: DecimalFormat = DecimalFormat("##.0")

    @JvmField
    val DECIMAL_FORMAT_1ZZ: DecimalFormat = DecimalFormat("#0.0")

    @JvmField
    val DECIMAL_FORMAT_2ZZZ: DecimalFormat = DecimalFormat("#0.00")

    @JvmStatic
    @JvmOverloads
    fun format0D(num: Double, str: String = ""): String {
        return DECIMAL_FORMAT_0.format(num) + str
    }

    @JvmStatic
    @JvmOverloads
    fun format1D(num: Double, str: String = ""): String {
        return DECIMAL_FORMAT_1.format(num) + str
    }

    @JvmStatic
    @JvmOverloads
    fun format2D(num: Double, str: String = ""): String {
        return DECIMAL_FORMAT_2.format(num) + str
    }

    @JvmStatic
    @JvmOverloads
    fun format1DZ(num: Double, str: String = ""): String {
        return DECIMAL_FORMAT_1Z.format(num) + str
    }

    @JvmStatic
    @JvmOverloads
    fun format1DZZ(num: Double, str: String = ""): String {
        return DECIMAL_FORMAT_1ZZ.format(num) + str
    }
}
