package com.atsuishio.superbwarfare.tools;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import java.lang.reflect.Field;
import java.util.Random;

public class SpritePixelHelper {
    private static final Random RANDOM = new Random();

    /**
     * 从 TextureAtlasSprite 中随机抽取一个像素，返回 ARGB 颜色值
     * @param sprite 纹理精灵
     * @param frame 帧索引（动画纹理时使用，通常为 0）
     * @return ARGB 颜色值，格式为 0xAARRGGBB
     */
    public static int getRandomPixelARGB(TextureAtlasSprite sprite, int frame) {
        // 获取纹理尺寸
        int width = sprite.contents().width();
        int height = sprite.contents().height();

        // 生成随机坐标
        int x = RANDOM.nextInt(width);
        int y = RANDOM.nextInt(height);

        // 获取像素值（注意：getPixelRGBA 返回的是 ABGR 格式！）
        int abgr = sprite.getPixelRGBA(frame, x, y);

        // 提取 ABGR 分量
        int alpha = (abgr >> 24) & 0xFF;
        int blue  = (abgr >> 16) & 0xFF;
        int green = (abgr >> 8)  & 0xFF;
        int red   = abgr & 0xFF;


        // 组合为 RGB
        return (red << 16) | (green << 8) | blue;
    }

    /**
     * 将 ARGB 颜色格式化为十六进制字符串
     */
    public static String formatARGB(int argb) {
        return String.format("0x%08X", argb);
    }
}