package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.ammo.AmmoSupplierItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.AmmoType;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.animation.AnimationCurves;
import com.atsuishio.superbwarfare.tools.animation.AnimationTimer;
import com.atsuishio.superbwarfare.tools.animation.ValueAnimator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class AmmoBarOverlay {

    private static final ResourceLocation LINE = ModUtils.loc("textures/gun_icon/fire_mode/line.png");
    private static final ResourceLocation SEMI = ModUtils.loc("textures/gun_icon/fire_mode/semi.png");
    private static final ResourceLocation BURST = ModUtils.loc("textures/gun_icon/fire_mode/burst.png");
    private static final ResourceLocation AUTO = ModUtils.loc("textures/gun_icon/fire_mode/auto.png");
    private static final ResourceLocation TOP = ModUtils.loc("textures/gun_icon/fire_mode/top.png");
    private static final ResourceLocation DIR = ModUtils.loc("textures/gun_icon/fire_mode/dir.png");
    private static final ResourceLocation MOUSE = ModUtils.loc("textures/gun_icon/fire_mode/mouse.png");

    private static boolean hasCreativeAmmo() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        return InventoryTool.hasCreativeAmmoBox(player);
    }

    @SubscribeEvent
    public static void renderWeaponInfo(RenderGuiEvent.Pre event) {
        if (!DisplayConfig.AMMO_HUD.get()) return;

        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();
        Player player = Minecraft.getInstance().player;

        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem gunItem && !(player.getVehicle() instanceof ArmedVehicleEntity vehicle && vehicle.banHand(player))) {
            PoseStack poseStack = event.getGuiGraphics().pose();

            // 渲染图标
            event.getGuiGraphics().blit(gunItem.getGunIcon(),
                    w - 135,
                    h - 40,
                    0,
                    0,
                    64,
                    16,
                    64,
                    16);

            // 渲染开火模式切换按键
            if (stack.getItem() != ModItems.MINIGUN.get()) {
                event.getGuiGraphics().drawString(
                        Minecraft.getInstance().font,
                        "[" + ModKeyMappings.FIRE_MODE.getKey().getDisplayName().getString() + "]",
                        w - 111.5f,
                        h - 20,
                        0xFFFFFF,
                        false
                );
            }

            // 渲染开火模式
            ResourceLocation fireMode = getFireMode(stack);

            if (stack.getItem() == ModItems.JAVELIN.get()) {
                fireMode = NBTTool.getBoolean(stack, "TopMode", false) ? TOP : DIR;
            }

            if (stack.getItem() == ModItems.MINIGUN.get()) {
                fireMode = MOUSE;
                // 渲染加特林射速
                event.getGuiGraphics().drawString(
                        Minecraft.getInstance().font,
                        GunsTool.getGunIntTag(stack, "RPM", 0) + " RPM",
                        w - 111f,
                        h - 20,
                        0xFFFFFF,
                        false
                );

                event.getGuiGraphics().blit(fireMode,
                        w - 126,
                        h - 22,
                        0,
                        0,
                        12,
                        12,
                        12,
                        12);
            } else {
                if (stack.getItem() != ModItems.TRACHELIUM.get()) {
                    event.getGuiGraphics().blit(fireMode,
                            w - 95,
                            h - 21,
                            0,
                            0,
                            8,
                            8,
                            8,
                            8);
                } else {
                    event.getGuiGraphics().drawString(
                            Minecraft.getInstance().font,
                            NBTTool.getBoolean(stack, "DA", false) ? Component.translatable("des.superbwarfare.revolver.sa").withStyle(ChatFormatting.BOLD) : Component.translatable("des.superbwarfare.revolver.da").withStyle(ChatFormatting.BOLD),
                            w - 96,
                            h - 20,
                            0xFFFFFF,
                            false
                    );
                }
            }

            if (stack.getItem() != ModItems.MINIGUN.get() && stack.getItem() != ModItems.TRACHELIUM.get()) {
                event.getGuiGraphics().blit(LINE,
                        w - 95,
                        h - 16,
                        0,
                        0,
                        8,
                        8,
                        8,
                        8);
            }

            // 渲染当前弹药量
            poseStack.pushPose();
            poseStack.scale(1.5f, 1.5f, 1f);

            if ((stack.getItem() == ModItems.MINIGUN.get() || stack.getItem() == ModItems.BOCEK.get()) && hasCreativeAmmo()) {
                event.getGuiGraphics().drawString(
                        Minecraft.getInstance().font,
                        "∞",
                        w / 1.5f - 64 / 1.5f,
                        h / 1.5f - 48 / 1.5f,
                        0xFFFFFF,
                        true
                );
            } else {
                event.getGuiGraphics().drawString(
                        Minecraft.getInstance().font,
                        getGunAmmoCount(player) + "",
                        w / 1.5f - 64 / 1.5f,
                        h / 1.5f - 48 / 1.5f,
                        0xFFFFFF,
                        true
                );
            }

            poseStack.popPose();

            // 渲染备弹量
            event.getGuiGraphics().drawString(
                    Minecraft.getInstance().font,
                    getPlayerAmmoCount(player),
                    w - 64,
                    h - 35,
                    0xCCCCCC,
                    true
            );

            poseStack.pushPose();
            poseStack.scale(0.9f, 0.9f, 1f);

            // 渲染物品名称
            String gunName = gunItem.getGunDisplayName();
            event.getGuiGraphics().drawString(
                    Minecraft.getInstance().font,
                    gunName,
                    w / 0.9f - (100 + Minecraft.getInstance().font.width(gunName) / 2f) / 0.9f,
                    h / 0.9f - 60 / 0.9f,
                    0xFFFFFF,
                    true
            );

            // 渲染弹药类型
            String ammoName = getGunAmmoType(stack);
            event.getGuiGraphics().drawString(
                    Minecraft.getInstance().font,
                    ammoName,
                    w / 0.9f - (100 + Minecraft.getInstance().font.width(ammoName) / 2f) / 0.9f,
                    h / 0.9f - 51 / 0.9f,
                    0xC8A679,
                    true
            );

            poseStack.popPose();
        }
    }

    private static final AnimationTimer ammoInfoTimer = new AnimationTimer(500, 2000)
            .forwardAnimation(AnimationCurves.EASE_OUT_EXPO)
            .backwardAnimation(AnimationCurves.EASE_IN_EXPO);
    private static final AnimationTimer ammoBoxTimer = new AnimationTimer(500)
            .forwardAnimation(AnimationCurves.EASE_OUT_EXPO)
            .backwardAnimation(AnimationCurves.EASE_IN_EXPO);

    private static final ValueAnimator<Integer>[] ammoCountAnimators = ValueAnimator.create(
            AmmoType.values().length, 800, 0
    );
    private static final ValueAnimator<Integer>[] ammoBoxAnimators = ValueAnimator.create(
            AmmoType.values().length, 800, 0
    );


    /**
     * 在手持弹药或弹药盒时，渲染玩家弹药总量信息
     */
    @SubscribeEvent
    public static void renderAmmoInfo(RenderGuiEvent.Pre event) {
        boolean startRenderingAmmoInfo = false;
        Player player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) return;

        boolean isAmmoBox = false;

        // 动画计算
        var currentTime = System.currentTimeMillis();
        ItemStack stack = player.getMainHandItem();
        if ((stack.getItem() instanceof AmmoSupplierItem || stack.getItem() == ModItems.AMMO_BOX.get())
                && !(player.getVehicle() instanceof ArmedVehicleEntity vehicle && vehicle.banHand(player))
        ) {
            // 刚拿出弹药物品时，视为开始弹药信息渲染
            startRenderingAmmoInfo = ammoInfoTimer.getProgress(currentTime) == 0;
            ammoInfoTimer.forward(currentTime);

            if (stack.getItem() == ModItems.AMMO_BOX.get()) {
                isAmmoBox = true;
                ammoBoxTimer.forward(currentTime);
            } else {
                ammoBoxTimer.backward(currentTime);
            }
        } else {
            ammoInfoTimer.backward(currentTime);
            ammoBoxTimer.backward(currentTime);
        }
        if (!ammoInfoTimer.isForward() && ammoInfoTimer.finished(currentTime)) return;

        var poseStack = event.getGuiGraphics().pose();
        poseStack.pushPose();

        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();

        var ammoX = ammoInfoTimer.lerp(w + 120, (float) w / 2 + 40, currentTime);
        final int fontHeight = 15;
        var yOffset = (-h - AmmoType.values().length * fontHeight) / 2f;

        // 渲染总弹药数量
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) cap = new PlayerVariable();
        var font = Minecraft.getInstance().font;

        for (var type : AmmoType.values()) {
            var index = type.ordinal();
            var ammoCount = type.get(cap);
            var animator = ammoCountAnimators[index];

            var boxAnimator = ammoBoxAnimators[index];
            var boxAmmoCount = boxAnimator.newValue();
            boolean boxAmmoSelected = false;

            if (isAmmoBox) {
                var tag = NBTTool.getTag(stack);
                var ammoBoxType = tag.getString("Type");
                boxAmmoCount = type.get(stack);
                if (ammoBoxType.equals("All") || ammoBoxType.equals(type.name)) {
                    boxAnimator.forward(currentTime);
                    boxAmmoSelected = true;
                } else {
                    boxAnimator.reset(boxAmmoCount);
                }
            }

            // 首次开始渲染弹药信息时，记录弹药数量，便于后续播放动画
            if (startRenderingAmmoInfo) {
                animator.reset(ammoCount);
                animator.endForward(currentTime);
                if (isAmmoBox) {
                    boxAnimator.reset(type.get(stack));
                    boxAnimator.endForward(currentTime);
                }
            }

            int ammoAdd = Integer.compare(ammoCount, animator.oldValue());
            // 弹药数量变化时，更新并开始播放弹药数量更改动画
            animator.compareAndUpdate(ammoCount, () -> {
                // 弹药数量变化时，开始播放弹药数量更改动画
                animator.beginForward(currentTime);
            });

            var progress = animator.getProgress(currentTime);
            var ammoCountStr = Integer.toString(
                    Math.round(animator.lerp(animator.oldValue(), ammoCount, currentTime))
            );

            // 弹药增加时，颜色由绿变白，否则由红变白
            var fontColor = FastColor.ARGB32.lerp(progress, switch (ammoAdd) {
                case 1 -> 0xFF00FF00;
                case -1 -> 0xFFFF0000;
                default -> 0xFFFFFFFF;
            }, 0xFFFFFFFF);

            RenderSystem.setShaderColor(1, 1, 1, ammoInfoTimer.lerp(0, 1, currentTime));

            // 弹药数量
            event.getGuiGraphics().drawString(
                    font,
                    ammoCountStr,
                    ammoX + (30 - font.width(ammoCountStr)),
                    h + yOffset,
                    fontColor,
                    true
            );

            // 弹药类型
            event.getGuiGraphics().drawString(
                    font,
                    Component.translatable(type.translatableKey).getString(),
                    ammoX + 35,
                    h + yOffset,
                    fontColor,
                    true
            );

            // 弹药盒信息渲染
            RenderSystem.setShaderColor(1, 1, 1, ammoBoxTimer.lerp(0, 1, currentTime));
            var ammoBoxX = ammoBoxTimer.lerp(-30, (float) w / 2, currentTime);

            int ammoBoxAdd = Integer.compare(boxAmmoCount, boxAnimator.oldValue());
            boxAnimator.compareAndUpdate(boxAmmoCount, () -> boxAnimator.beginForward(currentTime));

            // 选中时显示为黄色，否则为白色
            var targetColor = boxAmmoSelected ? 0xFFFFFF00 : 0xFFFFFFFF;

            var boxFontColor = FastColor.ARGB32.lerp(boxAnimator.getProgress(currentTime),
                    switch (ammoBoxAdd) {
                        case 1 -> 0xFF00FF00;
                        case -1 -> 0xFFFF0000;
                        default -> targetColor;
                    },
                    targetColor
            );

            // 弹药盒内弹药数量
            event.getGuiGraphics().drawString(
                    Minecraft.getInstance().font,
                    Integer.toString(
                            Math.round(boxAnimator.lerp(boxAnimator.oldValue(), boxAmmoCount, currentTime))
                    ),
                    ammoBoxX - 70,
                    h + yOffset,
                    boxFontColor,
                    true
            );

            yOffset += fontHeight;
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        poseStack.popPose();
    }

    private static ResourceLocation getFireMode(ItemStack stack) {
        return switch (GunsTool.getGunIntTag(stack, "FireMode")) {
            case 1 -> BURST;
            case 2 -> AUTO;
            default -> SEMI;
        };
    }

    private static int getGunAmmoCount(Player player) {
        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() == ModItems.MINIGUN.get()) {
            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            return cap != null ? cap.rifleAmmo : 0;
        }

        if (stack.getItem() == ModItems.BOCEK.get()) {
            return GunsTool.getGunIntTag(stack, "MaxAmmo");
        }

        return GunsTool.getGunIntTag(stack, "Ammo", 0);
    }

    private static String getPlayerAmmoCount(Player player) {
        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() == ModItems.MINIGUN.get() || stack.getItem() == ModItems.BOCEK.get()) {
            return "";
        }

        if (!hasCreativeAmmo()) {
            if (stack.is(ModTags.Items.LAUNCHER) || stack.getItem() == ModItems.TASER.get()) {
                return "" + GunsTool.getGunIntTag(stack, "MaxAmmo");
            }

            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            if (cap == null) return "";

            if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
                return "" + cap.rifleAmmo;
            }
            if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
                return "" + cap.handgunAmmo;
            }
            if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
                return "" + cap.shotgunAmmo;
            }
            if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
                return "" + cap.sniperAmmo;
            }
            if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
                return "" + cap.heavyAmmo;
            }
            return "";
        }

        return "∞";
    }

    private static String getGunAmmoType(ItemStack stack) {
        if (stack.getItem() == ModItems.BOCEK.get()) {
            return "Arrow";
        }
        if (stack.getItem() == ModItems.M_79.get() || stack.getItem() == ModItems.SECONDARY_CATACLYSM.get()) {
            return "40mm Grenade";
        }
        if (stack.getItem() == ModItems.RPG.get()) {
            return "Yassin105 TBG";
        }
        if (stack.getItem() == ModItems.JAVELIN.get()) {
            return "Javelin Missile";
        }
        if (stack.getItem() == ModItems.TASER.get()) {
            return "Electrode Rod";
        }
        if (stack.getItem() == ModItems.MINIGUN.get()) {
            return "Rifle Ammo";
        }
        if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
            return "Rifle Ammo";
        }
        if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
            return "Handgun Ammo";
        }
        if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
            return "Shotgun Ammo";
        }
        if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
            return "Sniper Ammo";
        }
        if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
            return "Heavy Ammo";
        }
        return "";
    }
}
