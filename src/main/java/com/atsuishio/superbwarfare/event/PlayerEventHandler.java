package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.config.common.GameplayConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.network.ModVariables;
import com.atsuishio.superbwarfare.network.message.SimulationDistanceMessage;
import com.atsuishio.superbwarfare.tools.AmmoType;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
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
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Random;

@Mod.EventBusSubscriber
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")) {
            stack.getOrCreateTag().putBoolean("Using", false);
        }
        for (ItemStack pStack : player.getInventory().items) {
            if (pStack.is(ModTags.Items.GUN)) {
                pStack.getOrCreateTag().putBoolean("draw", true);
                pStack.getOrCreateTag().putBoolean("init", false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();

        if (player == null) {
            return;
        }

        player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            capability.zoom = false;
            capability.tacticalSprintExhaustion = false;
            capability.tacticalSprintTime = 600;
            capability.syncPlayerVariables(player);
        });

        handleRespawnReload(player);
        handleRespawnAutoArmor(player);

        for (ItemStack pStack : player.getInventory().items) {
            if (pStack.is(ModTags.Items.GUN)) {
                pStack.getOrCreateTag().putBoolean("draw", true);
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
    }

    private static void handleBreath(Player player) {
        if (player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables()).breath) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.breathTime = Mth.clamp(player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                        .orElse(new ModVariables.PlayerVariables()).breathTime - 1, 0, 100);
                capability.syncPlayerVariables(player);
            });
        } else {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.breathTime = Mth.clamp(player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                        .orElse(new ModVariables.PlayerVariables()).breathTime + 1, 0, 100);
                capability.syncPlayerVariables(player);
            });
        }

        if (player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables()).breathTime == 0) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.breathExhaustion = true;
                capability.breath = false;
                capability.syncPlayerVariables(player);
            });
        }

        if (player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables()).breathTime == 100) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.breathExhaustion = false;
                capability.syncPlayerVariables(player);
            });
        }
    }

    private static void handleTacticalSprint(Player player) {
        ItemStack stack = player.getMainHandItem();

        int sprintCost;

        if (stack.is(ModTags.Items.GUN)) {
            double weight = GunsTool.getGunDoubleTag(stack, "Weight") + GunsTool.getGunDoubleTag(stack, "CustomWeight");
            sprintCost = (int) (5 + 0.2 * weight);
        } else {
            sprintCost = 5;
        }

        if (!player.isSprinting()) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.tacticalSprint = false;
                capability.syncPlayerVariables(player);
            });
            player.getPersistentData().putBoolean("canTacticalSprint", true);
        }

        if (player.isSprinting()
                && !(player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables())).tacticalSprintExhaustion
                && player.getPersistentData().getBoolean("canTacticalSprint")) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.tacticalSprint = true;
                capability.syncPlayerVariables(player);
            });
            player.getPersistentData().putBoolean("canTacticalSprint", false);
        }

        if (player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables()).tacticalSprint) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.tacticalSprintTime = Mth.clamp(player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables()).tacticalSprintTime - sprintCost, 0, 1000);
                capability.syncPlayerVariables(player);
            });

            if (MiscConfig.ALLOW_TACTICAL_SPRINT.get()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 0, false, false));
            }
        } else {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.tacticalSprintTime = Mth.clamp(player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables()).tacticalSprintTime + 7, 0, 1000);
                capability.syncPlayerVariables(player);
            });
        }

        if (player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables()).tacticalSprintTime == 0) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.tacticalSprintExhaustion = true;
                capability.tacticalSprint = false;
                capability.syncPlayerVariables(player);
            });
        }

        if (player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables()).tacticalSprintTime == 1000) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.tacticalSprintExhaustion = false;
                capability.syncPlayerVariables(player);
            });
            player.getPersistentData().putBoolean("canTacticalSprint", true);
        }
    }

    /**
     * 判断玩家是否在奔跑
     */
    private static void handlePlayerSprint(Player player) {
        if ((player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables())).holdFire) {
            player.getPersistentData().putDouble("noRun", 10);
        }

        if (player.isShiftKeyDown()
                || player.isPassenger()
                || player.isInWater()
                || (player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables())).zoom) {
            player.getPersistentData().putDouble("noRun", 3);
        }

        if (player.getPersistentData().getDouble("noRun") > 0) {
            player.getPersistentData().putDouble("noRun", (player.getPersistentData().getDouble("noRun") - 1));
        }

        if ((player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables())).zoom
                || (player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables())).holdFire) {
            player.setSprinting(false);
        }
    }

    private static void handleGround(Player player) {
        if (player.onGround()) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.playerDoubleJump = false;
                capability.syncPlayerVariables(player);
            });
        }
    }

    private static void handleSpecialWeaponAmmo(Player player) {
        ItemStack stack = player.getMainHandItem();

        if ((stack.is(ModItems.RPG.get()) || stack.is(ModItems.BOCEK.get())) && GunsTool.getGunIntTag(stack, "Ammo", 0) == 1) {
            stack.getOrCreateTag().putDouble("empty", 0);
        }
    }

    private static void handleBocekPulling(Player player) {
        ItemStack stack = player.getMainHandItem();

        if ((player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables())).bowPullHold) {
            if (stack.getItem() == ModItems.BOCEK.get()
                    && GunsTool.getGunIntTag(stack, "MaxAmmo") > 0
                    && !player.getCooldowns().isOnCooldown(stack.getItem())
                    && GunsTool.getGunDoubleTag(stack, "Power") < 12
            ) {
                GunsTool.setGunDoubleTag(stack, "Power", GunsTool.getGunDoubleTag(stack, "Power") + 1);

                player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                    capability.bowPull = true;
                    capability.tacticalSprint = false;
                    capability.syncPlayerVariables(player);
                });
                player.setSprinting(false);
            }
            if (GunsTool.getGunDoubleTag(stack, "Power") == 1) {
                if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.BOCEK_PULL_1P.get(), 2f, 1f);
                    player.level().playSound(null, player.blockPosition(), ModSounds.BOCEK_PULL_3P.get(), SoundSource.PLAYERS, 0.5f, 1);
                }
            }
        } else {
            if (stack.getItem() == ModItems.BOCEK.get()) {
                GunsTool.setGunDoubleTag(stack, "Power", 0);
            }
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.bowPull = false;
                capability.syncPlayerVariables(player);
            });
        }

        if (GunsTool.getGunDoubleTag(stack, "Power") > 0) {
            player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.tacticalSprint = false;
                capability.syncPlayerVariables(player);
            });
            player.setSprinting(false);
        }
    }

    private static void handleSimulationDistance(Player player) {
        if (player.level() instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            var distanceManager = serverLevel.getChunkSource().chunkMap.getDistanceManager();
            var playerTicketManager = distanceManager.playerTicketManager;
            int maxDistance = playerTicketManager.viewDistance;

            ModUtils.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SimulationDistanceMessage(maxDistance));
        }
    }

    private static void handleRespawnReload(Player player) {
        if (!GameplayConfig.RESPAWN_RELOAD.get()) return;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModTags.Items.GUN)) {
                if (!InventoryTool.hasCreativeAmmoBox(player)) {
                    var cap = player.getCapability(ModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ModVariables.PlayerVariables());

                    if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO) && cap.shotgunAmmo > 0) {
                        GunsTool.reload(player, stack, AmmoType.SHOTGUN);
                    }
                    if (stack.is(ModTags.Items.USE_SNIPER_AMMO) && cap.sniperAmmo > 0) {
                        GunsTool.reload(player, stack, AmmoType.SNIPER);
                    }
                    if (stack.is(ModTags.Items.USE_HANDGUN_AMMO) && cap.handgunAmmo > 0) {
                        GunsTool.reload(player, stack, AmmoType.HANDGUN);
                    }
                    if (stack.is(ModTags.Items.USE_RIFLE_AMMO) && cap.rifleAmmo > 0) {
                        GunsTool.reload(player, stack, AmmoType.RIFLE);
                    }
                    if (stack.is(ModTags.Items.USE_HEAVY_AMMO) && cap.heavyAmmo > 0) {
                        GunsTool.reload(player, stack, AmmoType.HEAVY);
                    }

                    if (stack.getItem() == ModItems.TASER.get() && GunsTool.getGunIntTag(stack, "MaxAmmo") > 0 && GunsTool.getGunIntTag(stack, "Ammo", 0) == 0) {
                        GunsTool.setGunIntTag(stack, "Ammo", 1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.TASER_ELECTRODE.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                    if (stack.getItem() == ModItems.M_79.get() && GunsTool.getGunIntTag(stack, "MaxAmmo") > 0 && GunsTool.getGunIntTag(stack, "Ammo", 0) == 0) {
                        GunsTool.setGunIntTag(stack, "Ammo", 1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.GRENADE_40MM.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                    if (stack.getItem() == ModItems.RPG.get() && GunsTool.getGunIntTag(stack, "MaxAmmo") > 0 && GunsTool.getGunIntTag(stack, "Ammo", 0) == 0) {
                        GunsTool.setGunIntTag(stack, "Ammo", 1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.ROCKET.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                    if (stack.getItem() == ModItems.JAVELIN.get() && GunsTool.getGunIntTag(stack, "MaxAmmo") > 0 && GunsTool.getGunIntTag(stack, "Ammo", 0) == 0) {
                        GunsTool.setGunIntTag(stack, "Ammo", 1);
                        player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.JAVELIN_MISSILE.get(), 1, player.inventoryMenu.getCraftSlots());
                    }
                } else {
                    GunsTool.setGunIntTag(stack, "Ammo", GunsTool.getGunIntTag(stack, "Magazine", 0)
                            + GunsTool.getGunIntTag(stack, "CustomMagazine", 0));
                }
                GunsTool.setGunBooleanTag(stack, "HoldOpen", false);
            }
        }
    }

    private static void handleRespawnAutoArmor(Player player) {
        if (!GameplayConfig.RESPAWN_AUTO_ARMOR.get()) return;

        ItemStack armor = player.getItemBySlot(EquipmentSlot.CHEST);
        if (armor == ItemStack.EMPTY) return;

        double armorPlate = armor.getOrCreateTag().getDouble("ArmorPlate");

        int armorLevel = 1;
        if (armor.is(ModTags.Items.MILITARY_ARMOR)) {
            armorLevel = 2;
        } else if (armor.is(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            armorLevel = 3;
        }

        if (armorPlate < armorLevel * 15) {
            for (var stack : player.getInventory().items) {
                if (stack.is(ModItems.ARMOR_PLATE.get())) {
                    if (stack.getTag() != null && stack.getTag().getBoolean("Infinite")) {
                        armor.getOrCreateTag().putDouble("ArmorPlate", armorLevel * 15);

                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.5f, 1);
                        }
                    } else {
                        for (int index0 = 0; index0 < Math.ceil(((armorLevel * 15) - armorPlate) / 15); index0++) {
                            stack.finishUsingItem(player.level(), player);
                        }
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

            double random = new Random().nextDouble();
            if (random < 0.05) {
                GunsTool.setGunDoubleTag(output, "UpgradePoint", GunsTool.getGunDoubleTag(output, "UpgradePoint", 0) + 1);
            }

            event.setOutput(output);
            event.setCost(10);
            event.setMaterialCost(1);
        }
    }
}
