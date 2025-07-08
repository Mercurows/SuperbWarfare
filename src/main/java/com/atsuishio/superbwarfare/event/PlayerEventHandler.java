package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.config.common.GameplayConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber
public class PlayerEventHandler {

    public static final UUID TACTICAL_SPRINT_UUID = UUID.fromString("fe8a1213-cf3d-4ec2-8ea8-29acca64b301");

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")) {
            stack.getOrCreateTag().putBoolean("Using", false);
        }
        for (ItemStack pStack : player.getInventory().items) {
            if (pStack.getItem() instanceof GunItem) {
                var data = GunData.from(pStack);
                data.draw.set(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();

        if (player == null) {
            return;
        }

        handleRespawnReload(player);
        handleRespawnAutoArmor(player);

        for (ItemStack pStack : player.getInventory().items) {
            if (pStack.getItem() instanceof GunItem) {
                GunData.from(pStack).draw.set(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (player == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        if (event.phase == TickEvent.Phase.END) {
            if (stack.getItem() instanceof GunItem) {
                handleSpecialWeaponAmmo(player);
            }

            if (event.side.isServer()) {
                handleTacticalAttribute(player);
            }
        }
    }

    private static void handleSpecialWeaponAmmo(Player player) {
        ItemStack stack = player.getMainHandItem();
        var data = GunData.from(stack);

        if ((stack.is(ModItems.RPG.get()) || stack.is(ModItems.BOCEK.get())) && data.ammo.get() == 1) {
            GunData.from(stack).isEmpty.set(false);
        }
    }

    private static void handleRespawnReload(Player player) {
        if (!GameplayConfig.RESPAWN_RELOAD.get()) return;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof GunItem) {
                var data = GunData.from(stack);
                if (!InventoryTool.hasCreativeAmmoBox(player)) {
                    data.reload(player);
                } else {
                    data.ammo.set(data.magazine());
                }
                data.holdOpen.set(false);
            }
        }
    }

    private static void handleRespawnAutoArmor(Player player) {
        if (!GameplayConfig.RESPAWN_AUTO_ARMOR.get()) return;

        ItemStack armor = player.getItemBySlot(EquipmentSlot.CHEST);
        if (armor == ItemStack.EMPTY) return;

        double armorPlate = armor.getOrCreateTag().getDouble("ArmorPlate");

        int armorLevel = MiscConfig.DEFAULT_ARMOR_LEVEL.get();
        if (armor.is(ModTags.Items.MILITARY_ARMOR)) {
            armorLevel = MiscConfig.MILITARY_ARMOR_LEVEL.get();
        } else if (armor.is(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            armorLevel = MiscConfig.HEAVY_MILITARY_ARMOR_LEVEL.get();
        }

        if (armorPlate < armorLevel * MiscConfig.ARMOR_PONT_PER_LEVEL.get()) {
            for (var stack : player.getInventory().items) {
                if (stack.is(ModItems.ARMOR_PLATE.get())) {
                    if (stack.getTag() != null && stack.getTag().getBoolean("Infinite")) {
                        armor.getOrCreateTag().putDouble("ArmorPlate", armorLevel * MiscConfig.ARMOR_PONT_PER_LEVEL.get());

                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.5f, 1);
                        }
                    } else {
                        for (int index0 = 0; index0 < Math.ceil(((armorLevel * MiscConfig.ARMOR_PONT_PER_LEVEL.get()) - armorPlate) / MiscConfig.ARMOR_PONT_PER_LEVEL.get()); index0++) {
                            stack.finishUsingItem(player.level(), player);
                        }
                    }
                }
            }
        }
    }

    public static void handleTacticalAttribute(Player player) {
        if (player == null) {
            return;
        }
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        if (attr.getModifier(TACTICAL_SPRINT_UUID) != null) {
            attr.removeModifier(TACTICAL_SPRINT_UUID);
        }

        if (MiscConfig.ALLOW_TACTICAL_SPRINT.get() && player.getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable()).tacticalSprint) {
            player.setSprinting(true);
            attr.addTransientModifier(new AttributeModifier(TACTICAL_SPRINT_UUID, Mod.ATTRIBUTE_MODIFIER,
                    0.25, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.getItem() instanceof GunItem && right.getItem() == ModItems.SHORTCUT_PACK.get()) {
            ItemStack output = left.copy();
            var data = GunData.from(output);

            data.upgradePoint.set(data.upgradePoint.get() + 1);

            event.setOutput(output);
            event.setCost(10);
            event.setMaterialCost(1);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        var target = event.getTarget();
        if (target instanceof VehicleEntity vehicle) {
            Vec3 position = TraceTool.playerFindLookingPos(event.getEntity(), vehicle, event.getEntity().getEntityReach());

            if (position != null) {
                if (vehicle.shouldSendHitSounds()) {
                    vehicle.level().playSound(null, BlockPos.containing(position), ModSounds.HIT.get(), SoundSource.PLAYERS, 1, 1);
                }

                if (vehicle.shouldSendHitParticles() && vehicle.level() instanceof ServerLevel serverLevel) {
                    sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), position.x, position.y, position.z,
                            2, 0, 0, 0, 0.2, false);
                }
            }
        }
    }
}
