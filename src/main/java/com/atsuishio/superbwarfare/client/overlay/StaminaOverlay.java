package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class StaminaOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_stamina";

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = gui.getMinecraft().player;

        if (player != null && ClientEventHandler.isEditing)
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

        RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), (float) screenWidth / 2 - 90, screenHeight - 23, (float) screenWidth / 2 + 90, screenHeight - 24, -90, -16777216);
        RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), (float) screenWidth / 2 - 90, (float) (screenHeight - 23), (float) (screenWidth / 2 + 90 - 1.8 * ClientEventHandler.stamina), screenHeight - 24, -90, -1);

        RenderSystem.setShaderColor(1, 1, 1, 1);

        guiGraphics.pose().popPose();
    }

    private static boolean shouldRender(Player player) {
        if (!DisplayConfig.STAMINA_HUD.get()) return false;
        if (player == null) return false;
        return ClientEventHandler.switchTime > 0;
    }
}
