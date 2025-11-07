package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class IglaHudOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_igla_9k38_hud";

    private static final ResourceLocation FRAME = Mod.loc("textures/overlay/frame/frame_diamond.png");
    private static final ResourceLocation PART_1 = Mod.loc("textures/overlay/igla_9k38/part_1.png");
    private static final ResourceLocation PART_2 = Mod.loc("textures/overlay/igla_9k38/part_2.png");
    private static final ResourceLocation PART_3 = Mod.loc("textures/overlay/igla_9k38/part_3.png");
    private static final ResourceLocation PART_4 = Mod.loc("textures/overlay/igla_9k38/part_4.png");
    private static final ResourceLocation HOLD = Mod.loc("textures/overlay/igla_9k38/hold.png");
    private static final ResourceLocation SHOOT = Mod.loc("textures/overlay/igla_9k38/shoot.png");
    private static final ResourceLocation IGLA_SCOPE = Mod.loc("textures/overlay/igla_9k38/igla_scope.png");

    private static float scopeScale = 1;
    private static float lerpSeeking = 1;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = gui.getMinecraft().player;
        PoseStack poseStack = guiGraphics.pose();
        Camera camera = gui.getMinecraft().gameRenderer.getMainCamera();

        if (player == null) return;
        ItemStack stack = player.getMainHandItem();

        if (ClientEventHandler.isEditing)
            return;
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player))
            return;

        if ((stack.getItem() == ModItems.IGLA_9K38.get() && ClientEventHandler.zoomPos > 0.83) && Minecraft.getInstance().options.getCameraType().isFirstPerson() && ClientEventHandler.zoom) {
            var data = GunData.from(stack);

            poseStack.pushPose();

            float deltaFrame = Minecraft.getInstance().getDeltaFrameTime();
            float moveX = (float) (-32 * ClientEventHandler.turnRot[1] - (player.isSprinting() ? 100 : 67) * ClientEventHandler.movePosX + 3 * ClientEventHandler.cameraRot[2]);
            float moveY = (float) (-32 * ClientEventHandler.turnRot[0] + 100 * (float) ClientEventHandler.velocityY - (player.isSprinting() ? 100 : 67) * ClientEventHandler.movePosY - 12 * ClientEventHandler.firePos + 3 * ClientEventHandler.cameraRot[1]);
            scopeScale = (float) Mth.lerp(0.5F * deltaFrame, scopeScale, 1.35F + (0.2f * ClientEventHandler.firePos));
            float f = (float) Math.min(screenWidth, screenHeight);
            float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * scopeScale;
            float i = Mth.floor(f * f1);
            float j = Mth.floor(f * f1);
            Vec3 pCross = VectorUtil.worldToScreen(camera.getPosition().add(new Vec3(camera.getLookVector())));
            float x0 = (float) pCross.x + 4 * moveX;
            float y0 = (float) pCross.y + 4 * moveY;

            BlockPos blockPos = player.blockPosition();
            int combinedLightLevel = player.level().getMaxLocalRawBrightness(blockPos);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor((float) combinedLightLevel / 15, (float) combinedLightLevel / 15, (float) combinedLightLevel / 15, 1);

            preciseBlit(guiGraphics, IGLA_SCOPE, x0 - 1.5f * i, y0 - 1.5f * j, 0, 0, 3 * i, 3 * j, 3 * i, 3 * j);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            Entity targetEntity = ClientEventHandler.lockOn ? ClientEventHandler.lockingEntity : ClientEventHandler.seekingEntity;
            int seekingTime = ClientEventHandler.seekingTime;
            lerpSeeking = Mth.lerp(partialTick, lerpSeeking, Mth.clamp(data.compute().seekTime - seekingTime, 0, data.compute().seekTime) * 0.6f);

            if (targetEntity != null) {
                Vec3 pos = VectorTool.lerpGetEntityBoundingBoxCenter(targetEntity, partialTick);
                Vec3 point = VectorUtil.worldToScreen(pos);
                float x = (float) point.x;
                float y = (float) point.y;
                poseStack.pushPose();

                poseStack.translate(x, y, 0);
                //我去这框
                RenderHelper.blit(poseStack, FRAME, -12, -12, 0, 0, 24, 24, 24, 24, 1f);

                //锁定进度
                RenderHelper.blit(poseStack, PART_1, -12 - lerpSeeking, -12 - lerpSeeking, 0, 0, 24, 24, 24, 24, 1f);
                RenderHelper.blit(poseStack, PART_2, -12 + lerpSeeking, -12 - lerpSeeking, 0, 0, 24, 24, 24, 24, 1f);
                RenderHelper.blit(poseStack, PART_3, -12 - lerpSeeking, -12 + lerpSeeking, 0, 0, 24, 24, 24, 24, 1f);
                RenderHelper.blit(poseStack, PART_4, -12 + lerpSeeking, -12 + lerpSeeking, 0, 0, 24, 24, 24, 24, 1f);

                //状态
                if (seekingTime >= data.compute().seekTime && data.ammo.get() > 0) {
                    RenderHelper.blit(poseStack, SHOOT, -12, -26, 0, 0, 24, 24, 24, 24, 1f);
                } else {
                    RenderHelper.blit(poseStack, HOLD, -12, -26, 0, 0, 24, 24, 24, 24, 1f);
                }

                //测距
                poseStack.pushPose();
                String range = FormatTool.format0D(player.distanceTo(targetEntity));
                int width = Minecraft.getInstance().font.width(range);
                poseStack.scale(0.8f, 0.8f, 1);
                poseStack.translate(0.1f, 0, 0);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(range), (int) (-(float) width / 2), 14, 0xFFD6B6, false);
                poseStack.popPose();

                poseStack.popPose();
            }
            poseStack.popPose();
        } else {
            scopeScale = 1;
        }
    }
}
