package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static com.atsuishio.superbwarfare.entity.vehicle.DroneEntity.CONTROLLER;

@OnlyIn(Dist.CLIENT)
public class VehicleTeamOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_vehicle_team";

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!DisplayConfig.VEHICLE_INFO.get()) return;

        Minecraft mc = gui.getMinecraft();
        Player player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewVec = new Vec3(camera.getLookVector());
        PoseStack poseStack = guiGraphics.pose();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();

        boolean lookAtEntity = false;

        double entityRange = 0;
        Entity lookingEntity = TraceTool.camerafFindLookingEntity(player, cameraPos, viewVec, VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get());

        if (lookingEntity != null) {
            lookAtEntity = true;
            entityRange = player.distanceTo(lookingEntity);
        }

        boolean usingDrone = stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked");
        boolean outOfRange = entityRange > VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get();

        if (lookAtEntity && lookingEntity instanceof VehicleEntity vehicle && !usingDrone && !outOfRange) {
            if (entityRange > VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get()) return;
            Vec3 pos = new Vec3(Mth.lerp(partialTick, lookingEntity.xo, lookingEntity.getX()), Mth.lerp(partialTick, lookingEntity.yo, lookingEntity.getY()) + lookingEntity.getBbHeight() / 2, Mth.lerp(partialTick, lookingEntity.zo, lookingEntity.getZ()))
                    .add(new Vec3(0, lookingEntity.getBbHeight() / 2 + 0.5, 0));

            if (VectorUtil.canSee(pos)) {

                Vec3 point = VectorUtil.worldToScreen(pos);

                float x = (float) point.x;
                float y = (float) point.y;

                poseStack.pushPose();
                poseStack.translate(x, y - 12, 0);

                float size = (float) Mth.clamp((50 / VectorUtil.fov) * 0.9f * Math.max((512 - entityRange) / 512, 0.1), 0.4, 1);
                poseStack.scale(size, size, size);
                var font = gui.getMinecraft().font;

                int color = -1;

                if (vehicle instanceof DroneEntity drone) {
                    Player controller = EntityFindUtil.findPlayer(drone.level(), drone.getEntityData().get(CONTROLLER));
                    if (controller != null) {
                        color = controller.getTeamColor();
                        String info = controller.getDisplayName().getString() + (controller.getTeam() == null ? "" : " <" + (controller.getTeam().getName()) + ">");
                        int width = Minecraft.getInstance().font.width(info);
                        guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
                    } else {
                        String info = lookingEntity.getDisplayName().getString();
                        int width = Minecraft.getInstance().font.width(info);
                        guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
                    }
                } else if (vehicle instanceof OwnableEntity ownableEntity) {
                    if (ownableEntity.getOwner() instanceof Player player1) {
                        color = player1.getTeamColor();
                        String info = player1.getDisplayName().getString() + (player1.getTeam() == null ? "" : " <" + (player1.getTeam().getName()) + ">");
                        int width = Minecraft.getInstance().font.width(info);
                        guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
                    } else {
                        String info = lookingEntity.getDisplayName().getString();
                        int width = Minecraft.getInstance().font.width(info);
                        guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
                    }
                } else {
                    if (vehicle.getMaxPassengers() > 0 && vehicle.getFirstPassenger() instanceof Player player1) {
                        color = player1.getTeamColor();
                        String info = player1.getDisplayName().getString() + (player1.getTeam() == null ? "" : " <" + (player1.getTeam().getName()) + ">");
                        int width = Minecraft.getInstance().font.width(info);
                        guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
                    } else {
                        String info = vehicle.getDisplayName().getString();
                        int width = Minecraft.getInstance().font.width(info);
                        guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
                    }
                }


                String range = FormatTool.format1D(entityRange, "M");
                int width2 = Minecraft.getInstance().font.width(range);
                int argb = (255 << 24) | color;

                guiGraphics.drawString(font, Component.literal(range), -width2 / 2, 7, color, false);

                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40.5f, -2f, 40.5f, 2f, 0, 0x80000000);

                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -41.5f, -3, -40.5f, 3, 0, argb);

                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40.5f, -3, 40.5f, -2, 0, argb);
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40.5f, 2, 40.5f, 3, 0, argb);

                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), 40.5f, -3, 41.5f, 3, 0, argb);

                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40, -1.5f, -40 + 80 * (vehicle.getHealth() / vehicle.getMaxHealth()), 1.5f, 0, argb);

                poseStack.popPose();
            }
        }
    }
}
