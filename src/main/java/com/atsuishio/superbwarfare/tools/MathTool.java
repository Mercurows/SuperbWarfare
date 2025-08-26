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
}
