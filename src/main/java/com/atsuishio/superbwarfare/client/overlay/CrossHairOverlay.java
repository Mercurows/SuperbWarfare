package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Mod.MODID, value = Dist.CLIENT)
public class CrossHairOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("cross_hair");

    private static final ResourceLocation REX_HORIZONTAL = Mod.loc("textures/screens/rex_horizontal.png");
    private static final ResourceLocation REX_VERTICAL = Mod.loc("textures/screens/rex_vertical.png");

    public static int HIT_INDICATOR = 0;
    public static int HEAD_INDICATOR = 0;
    public static int KILL_INDICATOR = 0;
    public static int VEHICLE_INDICATOR = 0;
    private static float scopeScale = 1f;
    public static float gunRot;

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (ClickHandler.isEditing)
            return;
        if (!player.getMainHandItem().is(ModTags.Items.GUN) || (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player)))
            return;

        ItemStack stack = player.getMainHandItem();
        double spread = ClientEventHandler.gunSpread + 1 * ClientEventHandler.firePos;
        float deltaFrame = deltaTracker.getGameTimeDeltaPartialTick(true);
        float moveX = 0;
        float moveY = 0;

        var data = GunData.from(stack);
        var perk = data.perk.get(Perk.Type.AMMO);

        if (DisplayConfig.FLOAT_CROSS_HAIR.get() && player.getVehicle() == null) {
            moveX = (float) (-6 * ClientEventHandler.turnRot[1] - (player.isSprinting() ? 10 : 6) * ClientEventHandler.movePosX);
            moveY = (float) (-6 * ClientEventHandler.turnRot[0] + 6 * (float) ClientEventHandler.velocityY - (player.isSprinting() ? 10 : 6) * ClientEventHandler.movePosY - 0.25 * ClientEventHandler.firePos);
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        scopeScale = (float) Mth.lerp(0.5F * deltaFrame, scopeScale, 1 + 1.5f * spread);
        float minLength = (float) Math.min(w, h);
        float scaledMinLength = Math.min((float) w / minLength, (float) h / minLength) * 0.012f * scopeScale;
        float finLength = Mth.floor(minLength * scaledMinLength);
        float finPosX = ((w - finLength) / 2) + moveX;
        float finPosY = ((h - finLength) / 2) + moveY;

        if (shouldRenderCrossHair(player) || (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON && (stack.is(ModItems.MINIGUN.get()) || stack.is(ModItems.AURELIA_SCEPTRE.get()))) || (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK && (ClientEventHandler.zoomTime > 0 || ClientEventHandler.bowPullPos > 0))) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/point.png"), w / 2f - 7.5f + moveX, h / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);
            if (!player.isSprinting() || ClientEventHandler.cantSprint > 0) {
                if (data.projectileAmount() > 1) {
                    shotgunCrossHair(guiGraphics, finPosX, finPosY, finLength);
                } else {
                    normalCrossHair(guiGraphics, w, h, spread, moveX, moveY);
                }
            }
        }

        if (stack.is(ModItems.BOCEK.get())) {
            if (ClientEventHandler.zoomPos < 0.7) {
                preciseBlit(guiGraphics, Mod.loc("textures/screens/point.png"), w / 2f - 7.5f + moveX, h / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);
                if (!player.isSprinting() || ClientEventHandler.cantSprint > 0 || ClientEventHandler.bowPullPos > 0) {
                    if (ClientEventHandler.zoomTime < 0.1) {
                        if (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug) {
                            normalCrossHair(guiGraphics, w, h, spread, moveX, moveY);
                        } else {
                            shotgunCrossHair(guiGraphics, finPosX, finPosY, finLength);
                        }
                    } else {
                        normalCrossHair(guiGraphics, w, h, spread, moveX, moveY);
                    }
                }
            }
        }

        // 在开启伤害指示器时才进行渲染
        if (DisplayConfig.KILL_INDICATION.get() && !(player.getVehicle() instanceof Ah6Entity ah6Entity && ah6Entity.getFirstPassenger() == player)) {
            renderKillIndicator(guiGraphics, w, h, moveX, moveY);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void normalCrossHair(GuiGraphics guiGraphics, int w, int h, double spread, float moveX, float moveY) {
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.rotateAround(Axis.ZP.rotationDegrees(-gunRot * Mth.RAD_TO_DEG), w / 2f + moveX, h / 2f + moveY, 0);
        preciseBlit(guiGraphics, REX_HORIZONTAL, (float) (w / 2f - 13.5f - 2.8f * spread) + moveX, h / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);
        preciseBlit(guiGraphics, REX_HORIZONTAL, (float) (w / 2f - 2.5f + 2.8f * spread) + moveX, h / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);
        preciseBlit(guiGraphics, REX_VERTICAL, w / 2f - 7.5f + moveX, (float) (h / 2f - 2.5f + 2.8f * spread) + moveY, 0, 0, 16, 16, 16, 16);
        preciseBlit(guiGraphics, REX_VERTICAL, w / 2f - 7.5f + moveX, (float) (h / 2f - 13.5f - 2.8f * spread) + moveY, 0, 0, 16, 16, 16, 16);
        poseStack.popPose();
    }

    private static void shotgunCrossHair(GuiGraphics guiGraphics, float finPosX, float finPosY, float finLength) {
        preciseBlit(guiGraphics, Mod.loc("textures/screens/shotgun_hud.png"), finPosX, finPosY, 0, 0.0F, finLength, finLength, finLength, finLength);
    }

    private static boolean shouldRenderCrossHair(Player player) {
        if (player == null) return false;

        if (player.isSpectator()) return false;
        if (!player.getMainHandItem().is(ModTags.Items.GUN) || ClientEventHandler.zoomTime > 0.8)
            return false;

        return !(player.getMainHandItem().getItem() == ModItems.M_79.get() || player.getMainHandItem().getItem() == ModItems.BOCEK.get() || player.getMainHandItem().getItem() == ModItems.SECONDARY_CATACLYSM.get())
                && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
    }

    private static void renderKillIndicator(GuiGraphics guiGraphics, int w, int h, float moveX, float moveY) {
        float posX = w / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
        float posY = h / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
        float rate = (40 - KILL_INDICATOR * 5) / 5.5f;

        if (HIT_INDICATOR > 0) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/hit_marker.png"), posX + moveX, posY + moveY, 0, 0, 16, 16, 16, 16);
        }

        if (VEHICLE_INDICATOR > 0) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/hit_marker_vehicle.png"), posX + moveX, posY + moveY, 0, 0, 16, 16, 16, 16);
        }

        if (HEAD_INDICATOR > 0) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/headshot_mark.png"), posX + moveX, posY + moveY, 0, 0, 16, 16, 16, 16);
        }

        if (KILL_INDICATOR > 0) {
            float posX1 = w / 2f - 7.5f - 2 + rate + moveX;
            float posY1 = h / 2f - 7.5f - 2 + rate + moveY;
            float posX2 = w / 2f - 7.5f + 2 - rate + moveX;
            float posY2 = h / 2f - 7.5f + 2 - rate + moveY;

            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark1.png"), posX1, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark2.png"), posX2, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark3.png"), posX1, posY2, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark4.png"), posX2, posY2, 0, 0, 16, 16, 16, 16);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        HEAD_INDICATOR = Math.max(0, HEAD_INDICATOR - 1);
        HIT_INDICATOR = Math.max(0, HIT_INDICATOR - 1);
        KILL_INDICATOR = Math.max(0, KILL_INDICATOR - 1);
        VEHICLE_INDICATOR = Math.max(0, VEHICLE_INDICATOR - 1);
    }
}
