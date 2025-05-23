package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen;
import com.atsuishio.superbwarfare.client.tooltip.ClientDogTagImageTooltip;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.config.client.KillMessageConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.KillMessageHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.DogTag;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import com.atsuishio.superbwarfare.tools.PlayerKillRecord;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.ParametersAreNonnullByDefault;

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
        boolean left = false;
        boolean bottom = false;

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
                posY = screenHeight - KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get() - 10;
                left = true;
                bottom = true;
            }
            case RIGHT_BOTTOM -> {
                posX = screenWidth - KillMessageConfig.KILL_MESSAGE_MARGIN_X.get();
                posY = screenHeight - KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get() - 10;
                bottom = true;
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
            posY = renderKillMessages(r, guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(true), posX, posY, left, bottom);
        }
    }

    private static float renderKillMessages(PlayerKillRecord record, GuiGraphics guiGraphics, float partialTick, int width, float baseTop, boolean left, boolean bottom) {
        float top = baseTop;

        Font font = Minecraft.getInstance().font;

        String targetName = getEntityName(record.target);
        int targetNameWidth = font.width(targetName);

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
            baseTop += 10 * (1 - rate) * (bottom ? -1 : 1);
        } else {
            baseTop += 10 * (bottom ? -1 : 1);
        }

        // 击杀提示默认是右对齐的，这里从右向左渲染
        if (!left) {
            float currentPosX = width - targetNameWidth - 10f;

            // 渲染被击杀者名称
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    targetName,
                    currentPosX,
                    top,
                    record.target.getTeamColor(),
                    false
            );

            // 渲染狗牌图标
            if (record.target instanceof LivingEntity living && shouldRenderDogTagIcon(living)) {
                currentPosX -= 14;
                renderDogTagIcon(guiGraphics, living, currentPosX, top - 0.5f);
            }

            // 渲染伤害类型图标
            ResourceLocation damageTypeIcon = getDamageTypeIcon(record);
            if (damageTypeIcon != null) {
                currentPosX -= 18;
                preciseBlit(guiGraphics,
                        damageTypeIcon,
                        currentPosX,
                        top - 2,
                        0,
                        0,
                        12,
                        12,
                        12,
                        12
                );
            }

            // 渲染武器图标
            ResourceLocation currentWeaponIcon = getWeaponIcon(record);
            if (currentWeaponIcon != null) {
                currentPosX -= 36;
                preciseBlit(guiGraphics,
                        currentWeaponIcon,
                        currentPosX,
                        top,
                        0,
                        0,
                        32,
                        8,
                        -32,
                        8
                );
            }

            // 渲染击杀者名称
            String attackerName = getEntityName(record.attacker);
            currentPosX -= font.width(attackerName) + 6;

            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    attackerName,
                    currentPosX,
                    top,
                    record.attacker.getTeamColor(),
                    false
            );

            // 渲染狗牌图标
            if (shouldRenderDogTagIcon(record.attacker)) {
                currentPosX -= 14;
                renderDogTagIcon(guiGraphics, record.attacker, currentPosX, top - 0.5f);
            }
        } else {
            float currentPosX = width + 10f;

            // 渲染狗牌图标
            if (shouldRenderDogTagIcon(record.attacker)) {
                renderDogTagIcon(guiGraphics, record.attacker, currentPosX, top - 0.5f);
                currentPosX += 14;
            }

            // 渲染击杀者名称
            String attackerName = getEntityName(record.attacker);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    attackerName,
                    currentPosX,
                    top,
                    record.attacker.getTeamColor(),
                    false
            );

            currentPosX += font.width(attackerName) + 6;

            // 渲染武器图标
            ResourceLocation currentWeaponIcon = getWeaponIcon(record);
            if (currentWeaponIcon != null) {
                preciseBlit(guiGraphics,
                        currentWeaponIcon,
                        currentPosX,
                        top,
                        0,
                        0,
                        32,
                        8,
                        -32,
                        8
                );
                currentPosX += 36;
            }

            // 渲染伤害类型图标
            ResourceLocation damageTypeIcon = getDamageTypeIcon(record);
            if (damageTypeIcon != null) {
                preciseBlit(guiGraphics,
                        damageTypeIcon,
                        currentPosX,
                        top - 2,
                        0,
                        0,
                        12,
                        12,
                        12,
                        12
                );
                currentPosX += 18;
            }

            // 渲染狗牌图标
            if (record.target instanceof LivingEntity living && shouldRenderDogTagIcon(living)) {
                renderDogTagIcon(guiGraphics, living, currentPosX, top - 0.5f);
                currentPosX += 14;
            }

            // 渲染被击杀者名称
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    targetName,
                    currentPosX,
                    top,
                    record.target.getTeamColor(),
                    false
            );
        }

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

    public static String getEntityName(Entity entity) {
        if (entity.getDisplayName() == null) return "";

        var name = entity.getDisplayName().getString();
        if (!DisplayConfig.DOG_TAG_NAME_VISIBLE.get()) return name;

        if (entity instanceof Player targetPlayer) {
            return CuriosApi.getCuriosInventory(targetPlayer)
                    .flatMap(c -> c.findFirstCurio(ModItems.DOG_TAG.get()))
                    .map(s -> s.stack().getHoverName().getString())
                    .orElse(name);
        }
        return name;
    }

    @Nullable
    public static ResourceLocation getWeaponIcon(PlayerKillRecord record) {
        Player player = record.attacker;
        if (player != null && player.getVehicle() instanceof VehicleEntity vehicleEntity) {
            // 载具图标
            if ((vehicleEntity instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player)) || record.damageType == ModDamageTypes.VEHICLE_STRIKE) {
                return vehicleEntity.getVehicleIcon();
            } else {
                if (record.stack.getItem() instanceof GunItem gunItem) {
                    return gunItem.getGunIcon();
                }
            }
        } else {
            // 如果是枪械击杀，则渲染枪械图标
            if (record.stack.getItem() instanceof GunItem gunItem) {
                return gunItem.getGunIcon();
            }

            // TODO 如果是特殊武器击杀，则渲染对应图标
            if (record.stack.getItem().getDescriptionId().equals("item.dreamaticvoyage.world_peace_staff")) {
                return WORLD_PEACE_STAFF;
            }
        }
        return null;
    }

    public static boolean shouldRenderDogTagIcon(LivingEntity living) {
        return CuriosApi.getCuriosInventory(living)
                .flatMap(c -> c.findFirstCurio(ModItems.DOG_TAG.get()))
                .map(s -> ClientDogTagImageTooltip.shouldRenderIcon(s.stack()))
                .orElse(false)
                && DisplayConfig.DOG_TAG_ICON_VISIBLE.get();
    }

    public static void renderDogTagIcon(GuiGraphics guiGraphics, LivingEntity living, float x, float y) {
        CuriosApi.getCuriosInventory(living).flatMap(c -> c.findFirstCurio(ModItems.DOG_TAG.get())).ifPresent(s -> {
            short[][] icon = DogTag.getColors(s.stack());
            guiGraphics.pose().pushPose();

            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    if (icon[i][j] == -1) continue;

                    var color = ChatFormatting.getById(icon[i][j]);
                    RenderHelper.fill(guiGraphics, RenderType.gui(),
                            x + i * 0.6f, y + j * 0.6f, x + (i + 1) * 0.6f, y + (j + 1) * 0.6f,
                            0, DogTagEditorScreen.getColorFromFormatting(color)
                    );
                }
            }

            guiGraphics.pose().popPose();
        });
    }
}
