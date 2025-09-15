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
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class AmmoBarOverlay implements LayeredDraw.Layer {
    public static final ResourceLocation ID = Mod.loc("ammo_bar");

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

    private static ResourceLocation getFireMode(GunData data) {
        return switch (data.fireMode.get()) {
            case SEMI -> SEMI;
            case BURST -> BURST;
            case AUTO -> AUTO;
        };
    }

    private static String getGunAmmoString(GunData data, Player player) {
        if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.ENERGY) {
            // TODO 修改为dynamic energy storage
            var cap = data.stack.getCapability(Capabilities.EnergyStorage.ITEM);
            double energy = cap != null ? (double) cap.getEnergyStored() / cap.getMaxEnergyStored() : 0;
            return (int) (energy * 100) + "%";
        }
        if (data.meleeOnly() || data.useBackpackAmmo() && data.hasInfiniteBackupAmmo(player)) return "∞";
        return data.useBackpackAmmo() ? data.countBackupAmmo(player) - data.virtualAmmo.get() + "" : data.ammo.get() + "";
    }

    private static String getBackupAmmoString(GunData data, Player player) {
        if (data.meleeOnly() || data.useBackpackAmmo() || data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.ENERGY)
            return "";
        return data.hasInfiniteBackupAmmo(player) ? "∞" : data.countBackupAmmo(player) - data.virtualAmmo.get() + "";
    }

    private static final Pattern REPLACE_FORMAT_CODE = Pattern.compile("§.");

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
        if (stack.getItem() instanceof GunItem gunItem && !(player.getVehicle() instanceof ArmedVehicleEntity vehicle && vehicle.banHand(player))) {
            final var tag = NBTTool.getTag(stack);
            int x = w + DisplayConfig.WEAPON_HUD_X_OFFSET.get();
            int y = h + DisplayConfig.WEAPON_HUD_Y_OFFSET.get();

            PoseStack poseStack = guiGraphics.pose();
            var data = GunData.from(stack);

            // 渲染图标
            guiGraphics.blit(gunItem.getGunIcon(data),
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
                fireMode = tag.getBoolean("TopMode") ? TOP : DIR;
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

            // 如果弹药种类大于1，渲染弹种信息
            int size = data.ammoConsumers.size();
            if (DisplayConfig.ADVANCED_AMMO_HUD.get()
                    && (size > 1 || size == 1 && data.selectedAmmoConsumer().type != AmmoConsumer.AmmoConsumeType.PLAYER_AMMO)
            ) {
                // 如果当前弹药为物品，渲染备弹物品数量
                var ammoConsumer = data.selectedAmmoConsumer();
                RenderHelper.preciseBlit(guiGraphics, AMMO_STACK,
                        x - 62,
                        y - 20.5f,
                        0,
                        0,
                        24,
                        8.5f,
                        24,
                        24
                );

                poseStack.pushPose();

                // 物品
                poseStack.translate(x - 57, y - 21, 0);
                poseStack.scale(0.75f, 0.75f, 1f);

                var consumerType = ammoConsumer.type;
                var renderStackCount = consumerType == AmmoConsumer.AmmoConsumeType.ITEM || consumerType == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO;
                if (renderStackCount) {
                    ItemStack ammoStack;
                    if (consumerType == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                        var ammoType = ammoConsumer.getPlayerAmmoType();
                        ammoStack = switch (ammoType) {
                            case HANDGUN -> new ItemStack(ModItems.HANDGUN_AMMO.get());
                            case RIFLE -> new ItemStack(ModItems.RIFLE_AMMO.get());
                            case SHOTGUN -> new ItemStack(ModItems.SHOTGUN_AMMO.get());
                            case SNIPER -> new ItemStack(ModItems.SNIPER_AMMO.get());
                            case HEAVY -> new ItemStack(ModItems.HEAVY_AMMO.get());
                        };
                    } else {
                        ammoStack = ammoConsumer.stack();
                    }

                    poseStack.translate(1.75f, 0, 0);
                    guiGraphics.renderFakeItem(ammoStack, 3, -1);
                    poseStack.translate(-1.75f, 0, 0);

                    // 数量
                    var text = "" + data.countBackupAmmoItem(player);
                    guiGraphics.drawString(
                            font,
                            text,
                            24,
                            8,
                            0xFFFFFF,
                            true
                    );
                }

                poseStack.popPose();

                // 这里不能和上面合并
                if (!renderStackCount) {
                    if (consumerType == AmmoConsumer.AmmoConsumeType.INVALID) {
                        RenderHelper.preciseBlit(guiGraphics, AMMO_STACK,
                                x - 50,
                                y - 19.5f,
                                12,
                                8.5f,
                                5,
                                8,
                                24,
                                24
                        );
                    } else if (consumerType == AmmoConsumer.AmmoConsumeType.ENERGY) {
                        RenderHelper.preciseBlit(guiGraphics, AMMO_STACK,
                                x - 50,
                                y - 19.5f,
                                12,
                                16.5f,
                                5,
                                8,
                                24,
                                24
                        );
                    } else {
                        RenderHelper.preciseBlit(guiGraphics, AMMO_STACK,
                                x - 51f,
                                y - 20,
                                0,
                                8.5f,
                                7,
                                8,
                                24,
                                24
                        );
                    }
                }

                // 渲染弹药种类切换提示
                if (size > 1) {
                    float offset = 47f;
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
            }

            poseStack.pushPose();
            poseStack.scale(1.5f, 1.5f, 1f);

            // 渲染当前弹药量
            var gunAmmoY = data.useBackpackAmmo() ? y - 38 : y + 5 - 48;

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
            if (data.virtualAmmo.get() > 0 && !data.meleeOnly()) {
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
            var ammoName = REPLACE_FORMAT_CODE.matcher(getAmmoDisplayName(data)).replaceAll("");

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
        } else if (consumer.type == AmmoConsumer.AmmoConsumeType.ENERGY) {
            return "Energy";
        } else if (!consumer.stack().isEmpty()) {
            var nameComponent = consumer.stack().getHoverName();
            if (nameComponent.getContents() instanceof TranslatableContents translatableComponent) {
                return ClientLanguageGetter.EN_US.getOrDefault(translatableComponent.getKey());
            }

            return ClientLanguageGetter.EN_US.getOrDefault(consumer.stack().getDescriptionId());
        } else {
            return "";
        }
    }
}
