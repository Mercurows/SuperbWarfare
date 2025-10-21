package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.animation.AnimationCurves;
import com.atsuishio.superbwarfare.client.animation.AnimationTimer;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class HeatBarOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("heat_bar");

    private static final ResourceLocation TEXTURE = Mod.loc("textures/overlay/heat_bar/heat_bar.png");

    private static final AnimationTimer ANIMATION_TIMER = new AnimationTimer(200)
            .animation(AnimationCurves.EASE_IN_QUART);

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        if (!DisplayConfig.ENABLE_HEAT_BAR_HUD.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        var screenWidth = guiGraphics.guiWidth();
        var screenHeight = guiGraphics.guiHeight();

        double heat;
        if (ClientEventHandler.isEditing
                || !(player.getMainHandItem().getItem() instanceof GunItem)
                || (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
        ) {
            heat = 0;
        } else {
            heat = GunData.from(player.getMainHandItem()).heat.get();
        }

        long currentTime = System.currentTimeMillis();
        if (heat <= 0) {
            ANIMATION_TIMER.forward(currentTime);
        } else {
            ANIMATION_TIMER.beginForward(currentTime);
        }

        if (ANIMATION_TIMER.finished(currentTime)) {
            return;
        }

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        int width = 16;
        int height = 64;

        int i = (screenWidth - width) / 2;
        int j = (screenHeight - height) / 2;

        float posX = i + 64 + DisplayConfig.HEAT_BAR_HUD_X_OFFSET.get() + ANIMATION_TIMER.lerp(0, 5, currentTime);
        float posY = j + 6 + DisplayConfig.HEAT_BAR_HUD_Y_OFFSET.get();

        float alpha = ANIMATION_TIMER.lerp(1, 0, currentTime);
        RenderSystem.setShaderColor(1, 1, 1, alpha);

        RenderHelper.preciseBlit(guiGraphics, TEXTURE, posX, posY, 0, 0, 37 / 4f, 233 / 4f, width, height);

        float rate = (float) (heat / 100.0);
        float barHeight = 56 * rate;

        poseStack.pushPose();

        var color = rate >= 0.795f ? calculateGradientColor(rate) : 0xFFFFFF;
        var red = FastColor.ARGB32.red(color) / 255f;
        var green = FastColor.ARGB32.green(color) / 255f;
        var blue = FastColor.ARGB32.blue(color) / 255f;

        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderHelper.preciseBlit(guiGraphics, TEXTURE, posX + 2.5f, posY + 1.5f + 56 - barHeight,
                10.5f, 0, 2.25f, barHeight, width, height);

        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        poseStack.popPose();
    }

    public static int calculateGradientColor(float rate) {
        float clampedRate = Mth.clamp(rate, 0.795f, 1.0f);
        float normalized = (clampedRate - 0.795f) / (1.0f - 0.795f);

        int red = 255;
        int green = (int) (255 * (1 - normalized));
        int blue = (int) (255 * (1 - normalized));

        return (red << 16) | (green << 8) | blue;
    }
}
