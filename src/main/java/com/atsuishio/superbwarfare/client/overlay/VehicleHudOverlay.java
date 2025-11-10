package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.animation.AnimationCurves;
import com.atsuishio.superbwarfare.client.animation.AnimationTimer;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.atomic.AtomicReference;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay.*;

@OnlyIn(Dist.CLIENT)
public class VehicleHudOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("vehicle_hud");
    public static final int ANIMATION_TIME = 300;

    private static final ResourceLocation ARMOR = Mod.loc("textures/overlay/vehicle/base/armor.png");
    private static final ResourceLocation ENERGY = Mod.loc("textures/overlay/vehicle/base/energy.png");
    private static final ResourceLocation VALUE_BAR = Mod.loc("textures/overlay/vehicle/base/value_bar.png");
    private static final ResourceLocation VALUE_FRAME = Mod.loc("textures/overlay/vehicle/base/value_frame.png");

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

    private static final ResourceLocation GEAR = Mod.loc("textures/overlay/vehicle/aircraft/gear.png");

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
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        Player player = Minecraft.getInstance().player;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

        if (!shouldRenderHud(player)) {
            wasRenderingWeapons = false;
            return;
        }

        Entity entity = player.getVehicle();
        if (!(entity instanceof VehicleEntity vehicle)) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

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
        if (!NBTTool.getTag(stack).contains("ArmorPlate")) return 0;
        if (!DisplayConfig.ARMOR_PLATE_HUD.get()) return 0;
        return 9;
    }

    public static void renderKillIndicator(GuiGraphics guiGraphics, float screenWidth, float screenHeight) {
        float posX = screenWidth / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
        float posY = screenHeight / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
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
            float posX1 = screenWidth / 2f - 7.5f - 2 + rate;
            float posY1 = screenHeight / 2f - 7.5f - 2 + rate;
            float posX2 = screenWidth / 2f - 7.5f + 2 - rate;
            float posY2 = screenHeight / 2f - 7.5f + 2 - rate;

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

    private static void renderPassengerInfo(GuiGraphics guiGraphics, VehicleEntity vehicle, int screenWidth, int screenHeight) {
        var passengers = vehicle.getOrderedPassengers();

        int index = 0;
        for (int i = passengers.size() - 1; i >= 0; i--) {
            var passenger = passengers.get(i);

            int y = screenHeight - 35 - index * 12;
            AtomicReference<String> name = new AtomicReference<>("---");

            if (passenger != null) {
                name.set(passenger.getName().getString());
            }

            if (passenger instanceof Player player) {
                CuriosApi.getCuriosInventory(player)
                        .flatMap(c -> c.findFirstCurio(ModItems.DOG_TAG.get()))
                        .ifPresent(s -> name.set(s.stack().getHoverName().getString()));
            }

            guiGraphics.drawString(Minecraft.getInstance().font, name.get(), 42, y, 0x66ff00, true);

            String num = "[" + (i + 1) + "]";
            guiGraphics.drawString(Minecraft.getInstance().font, num, 25 - Minecraft.getInstance().font.width(num), y, 0x66ff00, true);

            preciseBlit(guiGraphics, index == passengers.size() - 1 ? DRIVER : PASSENGER, 30, y, 100, 0, 0, 8, 8, 8, 8);
            index++;
        }
    }

    private static void renderWeaponInfo(GuiGraphics guiGraphics, VehicleEntity vehicle, int screenWidth, int screenHeight) {
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

            preciseBlit(guiGraphics, frame, screenWidth - 85 + xOffset, screenHeight - frameIndex * 18 - 20, 100, 0, 0, 75, 16, 75, 16);

            var data = vehicle.getGunData(vehicle.getSeatIndex(player), i);
            if (data == null) continue;

            // 当前选中武器
            if (weaponIndex == i) {
                var startY = Mth.lerp(progress,
                        screenHeight - (weapons.size() - 1 - oldRenderWeaponIndex) * 18 - 16,
                        screenHeight - (weapons.size() - 1 - weaponIndex) * 18 - 16
                );

                preciseBlit(guiGraphics, SELECTED, screenWidth - 95, startY, 100, 0, 0, 8, 8, 8, 8);
                var ammoCount = vehicle.getAmmoCount(player);

                if (ammoCount == Integer.MAX_VALUE) {
                    preciseBlit(guiGraphics, NUMBER, screenWidth - 28 + xOffset, screenHeight - frameIndex * 18 - 15, 100, 58, 0, 10, 7.5f, 75, 7.5f);
                } else {
                    boolean percent = data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.ENERGY;
                    if (percent) {
                        ammoCount /= (int) Math.max(1, (double) vehicle.getMaxEnergy() / 100);
                    }
                    renderNumber(guiGraphics, ammoCount, percent, screenWidth - 20 + xOffset, screenHeight - frameIndex * 18 - 15.5f, 0.25f);
                }
            }

            preciseBlit(guiGraphics, weapon.icon, screenWidth - 85 + xOffset, screenHeight - frameIndex * 18 - 20, 100, 0, 0, 75, 16, 75, 16);

            if (data.compute().autoIterativeReloadTime > 0) {
                var time = data.compute().autoIterativeReloadTime;
                var timer = data.autoIterativeReloadTimer.get();
                float reloadProgress = (float) (time - timer) / time;
                float alpha = Mth.lerp(progress, 0.4f, 1);

                if (timer > 0 && timer < time) {
                    RenderHelper.renderCircularRing(
                            guiGraphics,
                            screenWidth - 102 + xOffset, screenHeight - frameIndex * 18 - 12,
                            0.014f, 0.010f,
                            new float[]{0f, 0f, 0f, 0.4f * alpha},
                            new float[]{1f, 1f, 1f, alpha},
                            reloadProgress,
                            true
                    );
                }
            }

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

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
