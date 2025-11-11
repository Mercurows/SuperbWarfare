package com.atsuishio.superbwarfare.client.overlay.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

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

    public static void render(VehicleEntity vehicle, Player player, ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();

        int index = vehicle.getSeatIndex(player);
        var data = vehicle.getGunData(index);

        if (data == null) return;

        PoseStack poseStack = guiGraphics.pose();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewVec = new Vec3(camera.getLookVector());

        poseStack.pushPose();

        double yaw = -VehicleVecUtils.getYRotFromVector(vehicle.getBarrelVector(partialTick));
        double pitch = -VehicleVecUtils.getXRotFromVector(vehicle.getBarrelVector(partialTick));

        preciseBlit(guiGraphics, COMPASS, (float) screenWidth / 2 - 128, 10F, 128 + (64F / 45 * (float) yaw), 0, 256, 16, 512, 16);
        preciseBlit(guiGraphics, ROLL_IND_WHITE, (float) screenWidth / 2 - 4, 27, 0, 0F, 8, 8, 8, 8);

        int width = Minecraft.getInstance().font.width(FormatTool.DECIMAL_FORMAT_1ZZ.format(yaw));
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(yaw)), screenWidth / 2 - width / 2, 40, -1, false);

        preciseBlit(guiGraphics, CANNON_PITCH, (float) screenWidth / 2 + 166, (float) screenHeight / 2 - 64, 0, 0F, 8, 128, 8, 128);

        int widthP = Minecraft.getInstance().font.width(FormatTool.DECIMAL_FORMAT_1ZZ.format(pitch));

        poseStack.pushPose();

        guiGraphics.pose().translate(0, pitch * 0.7, 0);
        preciseBlit(guiGraphics, CANNON_PITCH_IND, (float) screenWidth / 2 + 158, (float) screenHeight / 2 - 4, 0, 0, 8, 8, 8, 8);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(pitch)), screenWidth / 2 + 157 - widthP, screenHeight / 2 - 4, -1, false);

        poseStack.popPose();

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
        poseStack.popPose();
    }
}
