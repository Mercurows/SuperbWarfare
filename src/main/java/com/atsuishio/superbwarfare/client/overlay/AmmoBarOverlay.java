package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.language.ClientLanguageGetter;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    private static final ResourceLocation CHOSEN = Mod.loc("textures/gui/attachment/chosen.png");
    private static final ResourceLocation NOT_CHOSEN = Mod.loc("textures/gui/attachment/not_chosen.png");
    private static final ResourceLocation AMMO_STACK = Mod.loc("textures/gui/attachment/ammo_stack.png");

    private static final ItemStack BARRIER_STACK = new ItemStack(Items.BARRIER);

    private static ResourceLocation getFireMode(GunData data) {
        return switch (data.fireMode.get()) {
            case SEMI -> SEMI;
            case BURST -> BURST;
            case AUTO -> AUTO;
        };
    }

    private static String getGunAmmoString(GunData data, Player player) {
        if (data.meleeOnly() || data.useBackpackAmmo() && data.hasInfiniteBackupAmmo(player)) return "∞";
        return data.useBackpackAmmo() ? data.countBackupAmmo(player) - data.virtualAmmo.get() + "" : data.ammo.get() + "";
    }

    private static String getBackupAmmoString(GunData data, Player player) {
        if (data.meleeOnly() || data.useBackpackAmmo()) return "";
        return data.hasInfiniteBackupAmmo(player) ? "∞" : data.countBackupAmmo(player) - data.virtualAmmo.get() + "";
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

            var font = Minecraft.getInstance().font;

            // 渲染开火模式切换按键
            if (stack.getItem() != ModItems.MINIGUN.get()) {
                guiGraphics.drawString(
                        font,
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
                        font,
                        data.get(GunProp.RPM) + " RPM",
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
            var ammoConsumer = data.selectedAmmoConsumer();
            RenderHelper.preciseBlit(guiGraphics, AMMO_STACK,
                    x - 62,
                    y - 22,
                    0,
                    0,
                    16,
                    10,
                    16,
                    16
            );

            poseStack.pushPose();

            // 物品
            poseStack.translate(x - 57, y - 22, 0);
            poseStack.scale(0.75f, 0.75f, 1f);

            // TODO 优化物品渲染效果
            var renderStackCount = ammoConsumer.type == AmmoConsumer.AmmoConsumeType.ITEM;
            var ammoStack = renderStackCount ? ammoConsumer.stack() : BARRIER_STACK;
            guiGraphics.renderFakeItem(ammoStack, 0, 0);

            if (renderStackCount) {
                // 数量
                var text = "" + data.countBackupAmmoItem(player);
                guiGraphics.drawString(
                        font,
                        text,
                        18,
                        8,
                        0xFFFFFF,
                        true
                );
            }

            poseStack.popPose();

            // 如果弹药种类大于1，渲染弹种信息
            int size = data.ammoConsumers.size();
            if (size > 1) {
                float offset = 50.5f;
                int count = size / 2;
                float posX = size % 2 == 0 ? x - count * 6 + 1 : x - count * 6 - 2;
                float posY = y - 8;
                for (int i = 0; i < size; i++) {
                    RenderHelper.preciseBlit(guiGraphics,
                            i == data.selectedAmmoType.get() ? CHOSEN : NOT_CHOSEN,
                            posX - offset + 6 * i, posY, 0, 0,
                            4, 4, 4, 4);
                }
            }

            poseStack.pushPose();
            poseStack.scale(1.5f, 1.5f, 1f);

            // 渲染当前弹药量
            var gunAmmoY = data.useBackpackAmmo() ? y - 35 : y + 5 - 48;

            guiGraphics.drawString(
                    font,
                    getGunAmmoString(data, player),
                    x / 1.5f - 64 / 1.5f,
                    gunAmmoY / 1.5F,
                    0xFFFFFF,
                    true
            );

            poseStack.popPose();

            // 虚拟弹药备弹
            if (data.virtualAmmo.get() > 0) {
                guiGraphics.drawString(
                        font,
                        "+" + data.virtualAmmo.get(),
                        x - 62 + font.width(getGunAmmoString(data, player)) * 1.5f,
                        y - 46,
                        0x55FFFF,
                        true
                );
            }

            // 渲染备弹量
            guiGraphics.drawString(
                    font,
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
                    font,
                    gunName,
                    x / 0.9f - (100 + font.width(gunName) / 2f) / 0.9f,
                    y / 0.9f - 60 / 0.9f,
                    0xFFFFFF,
                    true
            );

            // 渲染弹药类型
            var ammoName = getAmmoDisplayName(data);

            guiGraphics.drawString(
                    font,
                    ammoName,
                    x / 0.9f - (100 + font.width(ammoName) / 2f) / 0.9f,
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

    private static String getAmmoDisplayName(GunData data) {
        var consumer = data.selectedAmmoConsumer();
        if (consumer.type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
            return consumer.getPlayerAmmoType().displayName;
        } else if (consumer.type == AmmoConsumer.AmmoConsumeType.INFINITE) {
            return "Infinity";
        } else if (data.meleeOnly()) {
            return "Melee";
        } else if (!consumer.stack().isEmpty()) {
            return ClientLanguageGetter.EN_US.getOrDefault(consumer.stack().getDescriptionId());
        } else {
            return "";
        }
    }
}
