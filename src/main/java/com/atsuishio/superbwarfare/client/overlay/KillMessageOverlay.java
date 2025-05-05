package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.client.KillMessageConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.KillMessageHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import com.atsuishio.superbwarfare.tools.PlayerKillRecord;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.atomic.AtomicReference;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class KillMessageOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("kill_message");

    private static final ResourceLocation HEADSHOT = Mod.loc("textures/screens/damage_types/headshot.png");

    private static final ResourceLocation KNIFE = Mod.loc("textures/screens/damage_types/knife.png");
    private static final ResourceLocation EXPLOSION = Mod.loc("textures/screens/damage_types/explosion.png");
    private static final ResourceLocation CLAYMORE = Mod.loc("textures/screens/damage_types/claymore.png");
    private static final ResourceLocation GENERIC = Mod.loc("textures/screens/damage_types/generic.png");
    private static final ResourceLocation BEAST = Mod.loc("textures/screens/damage_types/beast.png");
    private static final ResourceLocation BLEEDING = Mod.loc("textures/screens/damage_types/bleeding.png");
    private static final ResourceLocation SHOCK = Mod.loc("textures/screens/damage_types/shock.png");
    private static final ResourceLocation BLOOD_CRYSTAL = Mod.loc("textures/screens/damage_types/blood_crystal.png");
    private static final ResourceLocation BURN = Mod.loc("textures/screens/damage_types/burn.png");
    private static final ResourceLocation DRONE = Mod.loc("textures/screens/damage_types/drone.png");
    private static final ResourceLocation LASER = Mod.loc("textures/screens/damage_types/laser.png");
    private static final ResourceLocation VEHICLE = Mod.loc("textures/screens/damage_types/vehicle_strike.png");

    private static final ResourceLocation WORLD_PEACE_STAFF = Mod.loc("textures/gun_icon/compat/world_peace_staff.png");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!KillMessageConfig.SHOW_KILL_MESSAGE.get()) {
            return;
        }

        Player player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        if (KillMessageHandler.QUEUE.isEmpty()) {
            return;
        }

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        var pos = KillMessageConfig.KILL_MESSAGE_POSITION.get();
        int posX = screenWidth;
        float posY = KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get();
        int count = KillMessageConfig.KILL_MESSAGE_COUNT.get();
        boolean left = false;

        switch (pos) {
            case LEFT_TOP -> {
                posX = KillMessageConfig.KILL_MESSAGE_MARGIN_X.get();
                posY = KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get();
                left = true;
            }
            case RIGHT_TOP -> {
                posX = screenWidth - KillMessageConfig.KILL_MESSAGE_MARGIN_X.get();
                posY = KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get();
            }
            case LEFT_BOTTOM -> {
                posX = KillMessageConfig.KILL_MESSAGE_MARGIN_X.get();
                posY = screenHeight - KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get() - count * 10;
                left = true;
            }
            case RIGHT_BOTTOM -> {
                posX = screenWidth - KillMessageConfig.KILL_MESSAGE_MARGIN_X.get();
                posY = screenHeight - KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get() - count * 10;
            }
        }

        var arr = KillMessageHandler.QUEUE.toArray(new PlayerKillRecord[0]);
        var record = arr[0];

        if (record.freeze) {
            for (var playerKillRecord : arr) {
                playerKillRecord.freeze = false;
            }
        }

        if (record.tick >= 80) {
            if (arr.length > 1 && record.tick - arr[1].tick < (record.fastRemove ? 2 : 20)) {
                arr[1].fastRemove = true;
                record.fastRemove = true;
                for (int j = 1; j < arr.length; j++) {
                    arr[j].freeze = true;
                }
            }
        }

        for (PlayerKillRecord r : KillMessageHandler.QUEUE) {
            posY = renderKillMessages(r, guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(true), posX, posY, left);
        }
    }

    private static float renderKillMessages(PlayerKillRecord record, GuiGraphics guiGraphics, float partialTick, int width, float baseTop, boolean left) {
        float top = baseTop;

        Font font = Minecraft.getInstance().font;

        AtomicReference<String> targetName = new AtomicReference<>(record.target.getDisplayName().getString());
        if (record.target instanceof Player targetPlayer) {
            CuriosApi.getCuriosInventory(targetPlayer)
                    .flatMap(c -> c.findFirstCurio(ModItems.DOG_TAG.get()))
                    .ifPresent(s -> targetName.set(s.stack().getHoverName().getString()));
        }

        int targetNameWidth = font.width(targetName.get());

        guiGraphics.pose().pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE
        );

        // 入场效果
        if (record.tick < 3) {
            guiGraphics.pose().translate((3 - record.tick - partialTick) * 33 * (left ? -1 : 1), 0, 0);
        }

        // 4s后开始消失
        if (record.tick >= 80) {
            int animationTickCount = record.fastRemove ? 2 : 20;
            float rate = (float) Math.pow((record.tick + partialTick - 80) / animationTickCount, 5);
            guiGraphics.pose().translate(rate * 100 * (left ? -1 : 1), 0, 0);
            guiGraphics.setColor(1, 1, 1, 1 - rate);
            baseTop += 10 * (1 - rate);
        } else {
            baseTop += 10;
        }

        // 击杀提示默认是右对齐的，这里从右向左渲染

        // 渲染被击杀者名称
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                targetName.get(),
                width - targetNameWidth - 10f,
                top,
                record.target.getTeamColor(),
                false
        );

        // 第一个图标：爆头/爆炸/近战等图标
        int damageTypeIconW = width - targetNameWidth - 28;

        ResourceLocation damageTypeIcon = getDamageTypeIcon(record);

        if (damageTypeIcon != null) {
            preciseBlit(guiGraphics,
                    damageTypeIcon,
                    damageTypeIconW,
                    top - 2,
                    0,
                    0,
                    12,
                    12,
                    12,
                    12
            );
        }

        Player player = record.attacker;
        boolean renderItem = false;
        int itemIconW = damageTypeIcon != null ? width - targetNameWidth - 64 : width - targetNameWidth - 46;

        if (player != null && player.getVehicle() instanceof VehicleEntity vehicleEntity) {
            // 载具图标
            if ((vehicleEntity instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player)) || record.damageType == ModDamageTypes.VEHICLE_STRIKE) {
                renderItem = true;

                ResourceLocation resourceLocation = vehicleEntity.getVehicleIcon();

                preciseBlit(guiGraphics,
                        resourceLocation,
                        itemIconW,
                        top,
                        0,
                        0,
                        32,
                        8,
                        -32,
                        8
                );
            } else {
                if (record.stack.getItem() instanceof GunItem gunItem) {
                    renderItem = true;

                    ResourceLocation resourceLocation = gunItem.getGunIcon();

                    preciseBlit(guiGraphics,
                            resourceLocation,
                            itemIconW,
                            top,
                            0,
                            0,
                            32,
                            8,
                            -32,
                            8
                    );
                }
            }
        } else {
            // 如果是枪械击杀，则渲染枪械图标
            if (record.stack.getItem() instanceof GunItem gunItem) {
                renderItem = true;

                ResourceLocation resourceLocation = gunItem.getGunIcon();

                preciseBlit(guiGraphics,
                        resourceLocation,
                        itemIconW,
                        top,
                        0,
                        0,
                        32,
                        8,
                        -32,
                        8
                );
            }

            // TODO 如果是特殊武器击杀，则渲染对应图标
            if (record.stack.getItem().getDescriptionId().equals("item.dreamaticvoyage.world_peace_staff")) {
                renderItem = true;

                preciseBlit(guiGraphics,
                        WORLD_PEACE_STAFF,
                        itemIconW,
                        top,
                        0,
                        0,
                        32,
                        8,
                        32,
                        8
                );
            }
        }

        // 渲染击杀者名称
        AtomicReference<String> attackerName = new AtomicReference<>(record.attacker.getDisplayName().getString());
        CuriosApi.getCuriosInventory(record.attacker)
                .flatMap(c -> c.findFirstCurio(ModItems.DOG_TAG.get()))
                .ifPresent(s -> attackerName.set(s.stack().getHoverName().getString()));

        int attackerNameWidth = font.width(attackerName.get());
        int nameW = width - targetNameWidth - 16 - attackerNameWidth;
        if (renderItem) {
            nameW -= 32;
        }
        if (damageTypeIcon != null) {
            nameW -= 18;
        }

        guiGraphics.drawString(
                Minecraft.getInstance().font,
                attackerName.get(),
                nameW,
                top,
                record.attacker.getTeamColor(),
                false
        );

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        guiGraphics.setColor(1, 1, 1, 1);
        guiGraphics.pose().popPose();

        return baseTop;
    }

    @Nullable
    private static ResourceLocation getDamageTypeIcon(PlayerKillRecord record) {
        ResourceLocation icon;
        // 渲染爆头图标
        if (record.headshot) {
            icon = HEADSHOT;
        } else {
            if (DamageTypeTool.isGunDamage(record.damageType)) {
                icon = null;
            } else {
                // 如果是其他伤害，则渲染对应图标
                if (record.damageType == DamageTypes.EXPLOSION || record.damageType == DamageTypes.PLAYER_EXPLOSION || record.damageType == ModDamageTypes.PROJECTILE_BOOM || record.damageType == DamageTypes.FIREWORKS) {
                    icon = EXPLOSION;
                } else if (record.damageType == DamageTypes.PLAYER_ATTACK) {
                    icon = KNIFE;
                } else if (record.damageType == ModDamageTypes.BEAST) {
                    icon = BEAST;
                } else if (record.damageType == ModDamageTypes.MINE) {
                    icon = CLAYMORE;
                } else if (record.damageType == ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "bleeding"))) {
                    icon = BLEEDING;
                } else if (record.damageType == ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("dreamaticvoyage", "blood_crystal"))) {
                    icon = BLOOD_CRYSTAL;
                } else if (record.damageType == ModDamageTypes.SHOCK) {
                    icon = SHOCK;
                } else if (record.damageType == ModDamageTypes.BURN || record.damageType == DamageTypes.IN_FIRE || record.damageType == DamageTypes.ON_FIRE || record.damageType == DamageTypes.LAVA) {
                    icon = BURN;
                } else if (record.damageType == ModDamageTypes.DRONE_HIT) {
                    icon = DRONE;
                } else if (record.damageType == ModDamageTypes.LASER || record.damageType == ModDamageTypes.LASER_HEADSHOT || record.damageType == ModDamageTypes.LASER_STATIC) {
                    icon = LASER;
                } else if (record.damageType == ModDamageTypes.VEHICLE_STRIKE) {
                    icon = VEHICLE;
                } else {
                    icon = GENERIC;
                }
            }
        }
        return icon;
    }
}
