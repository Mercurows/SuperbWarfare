package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.animation.AnimationCurves;
import com.atsuishio.superbwarfare.tools.animation.AnimationTimer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class HeatBarOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_heat_bar";

    private static final ResourceLocation TEXTURE = Mod.loc("textures/screens/heat_bar.png");

    private static final AnimationTimer timer = new AnimationTimer(200)
            .animation(AnimationCurves.EASE_IN_QUART);

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!DisplayConfig.ENABLE_HEAT_BAR_HUD.get()) return;

        Player player = gui.getMinecraft().player;
        if (player == null) return;

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
            timer.forward(currentTime);
        } else {
            timer.beginForward(currentTime);
        }
        if (timer.finished(currentTime)) {
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

        float posX = i + 64 + DisplayConfig.HEAT_BAR_HUD_X_OFFSET.get() + timer.lerp(0, 5, currentTime);
        float posY = j + 6 + DisplayConfig.HEAT_BAR_HUD_Y_OFFSET.get();

        float alpha = timer.lerp(1, 0, currentTime);
        RenderSystem.setShaderColor(1, 1, 1, alpha);

        RenderHelper.preciseBlit(guiGraphics, TEXTURE, posX, posY, 0, 0, 37 / 4f, 233 / 4f, width, height);

        float rate = (float) (heat / 100.0);
        float barHeight = 56 * rate;

        poseStack.pushPose();

        RenderHelper.blit(poseStack, TEXTURE, posX + 2.5f, posY + 1.5f + 56 - barHeight,
                10.5f, 0, 2.25f, barHeight, width, height, rate >= 0.795f ? calculateGradientColor(rate) : 0xFFFFFF);

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
