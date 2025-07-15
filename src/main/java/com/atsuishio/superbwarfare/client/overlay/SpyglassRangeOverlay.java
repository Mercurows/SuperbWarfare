package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.item.ArtilleryIndicator.TAG_CANNON;

@OnlyIn(Dist.CLIENT)
public class SpyglassRangeOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("spyglass_range");
    public static final ResourceLocation INDICATOR = Mod.loc("textures/screens/indicator.png");
    public static final ResourceLocation FRIENDLY_INDICATOR = Mod.loc("textures/screens/friendly_indicator.png");
    private static float scopeScale = 1;

    private static float lerpHoldArtilleryIndicator;

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = guiGraphics.pose();
        Player player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();
        var screenWidth = guiGraphics.guiWidth();
        var screenHeight = guiGraphics.guiHeight();

        if (player == null) return;

        lerpHoldArtilleryIndicator = Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), lerpHoldArtilleryIndicator, ClientEventHandler.holdArtilleryIndicator);

        if (ClientEventHandler.holdArtilleryIndicator > 0) {
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), (float) screenWidth / 2 - 40, (float) (screenHeight / 2 + 64), (float) screenWidth / 2 + 40, (float) screenHeight / 2 + 68, -90, -16777216);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), (float) screenWidth / 2 - 40, (float) (screenHeight / 2 + 64), (float) screenWidth / 2 - 40 + 8 * lerpHoldArtilleryIndicator, (float) screenHeight / 2 + 68, -90, -1);
        }

        if (((player.isUsingItem() && player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get())) || player.isScoping()) && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            if (player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                ItemStack stack = player.getUseItem();
                poseStack.pushPose();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderSystem.setShaderColor(1, 1, 1, 1);

                float deltaFrame = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
                scopeScale = (float) Mth.lerp(0.5F * deltaFrame, scopeScale, 1.35F + (0.2f * ClientEventHandler.firePos));
                float f = (float) Math.min(screenWidth, screenHeight);
                float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * scopeScale;
                float i = Mth.floor(f * f1);
                float j = Mth.floor(f * f1);
                float k = ((screenWidth - i) / 2);
                float l = ((screenHeight - j) / 2);
                float w = i * 21 / 9;
                preciseBlit(guiGraphics, Mod.loc("textures/screens/spyglass.png"), k - (2 * w / 7), l, 0, 0.0F, w, j, w, j);

                // 标记位置
                Vec3 pos;
                var parameters = stack.get(ModDataComponents.FIRING_PARAMETERS);
                if (parameters != null) {
                    var blockPos = parameters.pos();
                    pos = new Vec3(blockPos.getX() - 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                } else {
                    pos = Vec3.ZERO;
                }
                Vec3 point = VectorUtil.worldToScreen(pos);
                float x = (float) point.x;
                float y = (float) point.y;
                preciseBlit(guiGraphics, INDICATOR, Mth.clamp(x - 6, 0, screenWidth - 12), Mth.clamp(y - 6, 0, screenHeight - 12), 0, 0, 12, 12, 12, 12);

                // 火炮位置

                ListTag tags = NBTTool.getTag(stack).getList(TAG_CANNON, Tag.TAG_COMPOUND);
                for (int m = 0; m < tags.size(); m++) {
                    var tag = tags.getCompound(m);
                    Entity entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"));
                    if (entity != null) {
                        Vec3 posF = entity.getBoundingBox().getCenter();
                        Vec3 pointF = VectorUtil.worldToScreen(posF);
                        float xf = (float) pointF.x;
                        float yf = (float) pointF.y;

                        preciseBlit(guiGraphics, FRIENDLY_INDICATOR, Mth.clamp(xf - 6, 0, screenWidth - 12), Mth.clamp(yf - 6, 0, screenHeight - 12), 0, 0, 12, 12, 12, 12);
                    }
                }

                poseStack.popPose();
            }

            boolean lookAtEntity = false;

            BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1).scale(512)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            Vec3 hitPos = result.getLocation();

            double blockRange = player.getEyePosition(1).distanceTo(hitPos);

            double entityRange = 0;
            Entity lookingEntity = TraceTool.findLookingEntity(player, 520);

            if (lookingEntity instanceof VehicleEntity) return;

            if (lookingEntity != null) {
                lookAtEntity = true;
                entityRange = player.distanceTo(lookingEntity);
            }

            if (lookAtEntity) {
                guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                                .append(Component.literal(FormatTool.format1D(entityRange, "M ") + lookingEntity.getDisplayName().getString())),
                        screenWidth / 2 + 12, screenHeight / 2 - 28, -1, false);
            } else {
                if (blockRange > 500) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                            .append(Component.literal("---M")), screenWidth / 2 + 12, screenHeight / 2 - 28, -1, false);
                } else {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                                    .append(Component.literal(FormatTool.format1D(blockRange, "M"))),
                            screenWidth / 2 + 12, screenHeight / 2 - 28, -1, false);
                }
            }
        } else {
            scopeScale = 1;
        }
    }
}
