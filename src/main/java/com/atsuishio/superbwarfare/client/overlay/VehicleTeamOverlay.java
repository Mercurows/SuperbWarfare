package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.SpyglassRangeOverlay.FRIENDLY_INDICATOR;

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
        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked"))
            return;

        boolean lookAtEntity = false;

        double entityRange = 0;
        Entity lookingEntity = TraceTool.camerafFindLookingEntity(player, cameraPos, viewVec, 512);

        if (lookingEntity != null) {
            lookAtEntity = true;
            entityRange = player.distanceTo(lookingEntity);
        }

        if (lookAtEntity && lookingEntity instanceof VehicleEntity vehicle) {
            Vec3 pos = lookingEntity.getBoundingBox().getCenter().add(new Vec3(0, lookingEntity.getBbHeight() / 2 + 1, 0));
            Vec3 point = VectorUtil.worldToScreen(pos);

            float x = (float) point.x;
            float y = (float) point.y;

            poseStack.pushPose();
            poseStack.translate(x, y - 12, 0);

            float size = (float) Mth.clamp((50 / VectorUtil.fov) * 0.9f * Math.max((512 - entityRange) / 512, 0.1), 0.4, 1);
            poseStack.scale(size, size, size);
            var font = gui.getMinecraft().font;

            int color = -1;

            if (lookingEntity.getFirstPassenger() instanceof Player player1) {
                color = player1.getTeamColor();
                String info = player1.getDisplayName().getString() + (player1.getTeam() == null ? "" : " <" + (player1.getTeam().getName()) + ">");
                int width = Minecraft.getInstance().font.width(info);
                guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
            } else {
                String info = lookingEntity.getDisplayName().getString();
                int width = Minecraft.getInstance().font.width(info);
                guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
            }

            String range = FormatTool.format1D(entityRange, "M");
            int width2 = Minecraft.getInstance().font.width(range);
            guiGraphics.drawString(font, Component.literal(range), -width2 / 2, 7, color, false);

            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40, -2, 40, 2, 0, -16777216);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40, -2, -40 + 80 * (vehicle.getHealth() / vehicle.getMaxHealth()), 2, 0, -1);

            poseStack.popPose();
        }

        if (player.getVehicle() instanceof VehicleEntity) {
            List<Entity> entities = SeekTool.getPlayer(player, player.level());
            for (var e : entities) {
                if (e != null && e != player && VectorUtil.canSee(e.position())) {
                    Entity team = e;
                    if (e.getVehicle() != null) {
                        team = e.getVehicle();
                    }
                    Vec3 pos = new Vec3(Mth.lerp(partialTick, team.xo, team.getX()), Mth.lerp(partialTick, team.yo + team.getBbHeight() / 2, team.getY() + team.getBbHeight() / 2), Mth.lerp(partialTick, team.zo, team.getZ()));
                    Vec3 point = VectorUtil.worldToScreen(pos);
                    float xf = (float) point.x;
                    float yf = (float) point.y;

                    preciseBlit(guiGraphics, FRIENDLY_INDICATOR, Mth.clamp(xf - 6, 0, screenWidth - 12), Mth.clamp(yf - 6, 0, screenHeight - 12), 0, 0, 12, 12, 12, 12);
                }
            }
        }
    }

    public static double calculateAngle(Entity entityA, Camera camera) {
        Vec3 v1 = camera.getPosition().vectorTo(entityA.position());
        Vec3 v2 = new Vec3(camera.getLookVector());
        return VectorTool.calculateAngle(v1,v2);
    }
}
