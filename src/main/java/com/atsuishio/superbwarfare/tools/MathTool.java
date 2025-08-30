package com.atsuishio.superbwarfare.tools;

import java.awt.*;

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

    public static float[] rgbToHsv(int rgb) {
        int r = (rgb >> 16) & 0xFF; // 提取红色分量
        int g = (rgb >> 8) & 0xFF;  // 提取绿色分量
        int b = rgb & 0xFF;         // 提取蓝色分量
        float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv); // 转换并填充 hsv 数组
        return hsv; // 返回格式: [H（0-1）, S（0-1）, V（0-1）]
    }


    /**
     * 线性渐变模式 - 匀速变化
     */
    public static final int MODE_LINEAR = 0;

    /**
     * 平滑渐变模式 - 缓入缓出效果
     */
    public static final int MODE_SMOOTH = 1;

    /**
     * 获取渐变颜色
     * @param startColor 起始颜色 (16进制RGB)
     * @param endColor 结束颜色 (16进制RGB)
     * @param progress 渐变进度 (0-100)
     * @param mode 渐变模式
     * @return 渐变后的颜色 (16进制RGB)
     */
    public static int getGradientColor(int startColor, int endColor, int progress, int mode) {
        // 确保进度在0-100范围内
        progress = Math.max(0, Math.min(100, progress));

        // 计算实际进度比例 (0.0 - 1.0)
        float ratio;
        if (mode == MODE_SMOOTH) {
            // 平滑渐变 - 使用缓入缓出函数
            ratio = smoothStep(progress / 100.0f);
        } else {
            // 线性渐变
            ratio = progress / 100.0f;
        }

        // 分解起始颜色的RGB分量
        int startR = (startColor >> 16) & 0xFF;
        int startG = (startColor >> 8) & 0xFF;
        int startB = startColor & 0xFF;

        // 分解结束颜色的RGB分量
        int endR = (endColor >> 16) & 0xFF;
        int endG = (endColor >> 8) & 0xFF;
        int endB = endColor & 0xFF;

        // 计算每个分量的插值
        int currentR = (int) (startR + (endR - startR) * ratio);
        int currentG = (int) (startG + (endG - startG) * ratio);
        int currentB = (int) (startB + (endB - startB) * ratio);

        // 确保颜色值在有效范围内
        currentR = Math.max(0, Math.min(255, currentR));
        currentG = Math.max(0, Math.min(255, currentG));
        currentB = Math.max(0, Math.min(255, currentB));

        // 重新组合为RGB颜色
        return (currentR << 16) | (currentG << 8) | currentB;
    }

    /**
     * 平滑步进函数 (缓入缓出)
     * @param t 输入值 (0.0 - 1.0)
     * @return 平滑处理后的值
     */
    private static float smoothStep(float t) {
        // 三次缓入缓出函数: 3t² - 2t³
        return t * t * (3.0f - 2.0f * t);
    }
}
