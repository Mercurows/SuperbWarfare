package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.common.GameplayConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.receive.SimulationDistanceMessage;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class PlayerEventHandler {

    public static final ResourceLocation TACTICAL_SPRINT = Mod.loc("tactical_sprint");

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        ItemStack mainStack = player.getMainHandItem();
        var tag = NBTTool.getTag(mainStack);
        if (mainStack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using")) {
            tag.putBoolean("Using", false);
            NBTTool.saveTag(mainStack, tag);
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof GunItem) {
                var data = GunData.from(stack);
                data.draw.set(true);
                data.save();
            }
        }

        handleSimulationDistance(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();

        handleRespawnReload(player);
        handleRespawnAutoArmor(player);

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof GunItem) {
                var data = GunData.from(stack);
                data.draw.set(true);
                data.save();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() instanceof GunItem) {
            handleSpecialWeaponAmmo(player);
        }

        if (!player.level().isClientSide) {
            handleTacticalAttribute(player);
        }
    }

    private static void handleSpecialWeaponAmmo(Player player) {
        ItemStack stack = player.getMainHandItem();

        var data = GunData.from(stack);

        if ((stack.is(ModItems.RPG.get()) || stack.is(ModItems.BOCEK.get())) && data.ammo.get() == 1) {
            data.isEmpty.set(false);
        }
    }

    private static void handleSimulationDistance(Player player) {
        if (player.level() instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            var distance = serverLevel.getChunkSource().chunkMap.serverViewDistance;
            PacketDistributor.sendToPlayer(serverPlayer, new SimulationDistanceMessage(distance));
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
                data.save();
            }
        }
    }

    private static void handleRespawnAutoArmor(Player player) {
        if (!GameplayConfig.RESPAWN_AUTO_ARMOR.get()) return;

        ItemStack armor = player.getItemBySlot(EquipmentSlot.CHEST);
        if (armor == ItemStack.EMPTY) return;

        var tag = NBTTool.getTag(armor);
        double armorPlate = tag.getDouble("ArmorPlate");

        int armorLevel = MiscConfig.DEFAULT_ARMOR_LEVEL.get();
        if (armor.is(ModTags.Items.MILITARY_ARMOR)) {
            armorLevel = MiscConfig.MILITARY_ARMOR_LEVEL.get();
        } else if (armor.is(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            armorLevel = MiscConfig.HEAVY_MILITARY_ARMOR_LEVEL.get();
        }

        if (armorPlate >= armorLevel * MiscConfig.ARMOR_PONT_PER_LEVEL.get()) return;

        for (var stack : player.getInventory().items) {
            if (stack.is(ModItems.ARMOR_PLATE.get())) {
                var stackTag = NBTTool.getTag(stack);
                if (stackTag.getBoolean("Infinite")) {
                    tag.putDouble("ArmorPlate", armorLevel * MiscConfig.ARMOR_PONT_PER_LEVEL.get());
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.5f, 1);
                    }
                } else {
                    for (int index0 = 0; index0 < Math.ceil(((armorLevel * MiscConfig.ARMOR_PONT_PER_LEVEL.get()) - armorPlate) / MiscConfig.ARMOR_PONT_PER_LEVEL.get()); index0++) {
                        stack.finishUsingItem(player.level(), player);
                    }
                }
            }
        }

        NBTTool.saveTag(armor, tag);
    }

    public static void handleTacticalAttribute(Player player) {
        if (player == null) {
            return;
        }
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        if (attr.getModifier(TACTICAL_SPRINT) != null) {
            attr.removeModifier(TACTICAL_SPRINT);
        }

        if (MiscConfig.ALLOW_TACTICAL_SPRINT.get() && player.getData(ModAttachments.PLAYER_VARIABLE).tacticalSprint) {
            player.setSprinting(true);
            attr.addTransientModifier(new AttributeModifier(TACTICAL_SPRINT, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
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
            data.save();

            event.setOutput(output);
            event.setCost(10);
            event.setMaterialCost(1);
        }
    }
}
