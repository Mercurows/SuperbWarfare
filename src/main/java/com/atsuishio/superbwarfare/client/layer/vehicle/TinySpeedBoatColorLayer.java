package com.atsuishio.superbwarfare.client.layer.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.TinySpeedboatEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class TinySpeedBoatColorLayer extends GeoRenderLayer<TinySpeedboatEntity> {

    private static final ResourceLocation LAYER = Mod.loc("textures/entity/tiny_speedboat_color.png");

    public TinySpeedBoatColorLayer(GeoRenderer<TinySpeedboatEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, TinySpeedboatEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType glowRenderType = RenderType.entityTranslucent(LAYER);

        int id = animatable.getColorId();

        float[] color;
        int intColor;

        if (animatable.getCustomName() != null && animatable.getCustomName().getString().equals("jeb_")) {
            color = getRainbowColorHSL(animatable.tickCount);
            intColor = FastColor.ARGB32.colorFromFloat(color[0], color[1], color[2], color[3]);
            glowRenderType = RenderType.entityTranslucentEmissive(LAYER);
        } else {
            intColor = DyeColor.byId(id).getTextureDiffuseColor();
        }

        if (ClientEventHandler.activeThermalImaging) {
            color = new float[]{1, 1, 1, 1.0f};
            intColor = FastColor.ARGB32.colorFromFloat(color[0], color[1], color[2], color[3]);
            glowRenderType = RenderType.entityTranslucentEmissive(LAYER);
        }

        getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, glowRenderType, bufferSource.getBuffer(glowRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY, intColor);
    }

    public static float[] getRainbowColorHSL(int tickCount) {
        // 完整循环的tick数，调整这个值控制变化速度
        int cycleTicks = 80;

        // 计算色相（0-1范围）
        float hue = (tickCount % cycleTicks) / (float) cycleTicks;

        // 固定饱和度和亮度
        float saturation = 1.0f;
        float lightness = 0.5f;

        return hslToRgb(hue, saturation, lightness);
    }

    // HSL转RGB转换函数
    public static float[] hslToRgb(float h, float s, float l) {
        float r, g, b;

        if (s == 0f) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hueToRgb(p, q, h + 1f / 3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f / 3f);
        }

        return new float[]{r, g, b, 1.0f}; // Alpha保持1.0
    }

    private static float hueToRgb(float p, float q, float t) {
        if (t < 0f) t += 1f;
        if (t > 1f) t -= 1f;
        if (t < 1f / 6f) return p + (q - p) * 6f * t;
        if (t < 1f / 2f) return q;
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f;
        return p;
    }
}
