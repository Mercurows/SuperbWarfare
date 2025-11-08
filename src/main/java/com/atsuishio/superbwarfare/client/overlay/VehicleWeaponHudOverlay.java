package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.weapon.LandWeaponHud;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 控制载具武器的玩家显示的HUD
 */
@OnlyIn(Dist.CLIENT)
public class VehicleWeaponHudOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_vehicle_weapon_hud";
    public static final String EMPTY = "@Empty";

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        var player = gui.getMinecraft().player;
        if (player == null) return;
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) return;

        var type = vehicle.computed().weaponHudType;
        if (type.equals(EMPTY)) return;

        if (vehicle.getSeatIndex(player) != vehicle.computed().turretControllerIndex) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        switch (type) {
            case LandWeaponHud.ID ->
                    LandWeaponHud.renderLandArmorHud(vehicle, player, gui, guiGraphics, partialTick, screenWidth, screenHeight);
        }

        poseStack.popPose();
    }
}
