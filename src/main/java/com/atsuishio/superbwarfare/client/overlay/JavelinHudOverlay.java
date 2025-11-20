package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class JavelinHudOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("javelin_hud");

    private static final ResourceLocation FRAME = Mod.loc("textures/overlay/frame/frame.png");
    private static final ResourceLocation FRAME_TARGET = Mod.loc("textures/overlay/frame/frame_target_triangle.png");
    private static final ResourceLocation FRAME_LOCK = Mod.loc("textures/overlay/frame/frame_lock.png");
    private static final ResourceLocation JAVELIN_HUD = Mod.loc("textures/overlay/javelin/javelin_hud.png");
    private static final ResourceLocation TOP = Mod.loc("textures/overlay/javelin/top.png");
    private static final ResourceLocation DIR = Mod.loc("textures/overlay/javelin/dir.png");
    private static final ResourceLocation MISSILE_GREEN = Mod.loc("textures/overlay/javelin/missile_green.png");
    private static final ResourceLocation MISSILE_RED = Mod.loc("textures/overlay/javelin/missile_red.png");
    private static final ResourceLocation SEEK = Mod.loc("textures/overlay/javelin/seek.png");

    private static float scopeScale = 1;

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        Player player = Minecraft.getInstance().player;
        PoseStack poseStack = guiGraphics.pose();

        if (player == null) return;
        ItemStack stack = player.getMainHandItem();

        if (ClientEventHandler.isEditing)
            return;
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player))
            return;
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        Entity decoy = TraceTool.findLookDecoy(player, cameraPos, player.getViewVector(partialTick), 512);

        if (decoy != null && decoy.getType().is(ModTags.EntityTypes.DECOY)) return;

        if ((stack.getItem() == ModItems.JAVELIN.get() && ClientEventHandler.zoomPos > 0.8) && Minecraft.getInstance().options.getCameraType().isFirstPerson() && ClientEventHandler.zoom) {
            var data = GunData.from(stack);
            var tag = data.tag();

            poseStack.pushPose();

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            float deltaFrame = deltaTracker.getGameTimeDeltaPartialTick(true);
            float moveX = (float) (-32 * ClientEventHandler.turnRot[1] - (player.isSprinting() ? 100 : 67) * ClientEventHandler.movePosX + 3 * ClientEventHandler.cameraRot[2]);
            float moveY = (float) (-32 * ClientEventHandler.turnRot[0] + 100 * (float) ClientEventHandler.velocityY - (player.isSprinting() ? 100 : 67) * ClientEventHandler.movePosY - 12 * ClientEventHandler.firePos + 3 * ClientEventHandler.cameraRot[1]);
            scopeScale = (float) Mth.lerp(0.5F * deltaFrame, scopeScale, 1.35F + (0.2f * ClientEventHandler.firePos));
            float f = (float) Math.min(w, h);
            float f1 = Math.min((float) w / f, (float) h / f) * scopeScale;
            float i = Mth.floor(f * f1);
            float j = Mth.floor(f * f1);
            float k = ((w - i) / 2) + moveX;
            float l = ((h - j) / 2) + moveY;
            float i1 = k + i;
            float j1 = l + j;
            preciseBlit(guiGraphics, JAVELIN_HUD, k, l, 0, 0, i, j, i, j);
            preciseBlit(guiGraphics, data.selectedFireModeInfo().name.equals("Top") ? TOP : DIR, k, l, 0, 0, i, j, i, j);
            preciseBlit(guiGraphics, data.hasEnoughAmmoToShoot(player) ? MISSILE_GREEN : MISSILE_RED, k, l, 0, 0, i, j, i, j);
            if (tag.getInt("SeekTime") > 1 && tag.getInt("SeekTime") < 20) {
                preciseBlit(guiGraphics, SEEK, k, l, 0, 0, i, j, i, j);
            }

            guiGraphics.fill(RenderType.guiOverlay(), 0, (int) l, (int) k + 3, (int) j1, -90, -16777216);
            guiGraphics.fill(RenderType.guiOverlay(), (int) i1, (int) l, w, (int) j1, -90, -16777216);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);

            Entity targetEntity = ClientEventHandler.lockingEntity;
            List<Entity> entities = new SeekTool.Builder(player)
                    .withinRange(data.compute().seekRange)
                    .withinAngle(data.compute().seekAngle)
                    .baseFilter()
                    .heightRange(data.compute().minTargetHeight, data.compute().maxTargetHeight)
                    .smokeFilter()
                    .noVehicle()
                    .noClip()
                    .notFriendly()
                    .build();
            Entity nearestEntity = ClientEventHandler.nearestEntity;

            if (ClientEventHandler.guideType == 0) {
                for (var e : entities) {
                    Vec3 pos = VectorTool.lerpGetEntityBoundingBoxCenter(e, deltaTracker.getGameTimeDeltaPartialTick(true));
                    Vec3 point = VectorUtil.worldToScreen(pos);
                    boolean lockOn = ClientEventHandler.lockOn && e == targetEntity;
                    boolean nearest = e == nearestEntity;

                    poseStack.pushPose();
                    float x = (float) point.x;
                    float y = (float) point.y;

                    RenderHelper.preciseBlit(guiGraphics, lockOn ? FRAME_LOCK : nearest ? FRAME_TARGET : FRAME, x - 12, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                }
            } else {
                Vec3 pos = ClientEventHandler.lockingPos;
                boolean lockOn = ClientEventHandler.lockOn;

                Vec3 point = VectorUtil.worldToScreen(pos);
                if (VectorUtil.canSee(pos)) {
                    poseStack.pushPose();
                    float x = (float) point.x;
                    float y = (float) point.y;

                    RenderHelper.preciseBlit(guiGraphics, lockOn ? FRAME_LOCK : FRAME_TARGET, x - 12, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        } else {
            scopeScale = 1;
        }
    }
}