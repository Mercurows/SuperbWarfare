package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.value.ReloadState;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber
public class GunEventHandler {

    /**
     * 拉大栓
     */
    private static void handleGunBolt(@NotNull GunData data) {
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

    /**
     * 播放拉栓音效
     */
    public static void playGunBoltSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter != null && !shooter.level().isClientSide) {
            String origin = data.stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + "_bolt"));
            if (sound1p != null && shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 2f, 1f);

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
    }

    /**
     * 完成换弹过程，装填弹药
     */
    private static void finishReload(@Nullable Entity shooter, @NotNull GunData data) {
        if (data.item.isOpenBolt(data.stack)) {
            if (data.ammo.get() == 0) {
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
                data.ammo.set(data.get(GunProp.MAGAZINE));
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

    /**
     * 减少过热值
     */
    public static void handleCooldown(@Nullable Entity shooter, @NotNull GunData data) {
        double extraCooldown = 0;
        if (shooter != null) {
            if (shooter.wasInPowderSnow) {
                extraCooldown = 0.15;
            } else if (shooter.isInWaterOrRain()) {
                extraCooldown = 0.04;
            } else if (shooter.isOnFire() || shooter.isInLava()) {
                extraCooldown = -0.1;
            }
        }

        data.heat.set(Mth.clamp(data.heat.get() - 0.25 - extraCooldown, 0, 100));

        if (data.heat.get() < 80 && data.overHeat.get()) {
            data.overHeat.set(false);
        }
    }

    /**
     * 返还多余弹药
     */
    public static void redrawExtraAmmo(@Nullable Entity shooter, @NotNull GunData data) {
        var hasBulletInBarrel = data.item.hasBulletInBarrel(data.stack);
        var ammoCount = data.ammo.get();
        var magazine = data.get(GunProp.MAGAZINE);

        // TODO 修改为更正确的退弹药方式？
        if (((hasBulletInBarrel && ammoCount > magazine + 1) || (!hasBulletInBarrel && ammoCount > magazine))) {
            int count = ammoCount - magazine - (hasBulletInBarrel ? 1 : 0);

            if (shooter instanceof Player player) {
                PlayerVariable.modify(player, capability -> {
                    if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                        var ammoType = data.selectedAmmoConsumer().getPlayerAmmoType();
                        ammoType.add(capability, count);
                    }
                });
            }

            data.ammo.set(magazine + (hasBulletInBarrel ? 1 : 0));
        }
    }

    public static void gunTick(@Nullable Entity shooter, @NotNull GunData data, boolean inMainHand) {
        init(shooter, data);
        tickPerk(shooter, data);
        handleCooldown(shooter, data);
        redrawExtraAmmo(shooter, data);

        if (inMainHand) {
            handleGunBolt(data);

            // 启动换弹
            if (data.reload.reloadStarter.start()) {
                MinecraftForge.EVENT_BUS.post(new ReloadEvent.Pre(shooter, data));
                startReload(shooter, data);
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
            if (data.ammo.get() <= 5) {
                data.hideBulletChain.set(true);
            }
            if (data.ammo.get() == 0) {
                data.holdOpen.set(true);
                data.isEmpty.set(true);
            }
        }
    }

    private static void startReload(@Nullable Entity shooter, @NotNull GunData data) {
        var reload = data.reload;

        if (data.item.isOpenBolt(data.stack)) {
            if (data.ammo.get() == 0) {
                reload.setTime(data.get(GunProp.EMPTY_RELOAD_TIME) + 1);
                reload.setState(ReloadState.EMPTY_RELOADING);
                playGunEmptyReloadSounds(shooter, data);
            } else {
                reload.setTime(data.get(GunProp.NORMAL_RELOAD_TIME) + 1);
                reload.setState(ReloadState.NORMAL_RELOADING);
                playGunNormalReloadSounds(shooter, data);
            }
        } else {
            reload.setTime(data.get(GunProp.EMPTY_RELOAD_TIME) + 2);
            reload.setState(ReloadState.EMPTY_RELOADING);
            playGunEmptyReloadSounds(shooter, data);
        }
    }

    public static void finishGunNormalReload(@Nullable Entity shooter, @NotNull GunData data) {
        var stack = data.stack();
        var gunItem = data.item();

        data.reloadAmmo(shooter, gunItem.hasBulletInBarrel(stack));
        MinecraftForge.EVENT_BUS.post(new ReloadEvent.Post(shooter, data));
    }

    public static void finishGunEmptyReload(@Nullable Entity shooter, @NotNull GunData data) {
        data.reloadAmmo(shooter);
        MinecraftForge.EVENT_BUS.post(new ReloadEvent.Post(shooter, data));
    }

    public static void playGunEmptyReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        ItemStack stack = data.stack;

        if (shooter != null && !shooter.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + "_reload_empty"));
            if (sound1p != null && shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);
            }
        }
    }

    public static void playGunNormalReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        ItemStack stack = data.stack;

        if (shooter != null && !shooter.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + "_reload_normal"));

            if (sound1p != null && shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);
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
        if (reload.singleReloadStarter.start()) {
            MinecraftForge.EVENT_BUS.post(new ReloadEvent.Pre(shooter, data));

            if (data.get(GunProp.PREPARE_LOAD_TIME) != 0 && (data.ammo.get() == 0 || stack.is(ModItems.SECONDARY_CATACLYSM.get()))) {
                // 此处判断空仓换弹的时候，是否在准备阶段就需要装填一发，如M870
                playGunPrepareLoadReloadSounds(shooter, data);
                int prepareLoadTime = data.get(GunProp.PREPARE_LOAD_TIME);
                reload.prepareLoadTimer.set(prepareLoadTime + 1);
            } else if (data.get(GunProp.PREPARE_EMPTY_TIME) != 0 && data.ammo.get() == 0) {
                // 此处判断空仓换弹，如莫辛纳甘
                playGunEmptyPrepareSounds(shooter, data);
                int prepareEmptyTime = data.get(GunProp.PREPARE_EMPTY_TIME);
                reload.prepareTimer.set(prepareEmptyTime + 1);
            } else {
                playGunPrepareReloadSounds(shooter, data);
                int prepareTime = data.get(GunProp.PREPARE_TIME);
                reload.prepareTimer.set(prepareTime + 1);
            }

            data.forceStop.set(false);
            data.stopped.set(false);
            reload.setStage(1);
            reload.setState(ReloadState.NORMAL_RELOADING);
        }

        if (reload.prepareLoadTimer.get() == data.get(GunProp.PREPARE_AMMO_LOAD_TIME)) {
            iterativeLoad(shooter, data);
        }

        // 一阶段结束，检查备弹，如果有则二阶段启动，无则直接跳到三阶段
        if ((reload.prepareTimer.get() == 1 || reload.prepareLoadTimer.get() == 1)) {
            if (!data.hasBackupAmmo(shooter) || data.ammo.get() >= data.get(GunProp.MAGAZINE)) {
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
                && data.ammo.get() < data.get(GunProp.MAGAZINE)
        ) {
            playGunLoopReloadSounds(shooter, data);
            int iterativeTime = data.get(GunProp.ITERATIVE_TIME);
            reload.iterativeLoadTimer.set(iterativeTime);

            // 动画播放nbt
            data.loadIndex.set(data.loadIndex.get() == 1 ? 0 : 1);
        }

        // 装填
        if (data.get(GunProp.ITERATIVE_AMMO_LOAD_TIME) == reload.iterativeLoadTimer.get()) {
            iterativeLoad(shooter, data);
        }

        // 二阶段打断
        if (reload.iterativeLoadTimer.get() == 1) {
            // 装满或备弹耗尽结束
            if (!data.hasBackupAmmo(shooter) || data.ammo.get() >= data.get(GunProp.MAGAZINE)) {
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

            int finishTime = data.get(GunProp.FINISH_TIME);
            reload.finishTimer.set(finishTime + 2);

            playGunEndReloadSounds(shooter, data);
        }

        if (stack.getItem() == ModItems.MARLIN.get() && reload.finishTimer.get() == 10) {
            data.isEmpty.set(false);
        }

        // 三阶段结束
        if (reload.finishTimer.get() == 1) {
            reload.setStage(0);
            if (data.get(GunProp.BOLT_ACTION_TIME) > 0) {
                data.bolt.needed.set(false);
            }
            reload.setState(ReloadState.NOT_RELOADING);
            reload.singleReloadStarter.finish();

            MinecraftForge.EVENT_BUS.post(new ReloadEvent.Post(shooter, data));
        }
    }

    public static void iterativeLoad(@Nullable Entity shooter, @NotNull GunData data) {
        var required = Math.min(data.get(GunProp.MAGAZINE) - data.ammo.get(), data.get(GunProp.ITERATIVE_LOAD_AMOUNT));
        var available = Math.min(required, data.countBackupAmmo(shooter));
        data.ammo.add(available);

        if (!InventoryTool.hasCreativeAmmoBox(shooter)) {
            data.consumeBackupAmmo(shooter, 1);
        }
    }

    public static void playGunPrepareReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter != null && !shooter.level().isClientSide) {
            String origin = data.stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + "_prepare"));
            if (sound1p != null && shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);
            }
        }
    }

    public static void playGunEmptyPrepareSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter != null && !shooter.level().isClientSide) {
            String origin = data.stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + "_prepare_empty"));
            if (sound1p != null && shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);

                double shooterHeight = shooter.getEyePosition().distanceTo((Vec3.atLowerCornerOf(shooter.level().clip(new ClipContext(shooter.getEyePosition(), shooter.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter)).getBlockPos())));

                Mod.queueServerWork((int) (data.get(GunProp.PREPARE_EMPTY_TIME) / 2.0 + 3 + 1.5 * shooterHeight), () -> {
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
    }

    public static void playGunPrepareLoadReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        ItemStack stack = data.stack;

        if (shooter != null && !shooter.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + "_prepare_load"));
            if (sound1p != null && shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);

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
    }

    public static void playGunLoopReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter != null && !shooter.level().isClientSide) {
            String origin = data.stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + "_loop"));
            if (sound1p != null && shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);
            }
        }
    }

    public static void playGunEndReloadSounds(@Nullable Entity shooter, @NotNull GunData data) {
        if (shooter != null && !shooter.level().isClientSide) {
            String origin = data.stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc(name + "_end"));
            if (sound1p != null && shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);

                double shooterHeight = shooter.getEyePosition().distanceTo((Vec3.atLowerCornerOf(shooter.level().clip(new ClipContext(shooter.getEyePosition(), shooter.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter)).getBlockPos())));

                if (data.stack.is(ModItems.MARLIN.get())) {
                    Mod.queueServerWork((int) (5 + 1.5 * shooterHeight), () -> SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1));
                }
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

            SoundEvent sound1p = ForgeRegistries.SOUND_EVENTS.getValue(Mod.loc("sentinel_charge"));
            if (sound1p != null && entity instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 2f, 1f);
            }
        }

        data.charge.timer.reduce();

        if (data.charge.timer.get() == 17) {
            var cap = entity.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if (cap.resolve().isEmpty()) return;
            var itemHandler = cap.resolve().get();

            for (int i = 0; i < itemHandler.getSlots(); i++) {
                var cell = itemHandler.getStackInSlot(i);

                if (cell.is(ModItems.CELL.get())) {
                    var stackCap = data.stack().getCapability(ForgeCapabilities.ENERGY);
                    if (!stackCap.isPresent()) continue;

                    var stackStorage = stackCap.resolve().get();

                    int stackMaxEnergy = stackStorage.getMaxEnergyStored();
                    int stackEnergy = stackStorage.getEnergyStored();

                    var cellCap = cell.getCapability(ForgeCapabilities.ENERGY);
                    if (!cellCap.isPresent()) continue;

                    var cellStorage = cellCap.resolve().get();
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

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getAllMappings(Registries.ITEM)) {
            if (Mod.MODID.equals(mapping.getKey().getNamespace())) {
                var item = mapping.getKey().getPath();
                if (item.equals("abekiri")) {
                    mapping.remap(ModItems.HOMEMADE_SHOTGUN.get());
                }
                if (item.equals("m2hb_blueprint")) {
                    mapping.remap(ModItems.M_2_HB_BLUEPRINT.get());
                }
                if (item.equals("rocket_70")) {
                    mapping.remap(ModItems.SMALL_ROCKET.get());
                }
            }
        }
    }
}
