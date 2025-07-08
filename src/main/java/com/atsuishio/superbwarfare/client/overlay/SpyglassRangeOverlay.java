package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class SpyglassRangeOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_spyglass_range";

    private static float scopeScale = 1;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        PoseStack poseStack = guiGraphics.pose();
        Player player = gui.getMinecraft().player;
        if (player == null) return;

        if (((player.isUsingItem() && player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get())) || player.isScoping()) && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            if (player.getMainHandItem().getItem() == ModItems.ARTILLERY_INDICATOR.get()) {
                poseStack.pushPose();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderSystem.setShaderColor(1, 1, 1, 1);

                float deltaFrame = Minecraft.getInstance().getDeltaFrameTime();
                scopeScale = (float) Mth.lerp(0.5F * deltaFrame, scopeScale, 1.35F + (0.2f * ClientEventHandler.firePos));
                float f = (float) Math.min(screenWidth, screenHeight);
                float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * scopeScale;
                float i = Mth.floor(f * f1);
                float j = Mth.floor(f * f1);
                float k = ((screenWidth - i) / 2);
                float l = ((screenHeight - j) / 2);
                float w = i * 21 / 9;
                preciseBlit(guiGraphics, Mod.loc("textures/screens/spyglass.png"), k - (2 * w / 7) , l, 0, 0.0F, w, j, w, j);
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
