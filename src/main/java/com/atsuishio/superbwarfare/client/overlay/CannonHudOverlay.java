package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Math;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicator;
import static com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicator3P;

@EventBusSubscriber(value = Dist.CLIENT)
public class CannonHudOverlay {

    @SubscribeEvent
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        PoseStack poseStack = guiGraphics.pose();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        if (!shouldRenderCrossHair(player)) return;

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof CannonEntity cannonEntity && cannonEntity instanceof VehicleEntity cannon) {
            poseStack.pushPose();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            preciseBlit(event.getGuiGraphics(), Mod.loc("textures/screens/compass_white.png"), (float) w / 2 - 128, (float) 10, 128 + ((float) 64 / 45 * (Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), cannon.yRotO, cannon.getYRot()))), 0, 256, 16, 512, 16);
            preciseBlit(event.getGuiGraphics(), Mod.loc("textures/screens/roll_ind_white.png"), w / 2F - 4, 27, 0, 0.0F, 8, 8, 8, 8);

            String angle = FormatTool.DECIMAL_FORMAT_1ZZ.format(Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), cannon.yRotO, cannon.getYRot()));
            int width = Minecraft.getInstance().font.width(angle);
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.literal(angle), w / 2 - width / 2, 40, -1, false);

            preciseBlit(event.getGuiGraphics(), Mod.loc("textures/screens/cannon/cannon_pitch.png"), w / 2F + 166, h / 2F - 64, 0, 0.0F, 8, 128, 8, 128);

            String pitch = FormatTool.DECIMAL_FORMAT_1ZZ.format(-Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), cannon.xRotO, cannon.getXRot()));
            int widthP = Minecraft.getInstance().font.width(pitch);

            poseStack.pushPose();

            event.getGuiGraphics().pose().translate(0, Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), cannon.xRotO, cannon.getXRot()) * 0.7, 0);
            preciseBlit(event.getGuiGraphics(), Mod.loc("textures/screens/cannon/cannon_pitch_ind.png"), w / 2F + 158, h / 2F - 4, 0, 0.0F, 8, 8, 8, 8);
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.literal(pitch), w / 2 + 157 - widthP, h / 2 - 4, -1, false);
            poseStack.popPose();

            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
                float fovAdjust = (float) 70 / Minecraft.getInstance().options.fov().get();

                float f = (float) Math.min(w, h);
                float f1 = Math.min((float) w / f, (float) h / f) * fovAdjust;
                int i = Mth.floor(f * f1);
                int j = Mth.floor(f * f1);
                int k = (w - i) / 2;
                int l = (h - j) / 2;
                if (ClientEventHandler.zoomVehicle) {
                    Entity lookingEntity = TraceTool.findLookingEntity(player, 512);
                    boolean lookAtEntity = false;

                    BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1).scale(512)),
                            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                    Vec3 hitPos = result.getLocation();

                    double blockRange = player.getEyePosition(1).distanceTo(hitPos);

                    double entityRange = 0;
                    if (lookingEntity instanceof LivingEntity living) {
                        lookAtEntity = true;
                        entityRange = player.distanceTo(living);
                    }
                    if (lookAtEntity) {
                        var component = Component.translatable("tips.superbwarfare.drone.range").append(Component.literal(FormatTool.format1D(entityRange, "m ")));
                        if (lookingEntity.getDisplayName() != null) {
                            component.append(lookingEntity.getDisplayName());
                        }
                        event.getGuiGraphics().drawString(Minecraft.getInstance().font, component, w / 2 + 14, h / 2 - 20, -1, false);
                    } else {
                        if (blockRange > 511) {
                            event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                                    .append(Component.literal("---m")), w / 2 + 14, h / 2 - 20, -1, false);
                        } else {
                            event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                                            .append(Component.literal(FormatTool.format1D(blockRange, "m"))),
                                    w / 2 + 14, h / 2 - 20, -1, false);
                        }
                    }
                    if (cannon instanceof AnnihilatorEntity) {
                        preciseBlit(event.getGuiGraphics(), Mod.loc("textures/screens/cannon/laser_cannon_crosshair.png"), k, l, 0, 0.0F, i, j, i, j);
                    } else {
                        preciseBlit(event.getGuiGraphics(), Mod.loc("textures/screens/cannon/cannon_crosshair.png"), k, l, 0, 0.0F, i, j, i, j);
                    }
                    float diffY = -Mth.wrapDegrees(Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), player.yHeadRotO, player.getYHeadRot()) - Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), cannon.yRotO, cannon.getYRot()));

                    preciseBlit(event.getGuiGraphics(), Mod.loc("textures/screens/cannon/indicator.png"), w / 2F - 4.3f + 0.45f * diffY, h / 2F - 10, 0, 0.0F, 8, 8, 8, 8);
                } else {
                    preciseBlit(event.getGuiGraphics(), Mod.loc("textures/screens/cannon/cannon_crosshair_notzoom.png"), k, l, 0, 0.0F, i, j, i, j);
                }
                renderKillIndicator(guiGraphics, w, h);
            } else if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK && !ClientEventHandler.zoomVehicle) {
                Vec3 p = RenderHelper.worldToScreen(new Vec3(Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), player.xo, player.getX()), Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), player.yo, player.getY()),
                        Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), player.zo, player.getZ())).add(cannon.getViewVector(event.getPartialTick().getGameTimeDeltaPartialTick(true)).scale(128)), cameraPos);

                // 第三人称准星
                if (p != null) {
                    poseStack.pushPose();
                    float x = (float) p.x;
                    float y = (float) p.y;

                    poseStack.pushPose();
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/drone.png"), x - 12, y - 12, 0, 0, 24, 24, 24, 24);
                    renderKillIndicator3P(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                    poseStack.pushPose();

                    poseStack.translate(x, y, 0);
                    poseStack.scale(0.75f, 0.75f, 1);

                    if (player.getVehicle() instanceof Mk42Entity || player.getVehicle() instanceof Mle1934Entity) {
                        if (cannonEntity.getWeaponIndex(0) == 0) {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("AP SHELL " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : cannonEntity.getAmmoCount(player))), 30, -9, -1, false);
                        } else {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("HE SHELL " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : cannonEntity.getAmmoCount(player))), 30, -9, -1, false);
                        }
                    }

                    // 歼灭者
                    if (player.getVehicle() instanceof AnnihilatorEntity annihilatorEntity) {
                        guiGraphics.drawString(mc.font, Component.literal("LASER " + (FormatTool.format0D((double) (100 * annihilatorEntity.getEnergy()) / annihilatorEntity.getMaxEnergy()) + "％")), 30, -9, -1, false);
                    }

                    double heal = 1 - cannon.getHealth() / cannon.getMaxHealth();

                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("HP " +
                            FormatTool.format0D(100 * cannon.getHealth() / cannon.getMaxHealth())), 30, 1, Mth.hsvToRgb(0F, (float) heal, 1.0F), false);

                    poseStack.popPose();
                    poseStack.popPose();
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
    }

    private static boolean shouldRenderCrossHair(Player player) {
        if (player == null) return false;
        return !player.isSpectator()
                && (player.getVehicle() != null && (player.getVehicle() instanceof CannonEntity));
    }
}
