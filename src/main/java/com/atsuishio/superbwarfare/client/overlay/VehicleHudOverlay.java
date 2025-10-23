package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.animation.AnimationCurves;
import com.atsuishio.superbwarfare.client.animation.AnimationTimer;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.LaserWeapon;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.MathTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Math;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicReference;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay.*;
import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.*;

@OnlyIn(Dist.CLIENT)
public class VehicleHudOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_vehicle_hud";
    public static final int ANIMATION_TIME = 300;

    private static final ResourceLocation ARMOR = Mod.loc("textures/overlay/vehicle/base/armor.png");
    private static final ResourceLocation ENERGY = Mod.loc("textures/overlay/vehicle/base/energy.png");
    private static final ResourceLocation VALUE_BAR = Mod.loc("textures/overlay/vehicle/base/value_bar.png");
    private static final ResourceLocation VALUE_FRAME = Mod.loc("textures/overlay/vehicle/base/value_frame.png");
    private static final ResourceLocation COMPASS = Mod.loc("textures/overlay/vehicle/base/compass.png");
    private static final ResourceLocation DRIVER = Mod.loc("textures/overlay/vehicle/base/driver.png");
    private static final ResourceLocation PASSENGER = Mod.loc("textures/overlay/vehicle/base/passenger.png");

    private static final ResourceLocation SELECTED = Mod.loc("textures/overlay/vehicle/weapon/frame/selected.png");
    private static final ResourceLocation NUMBER = Mod.loc("textures/overlay/vehicle/weapon/frame/number.png");

    private static final ResourceLocation[] FRAMES = {
            Mod.loc("textures/overlay/vehicle/weapon/frame/frame_1.png"),
            Mod.loc("textures/overlay/vehicle/weapon/frame/frame_2.png"),
            Mod.loc("textures/overlay/vehicle/weapon/frame/frame_3.png"),
            Mod.loc("textures/overlay/vehicle/weapon/frame/frame_4.png"),
            Mod.loc("textures/overlay/vehicle/weapon/frame/frame_5.png"),
            Mod.loc("textures/overlay/vehicle/weapon/frame/frame_6.png"),
            Mod.loc("textures/overlay/vehicle/weapon/frame/frame_7.png"),
            Mod.loc("textures/overlay/vehicle/weapon/frame/frame_8.png"),
            Mod.loc("textures/overlay/vehicle/weapon/frame/frame_9.png")
    };

    private static final ResourceLocation GEAR = Mod.loc("textures/screens/aircraft/gear.png");

    private static final ResourceLocation FRAME = Mod.loc("textures/overlay/vehicle/land/tv_frame.png");
    private static final ResourceLocation LINE = Mod.loc("textures/overlay/vehicle/land/line.png");
    private static final ResourceLocation ROLL_IND = Mod.loc("textures/screens/helicopter/roll_ind.png");

    // 地面载具车身显示
    private static final ResourceLocation BARREL = Mod.loc("textures/overlay/vehicle/land/line.png");
    private static final ResourceLocation BODY = Mod.loc("textures/overlay/vehicle/land/body.png");
    private static final ResourceLocation LEFT_WHEEL = Mod.loc("textures/overlay/vehicle/land/left_wheel.png");
    private static final ResourceLocation RIGHT_WHEEL = Mod.loc("textures/overlay/vehicle/land/right_wheel.png");
    private static final ResourceLocation ENGINE = Mod.loc("textures/overlay/vehicle/land/engine.png");

    private static final ResourceLocation HIT_MARKER = Mod.loc("textures/overlay/crosshair/hit_marker.png");
    private static final ResourceLocation HIT_MARKER_VEHICLE = Mod.loc("textures/overlay/crosshair/hit_marker_vehicle.png");
    private static final ResourceLocation HEADSHOT_MARKER = Mod.loc("textures/overlay/crosshair/headshot_marker.png");
    private static final ResourceLocation KILL_MARKER_1 = Mod.loc("textures/overlay/crosshair/kill_marker_1.png");
    private static final ResourceLocation KILL_MARKER_2 = Mod.loc("textures/overlay/crosshair/kill_marker_2.png");
    private static final ResourceLocation KILL_MARKER_3 = Mod.loc("textures/overlay/crosshair/kill_marker_3.png");
    private static final ResourceLocation KILL_MARKER_4 = Mod.loc("textures/overlay/crosshair/kill_marker_4.png");

    private static final AnimationTimer[] WEAPON_SLOTS_TIMER = AnimationTimer.createTimers(9, ANIMATION_TIME, AnimationCurves.EASE_OUT_CIRC);
    private static final AnimationTimer WEAPON_INDEX_UPDATE_TIMER = new AnimationTimer(ANIMATION_TIME).animation(AnimationCurves.EASE_OUT_CIRC);

    private static boolean wasRenderingWeapons = false;
    private static int oldWeaponIndex = 0;
    private static int oldRenderWeaponIndex = 0;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = gui.getMinecraft().player;

        if (!shouldRenderHud(player)) {
            wasRenderingWeapons = false;
            return;
        }

        Entity entity = player.getVehicle();
        if (!(entity instanceof VehicleEntity vehicle)) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 渲染地面武装HUD
        renderLandArmorHud(gui, guiGraphics, partialTick, screenWidth, screenHeight);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        int compatHeight = getArmorPlateCompatHeight(player);

        if (vehicle.hasEnergyStorage()) {
            float energy = vehicle.getEnergy();
            float maxEnergy = vehicle.getMaxEnergy();

            preciseBlit(guiGraphics, ENERGY, 10, screenHeight - 22 - compatHeight, 100, 0, 0, 8, 8, 8, 8);
            preciseBlit(guiGraphics, VALUE_FRAME, 20, screenHeight - 21 - compatHeight, 100, 0, 0, 60, 6, 60, 6);
            preciseBlit(guiGraphics, VALUE_BAR, 20, screenHeight - 21 - compatHeight, 100, 0, 0, (int) (60 * energy / maxEnergy), 6, 60, 6);
        }

        float health = vehicle.getHealth();
        float maxHealth = vehicle.getMaxHealth();

        preciseBlit(guiGraphics, ARMOR, 10, screenHeight - 13 - compatHeight, 100, 0, 0, 8, 8, 8, 8);
        preciseBlit(guiGraphics, VALUE_FRAME, 20, screenHeight - 12 - compatHeight, 100, 0, 0, 60, 6, 60, 6);
        preciseBlit(guiGraphics, VALUE_BAR, 20, screenHeight - 12 - compatHeight, 100, 0, 0, (int) (60 * health / maxHealth), 6, 60, 6);

        renderWeaponInfo(guiGraphics, vehicle, screenWidth, screenHeight);
        renderPassengerInfo(guiGraphics, vehicle, screenWidth, screenHeight);

        if (vehicle.getVehicleType() == VehicleType.AIRPLANE) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            float angle = vehicle.gearRot(partialTick);
            poseStack.pushPose();
            poseStack.rotateAround(Axis.ZP.rotationDegrees(-90 + angle), 102, screenHeight - 20, 0);
            preciseBlit(guiGraphics, GEAR, 86, screenHeight - 36, 0, 0, 32, 32, 32, 32);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static boolean shouldRenderHud(Player player) {
        if (player == null) return false;
        return !player.isSpectator() && player.getVehicle() instanceof VehicleEntity;
    }

    private static int getArmorPlateCompatHeight(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (stack == ItemStack.EMPTY) return 0;
        if (stack.getTag() == null || !stack.getTag().contains("ArmorPlate")) return 0;
        if (!DisplayConfig.ARMOR_PLATE_HUD.get()) return 0;
        return 9;
    }

    public static void renderLandArmorHud(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();
        var player = mc.player;
        PoseStack poseStack = guiGraphics.pose();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewVec = new Vec3(camera.getLookVector());

        assert player != null;

        if (player.getVehicle() instanceof VehicleEntity vehicle
                && vehicle.amphibiousVehicle()
                && vehicle instanceof WeaponVehicleEntity weaponVehicle
                && weaponVehicle.isDriver(player)
                && !(player.getVehicle() instanceof SpeedboatEntity)
                && !(player.getVehicle() instanceof CannonEntity)) {
            int color = vehicle.getHudColor();

            poseStack.pushPose();

            poseStack.translate(0, 0 - 0.3 * ClientEventHandler.shakeTime + 3 * ClientEventHandler.cameraRoll, 0);
            poseStack.rotateAround(Axis.ZP.rotationDegrees(-0.3f * ClientEventHandler.cameraRoll), screenWidth / 2f, screenHeight / 2f, 0);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
                int addW = (screenWidth / screenHeight) * 48;
                int addH = (screenWidth / screenHeight) * 27;
                preciseBlit(guiGraphics, FRAME, (float) -addW / 2, (float) -addH / 2, 10, 0, 0.0F, screenWidth + addW, screenHeight + addH, screenWidth + addW, screenHeight + addH);
                RenderHelper.blit(poseStack, LINE, screenWidth / 2f - 64, screenHeight - 56, 0, 0.0F, 128, 1, 128, 1, color);

                // 指南针
                RenderHelper.blit(poseStack, COMPASS, (float) screenWidth / 2 - 128, (float) 10, 128 + ((float) 64 / 45 * player.getYRot()), 0, 256, 16, 512, 16, color);
                RenderHelper.blit(poseStack, ROLL_IND, screenWidth / 2f - 8, 30, 0, 0.0F, 16, 16, 16, 16, color);

                int turretHeal = (int) (100 - (100 * vehicle.getEntityData().get(TURRET_HEALTH) / vehicle.getTurretMaxHealth()));
                RenderHelper.blit(poseStack, BARREL, screenWidth / 2f + 112, screenHeight - 71, 0, 0.0F, 1, 16, 1, 16, MathTool.getGradientColor(color, 0xFF0000, turretHeal, 2));

                // 车身方向
                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, vehicle.turretYRotO, vehicle.getTurretYRot())), screenWidth / 2f + 112, screenHeight - 56, 0);
                int bodyHeal = (int) (100 - (100 * vehicle.getHealth() / vehicle.getMaxHealth()));
                RenderHelper.blit(poseStack, BODY, screenWidth / 2f + 96, screenHeight - 72, 0, 0.0F, 32, 32, 32, 32, MathTool.getGradientColor(color, 0xFF0000, bodyHeal, 2));
                int leftWheelHeal = (int) (100 - (100 * vehicle.getEntityData().get(L_WHEEL_HEALTH) / vehicle.getWheelMaxHealth()));
                RenderHelper.blit(poseStack, LEFT_WHEEL, screenWidth / 2f + 96, screenHeight - 72, 0, 0.0F, 32, 32, 32, 32, MathTool.getGradientColor(color, 0xFF0000, leftWheelHeal, 2));
                int rightWheelHeal = (int) (100 - (100 * vehicle.getEntityData().get(R_WHEEL_HEALTH) / vehicle.getWheelMaxHealth()));
                RenderHelper.blit(poseStack, RIGHT_WHEEL, screenWidth / 2f + 96, screenHeight - 72, 0, 0.0F, 32, 32, 32, 32, MathTool.getGradientColor(color, 0xFF0000, rightWheelHeal, 2));
                int engineHeal = (int) (100 - (100 * vehicle.getEntityData().get(ENGINE_HEALTH) / vehicle.getEngineMaxHealth()));
                RenderHelper.blit(poseStack, ENGINE, screenWidth / 2f + 96, screenHeight - 72, 0, 0.0F, 32, 32, 32, 32, MathTool.getGradientColor(color, 0xFF0000, engineHeal, 2));
                poseStack.popPose();

                // 时速
                guiGraphics.drawString(mc.font, Component.literal(FormatTool.format0D(vehicle.getDeltaMovement().dot(vehicle.getViewVector(partialTick)) * 72, " km/h")),
                        screenWidth / 2 + 160, screenHeight / 2 - 48, color, false);

                // 低电量警告
                if (vehicle.hasEnergyStorage()) {
                    if (vehicle.getEnergy() < 0.02 * vehicle.getMaxEnergy()) {
                        guiGraphics.drawString(mc.font, Component.literal("NO POWER!"),
                                screenWidth / 2 - 144, screenHeight / 2 + 14, -65536, false);
                    } else if (vehicle.getEnergy() < 0.2 * vehicle.getMaxEnergy()) {
                        guiGraphics.drawString(mc.font, Component.literal("LOW POWER"),
                                screenWidth / 2 - 144, screenHeight / 2 + 14, 0xFF6B00, false);
                    }
                }

                // 测距
                boolean lookAtEntity = false;

                BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1).scale(512)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                Vec3 hitPos = result.getLocation();

                double blockRange = player.getEyePosition(1).distanceTo(hitPos);
                double entityRange = 0;

                Entity lookingEntity = TraceTool.camerafFindLookingEntity(player, cameraPos, viewVec, 512);
                if (lookingEntity != null) {
                    lookAtEntity = true;
                    entityRange = player.distanceTo(lookingEntity);
                }

                // 测距
                if (lookAtEntity) {
                    guiGraphics.drawString(mc.font, Component.literal(FormatTool.format1D(entityRange, "m")),
                            screenWidth / 2 - 6, screenHeight - 53, color, false);
                } else {
                    if (blockRange > 500) {
                        guiGraphics.drawString(mc.font, Component.literal("---m"), screenWidth / 2 - 6, screenHeight - 53, color, false);
                    } else {
                        guiGraphics.drawString(mc.font, Component.literal(FormatTool.format1D(blockRange, "m")),
                                screenWidth / 2 - 6, screenHeight - 53, color, false);
                    }
                }

                // 血量
                int heal = (int) (100 - (100 * vehicle.getHealth() / vehicle.getMaxHealth()));
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(100 - heal)), screenWidth / 2 - 165, screenHeight / 2 - 46, MathTool.getGradientColor(color, 0xFF0000, bodyHeal, 2), false);

                // 诱饵
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("SMOKE " + vehicle.getEntityData().get(DECOY_READY)), screenWidth / 2 - 165, screenHeight / 2 - 36, color, false);

                renderKillIndicator(guiGraphics, screenWidth, screenHeight);
            }
            poseStack.popPose();
        }
    }

    public static void renderKillIndicator(GuiGraphics guiGraphics, float w, float h) {
        float posX = w / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
        float posY = h / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
        float rate = (40 - killIndicator * 5) / 5.5f;

        if (hitIndicator > 0) {
            preciseBlit(guiGraphics, HIT_MARKER, posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (vehicleIndicator > 0) {
            preciseBlit(guiGraphics, HIT_MARKER_VEHICLE, posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (headIndicator > 0) {
            preciseBlit(guiGraphics, HEADSHOT_MARKER, posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (killIndicator > 0) {
            float posX1 = w / 2f - 7.5f - 2 + rate;
            float posY1 = h / 2f - 7.5f - 2 + rate;
            float posX2 = w / 2f - 7.5f + 2 - rate;
            float posY2 = h / 2f - 7.5f + 2 - rate;

            preciseBlit(guiGraphics, KILL_MARKER_1, posX1, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, KILL_MARKER_2, posX2, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, KILL_MARKER_3, posX1, posY2, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, KILL_MARKER_4, posX2, posY2, 0, 0, 16, 16, 16, 16);
        }
    }

    public static void renderKillIndicator3P(GuiGraphics guiGraphics, float posX, float posY) {
        float rate = (40 - killIndicator * 5) / 5.5f;

        if (hitIndicator > 0) {
            preciseBlit(guiGraphics, HIT_MARKER, posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (vehicleIndicator > 0) {
            preciseBlit(guiGraphics, HIT_MARKER_VEHICLE, posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (headIndicator > 0) {
            preciseBlit(guiGraphics, HEADSHOT_MARKER, posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (killIndicator > 0) {
            float posX1 = posX - 2 + rate;
            float posY1 = posY - 2 + rate;
            float posX2 = posX + 2 - rate;
            float posY2 = posY + 2 - rate;

            preciseBlit(guiGraphics, KILL_MARKER_1, posX1, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, KILL_MARKER_2, posX2, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, KILL_MARKER_3, posX1, posY2, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, KILL_MARKER_4, posX2, posY2, 0, 0, 16, 16, 16, 16);
        }
    }

    private static void renderPassengerInfo(GuiGraphics guiGraphics, VehicleEntity vehicle, int w, int h) {
        var passengers = vehicle.getOrderedPassengers();

        int index = 0;
        for (int i = passengers.size() - 1; i >= 0; i--) {
            var passenger = passengers.get(i);

            int y = h - 35 - index * 12;
            AtomicReference<String> name = new AtomicReference<>("---");

            if (passenger != null) {
                name.set(passenger.getName().getString());
            }

            if (passenger instanceof Player player) {
                CuriosApi.getCuriosInventory(player).ifPresent(
                        c -> c.findFirstCurio(ModItems.DOG_TAG.get()).ifPresent(
                                s -> {
                                    if (s.stack().hasCustomHoverName()) {
                                        name.set(s.stack().getHoverName().getString());
                                    }
                                }
                        )
                );
            }

            guiGraphics.drawString(Minecraft.getInstance().font, name.get(), 42, y, 0x66ff00, true);

            String num = "[" + (i + 1) + "]";
            guiGraphics.drawString(Minecraft.getInstance().font, num, 25 - Minecraft.getInstance().font.width(num), y, 0x66ff00, true);

            preciseBlit(guiGraphics, index == passengers.size() - 1 ? DRIVER : PASSENGER, 30, y, 100, 0, 0, 8, 8, 8, 8);
            index++;
        }
    }

    private static void renderWeaponInfo(GuiGraphics guiGraphics, VehicleEntity vehicle, int w, int h) {
        Player player = Minecraft.getInstance().player;

        if (!vehicle.banHand(player)) return;
        if (!(vehicle instanceof WeaponVehicleEntity weaponVehicle)) return;

        var temp = wasRenderingWeapons;
        wasRenderingWeapons = false;

        assert player != null;

        int index = vehicle.getSeatIndex(player);
        if (index == -1) return;



        var weapons = weaponVehicle.getAvailableWeapons(index);
        if (weapons.isEmpty()) return;

        int weaponIndex = weaponVehicle.getWeaponIndex(index);
        if (weaponIndex == -1) return;

        wasRenderingWeapons = temp;

        var currentTime = System.currentTimeMillis();

        // 若上一帧未在渲染武器信息，则初始化动画相关变量
        if (!wasRenderingWeapons) {
            WEAPON_SLOTS_TIMER[weaponIndex].beginForward(currentTime);

            if (oldWeaponIndex != weaponIndex) {
                WEAPON_SLOTS_TIMER[oldWeaponIndex].endBackward(currentTime);

                oldWeaponIndex = weaponIndex;
                oldRenderWeaponIndex = weaponIndex;
            }

            WEAPON_INDEX_UPDATE_TIMER.beginForward(currentTime);
        }

        // 切换武器时，更新上一个武器槽位和当前武器槽位的动画信息
        if (weaponIndex != oldWeaponIndex) {
            WEAPON_SLOTS_TIMER[weaponIndex].forward(currentTime);
            WEAPON_SLOTS_TIMER[oldWeaponIndex].backward(currentTime);

            oldRenderWeaponIndex = oldWeaponIndex;
            oldWeaponIndex = weaponIndex;

            WEAPON_INDEX_UPDATE_TIMER.beginForward(currentTime);
        }

        var pose = guiGraphics.pose();

        pose.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        int frameIndex = 0;

        for (int i = weapons.size() - 1; i >= 0 && i < 9; i--) {
            var weapon = weapons.get(i);

            var frame = FRAMES[i];

            pose.pushPose();

            // 相对于最左边的偏移量
            float xOffset;
            // 向右偏移的最长长度
            var maxXOffset = 35;

            var currentSlotTimer = WEAPON_SLOTS_TIMER[i];
            var progress = currentSlotTimer.getProgress(currentTime);

            RenderSystem.setShaderColor(1, 1, 1, Mth.lerp(progress, 0.2f, 1));
            xOffset = Mth.lerp(progress, maxXOffset, 0);

            // 当前选中武器
            if (weaponIndex == i) {
                var startY = Mth.lerp(progress,
                        h - (weapons.size() - 1 - oldRenderWeaponIndex) * 18 - 16,
                        h - (weapons.size() - 1 - weaponIndex) * 18 - 16
                );

                preciseBlit(guiGraphics, SELECTED, w - 95, startY, 100, 0, 0, 8, 8, 8, 8);
                var ammoCount = vehicle.getAmmoCount(player, i);

                if (ammoCount == Integer.MAX_VALUE) {
                    preciseBlit(guiGraphics, NUMBER, w - 28 + xOffset, h - frameIndex * 18 - 15, 100, 58, 0, 10, 7.5f, 75, 7.5f);
                } else {
                    // TODO 替换LaserWeapon判断
                    renderNumber(guiGraphics, ammoCount, weapon instanceof LaserWeapon,
                            w - 20 + xOffset, h - frameIndex * 18 - 15.5f, 0.25f);
                }
            }

            preciseBlit(guiGraphics, frame, w - 85 + xOffset, h - frameIndex * 18 - 20, 100, 0, 0, 75, 16, 75, 16);
            preciseBlit(guiGraphics, weapon.icon, w - 85 + xOffset, h - frameIndex * 18 - 20, 100, 0, 0, 75, 16, 75, 16);

            pose.popPose();

            frameIndex++;
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        pose.popPose();

        // 切换武器光标动画播放结束后，更新上次选择槽位
        if (oldWeaponIndex != oldRenderWeaponIndex && WEAPON_INDEX_UPDATE_TIMER.finished(currentTime)) {
            oldRenderWeaponIndex = oldWeaponIndex;
        }
        wasRenderingWeapons = true;
    }

    private static void renderNumber(GuiGraphics guiGraphics, int number, boolean percent, float x, float y, float scale) {
        float pX = x;
        if (percent) {
            pX -= 32 * scale;
            preciseBlit(guiGraphics, NUMBER, pX + 20 * scale, y, 100,
                    200 * scale, 0, 32 * scale, 30 * scale, 300 * scale, 30 * scale);
        }

        int index = 0;
        if (number == 0) {
            preciseBlit(guiGraphics, NUMBER, pX, y, 100,
                    0, 0, 20 * scale, 30 * scale, 300 * scale, 30 * scale);
        }

        while (number > 0) {
            int digit = number % 10;
            preciseBlit(guiGraphics, NUMBER, pX - index * 20 * scale, y, 100,
                    digit * 20 * scale, 0, 20 * scale, 30 * scale, 300 * scale, 30 * scale);
            number /= 10;
            index++;
        }
    }
}
