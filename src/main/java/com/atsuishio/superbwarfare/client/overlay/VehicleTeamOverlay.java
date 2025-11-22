package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.atsuishio.superbwarfare.entity.vehicle.DroneEntity.CONTROLLER;
import static com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity.ACTIVE;

@OnlyIn(Dist.CLIENT)
public class VehicleTeamOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("vehicle_team");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!DisplayConfig.VEHICLE_INFO.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewVec = new Vec3(camera.getLookVector());
        PoseStack poseStack = guiGraphics.pose();

        var partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

        ItemStack stack = player.getMainHandItem();

        boolean lookAtEntity = false;

        double entityRange = 0;
        Entity lookingEntity = TraceTool.camerafFindLookingEntity(player, cameraPos, viewVec, VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get());

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            lookingEntity = vehicle.getPlayerLookAtEntityOnVehicle(player, 512, partialTick);
            viewVec = vehicle.getViewVec(player, partialTick);
        }

        Entity decoy = TraceTool.findLookDecoy(player, cameraPos, viewVec, 512);

        if (decoy != null && decoy.getType().is(ModTags.EntityTypes.DECOY)) return;

        if (lookingEntity instanceof SmokeDecoyEntity) return;

        if (lookingEntity != null) {
            lookAtEntity = true;
            entityRange = player.distanceTo(lookingEntity);
        }

        var tag = NBTTool.getTag(stack);

        boolean usingDrone = stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked");
        boolean outOfRange = entityRange > VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get();

        if (lookAtEntity && lookingEntity instanceof VehicleEntity vehicle && !usingDrone && !outOfRange) {
            if (entityRange > VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get()) return;

            Vec3 pos = VectorTool.lerpGetEntityBoundingBoxCenter(lookingEntity, partialTick)
                    .add(new Vec3(0, lookingEntity.getBbHeight() / 2 + 0.5, 0));

            Vec3 centerPos = VectorTool.lerpGetEntityBoundingBoxCenter(lookingEntity, partialTick);

            if (VectorUtil.canSee(pos)) {
                Vec3 point = VectorUtil.worldToScreen(pos);

                float x = (float) point.x;
                float y = (float) point.y;

                poseStack.pushPose();
                poseStack.translate(x, y - 12, 0);

                float size = (float) Mth.clamp((50 / VectorUtil.fov) * 0.9f * Math.max((512 - entityRange) / 512, 0.1), 0.4, 1);
                poseStack.scale(size, size, size);
                var font = Minecraft.getInstance().font;

                int color = -1;

                if (vehicle instanceof DroneEntity drone) {
                    Player controller = EntityFindUtil.findPlayer(drone.level(), drone.getEntityData().get(CONTROLLER));
                    if (controller != null) {
                        color = controller.getTeamColor();
                        String info = controller.getDisplayName().getString() + (controller.getTeam() == null ? "" : " <" + (controller.getTeam().getName()) + ">");
                        guiGraphics.drawString(font, Component.literal(info), -font.width(info) / 2, -13, color, false);
                    } else {
                        String info = lookingEntity.getDisplayName().getString();
                        guiGraphics.drawString(font, Component.literal(info), -font.width(info) / 2, -13, color, false);
                    }
                } else if (vehicle instanceof OwnableEntity ownableEntity) {
                    if (ownableEntity.getOwner() instanceof Player player1) {
                        color = player1.getTeamColor();
                        String info = player1.getDisplayName().getString() + (player1.getTeam() == null ? "" : " <" + (player1.getTeam().getName()) + ">");
                        guiGraphics.drawString(font, Component.literal(info), -font.width(info) / 2, -13, color, false);
                    } else {
                        String info = lookingEntity.getDisplayName().getString();
                        guiGraphics.drawString(font, Component.literal(info), -font.width(info) / 2, -13, color, false);
                    }
                } else {
                    if (vehicle.getMaxPassengers() > 0 && vehicle.getFirstPassenger() instanceof Player player1) {
                        color = player1.getTeamColor();
                        String info = player1.getDisplayName().getString() + (player1.getTeam() == null ? "" : " <" + (player1.getTeam().getName()) + ">");
                        guiGraphics.drawString(font, Component.literal(info), -font.width(info) / 2, -13, color, false);
                    } else {
                        String info = vehicle.getDisplayName().getString();
                        guiGraphics.drawString(font, Component.literal(info), -font.width(info) / 2, -13, color, false);
                    }
                }

                String range = FormatTool.format1D(entityRange, "M");
                int argb = (255 << 24) | color;

                guiGraphics.drawString(font, Component.literal(range), -font.width(range) / 2, 7, color, false);

                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40.5f, -2f, 40.5f, 2f, 0, 0x80000000);
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -41.5f, -3, -40.5f, 3, 0, argb);
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40.5f, -3, 40.5f, -2, 0, argb);
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40.5f, 2, 40.5f, 3, 0, argb);
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), 40.5f, -3, 41.5f, 3, 0, argb);
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40, -1.5f, -40 + 80 * (vehicle.getHealth() / vehicle.getMaxHealth()), 1.5f, 0, argb);

                poseStack.popPose();
            }

            if (vehicle instanceof AutoAimableEntity autoAimableEntity && VectorUtil.canSee(centerPos) && player.distanceTo(autoAimableEntity) < 4) {
                Vec3 point = VectorUtil.worldToScreen(centerPos);

                float x = (float) point.x;
                float y = (float) point.y;

                poseStack.pushPose();
                poseStack.translate(x, y - 12, 0);

                var font = Minecraft.getInstance().font;
                Entity entity = autoAimableEntity.getOwner();

                if (entity != null) {
                    int color = autoAimableEntity.getOwner().getTeamColor();
                    boolean active = autoAimableEntity.getEntityData().get(ACTIVE);

                    String info = active ? "tips.superbwarfare.auto_aimable_entity.active" : "tips.superbwarfare.auto_aimable_entity.inactive";
                    Component component = Component.translatable(info);
                    guiGraphics.drawString(font, component, -font.width(component) / 2, -5, color, false);

                    Component ownerInfo = Component.literal("[" + entity.getDisplayName().getString() + "]");
                    guiGraphics.drawString(font, ownerInfo, -font.width(ownerInfo) / 2, 5, color, false);
                }

                poseStack.popPose();
            }
        }
    }
}
