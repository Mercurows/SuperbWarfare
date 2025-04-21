package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class StaminaOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("stamina");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Player player = Minecraft.getInstance().player;
        var w = guiGraphics.guiWidth();
        var h = guiGraphics.guiHeight();

        if (player != null && ClickHandler.isEditing)
            return;
        if (player != null && player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
            return;
        if (!shouldRender(player)) return;

        guiGraphics.pose().pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        if (ClientEventHandler.exhaustion) {
            RenderSystem.setShaderColor(1, 0, 0, (float) Mth.clamp(ClientEventHandler.switchTime, 0, 1));
        } else {
            RenderSystem.setShaderColor(1, 1, 1, (float) Mth.clamp(ClientEventHandler.switchTime, 0, 1));
        }

        RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), (float) w / 2 - 90, h - 23, (float) w / 2 + 90, h - 24, -90, -16777216);
        RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), (float) w / 2 - 90, (float) (h - 23), (float) (w / 2 + 90 - 1.8 * ClientEventHandler.stamina), h - 24, -90, -1);

        RenderSystem.setShaderColor(1, 1, 1, 1);

        guiGraphics.pose().popPose();
    }

    private static boolean shouldRender(Player player) {
        if (!DisplayConfig.STAMINA_HUD.get()) return false;
        if (player == null) return false;
        return ClientEventHandler.switchTime > 0;
    }
}
