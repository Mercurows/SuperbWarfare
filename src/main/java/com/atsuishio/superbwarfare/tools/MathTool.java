package com.atsuishio.superbwarfare.tools;

public class MathTool {
    /**
     * 大小逐渐减弱到0的震荡函数
     * @param a 初始振幅
     * @param t 持续时间（秒）
     * @param c 震荡频率（Hz）
     * @param elapsedTime 已过去的时间（秒）
     * @return 当前时刻的震荡值
     */
    public static float decayingOscillation(float a, float t, float c, float elapsedTime) {
        // 如果时间已超过持续时间，返回0
        if (elapsedTime >= t) {
            return 0.0f;
        }

        // 计算衰减因子（指数衰减）
        float decayFactor = (float) Math.exp(-3.0f * elapsedTime / t);

        // 计算震荡部分（正弦波）
        float oscillation = (float) Math.sin(2.0f * Math.PI * c * elapsedTime);

        // 返回衰减后的震荡值
        return a * decayFactor * oscillation;
    }

    /**
     * 重载版本，使用Minecraft的tick时间系统
     * @param a 初始振幅
     * @param t 持续时间（秒）
     * @param c 震荡频率（Hz）
     * @param ticks 已过去的tick数
     * @return 当前时刻的震荡值
     */
    public static float decayingOscillation(float a, float t, float c, int ticks) {
        // 将tick转换为秒（Minecraft中20ticks=1秒）
        float elapsedTime = ticks / 20.0f;
        return decayingOscillation(a, t, c, elapsedTime);
    }

    /**
     * 获取渐变颜色
     * @param startColor 起始颜色 (16进制RGB)
     * @param endColor 结束颜色 (16进制RGB)
     * @param progress 渐变进度 (0-100)
     * @param mode 渐变模式 (HSV或HSL)
     * @return 渐变后的颜色 (16进制RGB)
     */
    public static int getGradientColor(int startColor, int endColor, int progress, int mode) {
        // 确保进度在0-100范围内
        progress = Math.max(0, Math.min(100, progress));
        float ratio = progress / 100.0f;

        if (mode == 2) {
            return hsvGradient(startColor, endColor, ratio);
        } else {
            return hslGradient(startColor, endColor, ratio);
        }
    }

    /**
     * 在HSV颜色空间中进行渐变
     */
    private static int hsvGradient(int startColor, int endColor, float ratio) {
        // 将RGB转换为HSV
        float[] startHSV = rgbToHsv(startColor);
        float[] endHSV = rgbToHsv(endColor);

        // 对HSV分量进行插值
        // 对于色相(H)，需要考虑色相环的循环特性
        float h = interpolateHue(startHSV[0], endHSV[0], ratio);
        float s = startHSV[1] + (endHSV[1] - startHSV[1]) * ratio;
        float v = startHSV[2] + (endHSV[2] - startHSV[2]) * ratio;

        // 将HSV转换回RGB
        return hsvToRgb(h, s, v);
    }

    /**
     * 在HSL颜色空间中进行渐变
     */
    private static int hslGradient(int startColor, int endColor, float ratio) {
        // 将RGB转换为HSL
        float[] startHSL = rgbToHsl(startColor);
        float[] endHSL = rgbToHsl(endColor);

        // 对HSL分量进行插值
        // 对于色相(H)，需要考虑色相环的循环特性
        float h = interpolateHue(startHSL[0], endHSL[0], ratio);
        float s = startHSL[1] + (endHSL[1] - startHSL[1]) * ratio;
        float l = startHSL[2] + (endHSL[2] - startHSL[2]) * ratio;

        // 将HSL转换回RGB
        return hslToRgb(h, s, l);
    }

    /**
     * 插值色相值，考虑色相环的循环特性
     */
    private static float interpolateHue(float startH, float endH, float ratio) {
        // 确保色相值在0-1范围内
        startH %= 1.0f;
        endH %= 1.0f;

        // 计算两个方向的差值
        float diff = endH - startH;

        // 如果差值大于0.5，说明应该从另一个方向绕色相环
        if (Math.abs(diff) > 0.5f) {
            if (diff > 0) {
                startH += 1.0f;
            } else {
                endH += 1.0f;
            }
        }

        // 线性插值
        return (startH + (endH - startH) * ratio) % 1.0f;
    }

    /**
     * 将RGB颜色转换为HSV
     * @param rgb RGB颜色
     * @return 包含H(0 - 1), S(0-1), V(0-1)的数组
     */
    private static float[] rgbToHsv(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        float rNorm = r / 255.0f;
        float gNorm = g / 255.0f;
        float bNorm = b / 255.0f;

        float max = Math.max(rNorm, Math.max(gNorm, bNorm));
        float min = Math.min(rNorm, Math.min(gNorm, bNorm));
        float delta = max - min;

        float h = 0;
        if (delta != 0) {
            if (max == rNorm) {
                h = (gNorm - bNorm) / delta % 6;
            } else if (max == gNorm) {
                h = (bNorm - rNorm) / delta + 2;
            } else {
                h = (rNorm - gNorm) / delta + 4;
            }
            h /= 6;
            if (h < 0) h += 1;
        }

        float s = max == 0 ? 0 : delta / max;
        float v = max;

        return new float[]{h, s, v};
    }

    /**
     * 将HSV转换为RGB
     */
    private static int hsvToRgb(float h, float s, float v) {
        h = h % 1.0f;
        if (h < 0) h += 1.0f;

        int hi = (int) (h * 6);
        float f = h * 6 - hi;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        float r, g, b;
        switch (hi) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
                break;
        }

        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    /**
     * 将RGB颜色转换为HSL
     */
    private static float[] rgbToHsl(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        float rNorm = r / 255.0f;
        float gNorm = g / 255.0f;
        float bNorm = b / 255.0f;

        float max = Math.max(rNorm, Math.max(gNorm, bNorm));
        float min = Math.min(rNorm, Math.min(gNorm, bNorm));
        float delta = max - min;

        float h = 0;
        if (delta != 0) {
            if (max == rNorm) {
                h = (gNorm - bNorm) / delta % 6;
            } else if (max == gNorm) {
                h = (bNorm - rNorm) / delta + 2;
            } else {
                h = (rNorm - gNorm) / delta + 4;
            }
            h /= 6;
            if (h < 0) h += 1;
        }

        float l = (max + min) / 2;
        float s = delta == 0 ? 0 : delta / (1 - Math.abs(2 * l - 1));

        return new float[]{h, s, l};
    }

    /**
     * 将HSL转换为RGB
     */
    private static int hslToRgb(float h, float s, float l) {
        h = h % 1.0f;
        if (h < 0) h += 1.0f;

        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = l - c / 2;

        float r, g, b;
        if (h < 1 / 6.0) {
            r = c;
            g = x;
            b = 0;
        } else if (h < 2 / 6.0) {
            r = x;
            g = c;
            b = 0;
        } else if (h < 3 / 6.0) {
            r = 0;
            g = c;
            b = x;
        } else if (h < 4 / 6.0) {
            r = 0;
            g = x;
            b = c;
        } else if (h < 5 / 6.0) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }

        r += m;
        g += m;
        b += m;

        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    /**
     * 获取渐变颜色 (默认HSV模式)
     */
    public static int getGradientColor(int startColor, int endColor, int progress) {
        return getGradientColor(startColor, endColor, progress, 1);
    }

    /**
     * 将RGB颜色转换为16进制字符串
     */
    public static String toHexString(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }
}
