package com.atsuishio.superbwarfare.tools;

import java.text.DecimalFormat;

public class FormatTool {

    public static final DecimalFormat DECIMAL_FORMAT_0 = new DecimalFormat("##");
    public static final DecimalFormat DECIMAL_FORMAT_1 = new DecimalFormat("##.#");
    public static final DecimalFormat DECIMAL_FORMAT_2 = new DecimalFormat("##.##");

    public static final DecimalFormat DECIMAL_FORMAT_1Z = new DecimalFormat("##.0");
    public static final DecimalFormat DECIMAL_FORMAT_1ZZ = new DecimalFormat("#0.0");

    public static final DecimalFormat DECIMAL_FORMAT_2ZZZ = new DecimalFormat("#0.00");

    public static String format0D(double num) {
        return format0D(num, "");
    }

    public static String format0D(double num, String str) {
        return DECIMAL_FORMAT_0.format(num) + str;
    }

    public static String format1D(double num) {
        return format1D(num, "");
    }

    public static String format1D(double num, String str) {
        return DECIMAL_FORMAT_1.format(num) + str;
    }

    public static String format2D(double num) {
        return format2D(num, "");
    }

    public static String format2D(double num, String str) {
        return DECIMAL_FORMAT_2.format(num) + str;
    }

    public static String format1DZ(double num) {
        return format1DZ(num, "");
    }

    public static String format1DZ(double num, String str) {
        return DECIMAL_FORMAT_1Z.format(num) + str;
    }

    public static String format1DZZ(double num) {
        return format1DZZ(num, "");
    }

    public static String format1DZZ(double num, String str) {
        return DECIMAL_FORMAT_1ZZ.format(num) + str;
    }
}
