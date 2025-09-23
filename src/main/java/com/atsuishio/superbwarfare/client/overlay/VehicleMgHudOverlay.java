package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity;
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.LandArmorEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Math;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicator3P;
import static com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity.MG_AMMO;
import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.AMMO;
import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.HEAT;

@OnlyIn(Dist.CLIENT)
public class VehicleMgHudOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_vehicle_mg_hud";

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();
        Player player = mc.player;
        PoseStack poseStack = guiGraphics.pose();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        if (!shouldRenderCrossHair(player)) return;

        Entity cannon = player.getVehicle();
        if (cannon == null) return;

        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (player.getVehicle() instanceof LandArmorEntity iLand && iLand instanceof WeaponVehicleEntity weaponVehicle && iLand instanceof VehicleEntity vehicle && weaponVehicle.hasWeapon(vehicle.getSeatIndex(player))) {
            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
                float fovAdjust = (float) 70 / Minecraft.getInstance().options.fov().get();

                float f = (float) Math.min(screenWidth, screenHeight);
                float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * fovAdjust;
                int i = Mth.floor(f * f1);
                int j = Mth.floor(f * f1);
                int k = (screenWidth - i) / 2;
                int l = (screenHeight - j) / 2;
                RenderHelper.preciseBlit(guiGraphics, Mod.loc("textures/screens/cannon/cannon_crosshair_notzoom.png"), k, l, 0, 0.0F, i, j, i, j);
                VehicleHudOverlay.renderKillIndicator(guiGraphics, screenWidth, screenHeight);
            } else if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK && !ClientEventHandler.zoomVehicle) {
                Vec3 pos;
                if (player.getVehicle() instanceof SpeedboatEntity) {
                    pos = vehicle.getTurretShootPos(player, partialTick).add(iLand.getGunVec(partialTick).scale(192));
                } else {
                    pos = vehicle.passengerWeaponShootPos(player, partialTick).add(iLand.getGunVec(partialTick).scale(192));
                }

                Vec3 p = VectorUtil.worldToScreen(pos);

                if (VectorUtil.canSee(pos)) {
                    // 第三人称准星
                    float x = (float) p.x;
                    float y = (float) p.y;

                    preciseBlit(guiGraphics, Mod.loc("textures/screens/drone.png"), x - 12, y - 12, 0, 0, 24, 24, 24, 24);
                    renderKillIndicator3P(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                    poseStack.pushPose();

                    poseStack.translate(x, y, 0);
                    poseStack.scale(0.75f, 0.75f, 1);

                    // YX-100
                    if (player.getVehicle() instanceof Yx100Entity yx100) {
                        double heat = yx100.getEntityData().get(HEAT) / 100.0F;
                        guiGraphics.drawString(mc.font, Component.literal(".50 HMG " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : yx100.getEntityData().get(MG_AMMO))), 30, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
                    }

                    // 快艇
                    if (player.getVehicle() instanceof SpeedboatEntity speedboat) {
                        double heat = speedboat.getEntityData().get(HEAT) / 100.0F;
                        guiGraphics.drawString(mc.font, Component.literal(".50 HMG " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : speedboat.getEntityData().get(AMMO))), 30, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
                    }

                    double heal = 1 - vehicle.getHealth() / vehicle.getMaxHealth();

                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("HP " +
                            FormatTool.format0D(100 * vehicle.getHealth() / vehicle.getMaxHealth())), 30, 1, Mth.hsvToRgb(0F, (float) heal, 1.0F), false);

                    poseStack.popPose();
                }
            }
        }

        poseStack.popPose();
    }

    private static boolean shouldRenderCrossHair(Player player) {
        if (player == null) return false;
        return !player.isSpectator()
                && (player.getVehicle() instanceof SpeedboatEntity
                || (player.getVehicle() instanceof Yx100Entity yx100 && yx100.getNthEntity(1) == player)
                || (player.getVehicle() instanceof Bmp2Entity bmp2 && bmp2.getNthEntity(0) != player && ((Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)))
        );
    }
}
