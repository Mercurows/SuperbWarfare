package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.ReloadState;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Mod.MODID)
public class GunEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);

            handleGunBolt(data);
            handleGunReload(player, data);
            handleGunSingleReload(player, data);
            handleSentinelCharge(player, data);

            data.save();
        }
    }

    /**
     * 拉大栓
     */
    private static void handleGunBolt(GunData data) {
        var stack = data.stack();

        if (stack.is(ModTags.Items.NORMAL_GUN)) {
            data.bolt.actionTimer.reduce();

            if (stack.getItem() == ModItems.MARLIN.get() && data.bolt.actionTimer.get() == 9) {
                data.isEmpty.set(false);
            }

            if (data.bolt.actionTimer.get() == 1) {
                data.bolt.needed.set(false);
            }
        }
    }

    public static void playGunBoltSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_bolt"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 2f, 1f);

                double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

                Mod.queueServerWork((int) (data.bolt.actionTimer.get() / 2 + 1.5 * shooterHeight), () -> {
                    var ammoType = data.ammoTypeInfo().playerAmmoType();
                    if (ammoType != null) {
                        switch (ammoType) {
                            case SHOTGUN ->
                                    SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_SHOTGUN.get(), (float) Math.max(0.75 - 0.12 * shooterHeight, 0), 1);
                            case SNIPER, HEAVY ->
                                    SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_50CAL.get(), (float) Math.max(1 - 0.15 * shooterHeight, 0), 1);
                            default ->
                                    SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1);
                        }
                    } else {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1);
                    }
                });
            }
        }
    }

    /**
     * 通用的武器换弹流程
     */
    private static void handleGunReload(Player player, GunData data) {
        var stack = data.stack();
        var gunItem = data.item();
        var reload = data.reload;

        // 启动换弹
        if (reload.reloadStarter.start()) {
            NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(player, data));

            if (gunItem.isOpenBolt(stack)) {
                if (data.ammo.get() == 0) {
                    reload.setTime(data.defaultEmptyReloadTime() + 1);
                    reload.setState(ReloadState.EMPTY_RELOADING);
                    playGunEmptyReloadSounds(player);
                } else {
                    reload.setTime(data.defaultNormalReloadTime() + 1);
                    reload.setState(ReloadState.NORMAL_RELOADING);
                    playGunNormalReloadSounds(player);
                }
            } else {
                reload.setTime(data.defaultEmptyReloadTime() + 2);
                reload.setState(ReloadState.EMPTY_RELOADING);
                playGunEmptyReloadSounds(player);
            }
        }

        reload.reduce();

        // 换弹时额外行为
        var behavior = gunItem.reloadTimeBehaviors.get(reload.time());
        if (behavior != null) {
            behavior.accept(data);
        }

        if (reload.time() == 1) {
            if (gunItem.isOpenBolt(stack)) {
                if (data.ammo.get() == 0) {
                    playGunEmptyReload(player, data);
                } else {
                    playGunNormalReload(player, data);
                }
            } else {
                playGunEmptyReload(player, data);
            }
            reload.setTime(0);
            reload.setState(ReloadState.NOT_RELOADING);

            reload.reloadStarter.finish();
        }
    }

    public static void playGunNormalReload(Player player, GunData data) {
        var stack = data.stack();
        var gunItem = data.item();

        if (player.getInventory().hasAnyMatching(item -> item.is(ModItems.CREATIVE_AMMO_BOX.get()))) {
            data.ammo.set(data.magazine() + (gunItem.hasBulletInBarrel(stack) ? 1 : 0));
        } else {
            var ammoTypeInfo = data.ammoTypeInfo();

            if (ammoTypeInfo.type() == GunData.AmmoConsumeType.PLAYER_AMMO) {
                data.reload(player, gunItem.hasBulletInBarrel(stack));
            }
        }
        data.reload.setState(ReloadState.NOT_RELOADING);
        NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, data));
    }

    public static void playGunEmptyReload(Player player, GunData data) {
        if (player.getInventory().hasAnyMatching(item -> item.is(ModItems.CREATIVE_AMMO_BOX.get()))) {
            data.ammo.set(data.magazine());
        } else {
            data.reload(player);
        }
        NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, data));
    }

    public static void playGunEmptyReloadSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_reload_empty"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);
            }
        }
    }

    public static void playGunNormalReloadSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_reload_normal"));

            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);
            }
        }
    }

    /**
     * 单发装填类的武器换弹流程
     */
    private static void handleGunSingleReload(Player player, GunData data) {
        var stack = data.stack();
        var reload = data.reload;

        // 换弹流程计时器
        reload.prepareTimer.reduce();
        reload.prepareLoadTimer.reduce();
        reload.iterativeLoadTimer.reduce();
        reload.finishTimer.reduce();

        // 一阶段
        if (reload.singleReloadStarter.start()) {
            NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(player, data));

            if (data.defaultPrepareLoadTime() != 0 && (data.ammo.get() == 0 || stack.is(ModItems.SECONDARY_CATACLYSM.get()))) {
                // 此处判断空仓换弹的时候，是否在准备阶段就需要装填一发，如M870
                playGunPrepareLoadReloadSounds(player);
                int prepareLoadTime = data.defaultPrepareLoadTime();
                reload.prepareLoadTimer.set(prepareLoadTime + 1);
                player.getCooldowns().addCooldown(stack.getItem(), prepareLoadTime);
            } else if (data.defaultPrepareEmptyTime() != 0 && data.ammo.get() == 0) {
                // 此处判断空仓换弹，如莫辛纳甘
                playGunEmptyPrepareSounds(player);
                int prepareEmptyTime = data.defaultPrepareEmptyTime();
                reload.prepareTimer.set(prepareEmptyTime + 1);
                player.getCooldowns().addCooldown(stack.getItem(), prepareEmptyTime);
            } else {
                playGunPrepareReloadSounds(player);
                int prepareTime = data.defaultPrepareTime();
                reload.prepareTimer.set(prepareTime + 1);
                player.getCooldowns().addCooldown(stack.getItem(), prepareTime);
            }

            data.forceStop.set(false);
            data.stopped.set(false);
            reload.setStage(1);
            reload.setState(ReloadState.NORMAL_RELOADING);
        }

        if (reload.prepareLoadTimer.get() == data.prepareAmmoLoadTime()) {
            iterativeLoad(player, data);
        }

        // 一阶段结束，检查备弹，如果有则二阶段启动，无则直接跳到三阶段
        if ((reload.prepareTimer.get() == 1 || reload.prepareLoadTimer.get() == 1)) {
            if (!data.hasBackupAmmo(player) || data.ammo.get() >= data.magazine()) {
                reload.stage3Starter.markStart();
            } else {
                reload.setStage(2);
            }
        }

        // 强制停止换弹，进入三阶段
        if (data.forceStop.get() && reload.stage() == 2 && reload.iterativeLoadTimer.get() > 0) {
            data.stopped.set(true);
        }

        // 二阶段
        if ((reload.prepareTimer.get() == 0 || reload.iterativeLoadTimer.get() == 0)
                && reload.stage() == 2
                && reload.iterativeLoadTimer.get() == 0
                && !data.stopped.get()
                && data.ammo.get() < data.magazine()
        ) {
            playGunLoopReloadSounds(player);
            int iterativeTime = data.defaultIterativeTime();
            reload.iterativeLoadTimer.set(iterativeTime);
            player.getCooldowns().addCooldown(stack.getItem(), iterativeTime);
            // 动画播放nbt
            data.loadIndex.set(data.loadIndex.get() == 1 ? 0 : 1);
        }

        // 装填
        if (data.iterativeAmmoLoadTime() == reload.iterativeLoadTimer.get()) {
            iterativeLoad(player, data);
        }

        // 二阶段打断
        if (reload.iterativeLoadTimer.get() == 1) {
            // 装满或备弹耗尽结束
            if (!data.hasBackupAmmo(player) || data.ammo.get() >= data.magazine()) {
                reload.setStage(3);
            }

            // 强制结束
            if (data.stopped.get()) {
                reload.setStage(3);
                data.stopped.set(false);
                data.forceStop.set(false);
            }
        }

        // 三阶段
        if ((reload.iterativeLoadTimer.get() == 1 && reload.stage() == 3) || reload.stage3Starter.shouldStart()) {
            reload.setStage(3);
            reload.stage3Starter.finish();

            int finishTime = data.defaultFinishTime();
            reload.finishTimer.set(finishTime + 2);
            player.getCooldowns().addCooldown(stack.getItem(), finishTime + 2);

            playGunEndReloadSounds(player);
        }

        if (stack.getItem() == ModItems.MARLIN.get() && reload.finishTimer.get() == 10) {
            data.isEmpty.set(false);
        }

        // 三阶段结束
        if (reload.finishTimer.get() == 1) {
            reload.setStage(0);
            if (data.defaultActionTime() > 0) {
                data.bolt.needed.set(false);
            }
            reload.setState(ReloadState.NOT_RELOADING);
            reload.singleReloadStarter.finish();

            NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, data));
        }
    }

    public static void iterativeLoad(Player player, GunData data) {
        var required = Math.min(data.magazine() - data.ammo.get(), data.iterativeLoadAmount());
        var available = Math.min(required, data.countBackupAmmo(player));
        data.ammo.add(available);

        if (!InventoryTool.hasCreativeAmmoBox(player)) {
            var cap = player.getData(ModAttachments.PLAYER_VARIABLE);
            data.consumeBackupAmmo(player, available);
            player.setData(ModAttachments.PLAYER_VARIABLE, cap);
        }
    }

    public static void playGunPrepareReloadSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_prepare"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);
            }
        }
    }

    public static void playGunEmptyPrepareSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_prepare_empty"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);

                double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

                Mod.queueServerWork((int) (data.defaultPrepareEmptyTime() / 2 + 3 + 1.5 * shooterHeight), () -> {
                    var ammoType = data.ammoTypeInfo().playerAmmoType();
                    if (ammoType != null) {
                        switch (ammoType) {
                            case SHOTGUN ->
                                    SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_SHOTGUN.get(), (float) Math.max(0.75 - 0.12 * shooterHeight, 0), 1);
                            case SNIPER, HEAVY ->
                                    SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_50CAL.get(), (float) Math.max(1 - 0.15 * shooterHeight, 0), 1);
                            default ->
                                    SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1);
                        }
                    } else {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1);
                    }
                });
            }
        }
    }

    public static void playGunPrepareLoadReloadSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_prepare_load"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);

                double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

                Mod.queueServerWork((int) (8 + 1.5 * shooterHeight), () -> {
                    var ammoType = data.ammoTypeInfo().playerAmmoType();
                    if (ammoType != null) {
                        switch (ammoType) {
                            case SHOTGUN ->
                                    SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_SHOTGUN.get(), (float) Math.max(0.75 - 0.12 * shooterHeight, 0), 1);
                            case SNIPER, HEAVY ->
                                    SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_50CAL.get(), (float) Math.max(1 - 0.15 * shooterHeight, 0), 1);
                            default ->
                                    SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1);
                        }
                    } else {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1);
                    }
                });
            }
        }
    }

    public static void playGunLoopReloadSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_loop"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);
            }
        }
    }

    public static void playGunEndReloadSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_end"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);

                double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

                if (stack.is(ModItems.MARLIN.get())) {
                    Mod.queueServerWork((int) (5 + 1.5 * shooterHeight), () -> SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1));
                }
            }
        }
    }

    /**
     * 哨兵充能
     */
    private static void handleSentinelCharge(Player player, GunData data) {
        // 启动充能
        if (data.charge.starter.start()) {
            data.charge.timer.set(127);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc("sentinel_charge"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 2f, 1f);
            }
        }

        data.charge.timer.reduce();

        if (data.charge.timer.get() == 17) {
            for (var cell : player.getInventory().items) {
                if (cell.is(ModItems.CELL.get())) {
                    var stackStorage = data.stack().getCapability(Capabilities.EnergyStorage.ITEM);
                    if (stackStorage == null) continue;

                    int stackMaxEnergy = stackStorage.getMaxEnergyStored();
                    int stackEnergy = stackStorage.getEnergyStored();

                    var cellStorage = cell.getCapability(Capabilities.EnergyStorage.ITEM);
                    if (cellStorage == null) continue;
                    int cellEnergy = cellStorage.getEnergyStored();

                    int stackEnergyNeed = Math.min(cellEnergy, stackMaxEnergy - stackEnergy);

                    if (cellEnergy > 0) {
                        stackStorage.receiveEnergy(stackEnergyNeed, false);
                    }
                    cellStorage.extractEnergy(stackEnergyNeed, false);
                }
            }
        }
    }
}
