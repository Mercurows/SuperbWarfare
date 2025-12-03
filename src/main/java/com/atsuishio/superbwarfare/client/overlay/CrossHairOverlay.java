package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.compat.realcamera.RealCameraCompatHolder;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.resource.gun.GunResource;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class CrossHairOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_cross_hair";

    public static final String CROSSHAIR_EMPTY = "@Empty";
    public static final String CROSSHAIR_CUSTOM = "@Custom";
    public static final String CROSSHAIR_GUN_DEFAULT = "@GunDefault";
    public static final String CROSSHAIR_GUN_REPAIR_TOOL = "@GunRepairTool";
    public static final String CROSSHAIR_GUN_BOCEK = "@GunBocek";
    public static final String CROSSHAIR_GUN_GRENADE = "@GunGrenade";

    private static final ResourceLocation REX = Mod.loc("textures/overlay/crosshair/rex.png");
    private static final ResourceLocation REX_HORIZONTAL = Mod.loc("textures/overlay/crosshair/rex_horizontal.png");
    private static final ResourceLocation REX_VERTICAL = Mod.loc("textures/overlay/crosshair/rex_vertical.png");
    private static final ResourceLocation POINT = Mod.loc("textures/overlay/crosshair/point.png");
    private static final ResourceLocation SHOTGUN = Mod.loc("textures/overlay/crosshair/rex_circle.png");
    private static final ResourceLocation HIT_MARKER = Mod.loc("textures/overlay/crosshair/hit_marker.png");
    private static final ResourceLocation HIT_MARKER_VEHICLE = Mod.loc("textures/overlay/crosshair/hit_marker_vehicle.png");
    private static final ResourceLocation HEADSHOT_MARKER = Mod.loc("textures/overlay/crosshair/headshot_marker.png");
    private static final ResourceLocation KILL_MARKER_1 = Mod.loc("textures/overlay/crosshair/kill_marker_1.png");
    private static final ResourceLocation KILL_MARKER_2 = Mod.loc("textures/overlay/crosshair/kill_marker_2.png");
    private static final ResourceLocation KILL_MARKER_3 = Mod.loc("textures/overlay/crosshair/kill_marker_3.png");
    private static final ResourceLocation KILL_MARKER_4 = Mod.loc("textures/overlay/crosshair/kill_marker_4.png");

    public static int hitIndicator = 0;
    public static int headIndicator = 0;
    public static int killIndicator = 0;
    public static int vehicleIndicator = 0;
    private static float scopeScale = 1f;
    public static float gunRot;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = gui.getMinecraft().player;
        if (player == null) return;
        if (player.isSpectator()) return;

        if (ClientEventHandler.isEditing) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem) || (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player)))
            return;

        var data = GunData.from(stack);

        var crosshair = data.compute().crosshair;
        if (crosshair.equals(CROSSHAIR_EMPTY) || crosshair.equals(CROSSHAIR_CUSTOM)) return;

        double spread = ClientEventHandler.gunSpread + 1 * ClientEventHandler.firePos;
        float deltaFrame = Minecraft.getInstance().getDeltaFrameTime();
        float moveX = 0;
        float moveY = 0;

        // 平滑准星
        if (DisplayConfig.FLOAT_CROSS_HAIR.get() && player.getVehicle() == null) {
            moveX = (float) (-6 * ClientEventHandler.turnRot[1] - (player.isSprinting() ? 10 : 6) * ClientEventHandler.movePosX);
            moveY = (float) (-6 * ClientEventHandler.turnRot[0] + 6 * (float) ClientEventHandler.velocityY - (player.isSprinting() ? 10 : 6) * ClientEventHandler.movePosY - 0.25 * ClientEventHandler.firePos);
            // 判断RC是否加载，用于适配动态准星
            if (RealCameraCompatHolder.hasMod()) {
                moveX = RealCameraCompatHolder.getCompatMoveX(moveX);
                moveY = RealCameraCompatHolder.getCompatMoveY(moveY);
            }
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        scopeScale = (float) Mth.lerp(0.5F * deltaFrame, scopeScale, 1 + 1.5f * spread);

        float minLength = (float) Math.min(screenWidth, screenHeight);
        float scaledMinLength = Math.min((float) screenWidth / minLength, (float) screenHeight / minLength) * 0.012f * scopeScale;
        float finLength = Mth.floor(minLength * scaledMinLength);
        float finPosX = (screenWidth - finLength) / 2 + moveX;
        float finPosY = (screenHeight - finLength) / 2 + moveY;

        // 第一人称下的准星
        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            switch (crosshair) {
                case CROSSHAIR_GUN_DEFAULT ->
                        renderGunDefaultCrosshair(guiGraphics, stack, player, screenWidth, screenHeight, moveX, moveY, finPosX, finPosY, finLength, spread);
                case CROSSHAIR_GUN_REPAIR_TOOL ->
                        renderRepairToolCrosshair(guiGraphics, data, player, screenWidth, screenHeight, moveX, moveY);
                case CROSSHAIR_GUN_BOCEK ->
                        renderBocekCrosshair(guiGraphics, data, player, screenWidth, screenHeight, moveX, moveY, finPosX, finPosY, finLength, spread);
                case CROSSHAIR_GUN_GRENADE -> renderGrenadeCrosshair(guiGraphics, stack, screenWidth, screenHeight);
            }
        }

        // 第三人称下的准星
        if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK && (ClientEventHandler.zoomTime > 0 || ClientEventHandler.bowPullPos > 0)) {
            renderGunDefaultCrosshair(guiGraphics, stack, player, screenWidth, screenHeight, moveX, moveY, finPosX, finPosY, finLength, spread);
        }

        // 在开启伤害指示器时才进行渲染
        if (DisplayConfig.KILL_INDICATION.get() && !(player.getVehicle() instanceof Ah6Entity ah6Entity && ah6Entity.getFirstPassenger() == player)) {
            renderKillIndicatorDynamic(guiGraphics, screenWidth, screenHeight, moveX, moveY);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /**
     * 渲染标准十字准星
     */
    public static void normalCrossHair(GuiGraphics guiGraphics, int w, int h, double spread, float moveX, float moveY) {
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.rotateAround(Axis.ZP.rotationDegrees(-gunRot * Mth.RAD_TO_DEG), w / 2f + moveX, h / 2f + moveY, 0);

        preciseBlit(guiGraphics, REX_HORIZONTAL, (float) (w / 2f - 13.5f - 2.8f * spread) + moveX, h / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);
        preciseBlit(guiGraphics, REX_HORIZONTAL, (float) (w / 2f - 2.5f + 2.8f * spread) + moveX, h / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);
        preciseBlit(guiGraphics, REX_VERTICAL, w / 2f - 7.5f + moveX, (float) (h / 2f - 2.5f + 2.8f * spread) + moveY, 0, 0, 16, 16, 16, 16);
        preciseBlit(guiGraphics, REX_VERTICAL, w / 2f - 7.5f + moveX, (float) (h / 2f - 13.5f - 2.8f * spread) + moveY, 0, 0, 16, 16, 16, 16);

        poseStack.popPose();
    }

    /**
     * 渲染圆形准星
     */
    public static void shotgunCrossHair(GuiGraphics guiGraphics, float finPosX, float finPosY, float finLength) {
        preciseBlit(guiGraphics, SHOTGUN, finPosX, finPosY, 0, 0, finLength, finLength, finLength, finLength);
    }

    public static void renderGunDefaultCrosshair(GuiGraphics guiGraphics, ItemStack stack, Player player, int screenWidth, int screenHeight,
                                                 float moveX, float moveY, float finPosX, float finPosY, float finLength, double spread) {
        GunData data = GunData.from(stack);

        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            if (ClientEventHandler.zoomTime > 0.8 && GunResource.compute(stack).hideCrosshairWhenZoom) return;
        }

        preciseBlit(guiGraphics, POINT, screenWidth / 2f - 7.5f + moveX, screenHeight / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);
        if (!player.isSprinting() || ClientEventHandler.noSprintTicks > 0) {
            if (data.compute().projectileAmount > 1) {
                shotgunCrossHair(guiGraphics, finPosX, finPosY, finLength);
            } else {
                normalCrossHair(guiGraphics, screenWidth, screenHeight, spread, moveX, moveY);
            }
        }
    }

    public static void renderRepairToolCrosshair(GuiGraphics guiGraphics, GunData data, Player player, int screenWidth, int screenHeight, float moveX, float moveY) {
        int range = data.compute().range;
        Entity lookingEntity = TraceTool.findLookingEntity(player, range);

        float health = 0;
        if (lookingEntity instanceof LivingEntity living) {
            health = living.getHealth() / living.getMaxHealth();
        } else if (lookingEntity instanceof VehicleEntity vehicle) {
            health = vehicle.getHealth() / vehicle.getMaxHealth();
        }

        preciseBlit(guiGraphics, POINT, screenWidth / 2f - 7.5f + moveX, screenHeight / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);

        if (health > 0) {
            RenderHelper.renderCircularRing(guiGraphics,
                    screenWidth / 2f + moveX, screenHeight / 2f + moveY,
                    0.035f, 0.028f,
                    new float[]{0f, 0f, 0f, 0.4f}, new float[]{1f, 1f, 1f, 1f},
                    health, true);
        }
    }

    public static void renderBocekCrosshair(GuiGraphics guiGraphics, GunData data, Player player, int screenWidth, int screenHeight,
                                            float moveX, float moveY, float finPosX, float finPosY, float finLength, double spread) {
        if (ClientEventHandler.zoomPos >= 0.7) return;

        var perk = data.perk.get(Perk.Type.AMMO);

        preciseBlit(guiGraphics, POINT, screenWidth / 2f - 7.5f + moveX, screenHeight / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);
        if (!player.isSprinting() || ClientEventHandler.noSprintTicks > 0 || ClientEventHandler.bowPullPos > 0) {
            if (ClientEventHandler.zoomTime < 0.1) {
                if (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug) {
                    normalCrossHair(guiGraphics, screenWidth, screenHeight, spread, moveX, moveY);
                } else {
                    shotgunCrossHair(guiGraphics, finPosX, finPosY, finLength);
                }
            } else {
                normalCrossHair(guiGraphics, screenWidth, screenHeight, spread, moveX, moveY);
            }
        }
    }

    public static void renderGrenadeCrosshair(GuiGraphics guiGraphics, ItemStack stack, int screenWidth, int screenHeight) {
        if (ClientEventHandler.zoomTime > 0.8 && GunResource.compute(stack).hideCrosshairWhenZoom) return;

        guiGraphics.blit(REX, screenWidth / 2 - 16, screenHeight / 2 - 16, 0, 0, 32, 32, 32, 32);
    }

    private static void renderKillIndicatorDynamic(GuiGraphics guiGraphics, int w, int h, float moveX, float moveY) {
        float posX = w / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
        float posY = h / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
        float rate = (40 - killIndicator * 5) / 5.5f;

        if (hitIndicator > 0) {
            preciseBlit(guiGraphics, HIT_MARKER, posX + moveX, posY + moveY, 0, 0, 16, 16, 16, 16);
        }

        if (vehicleIndicator > 0) {
            preciseBlit(guiGraphics, HIT_MARKER_VEHICLE, posX + moveX, posY + moveY, 0, 0, 16, 16, 16, 16);
        }

        if (headIndicator > 0) {
            preciseBlit(guiGraphics, HEADSHOT_MARKER, posX + moveX, posY + moveY, 0, 0, 16, 16, 16, 16);
        }

        if (killIndicator > 0) {
            float posX1 = w / 2f - 7.5f - 2 + rate + moveX;
            float posY1 = h / 2f - 7.5f - 2 + rate + moveY;
            float posX2 = w / 2f - 7.5f + 2 - rate + moveX;
            float posY2 = h / 2f - 7.5f + 2 - rate + moveY;

            preciseBlit(guiGraphics, KILL_MARKER_1, posX1, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, KILL_MARKER_2, posX2, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, KILL_MARKER_3, posX1, posY2, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, KILL_MARKER_4, posX2, posY2, 0, 0, 16, 16, 16, 16);
        }
    }

    public static void handleRenderDamageIndicator() {
        headIndicator = Math.max(0, headIndicator - 1);
        hitIndicator = Math.max(0, hitIndicator - 1);
        killIndicator = Math.max(0, killIndicator - 1);
        vehicleIndicator = Math.max(0, vehicleIndicator - 1);
    }
}
