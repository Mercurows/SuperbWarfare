package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ReloadType;
import com.atsuishio.superbwarfare.data.gun.value.ReloadState;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GunEventHandler {

    /**
     * 拉大栓
     */
    private static void handleGunBolt(@NotNull GunData data) {
        if (data.item.useSpecialFireProcedure(data)) return;

        data.bolt.actionTimer.reduce();

        // 执行拉栓期间额外行为
        var behavior = data.item.boltTimeBehaviors.get(data.bolt.actionTimer.get());
        if (behavior != null) {
            behavior.accept(data);
        }

        if (data.bolt.actionTimer.get() == 1) {
            data.bolt.needed.set(false);
        }
    }

    /**
     * 播放拉栓音效
     */
    public static void playGunBoltSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter instanceof ServerPlayer serverPlayer) {
            var soundInfo = data.compute().soundInfo;
            var sound = soundInfo.bolt;

            if (sound != null) {
                SoundTool.playLocalSound(serverPlayer, sound, 2f, 1f);
            }

            double shooterHeight = shooter.getEyePosition().distanceTo((Vec3.atLowerCornerOf(shooter.level().clip(new ClipContext(shooter.getEyePosition(), shooter.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter)).getBlockPos())));

            Mod.queueServerWork((int) (data.bolt.actionTimer.get() / 2.0 + 1.5 * shooterHeight), () -> {
                if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                    var ammoType = data.selectedAmmoConsumer().getPlayerAmmoType();
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

    /**
     * 完成换弹过程，装填弹药
     */
    private static void finishReload(@Nullable Entity shooter, @NotNull GunData data) {
        if (data.item.isOpenBolt(data)) {
            if (!data.hasEnoughAmmoToShoot(shooter)) {
                finishGunEmptyReload(shooter, data);
            } else {
                finishGunNormalReload(shooter, data);
            }
        } else {
            finishGunEmptyReload(shooter, data);
        }
        data.reload.setTime(0);
        data.reload.setState(ReloadState.NOT_RELOADING);

        data.reload.reloadStarter.finish();
    }

    /**
     * 初始化枪械ID和弹药数量
     */
    public static void init(@Nullable Entity shooter, @NotNull GunData data) {
        if (!data.initialized()) {
            data.initialize();
            if (shooter instanceof Player player && player.isCreative()) {
                data.ammo.set(data.compute().magazine);
            }
        }
    }

    /**
     * 更新perk相关属性
     */
    public static void tickPerk(@Nullable Entity shooter, @NotNull GunData data) {
        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().tick(data, instance, shooter);
            }
        }
    }

    public static void autoReload(@Nullable Entity shooter, GunData data, boolean inMainHand) {
        if (inMainHand && data.compute().autoReload && !data.hasEnoughAmmoToShoot(shooter)) {
            tryStartReload(shooter, data);
        }
    }

    public static void tryStartReload(@Nullable Entity shooter, GunData data) {
        if (data.useBackpackAmmo() || data.meleeOnly()) return;

        if ((shooter == null || !shooter.isSpectator())
                && !data.charging()
                && !data.reloading()
                && data.reload.time() == 0
                && data.bolt.actionTimer.get() == 0
        ) {
            // 检查备弹
            if (!data.hasBackupAmmo(shooter)) return;

            // Clip > Magazine > Iterative
            var computed = data.compute();
            var reloadTypes = computed.reloadTypes;
            boolean canMagazineReload = reloadTypes.contains(ReloadType.MAGAZINE) && !reloadTypes.contains(ReloadType.CLIP);
            boolean canClipLoad = !data.hasEnoughAmmoToShoot(shooter) && reloadTypes.contains(ReloadType.CLIP);
            boolean canSingleReload = reloadTypes.contains(ReloadType.ITERATIVE);

            if (canMagazineReload || canClipLoad) {
                int magazine = computed.magazine;
                var extra = (data.item.isOpenBolt(data) && data.item.hasBulletInBarrel(data)) ? 1 : 0;
                var maxAmmo = magazine + extra;

                if (data.ammo.get() < maxAmmo) {
                    data.startReload();
                }
            } else if (canSingleReload && data.ammo.get() < computed.magazine) {
                data.reload.singleReloadStarter.markStart();
            } else {
                return;
            }

            data.burstAmount.reset();
            data.save();
        }
    }

    /**
     * 减少过热值
     */
    public static void handleCooldown(@Nullable Entity shooter, @NotNull GunData data) {
        double rate = 1;
        var computed = data.compute();
        if (shooter != null) {
            if (shooter.wasInPowderSnow) {
                rate = computed.inSnowCooldownRate;
            } else if (shooter.isInWaterOrRain()) {
                rate = computed.inWaterCooldownRate;
            } else if (shooter.isOnFire()) {
                rate = computed.inFireCooldownRate;
            } else if (shooter.isInLava()) {
                rate = computed.inLavaCooldownRate;
            }
        }

        data.heat.set(Math.max(data.heat.get() - computed.naturalCooldown * rate, 0));

        if (data.heat.get() < 80 && data.overHeat.get()) {
            data.overHeat.set(false);
        }
    }

    /**
     * 返还多余弹药
     */
    public static void redrawExtraAmmo(@Nullable Entity shooter, @NotNull GunData data) {
        var hasBulletInBarrel = data.item.hasBulletInBarrel(data);
        var ammoCount = data.ammo.get();
        var magazine = data.compute().magazine;

        // TODO 修改为更正确的退弹药方式？
        if ((hasBulletInBarrel && ammoCount > magazine + 1) || (!hasBulletInBarrel && ammoCount > magazine)) {
            int count = ammoCount - magazine - (hasBulletInBarrel ? 1 : 0);

            if (shooter instanceof Player player) {
                var capability = player.getData(ModAttachments.PLAYER_VARIABLE).watch();
                if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                    var ammoType = data.selectedAmmoConsumer().getPlayerAmmoType();
                    ammoType.add(capability, count);
                }
                player.setData(ModAttachments.PLAYER_VARIABLE, capability);
                capability.sync(player);
            }

            data.ammo.set(magazine + (hasBulletInBarrel ? 1 : 0));
        }
    }

    // 自动单发装填
    public static void autoIterativeReload(@Nullable Entity ammoSupplier, @NotNull GunData data) {
        var computed = data.compute();
        var autoIterativeReloadTime = computed.autoIterativeReloadTime;

        if (autoIterativeReloadTime <= 0
                || data.bolt.needed.get()
                || data.reloading()
                || data.charging()
                || data.ammo.get() >= computed.magazine
                || !data.hasBackupAmmo(ammoSupplier)
        ) {
            data.autoIterativeReloadTimer.set(autoIterativeReloadTime);
            return;
        }

        if (data.autoIterativeReloadTimer.get() == autoIterativeReloadTime - 1) {
            var soundInfo = computed.soundInfo;
            var sound1p = soundInfo.vehicleReload;

            if (sound1p != null && ammoSupplier instanceof VehicleEntity vehicle) {
                for (Entity passenger : vehicle.getPassengers()) {
                    if (passenger instanceof ServerPlayer serverPlayer) {
                        SoundTool.playLocalSound(serverPlayer, sound1p, 8, 1);
                    }
                }
            }
        }


        data.autoIterativeReloadTimer.reduce();

        if (data.autoIterativeReloadTimer.get() == 0) {
            iterativeLoad(ammoSupplier, data);
            data.autoIterativeReloadTimer.set(autoIterativeReloadTime);
            var soundInfo = computed.soundInfo;
            var sound = soundInfo.vehicleReload3p;
            if (sound != null && ammoSupplier != null) {
                ammoSupplier.level().playSound(ammoSupplier, ammoSupplier.getOnPos(), sound, SoundSource.PLAYERS, 2, 1);
            }
        }
    }

    public static void gunTick(@Nullable Entity shooter, @NotNull GunData data, boolean inMainHand) {
        init(shooter, data);
        autoReload(shooter, data, inMainHand);
        tickPerk(shooter, data);
        handleCooldown(shooter, data);
        redrawExtraAmmo(shooter, data);
        data.shootAnimationTimer.set(Math.max(data.shootAnimationTimer.get() - 1, 0));
        var computed = data.compute();

        if (inMainHand) {
            handleGunBolt(data);

            // 启动换弹
            if (data.reload.reloadStarter.start()) {
                NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(shooter, data));
                startReload(shooter, data);
            }

            var soundInfo = computed.soundInfo;
            var sound1p = soundInfo.vehicleReload;

            if (data.reload.time() == (soundInfo.vehicleReloadSoundTime != 0 ? Mth.clamp(soundInfo.vehicleReloadSoundTime, 1, data.compute().emptyReloadTime - 1) : data.compute().emptyReloadTime - 1)) {
                if (shooter instanceof VehicleEntity vehicle) {
                    if (sound1p != null) {
                        for (Entity passenger: vehicle.getPassengers()) {
                            if (passenger instanceof ServerPlayer serverPlayer) {
                                SoundTool.playLocalSound(serverPlayer, sound1p, 3, 1);
                            }
                        }
                    }

                    var sound = soundInfo.vehicleReload3p;
                    if (sound != null) {
                        vehicle.level().playSound(vehicle, vehicle.getOnPos(), sound, SoundSource.PLAYERS, 2, 1);
                    }
                }
            }

            // 减少换弹剩余时间
            data.reload.reduce();

            // 执行换弹期间额外行为
            var behavior = data.item.reloadTimeBehaviors.get(data.reload.time());
            if (behavior != null) {
                behavior.accept(data);
            }

            // 换弹完成
            if (data.reload.time() == 1) {
                finishReload(shooter, data);
            }

            handleGunSingleReload(shooter, data);
            handleSentinelCharge(shooter, data);
        }

        if (inMainHand && !data.reloading()) {
            if (data.currentAvailableShots(shooter) <= data.item.hideBulletChainBelowShots()) {
                data.hideBulletChain.set(true);
            }
            if (!data.hasEnoughAmmoToShoot(shooter)) {
                data.item.whenNoAmmo(data);
            }
        }

        data.save();
    }

    private static void startReload(@Nullable Entity shooter, @NotNull GunData data) {
        var reload = data.reload;

        var computed = data.compute();
        if (data.item.isOpenBolt(data)) {
            if (!data.hasEnoughAmmoToShoot(shooter)) {
                reload.setTime(computed.emptyReloadTime + 1);
                reload.setState(ReloadState.EMPTY_RELOADING);
                playGunEmptyReloadSounds(shooter, data);
            } else {
                reload.setTime(computed.normalReloadTime + 1);
                reload.setState(ReloadState.NORMAL_RELOADING);
                playGunNormalReloadSounds(shooter, data);
            }
        } else {
            reload.setTime(computed.emptyReloadTime + 2);
            reload.setState(ReloadState.EMPTY_RELOADING);
            playGunEmptyReloadSounds(shooter, data);
        }
    }

    public static void finishGunNormalReload(@Nullable Entity shooter, @NotNull GunData data) {
        var gunItem = data.item();
        data.reloadAmmo(shooter, gunItem.hasBulletInBarrel(data));
        NeoForge.EVENT_BUS.post(new ReloadEvent.Post(shooter, data));
    }

    public static void finishGunEmptyReload(@Nullable Entity shooter, @NotNull GunData data) {
        data.reloadAmmo(shooter);
        NeoForge.EVENT_BUS.post(new ReloadEvent.Post(shooter, data));
    }

    public static void playGunEmptyReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter instanceof ServerPlayer serverPlayer) {
            var soundInfo = data.compute().soundInfo;
            var sound = soundInfo.reloadEmpty;

            if (sound != null) {
                SoundTool.playLocalSound(serverPlayer, sound, 8f, 1f);
            }
        }
    }

    public static void playGunNormalReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter instanceof ServerPlayer serverPlayer) {
            var soundInfo = data.compute().soundInfo;
            var sound = soundInfo.reloadNormal;

            if (sound != null) {
                SoundTool.playLocalSound(serverPlayer, sound, 8f, 1f);
            }
        }
    }

    /**
     * 单发装填类的武器换弹流程
     */
    private static void handleGunSingleReload(@Nullable Entity shooter, @NotNull GunData data) {
        var stack = data.stack();
        var reload = data.reload;

        // 换弹流程计时器
        reload.prepareTimer.reduce();
        reload.prepareLoadTimer.reduce();
        reload.iterativeLoadTimer.reduce();
        reload.finishTimer.reduce();

        // 一阶段
        var computed = data.compute();
        if (reload.singleReloadStarter.start()) {
            NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(shooter, data));

            if (computed.prepareLoadTime != 0 && (!data.hasEnoughAmmoToShoot(shooter) || stack.is(ModItems.SECONDARY_CATACLYSM.get()))) {
                // 此处判断空仓换弹的时候，是否在准备阶段就需要装填一发，如M870
                playGunPrepareLoadReloadSounds(shooter, data);
                int prepareLoadTime = computed.prepareLoadTime;
                reload.prepareLoadTimer.set(prepareLoadTime + 1);
            } else if (computed.prepareEmptyTime != 0 && !data.hasEnoughAmmoToShoot(shooter)) {
                // 此处判断空仓换弹，如莫辛纳甘
                playGunEmptyPrepareSounds(shooter, data);
                int prepareEmptyTime = computed.prepareEmptyTime;
                reload.prepareTimer.set(prepareEmptyTime + 1);
            } else {
                playGunPrepareReloadSounds(shooter, data);
                int prepareTime = computed.prepareTime;
                reload.prepareTimer.set(prepareTime + 1);
            }

            data.forceStop.set(false);
            data.stopped.set(false);
            reload.setStage(1);
            reload.setState(ReloadState.NORMAL_RELOADING);
        }

        if (reload.prepareLoadTimer.get() == computed.prepareAmmoLoadTime) {
            iterativeLoad(shooter, data);
        }

        // 一阶段结束，检查备弹，如果有则二阶段启动，无则直接跳到三阶段
        if ((reload.prepareTimer.get() == 1 || reload.prepareLoadTimer.get() == 1)) {
            if (!data.hasBackupAmmo(shooter) || data.ammo.get() >= computed.magazine) {
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
                && data.ammo.get() < computed.magazine
        ) {
            playGunLoopReloadSounds(shooter, data);
            int iterativeTime = data.compute().iterativeTime;
            reload.iterativeLoadTimer.set(iterativeTime);

            // 动画播放nbt
            data.loadIndex.set(data.loadIndex.get() == 1 ? 0 : 1);
        }

        // 装填
        if (computed.iterativeAmmoLoadTime == reload.iterativeLoadTimer.get()) {
            iterativeLoad(shooter, data);
        }

        // 二阶段打断
        if (reload.iterativeLoadTimer.get() == 1) {
            // 装满或备弹耗尽结束
            if (!data.hasBackupAmmo(shooter) || data.ammo.get() >= computed.magazine) {
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

            int finishTime = data.compute().finishTime;
            reload.finishTimer.set(finishTime + 2);

            playGunEndReloadSounds(shooter, data);
        }

        if (stack.getItem() == ModItems.MARLIN.get() && reload.finishTimer.get() == 10) {
            data.isEmpty.set(false);
        }

        // 三阶段结束
        if (reload.finishTimer.get() == 1) {
            reload.setStage(0);
            if (computed.boltActionTime > 0) {
                data.bolt.needed.set(false);
            }
            reload.setState(ReloadState.NOT_RELOADING);
            reload.singleReloadStarter.finish();

            NeoForge.EVENT_BUS.post(new ReloadEvent.Post(shooter, data));
        }
    }

    public static void iterativeLoad(@Nullable Entity shooter, @NotNull GunData data) {
        var required = Math.min(data.compute().magazine - data.ammo.get(), data.compute().iterativeLoadAmount);
        var available = Math.min(required, data.countBackupAmmo(shooter));
        data.ammo.add(available);
        if (!InventoryTool.hasCreativeAmmoBox(shooter)) {
            if (shooter != null) {
                var cap = shooter.getData(ModAttachments.PLAYER_VARIABLE);
                shooter.setData(ModAttachments.PLAYER_VARIABLE, cap);
            }
            data.consumeBackupAmmo(shooter, available);
        }
    }

    public static void playGunPrepareReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter instanceof ServerPlayer serverPlayer) {
            var soundInfo = data.compute().soundInfo;
            var sound = soundInfo.reloadPrepare;

            if (sound != null) {
                SoundTool.playLocalSound(serverPlayer, sound, 10f, 1f);
            }
        }
    }

    public static void playGunEmptyPrepareSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter instanceof ServerPlayer serverPlayer) {
            var soundInfo = data.compute().soundInfo;
            var sound = soundInfo.reloadPrepareEmpty;

            if (sound != null) {
                SoundTool.playLocalSound(serverPlayer, sound, 10f, 1f);
            }

            double shooterHeight = shooter.getEyePosition().distanceTo((Vec3.atLowerCornerOf(shooter.level().clip(new ClipContext(shooter.getEyePosition(), shooter.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter)).getBlockPos())));

            Mod.queueServerWork((int) (data.compute().prepareEmptyTime / 2.0 + 3 + 1.5 * shooterHeight), () -> {
                if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                    var ammoType = data.selectedAmmoConsumer().getPlayerAmmoType();
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

    public static void playGunPrepareLoadReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter instanceof ServerPlayer serverPlayer) {
            var soundInfo = data.compute().soundInfo;
            var sound = soundInfo.reloadPrepareLoad;

            if (sound != null) {
                SoundTool.playLocalSound(serverPlayer, sound, 10f, 1f);
            }

            double shooterHeight = shooter.getEyePosition().distanceTo((Vec3.atLowerCornerOf(shooter.level().clip(new ClipContext(shooter.getEyePosition(), shooter.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter)).getBlockPos())));

            Mod.queueServerWork((int) (8 + 1.5 * shooterHeight), () -> {
                if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                    var ammoType = data.selectedAmmoConsumer().getPlayerAmmoType();
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

    public static void playGunLoopReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter instanceof ServerPlayer serverPlayer) {
            var soundInfo = data.compute().soundInfo;
            var sound = soundInfo.reloadLoop;

            if (sound != null) {
                SoundTool.playLocalSound(serverPlayer, sound, 10f, 1f);
            }
        }
    }

    public static void playGunEndReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter instanceof ServerPlayer serverPlayer) {
            var soundInfo = data.compute().soundInfo;
            var sound = soundInfo.reloadEnd;

            if (sound != null) {
                SoundTool.playLocalSound(serverPlayer, sound, 10f, 1f);
            }

            double shooterHeight = shooter.getEyePosition().distanceTo((Vec3.atLowerCornerOf(shooter.level().clip(new ClipContext(shooter.getEyePosition(), shooter.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter)).getBlockPos())));

            // TODO 为什么要特判这个
            if (data.stack.is(ModItems.MARLIN.get())) {
                Mod.queueServerWork((int) (5 + 1.5 * shooterHeight), () -> SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1));
            }
        }
    }

    /**
     * 哨兵充能
     */
    private static void handleSentinelCharge(Entity entity, GunData data) {
        // 启动充能
        if (data.charge.starter.start()) {
            data.charge.timer.set(127);

            if (entity instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.SENTINEL_CHARGE.get(), 2f, 1f);
            }
        }

        data.charge.timer.reduce();

        if (data.charge.timer.get() == 17) {
            var itemHandler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
            if (itemHandler == null) return;

            for (int i = 0; i < itemHandler.getSlots(); i++) {
                var cell = itemHandler.getStackInSlot(i);

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

    // TODO 正确实现更新注册名
//    @SubscribeEvent
//    public static void onMissingMappings(MissingMappingsEvent event) {
//        for (MissingMappingsEvent.Mapping<Item> mapping : event.getAllMappings(Registries.ITEM)) {
//            if (Mod.MODID.equals(mapping.getKey().getNamespace())) {
//                var item = mapping.getKey().getPath();
//                if (item.equals("abekiri")) {
//                    mapping.remap(ModItems.HOMEMADE_SHOTGUN.get());
//                }
//                if (item.equals("m2hb_blueprint")) {
//                    mapping.remap(ModItems.M_2_HB_BLUEPRINT.get());
//                }
//                if (item.equals("rocket_70")) {
//                    mapping.remap(ModItems.SMALL_ROCKET.get());
//                }
//                if (item.equals("us_helmet_pastg")) {
//                    mapping.remap(ModItems.US_HELMET_PASGT.get());
//                }
//            }
//        }
//    }
}
