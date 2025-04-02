package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.vehicle.*;
import com.atsuishio.superbwarfare.entity.vehicle.base.*;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.HeliRocketWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.LaserWeapon;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.animation.AnimationCurves;
import com.atsuishio.superbwarfare.tools.animation.AnimationTimer;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Math;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicReference;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay.*;
import static com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity.LOADED_MISSILE;
import static com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity.MISSILE_COUNT;
import static com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity.*;

@EventBusSubscriber(value = Dist.CLIENT)
public class VehicleHudOverlay {

    private static float scopeScale = 1;
    private static final ResourceLocation FRAME = Mod.loc("textures/screens/land/tv_frame.png");
    private static final ResourceLocation ARMOR = Mod.loc("textures/screens/armor.png");
    private static final ResourceLocation ENERGY = Mod.loc("textures/screens/energy.png");
    private static final ResourceLocation HEALTH = Mod.loc("textures/screens/armor_value.png");
    private static final ResourceLocation HEALTH_FRAME = Mod.loc("textures/screens/armor_value_frame.png");
    private static final ResourceLocation DRIVER = Mod.loc("textures/screens/driver.png");
    private static final ResourceLocation PASSENGER = Mod.loc("textures/screens/passenger.png");
    private static final ResourceLocation SELECTED = Mod.loc("textures/screens/vehicle_weapon/selected.png");
    private static final ResourceLocation NUMBER = Mod.loc("textures/screens/vehicle_weapon/number.png");

    public static final int ANIMATION_TIME = 300;
    private static final AnimationTimer[] weaponSlotsTimer = AnimationTimer.createTimers(9, ANIMATION_TIME, AnimationCurves.EASE_OUT_CIRC);
    private static boolean wasRenderingWeapons = false;
    private static int oldWeaponIndex = 0;
    private static int oldRenderWeaponIndex = 0;
    private static final AnimationTimer weaponIndexUpdateTimer = new AnimationTimer(ANIMATION_TIME).animation(AnimationCurves.EASE_OUT_CIRC);


    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();
        Player player = Minecraft.getInstance().player;

        if (!shouldRenderHud(player)) {
            wasRenderingWeapons = false;
            return;
        }

        Entity vehicle = player.getVehicle();

        GuiGraphics guiGraphics = event.getGuiGraphics();
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        // 渲染地面武装HUD
        renderLandArmorHud(event, w, h);

        int compatHeight = getArmorPlateCompatHeight(player);

        if (vehicle instanceof EnergyVehicleEntity energyVehicleEntity) {
            float energy = energyVehicleEntity.getEnergy();
            float maxEnergy = energyVehicleEntity.getMaxEnergy();
            preciseBlit(guiGraphics, ENERGY, 10, h - 22 - compatHeight, 100, 0, 0, 8, 8, 8, 8);
            preciseBlit(guiGraphics, HEALTH_FRAME, 20, h - 21 - compatHeight, 100, 0, 0, 60, 6, 60, 6);
            preciseBlit(guiGraphics, HEALTH, 20, h - 21 - compatHeight, 100, 0, 0, (int) (60 * energy / maxEnergy), 6, 60, 6);
        }

        if (vehicle instanceof VehicleEntity pVehicle) {
            float health = pVehicle.getHealth();
            float maxHealth = pVehicle.getMaxHealth();
            preciseBlit(guiGraphics, ARMOR, 10, h - 13 - compatHeight, 100, 0, 0, 8, 8, 8, 8);
            preciseBlit(guiGraphics, HEALTH_FRAME, 20, h - 12 - compatHeight, 100, 0, 0, 60, 6, 60, 6);
            preciseBlit(guiGraphics, HEALTH, 20, h - 12 - compatHeight, 100, 0, 0, (int) (60 * health / maxHealth), 6, 60, 6);

            renderWeaponInfo(guiGraphics, pVehicle, w, h);
            renderPassengerInfo(guiGraphics, pVehicle, w, h);
        }

        poseStack.popPose();
    }

    private static boolean shouldRenderHud(Player player) {
        if (player == null) return false;
        return !player.isSpectator() && (player.getVehicle() != null && player.getVehicle() instanceof VehicleEntity);
    }

    private static int getArmorPlateCompatHeight(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (stack == ItemStack.EMPTY) return 0;
        if (!NBTTool.getTag(stack).contains("ArmorPlate")) return 0;
        if (!DisplayConfig.ARMOR_PLATE_HUD.get()) return 0;
        return 9;
    }

    public static void renderLandArmorHud(RenderGuiEvent.Pre event, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        PoseStack poseStack = guiGraphics.pose();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        assert player != null;

        if (player.getVehicle() instanceof LandArmorEntity iLand && iLand.isDriver(player)
                && iLand instanceof WeaponVehicleEntity weaponVehicle
                && iLand instanceof MobileVehicleEntity mobileVehicle
                && !(player.getVehicle() instanceof SpeedboatEntity)) {
            poseStack.pushPose();

            poseStack.translate(0, 0 - 0.3 * ClientEventHandler.shakeTime + 3 * ClientEventHandler.cameraRoll, 0);
            poseStack.rotateAround(Axis.ZP.rotationDegrees(-0.3f * ClientEventHandler.cameraRoll), w / 2f, h / 2f, 0);
            poseStack.translate(0.2 * ClientEventHandler.shakeTime + 5 * ClientEventHandler.cameraRoll, 0 - 0.3 * ClientEventHandler.shakeTime + 5 * ClientEventHandler.cameraRoll, 0);
            poseStack.rotateAround(Axis.ZP.rotationDegrees(-0.5f * ClientEventHandler.cameraRoll), w / 2f, h / 2f, 0);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            scopeScale = Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), scopeScale, 1F);
            float f = (float) Math.min(w, h);
            float f1 = Math.min((float) w / f, (float) h / f) * scopeScale;
            float i = Mth.floor(f * f1);
            float j = Mth.floor(f * f1);
            float k = ((w - i) / 2);
            float l = ((h - j) / 2);

            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
                int addW = (w / h) * 48;
                int addH = (w / h) * 27;
                preciseBlit(guiGraphics, FRAME, (float) -addW / 2, (float) -addH / 2, 10, 0, 0.0F, w + addW, h + addH, w + addW, h + addH);
                preciseBlit(guiGraphics, Mod.loc("textures/screens/land/line.png"), w / 2f - 64, h - 56, 0, 0.0F, 128, 1, 128, 1);
                preciseBlit(guiGraphics, Mod.loc("textures/screens/land/line.png"), w / 2f + 112, h - 71, 0, 0.0F, 1, 16, 1, 16);

                // 不同武器种类的准星
                if (weaponVehicle instanceof Yx100Entity) {
                    if (weaponVehicle.getWeaponIndex(0) == 0) {
                        preciseBlit(guiGraphics, Mod.loc("textures/screens/land/tank_cannon_cross_ap.png"), k, l, 0, 0.0F, i, j, i, j);
                    } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                        preciseBlit(guiGraphics, Mod.loc("textures/screens/land/tank_cannon_cross_he.png"), k, l, 0, 0.0F, i, j, i, j);
                    } else if (weaponVehicle.getWeaponIndex(0) == 2) {
                        preciseBlit(guiGraphics, Mod.loc("textures/screens/land/lav_gun_cross.png"), k, l, 0, 0.0F, i, j, i, j);
                    }

                } else if (weaponVehicle instanceof PrismTankEntity) {
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/land/lav_missile_cross.png"), k, l, 0, 0.0F, i, j, i, j);
                } else {
                    if (weaponVehicle.getWeaponIndex(0) == 0) {
                        preciseBlit(guiGraphics, Mod.loc("textures/screens/land/lav_cannon_cross.png"), k, l, 0, 0.0F, i, j, i, j);
                    } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                        preciseBlit(guiGraphics, Mod.loc("textures/screens/land/lav_gun_cross.png"), k, l, 0, 0.0F, i, j, i, j);
                    } else if (weaponVehicle.getWeaponIndex(0) == 2) {
                        preciseBlit(guiGraphics, Mod.loc("textures/screens/land/lav_missile_cross.png"), k, l, 0, 0.0F, i, j, i, j);
                    }
                }

                // 指南针
                preciseBlit(guiGraphics, Mod.loc("textures/screens/compass.png"), (float) w / 2 - 128, (float) 10, 128 + ((float) 64 / 45 * player.getYRot()), 0, 256, 16, 512, 16);
                preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/roll_ind.png"), w / 2f - 8, 30, 0, 0.0F, 16, 16, 16, 16);

                // 炮塔方向
                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), iLand.turretYRotO(), iLand.turretYRot())), w / 2f + 112, h - 56, 0);
                preciseBlit(guiGraphics, Mod.loc("textures/screens/land/body.png"), w / 2f + 96, h - 72, 0, 0.0F, 32, 32, 32, 32);
                poseStack.popPose();

                // 时速
                guiGraphics.drawString(mc.font, Component.literal(FormatTool.format0D(mobileVehicle.getDeltaMovement().length() * 72, " km/h")),
                        w / 2 + 160, h / 2 - 48, 0x66FF00, false);

                // 低电量警告
                if (mobileVehicle.getEnergy() < 0.02 * mobileVehicle.getMaxEnergy()) {
                    guiGraphics.drawString(mc.font, Component.literal("NO POWER!"),
                            w / 2 - 144, h / 2 + 14, -65536, false);
                } else if (mobileVehicle.getEnergy() < 0.2 * mobileVehicle.getMaxEnergy()) {
                    guiGraphics.drawString(mc.font, Component.literal("LOW POWER"),
                            w / 2 - 144, h / 2 + 14, 0xFF6B00, false);
                }

                // 测距
                boolean lookAtEntity = false;

                BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1).scale(512)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                Vec3 hitPos = result.getLocation();

                double blockRange = player.getEyePosition(1).distanceTo(hitPos);

                double entityRange = 0;

                Entity lookingEntity = SeekTool.seekLivingEntity(player, player.level(), 512, 1);
                if (lookingEntity != null) {
                    lookAtEntity = true;
                    entityRange = player.distanceTo(lookingEntity);
                }

                if (lookAtEntity) {
                    guiGraphics.drawString(mc.font, Component.literal(FormatTool.format1D(entityRange, "m")),
                            w / 2 - 6, h - 53, 0x66FF00, false);
                } else {
                    if (blockRange > 500) {
                        guiGraphics.drawString(mc.font, Component.literal("---m"), w / 2 - 6, h - 53, 0x66FF00, false);
                    } else {
                        guiGraphics.drawString(mc.font, Component.literal(FormatTool.format1D(blockRange, "m")),
                                w / 2 - 6, h - 53, 0x66FF00, false);
                    }
                }

                // 武器名称
                // LAV-150
                if (player.getVehicle() instanceof Lav150Entity lav) {
                    if (weaponVehicle.getWeaponIndex(0) == 0) {
                        double heat = 1 - lav.getEntityData().get(HEAT) / 100.0F;
                        guiGraphics.drawString(mc.font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : lav.getAmmoCount(player))), w / 2 - 33, h - 65, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
                    } else {
                        double heat = 1 - lav.getEntityData().get(COAX_HEAT) / 100.0F;
                        guiGraphics.drawString(mc.font, Component.literal("7.62MM COAX " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : lav.getAmmoCount(player))), w / 2 - 33, h - 65, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
                    }
                }

                // BMP-2
                if (player.getVehicle() instanceof Bmp2Entity bmp2) {
                    if (weaponVehicle.getWeaponIndex(0) == 0) {
                        double heat = 1 - bmp2.getEntityData().get(HEAT) / 100.0F;
                        guiGraphics.drawString(mc.font, Component.literal(" 30MM 2A42 " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : bmp2.getAmmoCount(player))), w / 2 - 33, h - 65, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
                    } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                        double heat = 1 - bmp2.getEntityData().get(COAX_HEAT) / 100.0F;
                        guiGraphics.drawString(mc.font, Component.literal(" 7.62MM ПКТ " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : bmp2.getAmmoCount(player))), w / 2 - 33, h - 65, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
                    } else {
                        guiGraphics.drawString(mc.font, Component.literal("    9M113  " + bmp2.getEntityData().get(LOADED_MISSILE) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : bmp2.getEntityData().get(MISSILE_COUNT))), w / 2 - 33, h - 65, 0x66FF00, false);
                    }

                }

                // YX-100
                if (player.getVehicle() instanceof Yx100Entity yx100) {
                    if (weaponVehicle.getWeaponIndex(0) == 0) {
                        guiGraphics.drawString(mc.font, Component.literal("AP SHELL  " + yx100.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : yx100.getEntityData().get(AMMO))), w / 2 - 33, h - 65, 0x66FF00, false);
                    } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                        guiGraphics.drawString(mc.font, Component.literal("HE SHELL  " + yx100.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : yx100.getEntityData().get(AMMO))), w / 2 - 33, h - 65, 0x66FF00, false);
                    } else if (weaponVehicle.getWeaponIndex(0) == 2) {
                        double heat = 1 - yx100.getEntityData().get(COAX_HEAT) / 100.0F;
                        guiGraphics.drawString(mc.font, Component.literal(" 12.7MM HMG " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : yx100.getAmmoCount(player))), w / 2 - 33, h - 65, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
                    }
                }

                // 血量
                double heal = mobileVehicle.getHealth() / mobileVehicle.getMaxHealth();
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(100 * heal)), w / 2 - 165, h / 2 - 46, Mth.hsvToRgb((float) heal / 3.745318352059925F, 1.0F, 1.0F), false);

                renderKillIndicator(guiGraphics, w, h);
            } else if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK && !ClientEventHandler.zoomVehicle) {
                Vec3 p = RenderHelper.worldToScreen(new Vec3(Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), player.xo, player.getX()), Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), player.zo, player.getZ())).add(iLand.getBarrelVec(event.getPartialTick().getGameTimeDeltaPartialTick(true)).scale(192)), cameraPos);
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

                    // LAV-150
                    if (weaponVehicle instanceof Lav150Entity lav1501) {
                        if (weaponVehicle.getWeaponIndex(0) == 0) {
                            double heat = lav1501.getEntityData().get(HEAT) / 100.0F;
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : lav1501.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
                        } else {
                            double heat2 = lav1501.getEntityData().get(COAX_HEAT) / 100.0F;
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("7.62MM COAX " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : lav1501.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat2, 1.0F), false);
                        }
                    }
                    // BMP-2
                    if (weaponVehicle instanceof Bmp2Entity bmp201) {
                        if (weaponVehicle.getWeaponIndex(0) == 0) {
                            double heat = bmp201.getEntityData().get(HEAT) / 100.0F;
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("30MM 2A42 " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : bmp201.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                            double heat2 = bmp201.getEntityData().get(COAX_HEAT) / 100.0F;
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("7.62MM ПКТ " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : bmp201.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat2, 1.0F), false);
                        } else {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("9M113 " + bmp201.getEntityData().get(LOADED_MISSILE) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : bmp201.getEntityData().get(MISSILE_COUNT))), 30, -9, -1, false);
                        }
                    }
                    // YX-100
                    if (weaponVehicle instanceof Yx100Entity yx100) {
                        if (weaponVehicle.getWeaponIndex(0) == 0) {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("AP SHELL " + yx100.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : yx100.getEntityData().get(AMMO))), 30, -9, -1, false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("HE SHELL " + yx100.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : yx100.getEntityData().get(AMMO))), 30, -9, -1, false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 2) {
                            double heat2 = yx100.getEntityData().get(COAX_HEAT) / 100.0F;
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("7.62MM ПКТ " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : yx100.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat2, 1.0F), false);
                        }
                    }

                    double heal = 1 - mobileVehicle.getHealth() / mobileVehicle.getMaxHealth();

                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("HP " +
                            FormatTool.format0D(100 * mobileVehicle.getHealth() / mobileVehicle.getMaxHealth())), 30, 1, Mth.hsvToRgb(0F, (float) heal, 1.0F), false);

                    poseStack.popPose();
                    poseStack.popPose();
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        } else {
            scopeScale = 0.7f;
        }
    }

    public static void renderKillIndicator(GuiGraphics guiGraphics, float w, float h) {
        float posX = w / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
        float posY = h / 2f - 7.5f + (float) (2 * (Math.random() - 0.5f));
        float rate = (40 - KILL_INDICATOR * 5) / 5.5f;

        if (HIT_INDICATOR > 0) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/hit_marker.png"), posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (VEHICLE_INDICATOR > 0) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/hit_marker_vehicle.png"), posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (HEAD_INDICATOR > 0) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/headshot_mark.png"), posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (KILL_INDICATOR > 0) {
            float posX1 = w / 2f - 7.5f - 2 + rate;
            float posY1 = h / 2f - 7.5f - 2 + rate;
            float posX2 = w / 2f - 7.5f + 2 - rate;
            float posY2 = h / 2f - 7.5f + 2 - rate;

            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark1.png"), posX1, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark2.png"), posX2, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark3.png"), posX1, posY2, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark4.png"), posX2, posY2, 0, 0, 16, 16, 16, 16);
        }
    }

    public static void renderKillIndicator3P(GuiGraphics guiGraphics, float posX, float posY) {
        float rate = (40 - KILL_INDICATOR * 5) / 5.5f;

        if (HIT_INDICATOR > 0) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/hit_marker.png"), posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (VEHICLE_INDICATOR > 0) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/hit_marker_vehicle.png"), posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (HEAD_INDICATOR > 0) {
            preciseBlit(guiGraphics, Mod.loc("textures/screens/headshot_mark.png"), posX, posY, 0, 0, 16, 16, 16, 16);
        }

        if (KILL_INDICATOR > 0) {
            float posX1 = posX - 2 + rate;
            float posY1 = posY - 2 + rate;
            float posX2 = posX + 2 - rate;
            float posY2 = posY + 2 - rate;

            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark1.png"), posX1, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark2.png"), posX2, posY1, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark3.png"), posX1, posY2, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, Mod.loc("textures/screens/kill_mark4.png"), posX2, posY2, 0, 0, 16, 16, 16, 16);
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

    private static void renderWeaponInfo(GuiGraphics guiGraphics, VehicleEntity vehicle, int w, int h) {
        if (!(vehicle instanceof WeaponVehicleEntity weaponVehicle)) return;

        var temp = wasRenderingWeapons;
        wasRenderingWeapons = false;

        Player player = Minecraft.getInstance().player;
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
            weaponSlotsTimer[weaponIndex].beginForward(currentTime);

            if (oldWeaponIndex != weaponIndex) {
                weaponSlotsTimer[oldWeaponIndex].endBackward(currentTime);

                oldWeaponIndex = weaponIndex;
                oldRenderWeaponIndex = weaponIndex;
            }

            weaponIndexUpdateTimer.beginForward(currentTime);
        }

        // 切换武器时，更新上一个武器槽位和当前武器槽位的动画信息
        if (weaponIndex != oldWeaponIndex) {
            weaponSlotsTimer[weaponIndex].forward(currentTime);
            weaponSlotsTimer[oldWeaponIndex].backward(currentTime);

            oldRenderWeaponIndex = oldWeaponIndex;
            oldWeaponIndex = weaponIndex;

            weaponIndexUpdateTimer.beginForward(currentTime);
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

            ResourceLocation frame = Mod.loc("textures/screens/vehicle_weapon/frame_" + (i + 1) + ".png");
            pose.pushPose();

            // 相对于最左边的偏移量
            float xOffset;
            // 向右偏移的最长长度
            var maxXOffset = 35;

            var currentSlotTimer = weaponSlotsTimer[i];
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
                if (InventoryTool.hasCreativeAmmoBox(player) && !(weapon instanceof LaserWeapon) && !(weapon instanceof HeliRocketWeapon)) {
                    preciseBlit(guiGraphics, NUMBER, w - 28 + xOffset, h - frameIndex * 18 - 15, 100, 58, 0, 10, 7.5f, 75, 7.5f);
                } else {
                    renderNumber(guiGraphics, weaponVehicle.getAmmoCount(player), weapon instanceof LaserWeapon,
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
        if (oldWeaponIndex != oldRenderWeaponIndex && weaponIndexUpdateTimer.finished(currentTime)) {
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
