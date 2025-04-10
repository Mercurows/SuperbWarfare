package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.config.common.GameplayConfig;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.network.message.receive.SimulationDistanceMessage;
import com.atsuishio.superbwarfare.tools.AmmoType;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
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
                tag = data.tag();

                tag.putBoolean("draw", true);

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
            if (stack.is(ModTags.Items.GUN)) {
                var data = GunData.from(stack);
                final var tag = data.tag();

                tag.putBoolean("draw", true);

                data.save();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModTags.Items.GUN)) {
            handleSpecialWeaponAmmo(player);
        }
    }

    private static void handleSpecialWeaponAmmo(Player player) {
        ItemStack stack = player.getMainHandItem();

        var data = GunData.from(stack);

        if ((stack.is(ModItems.RPG.get()) || stack.is(ModItems.BOCEK.get())) && data.ammo() == 1) {
            data.setIsEmpty(false);
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
            if (stack.is(ModTags.Items.GUN)) {
                var data = GunData.from(stack);
                var tag = data.tag();

                if (!InventoryTool.hasCreativeAmmoBox(player)) {
                    var cap = player.getData(ModAttachments.PLAYER_VARIABLE);

                    if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO) && cap.shotgunAmmo > 0) {
                        GunsTool.reload(player, stack, data, AmmoType.SHOTGUN);
                    }
                    if (stack.is(ModTags.Items.USE_SNIPER_AMMO) && cap.sniperAmmo > 0) {
                        GunsTool.reload(player, stack, data, AmmoType.SNIPER);
                    }
                    if (stack.is(ModTags.Items.USE_HANDGUN_AMMO) && cap.handgunAmmo > 0) {
                        GunsTool.reload(player, stack, data, AmmoType.HANDGUN);
                    }
                    if (stack.is(ModTags.Items.USE_RIFLE_AMMO) && cap.rifleAmmo > 0) {
                        GunsTool.reload(player, stack, data, AmmoType.RIFLE);
                    }
                    if (stack.is(ModTags.Items.USE_HEAVY_AMMO) && cap.heavyAmmo > 0) {
                        GunsTool.reload(player, stack, data, AmmoType.HEAVY);
                    }

                    if (stack.getItem() == ModItems.TASER.get() && data.maxAmmo() > 0 && data.ammo() == 0) {
                        data.setAmmo(1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.TASER_ELECTRODE.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                    if (stack.getItem() == ModItems.M_79.get() && data.maxAmmo() > 0 && data.ammo() == 0) {
                        data.setAmmo(1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.GRENADE_40MM.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                    if (stack.getItem() == ModItems.RPG.get() && data.maxAmmo() > 0 && data.ammo() == 0) {
                        data.setAmmo(1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.ROCKET.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                    if (stack.getItem() == ModItems.JAVELIN.get() && data.maxAmmo() > 0 && data.ammo() == 0) {
                        data.setAmmo(1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.JAVELIN_MISSILE.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                } else {
                    data.setAmmo(data.magazine());
                }
                GunsTool.setGunBooleanTag(tag, "HoldOpen", false);
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

        int armorLevel = 1;
        if (armor.is(ModTags.Items.MILITARY_ARMOR)) {
            armorLevel = 2;
        } else if (armor.is(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            armorLevel = 3;
        }

        if (armorPlate >= armorLevel * 15) return;

        for (var stack : player.getInventory().items) {
            if (stack.is(ModItems.ARMOR_PLATE.get())) {
                var stackTag = NBTTool.getTag(stack);
                if (stackTag.getBoolean("Infinite")) {
                    tag.putDouble("ArmorPlate", armorLevel * 15);
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.5f, 1);
                    }
                } else {
                    for (int index0 = 0; index0 < Math.ceil(((armorLevel * 15) - armorPlate) / 15); index0++) {
                        stack.finishUsingItem(player.level(), player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.is(ModTags.Items.GUN) && right.getItem() == ModItems.SHORTCUT_PACK.get()) {
            ItemStack output = left.copy();

            var data = GunData.from(output);
            data.setUpgradePoint(data.upgradePoint() + 1);
            data.save();

            event.setOutput(output);
            event.setCost(10);
            event.setMaterialCost(1);
        }
    }
}
