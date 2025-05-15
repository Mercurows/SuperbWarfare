package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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

    private static final ResourceLocation FRAME = Mod.loc("textures/screens/frame/frame.png");
    private static final ResourceLocation FRAME_TARGET = Mod.loc("textures/screens/frame/frame_target.png");
    private static final ResourceLocation FRAME_LOCK = Mod.loc("textures/screens/frame/frame_lock.png");
    private static float scopeScale = 1;

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        Player player = Minecraft.getInstance().player;
        PoseStack poseStack = guiGraphics.pose();

        if (player != null) {
            ItemStack stack = player.getMainHandItem();

            if (ClickHandler.isEditing)
                return;
            if (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
                return;

            if ((stack.getItem() == ModItems.JAVELIN.get() && ClientEventHandler.zoomPos > 0.8) && Minecraft.getInstance().options.getCameraType().isFirstPerson() && ClientEventHandler.zoom) {
                var data = GunData.from(stack);
                var tag = data.tag();

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                float deltaFrame = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
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
                preciseBlit(guiGraphics, Mod.loc("textures/screens/javelin/javelin_hud.png"), k, l, 0, 0.0F, i, j, i, j);
                preciseBlit(guiGraphics, Mod.loc(tag.getBoolean("TopMode") ? "textures/screens/javelin/top.png" : "textures/screens/javelin/dir.png"), k, l, 0, 0.0F, i, j, i, j);
                preciseBlit(guiGraphics, Mod.loc(data.hasEnoughAmmoToShoot(player) ? "textures/screens/javelin/missile_green.png" : "textures/screens/javelin/missile_red.png"), k, l, 0, 0.0F, i, j, i, j);
                if (tag.getInt("SeekTime") > 1 && tag.getInt("SeekTime") < 20) {
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/javelin/seek.png"), k, l, 0, 0.0F, i, j, i, j);
                }

                guiGraphics.fill(RenderType.guiOverlay(), 0, (int) l, (int) k + 3, (int) j1, -90, -16777216);
                guiGraphics.fill(RenderType.guiOverlay(), (int) i1, (int) l, w, (int) j1, -90, -16777216);
                RenderSystem.depthMask(true);
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
                RenderSystem.setShaderColor(1, 1, 1, 1);

                float fovAdjust = (float) Minecraft.getInstance().options.fov().get() / 80;

                Entity targetEntity = EntityFindUtil.findEntity(player.level(), tag.getString("TargetEntity"));
                List<Entity> entities = SeekTool.seekLivingEntities(player, player.level(), 512, 8 * fovAdjust);
                Entity naerestEntity = SeekTool.seekLivingEntity(player, player.level(), 512, 6);

                float fovAdjust2 = (float) (Minecraft.getInstance().options.fov().get() / 30) - 1;

                double zoom = Minecraft.getInstance().options.fov().get() / ClientEventHandler.fov + 0.5 * fovAdjust2;

                Vec3 playerVec = new Vec3(Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), player.xo, player.getX()), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), player.zo, player.getZ()));

                if (tag.getInt("GuideType") == 0) {
                    for (var e : entities) {
                        Vec3 pos = new Vec3(Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), e.xo, e.getX()), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), e.yo + e.getEyeHeight(), e.getEyeY()), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), e.zo, e.getZ()));
                        Vec3 lookAngle = player.getLookAngle().normalize().scale(pos.distanceTo(playerVec) * (1 - 1.0 / zoom));

                        var cPos = playerVec.add(lookAngle);
                        Vec3 point = RenderHelper.worldToScreen(pos, cPos);
                        if (point == null) return;

                        boolean lockOn = tag.getInt("SeekTime") > 20 && e == targetEntity;
                        boolean nearest = e == naerestEntity;

                        poseStack.pushPose();
                        float x = (float) point.x;
                        float y = (float) point.y;

                        RenderHelper.preciseBlit(guiGraphics, lockOn ? FRAME_LOCK : nearest ? FRAME_TARGET : FRAME, x - 12, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                        poseStack.popPose();
                    }
                } else {
                    Vec3 pos = new Vec3(tag.getDouble("TargetPosX"), tag.getDouble("TargetPosY"), tag.getDouble("TargetPosZ"));
                    Vec3 lookAngle = player.getLookAngle().normalize().scale(pos.distanceTo(playerVec) * (1 - 1.0 / zoom));

                    boolean lockOn = tag.getInt("SeekTime") > 20;

                    var cPos = playerVec.add(lookAngle);
                    Vec3 point = RenderHelper.worldToScreen(pos, cPos);
                    if (point == null) return;

                    poseStack.pushPose();
                    float x = (float) point.x;
                    float y = (float) point.y;

                    RenderHelper.preciseBlit(guiGraphics, lockOn ? FRAME_LOCK : FRAME_TARGET, x - 12, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                    poseStack.popPose();
                }

            } else {
                scopeScale = 1;
            }
        }
    }
}
