package com.atsuishio.superbwarfare.client.renderer;// 在你的模组类或客户端事件处理器中

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class TextureBrightnessHandler {

    // 缓存处理过的纹理，避免重复处理
    private static final Map<ResourceLocation, ResourceLocation> BRIGHTENED_TEXTURES = new HashMap<>();

    public static ResourceLocation getBrightenedTexture(ResourceLocation originalTextureLoc, float brightnessMultiplier) {
        // 检查是否已缓存
        if (BRIGHTENED_TEXTURES.containsKey(originalTextureLoc)) {
            return BRIGHTENED_TEXTURES.get(originalTextureLoc);
        }

        try {
            // 1. 获取原始纹理
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resource = resourceManager.getResource(originalTextureLoc).orElseThrow();

            // 2. 读取图像
            NativeImage originalImage = NativeImage.read(resource.open());
            NativeImage brightenedImage = brightenImage(originalImage, brightnessMultiplier);

            // 3. 创建新的纹理资源
            ResourceLocation newTextureLoc = ResourceLocation.fromNamespaceAndPath(
                    originalTextureLoc.getNamespace(),
                    originalTextureLoc.getPath().replace(".png", "_bright.png")
            );

            // 4. 注册到纹理管理器
            Minecraft.getInstance().getTextureManager().register(
                    newTextureLoc,
                    new DynamicTexture(brightenedImage)
            );

            // 5. 缓存并返回
            BRIGHTENED_TEXTURES.put(originalTextureLoc, newTextureLoc);
            return newTextureLoc;

        } catch (Exception e) {
            // 出错时返回原始纹理
            e.printStackTrace();
            return originalTextureLoc;
        }
    }

    private static NativeImage brightenImage(NativeImage original, float multiplier) {
        // 创建相同尺寸的新图像
        NativeImage brightened = new NativeImage(original.getWidth(), original.getHeight(), false);

        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                int color = original.getPixelRGBA(x, y);

                // 提取 ARGB 通道
                int alpha = (color >> 24) & 0xFF;
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;

                // 增加亮度（确保不超过255）
                red = Math.min(255, (int) (red * multiplier));
                green = Math.min(255, (int) (green * multiplier));
                blue = Math.min(255, (int) (blue * multiplier));

                // 重新组合颜色
                int newColor = (alpha << 24) | (red << 16) | (green << 8) | blue;
                brightened.setPixelRGBA(x, y, newColor);
            }
        }

        return brightened;
    }
}