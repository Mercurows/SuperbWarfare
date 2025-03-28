package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.entity.vehicle.DroneEntity.AMMO;
import static com.atsuishio.superbwarfare.entity.vehicle.DroneEntity.KAMIKAZE_MODE;

@EventBusSubscriber(value = Dist.CLIENT)
public class DroneUIOverlay {

    public static int MAX_DISTANCE = 256;
    private static final ResourceLocation FRAME = ModUtils.loc("textures/screens/frame/frame.png");

    @SubscribeEvent
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        PoseStack poseStack = guiGraphics.pose();

        ItemStack stack = player.getMainHandItem();

        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        var tag = NBTTool.getTag(stack);
        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            guiGraphics.blit(ModUtils.loc("textures/screens/drone.png"), w / 2 - 16, h / 2 - 16, 0, 0, 32, 32, 32, 32);
            guiGraphics.blit(ModUtils.loc("textures/screens/drone_fov.png"), w / 2 + 100, h / 2 - 64, 0, 0, 64, 129, 64, 129);

            preciseBlit(guiGraphics, ModUtils.loc("textures/screens/drone_fov_move.png"), (float) w / 2 + 100, (float) (h / 2 - 64 - ((ClientEventHandler.droneFovLerp - 1) * 23.8)), 0, 0, 64, 129, 64, 129);
            guiGraphics.drawString(mc.font, Component.literal(FormatTool.format1D(ClientEventHandler.droneFovLerp, "x")),
                    w / 2 + 144, h / 2 + 56 - (int) ((ClientEventHandler.droneFovLerp - 1) * 23.8), -1, false);

            DroneEntity entity = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));

            if (entity != null) {
                boolean lookAtEntity = false;
                double distance = player.distanceTo(entity);

                BlockHitResult result = entity.level().clip(new ClipContext(entity.getEyePosition(), entity.getEyePosition().add(player.getViewVector(1).scale(512)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
                Vec3 hitPos = result.getLocation();

                double blockRange = player.getEyePosition(1).distanceTo(hitPos);

                double entityRange = 0;

                Entity lookingEntity = SeekTool.seekLivingEntity(entity, entity.level(), 512, 2);
                if (lookingEntity != null) {
                    lookAtEntity = true;
                    entityRange = entity.distanceTo(lookingEntity);
                }

                int color = -1;

                // 超出距离警告
                if (distance > MAX_DISTANCE - 48) {
                    guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.warning"),
                            w / 2 - 18, h / 2 - 47, -65536, false);
                    color = -65536;
                }

                // 距离
                guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.distance")
                                .append(Component.literal(FormatTool.format1D(distance, "m"))),
                        w / 2 + 10, h / 2 + 33, color, false);

                // 血量
                guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.health")
                                .append(Component.literal(FormatTool.format1D(entity.getHealth()) + " / " + FormatTool.format1D(entity.getMaxHealth()))),
                        w / 2 - 77, h / 2 + 33, -1, false);
                if (entity.getEntityData().get(KAMIKAZE_MODE) == 0) {
                    // 弹药
                    guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.ammo")
                                    .append(Component.literal(FormatTool.format1D(entity.getEntityData().get(AMMO), " / 6"))),
                            w / 2 + 12, h / 2 - 37, -1, false);
                } else {
                    // 神风
                    guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.kamikaze"),
                            w / 2 + 12, h / 2 - 37, -65536, false);
                }

                if (lookAtEntity) {
                    // 实体距离
                    guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.range")
                                    .append(Component.literal(FormatTool.format1D(entityRange, "m ") + lookingEntity.getDisplayName().getString())),
                            w / 2 + 12, h / 2 - 28, color, false);
                } else {
                    // 方块距离
                    if (blockRange > 500) {
                        guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.range")
                                .append(Component.literal("---m")), w / 2 + 12, h / 2 - 28, color, false);
                    } else {
                        guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.range")
                                        .append(Component.literal(FormatTool.format1D(blockRange, "m"))),
                                w / 2 + 12, h / 2 - 28, color, false);
                    }
                }


                List<Entity> entities = SeekTool.seekLivingEntities(entity, entity.level(), 256, 30);
                float fovAdjust2 = (float) (mc.options.fov().get() / 30) - 1;
                double zoom = 0.975 * ClientEventHandler.droneFovLerp + 0.06 * fovAdjust2;

                for (var e : entities) {
                    Vec3 droneVec = new Vec3(Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), entity.xo, entity.getX()), Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), entity.yo + entity.getEyeHeight(), entity.getEyeY()), Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), entity.zo, entity.getZ()));
                    Vec3 pos = new Vec3(Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), e.xo, e.getX()), Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), e.yo + e.getEyeHeight(), e.getEyeY()), Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), e.zo, e.getZ()));

                    Vec3 lookAngle = entity.getLookAngle().normalize().scale(pos.distanceTo(droneVec) * (1 - 1.0 / zoom));

                    var cPos = droneVec.add(lookAngle);
                    Vec3 point = RenderHelper.worldToScreen(pos, cPos);
                    if (point != null) {
                        poseStack.pushPose();
                        float x = (float) point.x;
                        float y = (float) point.y;

                        RenderHelper.preciseBlit(guiGraphics, FRAME, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                        poseStack.popPose();
                    }
                }
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        poseStack.popPose();
    }
}
