package com.atsuishio.superbwarfare.client.overlay.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;

import java.util.Map;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicator;
import static com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicator3P;

@OnlyIn(Dist.CLIENT)
public class ArtilleryHud {

    public static final String ID = "@Artillery";

    private static final ResourceOnceLogger LOGGER = new ResourceOnceLogger();

    private static final ResourceLocation COMPASS = Mod.loc("textures/overlay/vehicle/base/compass.png");
    private static final ResourceLocation THIRD_CROSSHAIR = Mod.loc("textures/overlay/vehicle/crosshair/third_camera.png");
    private static final ResourceLocation ROLL_IND_WHITE = Mod.loc("textures/overlay/vehicle/cannon/roll_ind_white.png");
    private static final ResourceLocation CANNON_PITCH = Mod.loc("textures/overlay/vehicle/cannon/cannon_pitch.png");
    private static final ResourceLocation CANNON_PITCH_IND = Mod.loc("textures/overlay/vehicle/cannon/cannon_pitch_ind.png");
    private static final ResourceLocation INDICATOR = Mod.loc("textures/overlay/vehicle/cannon/indicator.png");

    public static final Map<String, ResourceLocation> CROSSHAIR_MAP = Map.ofEntries(
            Map.entry("@VehicleCommonCross", Mod.loc("textures/overlay/vehicle/crosshair/common_cross.png")),
            Map.entry("@VehicleCnHpjZooming", Mod.loc("textures/overlay/vehicle/crosshair/cn_hpj_zooming.png")),
            Map.entry("@VehicleCommonCannon", Mod.loc("textures/overlay/vehicle/crosshair/common_cannon.png")),
            Map.entry("@VehicleCommonCannonZooming", Mod.loc("textures/overlay/vehicle/crosshair/common_cannon_zooming.png")),
            Map.entry("@VehicleLaserCannon", Mod.loc("textures/overlay/vehicle/crosshair/laser_cannon.png"))
    );

    public static void render(VehicleEntity vehicle, Player player, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();

        int index = vehicle.getSeatIndex(player);
        var data = vehicle.getGunData(index);

        if (data == null) return;

        String zoomCross = data.compute().crosshairZooming;
        String crosshair = data.compute().crosshair;
        if (zoomCross.equals(CrossHairOverlay.CROSSHAIR_EMPTY) && crosshair.equals(CrossHairOverlay.CROSSHAIR_EMPTY))
            return;

        int color = data.compute().crosshairColor.get();

        PoseStack poseStack = guiGraphics.pose();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewVec = new Vec3(camera.getLookVector());

        poseStack.pushPose();

        preciseBlit(guiGraphics, COMPASS, (float) screenWidth / 2 - 128, 10F, 128 + (64F / 45 * (Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot()))), 0, 256, 16, 512, 16);
        preciseBlit(guiGraphics, ROLL_IND_WHITE, (float) screenWidth / 2 - 4, 27, 0, 0F, 8, 8, 8, 8);

        String angle = FormatTool.DECIMAL_FORMAT_1ZZ.format(Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot()));
        int width = Minecraft.getInstance().font.width(angle);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(angle), screenWidth / 2 - width / 2, 40, -1, false);

        preciseBlit(guiGraphics, CANNON_PITCH, (float) screenWidth / 2 + 166, (float) screenHeight / 2 - 64, 0, 0F, 8, 128, 8, 128);

        String pitch = FormatTool.DECIMAL_FORMAT_1ZZ.format(-Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot()));
        int widthP = Minecraft.getInstance().font.width(pitch);

        poseStack.pushPose();

        guiGraphics.pose().translate(0, Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot()) * 0.7, 0);
        preciseBlit(guiGraphics, CANNON_PITCH_IND, (float) screenWidth / 2 + 158, (float) screenHeight / 2 - 4, 0, 0, 8, 8, 8, 8);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(pitch), screenWidth / 2 + 157 - widthP, screenHeight / 2 - 4, -1, false);

        poseStack.popPose();

        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
            float fovAdjust = 70F / Minecraft.getInstance().options.fov().get();
            float f = (float) Math.min(screenWidth, screenHeight);
            float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * fovAdjust;
            int i = Mth.floor(f * f1);
            int j = Mth.floor(f * f1);
            int k = (screenWidth - i) / 2;
            int l = (screenHeight - j) / 2;

            // 瞄准时使用zooming准星
            if (ClientEventHandler.zoomVehicle) {
                Vec3 shootPos = player.getEyePosition(partialTick);

                if (!(vehicle instanceof AnnihilatorEntity)) {
                    shootPos = vehicle.getZoomPos(player, partialTick);
                }

                Entity lookingEntity = TraceTool.camerafFindLookingEntity(player, cameraPos, viewVec, 512);
                boolean lookAtEntity = false;

                BlockHitResult result = player.level().clip(new ClipContext(shootPos, shootPos.add(player.getViewVector(1).scale(512)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                Vec3 hitPos = result.getLocation();

                double blockRange = player.getEyePosition(1).distanceTo(hitPos);
                double entityRange = 0;

                if (lookingEntity instanceof LivingEntity living) {
                    lookAtEntity = true;
                    entityRange = player.distanceTo(living);
                }

                if (lookAtEntity) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                                    .append(Component.literal(FormatTool.format1D(entityRange, "m ") + lookingEntity.getDisplayName().getString())),
                            screenWidth / 2 + 14, screenHeight / 2 - 20, -1, false);
                } else {
                    if (blockRange > 511) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                                .append(Component.literal("---m")), screenWidth / 2 + 14, screenHeight / 2 - 20, -1, false);
                    } else {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                                        .append(Component.literal(FormatTool.format1D(blockRange, "m"))),
                                screenWidth / 2 + 14, screenHeight / 2 - 20, -1, false);
                    }
                }

                if (zoomCross.equals(CrossHairOverlay.CROSSHAIR_CUSTOM)) {
                    // 载具自定义第一人称渲染
                    vehicle.renderFirstPersonOverlay(guiGraphics, poseStack, mc.font, player, screenWidth, screenHeight, 1, color);
                } else {
                    ResourceLocation texture;
                    if (zoomCross.startsWith("@")) {
                        texture = CROSSHAIR_MAP.get(zoomCross);
                    } else {
                        texture = ResourceLocation.tryParse(zoomCross);
                    }

                    if (texture == null) {
                        LOGGER.log(zoomCross, logger -> logger.error("Failed to load crosshair texture for {}", zoomCross));
                    } else {
                        if (!zoomCross.equals("@VehicleCommonCross")) {
                            float diffY = -Mth.wrapDegrees(Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot()) - Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot()));
                            preciseBlit(guiGraphics, INDICATOR, (float) screenWidth / 2 - 4.3f + 0.45f * diffY, (float) screenHeight / 2 - 10, 0, 0F, 8, 8, 8, 8);
                        }

                        preciseBlit(guiGraphics, texture, k, l, 0, 0, i, j, i, j);
                    }
                }
            } else {
                if (crosshair.equals(CrossHairOverlay.CROSSHAIR_CUSTOM)) {
                    // 载具自定义第一人称渲染
                    vehicle.renderFirstPersonOverlay(guiGraphics, poseStack, mc.font, player, screenWidth, screenHeight, 1, color);
                } else {
                    ResourceLocation texture;
                    if (zoomCross.startsWith("@")) {
                        texture = CROSSHAIR_MAP.get(zoomCross);
                    } else {
                        texture = ResourceLocation.tryParse(zoomCross);
                    }

                    if (texture == null) {
                        LOGGER.log(zoomCross, logger -> logger.error("Failed to load crosshair texture for {}", zoomCross));
                    } else {
                        preciseBlit(guiGraphics, texture, k, l, 0, 0, i, j, i, j);
                    }
                }

                // TODO 实现舰炮武器数据包化
//                if (!(vehicle instanceof Hpj11Entity)) {
//                    preciseBlit(guiGraphics, COMMON_CANNON, k, l, 0, 0, i, j, i, j);
//                } else {
//                    preciseBlit(guiGraphics, HPJ_CROSSHAIR_NOTZOOM, k, l, 0, 0, i, j, i, j);
//                }
            }

            renderKillIndicator(guiGraphics, screenWidth, screenHeight);
        } else if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            Vec3 pos = cameraPos.add(vehicle.getViewVector(partialTick).scale(128));
            Vec3 p = VectorUtil.worldToScreen(pos);

            if (VectorUtil.canSee(pos)) {
                // 第三人称准星
                float x = (float) p.x;
                float y = (float) p.y;

                preciseBlit(guiGraphics, THIRD_CROSSHAIR, x - 12, y - 12, 0, 0, 24, 24, 24, 24);
                renderKillIndicator3P(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                poseStack.pushPose();

                poseStack.translate(x, y, 0);
                poseStack.scale(0.75f, 0.75f, 1);

//                if (player.getVehicle() instanceof Mk42Entity || player.getVehicle() instanceof Mle1934Entity) {
//                    if (cannonEntity.getWeaponIndex(0) == 0) {
//                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("AP SHELL " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : cannon.getAmmoCount(player))), 30, -9, -1, false);
//                    } else {
//                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("HE SHELL " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : cannon.getAmmoCount(player))), 30, -9, -1, false);
//                    }
//                }

                // 歼灭者
//                if (player.getVehicle() instanceof AnnihilatorEntity annihilatorEntity) {
//                    guiGraphics.drawString(mc.font, Component.literal("LASER " + (FormatTool.format0D((double) (100 * annihilatorEntity.getEnergy()) / annihilatorEntity.getMaxEnergy()) + "％")), 30, -9, -1, false);
//                }

                double heal = 1 - vehicle.getHealth() / vehicle.getMaxHealth();
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("HP " +
                        FormatTool.format0D(100 * vehicle.getHealth() / vehicle.getMaxHealth())), 30, 1, Mth.hsvToRgb(0F, (float) heal, 1), false);

                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }
}
