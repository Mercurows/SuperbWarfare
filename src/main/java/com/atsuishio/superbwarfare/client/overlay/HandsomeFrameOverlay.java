package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class HandsomeFrameOverlay {

    private static final ResourceLocation FRAME = ModUtils.loc("textures/screens/frame/frame.png");
    private static final ResourceLocation FRAME_WEAK = ModUtils.loc("textures/screens/frame/frame_weak.png");
    private static final ResourceLocation FRAME_TARGET = ModUtils.loc("textures/screens/frame/frame_target.png");
    private static final ResourceLocation FRAME_LOCK = ModUtils.loc("textures/screens/frame/frame_lock.png");

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        Player player = Minecraft.getInstance().player;
        PoseStack poseStack = event.getGuiGraphics().pose();

        if (player != null) {
            ItemStack stack = player.getMainHandItem();
            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            if (cap == null || cap.edit) return;
            if (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
                return;

            if (stack.getItem() instanceof GunItem && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {


                int level = PerkHelper.getItemPerkLevel(ModPerks.INTELLIGENT_CHIP.get(), stack);
                if (level == 0) return;

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderSystem.setShaderColor(1, 1, 1, 1);

                List<Entity> allEntities = SeekTool.seekLivingEntitiesThroughWall(player, player.level(), 32 + 8 * (level - 1), 30);
                List<Entity> visibleEntities = SeekTool.seekLivingEntities(player, player.level(), 32 + 8 * (level - 1), 30);

                Entity naerestEntity = SeekTool.seekLivingEntity(player, player.level(), 32 + 8 * (level - 1), 30);
                Entity targetEntity = ClientEventHandler.entity;


                float fovAdjust2 = (float) (Minecraft.getInstance().options.fov().get() / 30) - 1;

                double zoom = 1;

                if (ClientEventHandler.zoom) {
                    zoom = Minecraft.getInstance().options.fov().get() / ClientEventHandler.fov + 0.05 * fovAdjust2;
                }

                for (var e : allEntities) {
                    Vec3 playerVec = new Vec3(Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), player.xo, player.getX()), Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), player.zo, player.getZ()));
                    Vec3 pos = new Vec3(Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), e.xo, e.getX()), Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), e.yo + e.getEyeHeight(), e.getEyeY()), Mth.lerp(event.getPartialTick().getGameTimeDeltaTicks(), e.zo, e.getZ()));
                    Vec3 lookAngle = player.getLookAngle().normalize().scale(pos.distanceTo(playerVec) * (1 - 1.0 / zoom));

                    var cPos = playerVec.add(lookAngle);
                    Vec3 point = RenderHelper.worldToScreen(pos, cPos);
                    if (point == null) return;

                    boolean lockOn = e == targetEntity;
                    boolean isNearestEntity = e == naerestEntity;

                    poseStack.pushPose();
                    float x = (float) point.x;
                    float y = (float) point.y;

                    var canBeSeen = visibleEntities.contains(e);

                    ResourceLocation icon;
                    if (lockOn) {
                        icon = FRAME_LOCK;
                    } else if (canBeSeen) {
                        if (isNearestEntity) {
                            icon = FRAME_TARGET;
                        } else {
                            icon = FRAME;
                        }
                    } else {
                        icon = FRAME_WEAK;
                    }

                    RenderHelper.preciseBlit(event.getGuiGraphics(), icon, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                    poseStack.popPose();
                }
            }
        }
    }
}
