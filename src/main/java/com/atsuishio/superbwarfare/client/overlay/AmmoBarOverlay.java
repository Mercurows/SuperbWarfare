package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModCapabilities;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;


public class AmmoBarOverlay implements LayeredDraw.Layer {
    public static final ResourceLocation ID = Mod.loc("ammo_bar");

    private static final ResourceLocation LINE = Mod.loc("textures/gun_icon/fire_mode/line.png");
    private static final ResourceLocation SEMI = Mod.loc("textures/gun_icon/fire_mode/semi.png");
    private static final ResourceLocation BURST = Mod.loc("textures/gun_icon/fire_mode/burst.png");
    private static final ResourceLocation AUTO = Mod.loc("textures/gun_icon/fire_mode/auto.png");
    private static final ResourceLocation TOP = Mod.loc("textures/gun_icon/fire_mode/top.png");
    private static final ResourceLocation DIR = Mod.loc("textures/gun_icon/fire_mode/dir.png");
    private static final ResourceLocation MOUSE = Mod.loc("textures/gun_icon/fire_mode/mouse.png");

    private static boolean hasCreativeAmmo() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        return InventoryTool.hasCreativeAmmoBox(player);
    }

    private static int getGunAmmoCount(Player player) {
        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() == ModItems.MINIGUN.get()) {
            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            return cap != null ? cap.rifleAmmo : 0;
        }

        if (stack.getItem() == ModItems.BOCEK.get()) {
            return GunData.from(stack).data().getInt("MaxAmmo");
        }

        return GunData.from(stack).ammo();
    }

    private static String getPlayerAmmoCount(Player player) {
        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() == ModItems.MINIGUN.get() || stack.getItem() == ModItems.BOCEK.get()) {
            return "";
        }

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) cap = new PlayerVariable();
        if (!hasCreativeAmmo()) {
            var data = GunData.from(stack);
            if (stack.is(ModTags.Items.LAUNCHER) || stack.getItem() == ModItems.TASER.get()) {
                return "" + data.data().getInt("MaxAmmo");
            }
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

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!DisplayConfig.AMMO_HUD.get()) return;

        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        Player player = Minecraft.getInstance().player;

        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getMainHandItem();
        final var tag = NBTTool.getTag(stack);
        if (stack.getItem() instanceof GunItem gunItem && !(player.getVehicle() instanceof ArmedVehicleEntity vehicle && vehicle.banHand(player))) {
            PoseStack poseStack = guiGraphics.pose();
            var data = GunData.from(stack);

            // 渲染图标
            guiGraphics.blit(gunItem.getGunIcon(),
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
                guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "[" + ModKeyMappings.FIRE_MODE.getKey().getDisplayName().getString() + "]",
                        w - 111.5f,
                        h - 20,
                        0xFFFFFF,
                        false
                );
            }

            // 渲染开火模式
            ResourceLocation fireMode = getFireMode(data);

            if (stack.getItem() == ModItems.JAVELIN.get()) {
                fireMode = tag.getBoolean("TopMode") ? TOP : DIR;
            }

            if (stack.getItem() == ModItems.MINIGUN.get()) {
                fireMode = MOUSE;
                // 渲染加特林射速
                guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        data.rpm() + " RPM",
                        w - 111f,
                        h - 20,
                        0xFFFFFF,
                        false
                );

                guiGraphics.blit(fireMode,
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
                    guiGraphics.blit(fireMode,
                            w - 95,
                            h - 21,
                            0,
                            0,
                            8,
                            8,
                            8,
                            8);
                } else {
                    guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            tag.getBoolean("DA") ? Component.translatable("des.superbwarfare.revolver.sa").withStyle(ChatFormatting.BOLD) : Component.translatable("des.superbwarfare.revolver.da").withStyle(ChatFormatting.BOLD),
                            w - 96,
                            h - 20,
                            0xFFFFFF,
                            false
                    );
                }
            }

            if (stack.getItem() != ModItems.MINIGUN.get() && stack.getItem() != ModItems.TRACHELIUM.get()) {
                guiGraphics.blit(LINE,
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
                guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "∞",
                        w / 1.5f - 64 / 1.5f,
                        h / 1.5f - 48 / 1.5f,
                        0xFFFFFF,
                        true
                );
            } else {
                guiGraphics.drawString(
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
            guiGraphics.drawString(
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
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    gunName,
                    w / 0.9f - (100 + Minecraft.getInstance().font.width(gunName) / 2f) / 0.9f,
                    h / 0.9f - 60 / 0.9f,
                    0xFFFFFF,
                    true
            );

            // 渲染弹药类型
            String ammoName = gunItem.getAmmoDisplayName(stack);
            guiGraphics.drawString(
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

    private static ResourceLocation getFireMode(GunData data) {
        return switch (data.fireMode()) {
            case 1 -> BURST;
            case 2 -> AUTO;
            default -> SEMI;
        };
    }
}
