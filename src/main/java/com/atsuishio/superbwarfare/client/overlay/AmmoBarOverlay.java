package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.language.ClientLanguageGetter;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class AmmoBarOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_ammo_bar";

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
        return player.isCreative() || InventoryTool.hasCreativeAmmoBox(player);
    }

    private static ResourceLocation getFireMode(GunData data) {
        return switch (data.fireMode.get()) {
            case SEMI -> SEMI;
            case BURST -> BURST;
            case AUTO -> AUTO;
        };
    }

    private static String getGunAmmoString(GunData data, Player player) {
        if (data.useBackpackAmmo() && hasCreativeAmmo()) return "∞";
        return data.useBackpackAmmo() ? data.countBackupAmmo(player) - data.virtualAmmo.get() + "" : data.ammo.get() + "";
    }

    private static String getBackupAmmoString(GunData data, Player player) {
        if (data.useBackpackAmmo()) return "";
        return hasCreativeAmmo() ? "∞" : data.countBackupAmmo(player) - data.virtualAmmo.get() + "";
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!DisplayConfig.AMMO_HUD.get()) return;

        Player player = gui.getMinecraft().player;

        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem gunItem && !(player.getVehicle() instanceof ArmedVehicleEntity vehicle && vehicle.banHand(player))) {
            int x = screenWidth + DisplayConfig.WEAPON_HUD_X_OFFSET.get();
            int y = screenHeight + DisplayConfig.WEAPON_HUD_Y_OFFSET.get();

            PoseStack poseStack = guiGraphics.pose();
            var data = GunData.from(stack);

            // 渲染图标
            guiGraphics.blit(gunItem.getGunIcon(stack),
                    x - 135,
                    y - 40,
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
                        x - 111.5f,
                        y - 20,
                        0xFFFFFF,
                        false
                );
            }

            // 渲染开火模式
            ResourceLocation fireMode = getFireMode(data);

            if (stack.getItem() == ModItems.JAVELIN.get()) {
                fireMode = stack.getOrCreateTag().getBoolean("TopMode") ? TOP : DIR;
            }

            if (stack.getItem() == ModItems.MINIGUN.get()) {
                fireMode = MOUSE;
                // 渲染加特林射速
                guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        data.rpm() + " RPM",
                        x - 111f,
                        y - 20,
                        0xFFFFFF,
                        false
                );

                guiGraphics.blit(fireMode,
                        x - 126,
                        y - 22,
                        0,
                        0,
                        12,
                        12,
                        12,
                        12);
            } else {
                guiGraphics.blit(fireMode,
                        x - 95,
                        y - 21,
                        0,
                        0,
                        8,
                        8,
                        8,
                        8);
            }

            if (stack.getItem() != ModItems.MINIGUN.get()) {
                guiGraphics.blit(LINE,
                        x - 95,
                        y - 16,
                        0,
                        0,
                        8,
                        8,
                        8,
                        8);
            }

            // 如果当前弹药为物品，渲染备弹物品数量
            if (!data.selectedAmmoConsumer().stack().isEmpty()) {
                poseStack.pushPose();

                // 物品
                poseStack.translate(x - 80, y - 22, 0);
                poseStack.scale(0.75f, 0.75f, 1f);
                guiGraphics.renderFakeItem(data.selectedAmmoConsumer().stack(), 0, 0);

                // 数量
                var text = "" + data.countBackupAmmoItem(player);
                var length = Minecraft.getInstance().font.width(text);
                poseStack.translate(16 - length, 8, 0);

                guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        text,
                        0,
                        0,
                        0xFFFFFF,
                        true
                );

                poseStack.popPose();
            }

            // 渲染当前弹药量
            poseStack.pushPose();
            poseStack.scale(1.5f, 1.5f, 1f);

            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    getGunAmmoString(data, player),
                    x / 1.5f - 64 / 1.5f,
                    (y + 5) / 1.5f - 48 / 1.5f,
                    0xFFFFFF,
                    true
            );

            poseStack.popPose();

            if (data.virtualAmmo.get() > 0) {
                guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "+" + data.virtualAmmo.get(),
                        x - 64,
                        y - 21,
                        0x55FFFF,
                        true
                );
            }

            // 渲染备弹量
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    getBackupAmmoString(data, player),
                    x - 64,
                    y - 30,
                    0xCCCCCC,
                    true
            );

            poseStack.pushPose();
            poseStack.scale(0.9f, 0.9f, 1f);

            // 渲染物品名称
            String gunName = getGunDisplayName(stack);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    gunName,
                    x / 0.9f - (100 + Minecraft.getInstance().font.width(gunName) / 2f) / 0.9f,
                    y / 0.9f - 60 / 0.9f,
                    0xFFFFFF,
                    true
            );

            // 渲染弹药类型
            var ammoName = getAmmoDisplayName(data, data.selectedAmmoConsumer().stack());

            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    ammoName,
                    x / 0.9f - (100 + Minecraft.getInstance().font.width(ammoName) / 2f) / 0.9f,
                    y / 0.9f - 51 / 0.9f,
                    0xC8A679,
                    true
            );

            poseStack.popPose();
        }
    }

    private static String getGunDisplayName(ItemStack stack) {
        if (!stack.isEmpty()) {
            return ClientLanguageGetter.EN_US.getOrDefault(stack.getDescriptionId());
        } else {
            return "";
        }
    }

    private static String getAmmoDisplayName(GunData data, ItemStack stack) {
        var type = data.ammoTypeInfo().playerAmmoType();
        if (type != null) {
            return type.displayName;
        } else if (!stack.isEmpty()) {
            return ClientLanguageGetter.EN_US.getOrDefault(stack.getDescriptionId());
        } else {
            return "";
        }
    }
}
