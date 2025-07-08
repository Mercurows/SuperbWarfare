package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.entity.vehicle.DroneEntity.*;

@OnlyIn(Dist.CLIENT)
public class DroneHudOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_drone_hud";

    public static int getMaxDistance() {
        var connection = Minecraft.getInstance().getConnection();
        return (connection == null ? 16 : connection.serverSimulationDistance) * 16;
    }

    private static final ResourceLocation FRAME = Mod.loc("textures/screens/frame/frame.png");
    private static final ResourceLocation TV_FRAME = Mod.loc("textures/screens/land/tv_frame.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();
        Player player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        if (player == null) return;

        PoseStack poseStack = guiGraphics.pose();

        ItemStack stack = player.getMainHandItem();

        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            guiGraphics.blit(Mod.loc("textures/screens/drone.png"), screenWidth / 2 - 16, screenHeight / 2 - 16, 0, 0, 32, 32, 32, 32);
            guiGraphics.blit(Mod.loc("textures/screens/drone_fov.png"), screenWidth / 2 + 100, screenHeight / 2 - 64, 0, 0, 64, 129, 64, 129);
            int addW = (screenWidth / screenHeight) * 48;
            int addH = (screenWidth / screenHeight) * 27;
            preciseBlit(guiGraphics, TV_FRAME, (float) -addW / 2, (float) -addH / 2, 10, 0, 0.0F, screenWidth + addW, screenHeight + addH, screenWidth + addW, screenHeight + addH);

            preciseBlit(guiGraphics, Mod.loc("textures/screens/drone_fov_move.png"), (float) screenWidth / 2 + 100, (float) (screenHeight / 2f - 64 - ((ClientEventHandler.droneFovLerp - 1) * 23.8)), 0, 0, 64, 129, 64, 129);
            guiGraphics.drawString(mc.font, Component.literal(FormatTool.format1D(ClientEventHandler.droneFovLerp, "x")),
                    screenWidth / 2 + 144, screenHeight / 2 + 56 - (int) ((ClientEventHandler.droneFovLerp - 1) * 23.8), -1, false);

            DroneEntity entity = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone"));

            if (entity != null) {
                boolean lookAtEntity = false;
                double distance = player.distanceTo(entity);

                BlockHitResult result = entity.level().clip(new ClipContext(cameraPos, cameraPos.add(entity.getViewVector(1).scale(512)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
                Vec3 hitPos = result.getLocation();

                double blockRange = cameraPos.distanceTo(hitPos);

                double entityRange = 0;

                Entity lookingEntity = TraceTool.droneFindLookingEntity(entity, cameraPos, 512, partialTick);
                if (lookingEntity != null) {
                    lookAtEntity = true;
                    entityRange = entity.distanceTo(lookingEntity);
                }

                int color = -1;

                // 超出距离警告
                if (distance > getMaxDistance() - 48) {
                    guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.warning"),
                            screenWidth / 2 - 18, screenHeight / 2 - 47, -65536, false);
                    color = -65536;
                }

                // 距离
                guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.distance")
                                .append(Component.literal(FormatTool.format1D(distance, "m"))),
                        screenWidth / 2 + 10, screenHeight / 2 + 33, color, false);

                // 血量
                guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.health")
                                .append(Component.literal(FormatTool.format1D(entity.getHealth()) + " / " + FormatTool.format1D(entity.getMaxHealth()))),
                        screenWidth / 2 - 77, screenHeight / 2 + 33, -1, false);
                if (!entity.getEntityData().get(IS_KAMIKAZE)) {
                    // 弹药
                    guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.ammo")
                                    .append(Component.literal(entity.getEntityData().get(AMMO) + " / " + entity.getEntityData().get(MAX_AMMO))),
                            screenWidth / 2 + 12, screenHeight / 2 - 37, -1, false);
                } else {
                    // 神风
                    guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.kamikaze"),
                            screenWidth / 2 + 12, screenHeight / 2 - 37, -65536, false);
                }

                if (lookAtEntity) {
                    // 实体距离
                    guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.range")
                                    .append(Component.literal(FormatTool.format1D(entityRange, "m ") + lookingEntity.getDisplayName().getString())),
                            screenWidth / 2 + 12, screenHeight / 2 - 28, color, false);
                } else {
                    // 方块距离
                    if (blockRange > 500) {
                        guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.range")
                                .append(Component.literal("---m")), screenWidth / 2 + 12, screenHeight / 2 - 28, color, false);
                    } else {
                        guiGraphics.drawString(mc.font, Component.translatable("tips.superbwarfare.drone.range")
                                        .append(Component.literal(FormatTool.format1D(blockRange, "m"))),
                                screenWidth / 2 + 12, screenHeight / 2 - 28, color, false);
                    }
                }


                List<Entity> entities = SeekTool.seekLivingEntities(entity, entity.level(), 256, 30);
                for (var e : entities) {
                    Vec3 pos = new Vec3(Mth.lerp(partialTick, e.xo, e.getX()), Mth.lerp(partialTick, e.yo + e.getEyeHeight(), e.getEyeY()), Mth.lerp(partialTick, e.zo, e.getZ()));
                    Vec3 point = VectorUtil.worldToScreen(pos, cameraPos);
                    if (point != null) {
                        poseStack.pushPose();
                        float x = (float) point.x;
                        float y = (float) point.y;

                        RenderHelper.blit(poseStack, FRAME, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 1f);
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
