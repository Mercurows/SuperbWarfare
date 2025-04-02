package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.config.common.GameplayConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class PlayerEventHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);
        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using")) {
            tag.putBoolean("Using", false);
            NBTTool.saveTag(stack, tag);
        }
        for (ItemStack pStack : player.getInventory().items) {
            if (pStack.is(ModTags.Items.GUN)) {
                tag = NBTTool.getTag(pStack);
                tag.putBoolean("draw", true);
                tag.putBoolean("init", false);
                NBTTool.saveTag(pStack, tag);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap != null) {
            cap.zoom = false;
            cap.tacticalSprintExhaustion = false;
            cap.tacticalSprintTime = 600;
            cap.syncPlayerVariables(player);
        }

        handleRespawnReload(player);
        handleRespawnAutoArmor(player);

        for (ItemStack pStack : player.getInventory().items) {
            if (pStack.is(ModTags.Items.GUN)) {
                var tag = NBTTool.getTag(pStack);
                tag.putBoolean("draw", true);
                NBTTool.saveTag(pStack, tag);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModTags.Items.GUN)) {
            handlePlayerSprint(player);
            handleSpecialWeaponAmmo(player);
            handleBocekPulling(player);
        }

        handleGround(player);
        handleSimulationDistance(player);
        handleTacticalSprint(player);
        handleBreath(player);
    }

    private static void handleBreath(Player player) {
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return;

        if (cap.breath) {
            cap.breathTime = Mth.clamp(cap.breathTime - 1, 0, 100);
        } else {
            cap.breathTime = Mth.clamp(cap.breathTime + 1, 0, 100);
        }

        if (cap.breathTime == 0) {
            cap.breathExhaustion = true;
            cap.breath = false;
        }

        if (cap.breathTime == 100) {
            cap.breathExhaustion = false;
        }

        cap.syncPlayerVariables(player);
    }

    private static void handleTacticalSprint(Player player) {
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return;

        ItemStack stack = player.getMainHandItem();
        int sprintCost;

        if (stack.is(ModTags.Items.GUN)) {
            final var tag = NBTTool.getTag(stack);
            double weight = GunsTool.getGunDoubleTag(tag, "Weight") + GunsTool.getGunDoubleTag(tag, "CustomWeight");
            sprintCost = (int) (5 + 0.2 * weight);
        } else {
            sprintCost = 5;
        }

        if (!player.isSprinting()) {
            cap.tacticalSprint = false;
            player.getPersistentData().putBoolean("canTacticalSprint", true);
        }

        if (player.isSprinting()
                && !cap.tacticalSprintExhaustion
                && player.getPersistentData().getBoolean("canTacticalSprint")
        ) {
            cap.tacticalSprint = true;
            player.getPersistentData().putBoolean("canTacticalSprint", false);
        }

        if (cap.tacticalSprint) {
            cap.tacticalSprintTime = Mth.clamp(cap.tacticalSprintTime - sprintCost, 0, 1000);

            if (MiscConfig.ALLOW_TACTICAL_SPRINT.get()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 0, false, false));
            }
        } else {
            cap.tacticalSprintTime = Mth.clamp(cap.tacticalSprintTime + 7, 0, 1000);
        }

        if (cap.tacticalSprintTime == 0) {
            cap.tacticalSprintExhaustion = true;
            cap.tacticalSprint = false;
        }

        if (cap.tacticalSprintTime == 1000) {
            cap.tacticalSprintExhaustion = false;
            player.getPersistentData().putBoolean("canTacticalSprint", true);
        }

        cap.syncPlayerVariables(player);
    }

    /**
     * 判断玩家是否在奔跑
     */
    private static void handlePlayerSprint(Player player) {
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return;

        if (cap.holdFire) {
            player.getPersistentData().putDouble("noRun", 10);
        }

        if (player.isShiftKeyDown()
                || player.isPassenger()
                || player.isInWater()
                || cap.zoom
        ) player.getPersistentData().putDouble("noRun", 3);

        if (player.getPersistentData().getDouble("noRun") > 0) {
            player.getPersistentData().putDouble("noRun", (player.getPersistentData().getDouble("noRun") - 1));
        }

        if (cap.zoom || cap.holdFire) {
            player.setSprinting(false);
        }
    }

    private static void handleGround(Player player) {
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return;

        if (player.onGround()) {
            cap.playerDoubleJump = false;
            cap.syncPlayerVariables(player);
        }
    }

    private static void handleSpecialWeaponAmmo(Player player) {
        ItemStack stack = player.getMainHandItem();

        final var tag = NBTTool.getTag(stack);
        if ((stack.is(ModItems.RPG.get()) || stack.is(ModItems.BOCEK.get())) && GunsTool.getGunIntTag(tag, "Ammo") == 1) {
            tag.putDouble("empty", 0);
            NBTTool.saveTag(stack, tag);
        }
    }

    private static void handleBocekPulling(Player player) {
        ItemStack stack = player.getMainHandItem();

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return;

        final var tag = NBTTool.getTag(stack);
        if (cap.bowPullHold) {
            if (stack.getItem() == ModItems.BOCEK.get()
                    && GunsTool.getGunIntTag(tag, "MaxAmmo") > 0
                    && !player.getCooldowns().isOnCooldown(stack.getItem())
                    && GunsTool.getGunDoubleTag(tag, "Power") < 12
            ) {
                GunsTool.setGunDoubleTag(tag, "Power", GunsTool.getGunDoubleTag(tag, "Power") + 1);

                cap.bowPull = true;
                cap.tacticalSprint = false;
                cap.syncPlayerVariables(player);
                player.setSprinting(false);
            }
            if (GunsTool.getGunDoubleTag(tag, "Power") == 1) {
                if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.BOCEK_PULL_1P.get(), 2f, 1f);
                    player.level().playSound(null, player.blockPosition(), ModSounds.BOCEK_PULL_3P.get(), SoundSource.PLAYERS, 0.5f, 1);
                }
            }
        } else {
            if (stack.getItem() == ModItems.BOCEK.get()) {
                GunsTool.setGunDoubleTag(tag, "Power", 0);
            }
            cap.bowPull = false;
            cap.syncPlayerVariables(player);
        }

        if (GunsTool.getGunDoubleTag(tag, "Power") > 0) {
            cap.tacticalSprint = false;
            cap.syncPlayerVariables(player);
            player.setSprinting(false);
        }

        NBTTool.saveTag(stack, tag);
    }

    private static void handleSimulationDistance(Player player) {
        if (player.level() instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            // TODO send simulation distance to client
//            var distanceManager = serverLevel.getChunkSource().chunkMap.getDistanceManager();
//            var playerTicketManager = distanceManager.playerTicketManager;
//            int maxDistance = playerTicketManager.viewDistance;
//
//            ModUtils.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SimulationDistanceMessage(maxDistance));
        }
    }

    private static void handleRespawnReload(Player player) {
        if (!GameplayConfig.RESPAWN_RELOAD.get()) return;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModTags.Items.GUN)) {
                final var tag = NBTTool.getTag(stack);
                if (!InventoryTool.hasCreativeAmmoBox(player)) {
                    var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
                    if (cap == null) return;

                    if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO) && cap.shotgunAmmo > 0) {
                        GunsTool.reload(player, stack, tag, AmmoType.SHOTGUN);
                    }
                    if (stack.is(ModTags.Items.USE_SNIPER_AMMO) && cap.sniperAmmo > 0) {
                        GunsTool.reload(player, stack, tag, AmmoType.SNIPER);
                    }
                    if (stack.is(ModTags.Items.USE_HANDGUN_AMMO) && cap.handgunAmmo > 0) {
                        GunsTool.reload(player, stack, tag, AmmoType.HANDGUN);
                    }
                    if (stack.is(ModTags.Items.USE_RIFLE_AMMO) && cap.rifleAmmo > 0) {
                        GunsTool.reload(player, stack, tag, AmmoType.RIFLE);
                    }
                    if (stack.is(ModTags.Items.USE_HEAVY_AMMO) && cap.heavyAmmo > 0) {
                        GunsTool.reload(player, stack, tag, AmmoType.HEAVY);
                    }

                    if (stack.getItem() == ModItems.TASER.get() && GunsTool.getGunIntTag(tag, "MaxAmmo") > 0 && GunsTool.getGunIntTag(tag, "Ammo") == 0) {
                        GunsTool.setGunIntTag(tag, "Ammo", 1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.TASER_ELECTRODE.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                    if (stack.getItem() == ModItems.M_79.get() && GunsTool.getGunIntTag(tag, "MaxAmmo") > 0 && GunsTool.getGunIntTag(tag, "Ammo") == 0) {
                        GunsTool.setGunIntTag(tag, "Ammo", 1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.GRENADE_40MM.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                    if (stack.getItem() == ModItems.RPG.get() && GunsTool.getGunIntTag(tag, "MaxAmmo") > 0 && GunsTool.getGunIntTag(tag, "Ammo") == 0) {
                        GunsTool.setGunIntTag(tag, "Ammo", 1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.ROCKET.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                    if (stack.getItem() == ModItems.JAVELIN.get() && GunsTool.getGunIntTag(tag, "MaxAmmo") > 0 && GunsTool.getGunIntTag(tag, "Ammo") == 0) {
                        GunsTool.setGunIntTag(tag, "Ammo", 1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.JAVELIN_MISSILE.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                } else {
                    GunsTool.setGunIntTag(tag, "Ammo", GunsTool.getGunIntTag(tag, "Magazine")
                            + GunsTool.getGunIntTag(tag, "CustomMagazine"));
                }
                GunsTool.setGunBooleanTag(tag, "HoldOpen", false);
                NBTTool.saveTag(stack, tag);
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

            final var outputTag = NBTTool.getTag(output);
            GunsTool.setGunDoubleTag(outputTag, "UpgradePoint", GunsTool.getGunDoubleTag(outputTag, "UpgradePoint") + 1);
            NBTTool.saveTag(output, outputTag);

            event.setOutput(output);
            event.setCost(10);
            event.setMaterialCost(1);
        }
    }
}
