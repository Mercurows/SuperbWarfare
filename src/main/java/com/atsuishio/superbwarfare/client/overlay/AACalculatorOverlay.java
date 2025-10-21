package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.tools.RangeTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class AACalculatorOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_aa_calculator";

    private static Entity lockedEntity;
    private static final ResourceLocation FRAME_TARGET = Mod.loc("textures/screens/frame/frame_target.png");
    private static final ResourceLocation FRAME_LOCK = Mod.loc("textures/screens/frame/frame_lock.png");
    private static final ResourceLocation SHOOT_INDICATOR = Mod.loc("textures/screens/igla_9k38/frame.png");
    private static final ResourceLocation BLOCK = Mod.loc("textures/screens/block.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();
        var options = mc.options;
        Player player = mc.player;
        PoseStack poseStack = guiGraphics.pose();

        if (!shouldRenderCrossHair(player)) return;

        Entity aa = player.getVehicle();
        if (aa == null) return;

        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
                Entity naerestEntity = new SeekTool.Builder(vehicle)
                        .withinRange(512)
                        .withinAngle(20)
                        .baseFilter()
                        .smokeFilter()
                        .noVehicle()
                        .notFriendly()
                        .vehicleNoClip()
                        .buildWithClosest();

                Entity target;

                if (options.keySprint.isDown()) {
                    if (lockedEntity == null) {
                        lockedEntity = naerestEntity;
                    }
                    target = lockedEntity;
                } else {
                    lockedEntity = null;
                    target = naerestEntity;
                }

                if (lockedEntity != null && !lockedEntity.isAlive()) {
                    lockedEntity = null;
                }

                boolean lockOn = lockedEntity != null && options.keySprint.isDown();

                if (target != null) {
                    Vec3 pos = VectorTool.lerpGetEntityBoundingBoxCenter(target, partialTick);
                    Vec3 point = VectorUtil.worldToScreen(pos);

                    Vec3 targetHudPos = null;
                    Vec3 shootHudPos = null;

                    if (VectorUtil.canSee(pos)) {
                        poseStack.pushPose();
                        float x = (float) point.x;
                        float y = (float) point.y;

                        targetHudPos = new Vec3(x, y, 0);

                        RenderHelper.blit(poseStack, lockOn ? FRAME_LOCK : FRAME_TARGET, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                        poseStack.popPose();
                    }

                    if (lockOn) {
                        Vec3 shootVector = RangeTool.calculateFiringSolution(vehicle.getShootPos(player, partialTick), VectorTool.lerpGetEntityBoundingBoxCenter(target, partialTick), target.getDeltaMovement().scale(1.25), vehicle.projectileVelocity(player), vehicle.projectileGravity(player)).normalize();
                        Vec3 shootPos = vehicle.getShootPos(player, partialTick).add(shootVector.scale(vehicle.getShootPos(player, partialTick).distanceTo(VectorTool.lerpGetEntityBoundingBoxCenter(target, partialTick))));
                        Vec3 point0 = VectorUtil.worldToScreen(shootPos);

                        if (VectorUtil.canSee(shootPos)) {
                            poseStack.pushPose();
                            float x0 = (float) point0.x;
                            float y0 = (float) point0.y;

                            shootHudPos = new Vec3(x0, y0, 0);

                            RenderHelper.blit(poseStack, SHOOT_INDICATOR, x0 - 12, y0 - 12, 0, 0, 24, 24, 24, 24, 1f);
                            poseStack.popPose();
                        }
                    }

                    if (targetHudPos != null && shootHudPos != null) {
                        double dis = targetHudPos.distanceTo(shootHudPos);
                        for (double i = 3; i < dis - 3; i += 3) {
                            Vec3 toVec = targetHudPos.vectorTo(shootHudPos).normalize();
                            Vec3 p0 = targetHudPos.add(toVec.scale(i));
                            RenderHelper.blit(poseStack, BLOCK, (float) (p0.x - 0.5), (float) (p0.y - 0.5), 0, 0, 1, 1, 1, 1, 1f);
                        }
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private static boolean shouldRenderCrossHair(Player player) {
        if (player == null) return false;
        return !player.isSpectator() && player.getVehicle() instanceof VehicleEntity vehicle
                && vehicle instanceof WeaponVehicleEntity weaponVehicle
                && weaponVehicle.hasWeapon(vehicle.getSeatIndex(player))
                && weaponVehicle.getWeapon(vehicle.getSeatIndex(player)).aaProjectileWeapon;
    }
}
