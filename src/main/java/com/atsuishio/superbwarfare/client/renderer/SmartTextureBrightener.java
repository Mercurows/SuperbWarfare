package com.atsuishio.superbwarfare.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;

public class SmartTextureBrightener {
    
    private static final Map<ResourceLocation, Float> BRIGHTNESS_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> PROCESSED_TEXTURES = new HashMap<>();
    
    // 计算图像的感知亮度（使用人类视觉的加权平均）
    public static float calculatePerceivedBrightness(NativeImage image) {
        long totalLuminance = 0;
        int pixelCount = 0;
        
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int color = image.getPixelRGBA(x, y);
                int alpha = (color >> 24) & 0xFF;
                
                // 只处理不透明或半透明的像素
                if (alpha > 10) {
                    float r = ((color >> 16) & 0xFF) / 255.0f;
                    float g = ((color >> 8) & 0xFF) / 255.0f;
                    float b = (color & 0xFF) / 255.0f;
                    
                    // 计算感知亮度（ITU-R BT.709标准）
                    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
                    totalLuminance += (int)(luminance * 255);
                    pixelCount++;
                }
            }
        }
        
        if (pixelCount == 0) return 0.5f; // 默认值
        
        return (totalLuminance / (float)(pixelCount * 255));
    }
    
    // 计算图像亮度分布（判断是偏暗、正常还是偏亮）
    public static BrightnessCategory analyzeBrightnessDistribution(NativeImage image) {
        int darkPixels = 0;
        int midPixels = 0;
        int brightPixels = 0;
        int totalPixels = 0;
        
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int color = image.getPixelRGBA(x, y);
                int alpha = (color >> 24) & 0xFF;
                
                if (alpha > 10) {
                    float r = ((color >> 16) & 0xFF) / 255.0f;
                    float g = ((color >> 8) & 0xFF) / 255.0f;
                    float b = (color & 0xFF) / 255.0f;
                    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
                    
                    if (luminance < 0.3f) darkPixels++;
                    else if (luminance < 0.7f) midPixels++;
                    else brightPixels++;
                    
                    totalPixels++;
                }
            }
        }
        
        if (totalPixels == 0) return BrightnessCategory.NORMAL;
        
        float darkRatio = darkPixels / (float) totalPixels;
        float brightRatio = brightPixels / (float) totalPixels;
        
        if (darkRatio > 0.6f) return BrightnessCategory.DARK;
        if (brightRatio > 0.6f) return BrightnessCategory.BRIGHT;
        return BrightnessCategory.NORMAL;
    }
    
    // 根据图像特征动态计算亮度系数
    public static float calculateDynamicBrightnessFactor(NativeImage image) {
        float averageBrightness = calculatePerceivedBrightness(image);
        BrightnessCategory category = analyzeBrightnessDistribution(image);
        
        // 基础调整：根据平均亮度
        float baseFactor;
        if (averageBrightness < 0.2f) {
            // 很暗的贴图：大幅提亮
            baseFactor = 1.8f + (0.2f - averageBrightness) * 2.0f;
        } else if (averageBrightness < 0.4f) {
            // 较暗的贴图：中等提亮
            baseFactor = 1.3f + (0.4f - averageBrightness) * 1.25f;
        } else if (averageBrightness < 0.6f) {
            // 正常贴图：轻微提亮
            baseFactor = 1.0f + (0.6f - averageBrightness) * 0.5f;
        } else if (averageBrightness < 0.8f) {
            // 较亮贴图：基本保持
            baseFactor = 1.0f - (averageBrightness - 0.6f) * 0.25f;
        } else {
            // 很亮贴图：稍微压暗
            baseFactor = 0.85f - (averageBrightness - 0.8f) * 0.5f;
        }
        
        // 根据分布微调
        switch (category) {
            case DARK:
                // 整体偏暗：额外提亮一点
                baseFactor *= 1.1f;
                break;
            case BRIGHT:
                // 整体偏亮：少提亮一点
                baseFactor *= 0.9f;
                break;
        }
        
        // 限制在合理范围内
        return Math.max(0.5f, Math.min(3.0f, baseFactor));
    }
    
    // 智能调整亮度（自适应）
    public static NativeImage smartBrighten(NativeImage original, float targetBrightness) {
        float currentBrightness = calculatePerceivedBrightness(original);
        float dynamicFactor = calculateDynamicBrightnessFactor(original);
        
        // 如果指定了目标亮度，则基于目标调整
        float factor;
        if (targetBrightness > 0) {
            factor = targetBrightness / Math.max(currentBrightness, 0.01f);
            // 结合动态因子平滑调整
            factor = (factor + dynamicFactor) / 2.0f;
        } else {
            factor = dynamicFactor;
        }
        
        return applyAdaptiveBrightness(original, factor, currentBrightness);
    }

    // 自适应亮度调整（不同区域不同处理）
    public static NativeImage applyAdaptiveBrightness(NativeImage original, float baseFactor, float avgBrightness) {
        NativeImage result = new NativeImage(original.getWidth(), original.getHeight(), false);

        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                int color = original.getPixelRGBA(x, y);
                int alpha = (color >> 24) & 0xFF;

                if (alpha > 10) {
                    float r = ((color >> 16) & 0xFF) / 255.0f;
                    float g = ((color >> 8) & 0xFF) / 255.0f;
                    float b = (color & 0xFF) / 255.0f;
                    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;

                    // 根据当前像素的亮度动态调整系数
                    // 暗部提亮更多，亮部提亮更少
                    float localFactor;
                    if (luminance < 0.2f) {
                        // 暗部：提亮更多
                        localFactor = baseFactor * 1.1f;
                    } else if (luminance < 0.5f) {
                        // 中间调：正常提亮
                        localFactor = baseFactor;
                    } else if (luminance < 0.8f) {
                        // 亮部：少提亮
                        localFactor = baseFactor * 0.95f;
                    } else {
                        // 高光：几乎不提亮
                        localFactor = baseFactor * 0.9f;
                    }

                    // 应用调整，使用曲线调整避免过曝
                    r = applyBrightnessCurve(r + 0.05f, localFactor);
                    g = applyBrightnessCurve(g + 0.05f, localFactor);
                    b = applyBrightnessCurve(b + 0.05f, localFactor);

                    int newColor = (alpha << 24) |
                        ((int)(r * 255) << 16) |
                        ((int)(g * 255) << 8) |
                        (int)(b * 255);

                    result.setPixelRGBA(x, y, newColor);
                } else {
                    result.setPixelRGBA(x, y, color);
                }
            }
        }

        return result;
    }
    
    // 使用曲线调整亮度（避免线性调整的过曝问题）
    private static float applyBrightnessCurve(float value, float factor) {
        if (factor >= 1.0f) {
            // 提亮时使用非线性曲线，避免高光过曝
            return 1.0f - (float)Math.exp(-factor * value);
        } else {
            // 压暗时使用幂函数，保持对比度
            return (float)Math.pow(value, 1.0f / factor);
        }
    }
    
    // 获取或创建智能调整后的纹理
    public static ResourceLocation getSmartBrightenedTexture(ResourceLocation originalLoc, float targetBrightness) {
        if (PROCESSED_TEXTURES.containsKey(originalLoc)) {
            return PROCESSED_TEXTURES.get(originalLoc);
        }
        
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resource = resourceManager.getResource(originalLoc).orElseThrow();
            
            NativeImage originalImage = NativeImage.read(resource.open());
            NativeImage brightenedImage = smartBrighten(originalImage, targetBrightness);
            originalImage.close();
            
            ResourceLocation newTextureLoc = new ResourceLocation(
                originalLoc.getNamespace(),
                originalLoc.getPath().replace(".png", "_smartbright.png")
            );
            
            Minecraft.getInstance().getTextureManager().register(
                newTextureLoc,
                new DynamicTexture(brightenedImage)
            );
            
            // 计算并存储实际使用的亮度系数
            float usedFactor = calculateDynamicBrightnessFactor(brightenedImage);
            BRIGHTNESS_CACHE.put(originalLoc, usedFactor);
            PROCESSED_TEXTURES.put(originalLoc, newTextureLoc);
            
            brightenedImage.close();
            
            return newTextureLoc;
            
        } catch (Exception e) {
            e.printStackTrace();
            return originalLoc;
        }
    }


    // 枚举：亮度分类
    public enum BrightnessCategory {
        DARK, NORMAL, BRIGHT
    }
}