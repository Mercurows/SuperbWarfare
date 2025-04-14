package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.event.events.ReloadEvent;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.PressFireSpecialWeapon;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.AttachmentType;
import com.atsuishio.superbwarfare.item.gun.data.value.ReloadState;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.AmmoType;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;

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
                if (stack.is(ModTags.Items.REVOLVER)) {
                    data.canImmediatelyShoot.set(true);
                }
            }
        }
    }

    /**
     * 根据武器的注册名来寻找音效并播放
     */
    public static void playGunSounds(Player player, boolean zoom) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            if (stack.getItem() == ModItems.SENTINEL.get()) {
                var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);

                if (cap != null && cap.getEnergyStored() > 0) {
                    float soundRadius = (float) data.soundRadius();

                    player.playSound(ModSounds.SENTINEL_CHARGE_FAR.get(), soundRadius * 0.7f, 1f);
                    player.playSound(ModSounds.SENTINEL_CHARGE_FIRE_3P.get(), soundRadius * 0.4f, 1f);
                    player.playSound(ModSounds.SENTINEL_CHARGE_VERYFAR.get(), soundRadius, 1f);

                    return;
                }
            }

            if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get()) {
                var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);

                if (cap != null && cap.getEnergyStored() > 3000 && zoom) {
                    float soundRadius = (float) data.soundRadius();

                    player.playSound(ModSounds.SECONDARY_CATACLYSM_FIRE_3P_CHARGE.get(), soundRadius * 0.4f, 1f);
                    player.playSound(ModSounds.SECONDARY_CATACLYSM_FAR_CHARGE.get(), soundRadius * 0.7f, 1f);
                    player.playSound(ModSounds.SECONDARY_CATACLYSM_VERYFAR_CHARGE.get(), soundRadius, 1f);

                    return;
                }
            }

            var perk = data.perk.get(Perk.Type.AMMO);
            if (perk == ModPerks.BEAST_BULLET.get()) {
                player.playSound(ModSounds.HENG.get(), 4f, 1f);
            }

            float soundRadius = (float) data.soundRadius();
            int barrelType = data.attachment.get(AttachmentType.BARREL);

            SoundEvent sound3p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + (barrelType == 2 ? "_fire_3p_s" : "_fire_3p")));
            if (sound3p != null) {
                player.playSound(sound3p, soundRadius * 0.4f, 1f);
            }

            SoundEvent soundFar = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + (barrelType == 2 ? "_far_s" : "_far")));
            if (soundFar != null) {
                player.playSound(soundFar, soundRadius * 0.7f, 1f);
            }

            SoundEvent soundVeryFar = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + (barrelType == 2 ? "_veryfar_s" : "_veryfar")));
            if (soundVeryFar != null) {
                player.playSound(soundVeryFar, soundRadius, 1f);
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

                if (stack.is(ModTags.Items.REVOLVER)) return;

                Mod.queueServerWork((int) (data.defaultActionTime() / 2 + 1.5 * shooterHeight), () -> {
                    if (stack.is(ModTags.Items.SHOTGUN)) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_SHOTGUN.get(), (float) Math.max(0.75 - 0.12 * shooterHeight, 0), 1);
                    } else if (stack.is(ModTags.Items.SNIPER_RIFLE)) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_50CAL.get(), (float) Math.max(1 - 0.15 * shooterHeight, 0), 1);
                    } else {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_NORMAL.get(), (float) Math.max(1.5 - 0.2 * shooterHeight, 0), 1);
                    }
                });
            }
        }
    }

    public static void gunShoot(Player player, GunData data, double spared, boolean zoom) {
        var stack = data.stack();

        if (!player.level().isClientSide()) {
            if (stack.getItem() instanceof PressFireSpecialWeapon pressFireSpecialWeapon) {
                pressFireSpecialWeapon.fireOnPress(player, data, spared, zoom);
            } else {
                float headshot = (float) data.headshot();
                float damage = (float) data.damage();
                float velocity = (float) (data.velocity() * perkSpeed(data));
                int projectileAmount = data.projectileAmount();
                float bypassArmorRate = (float) data.bypassArmor();
                var perkInstance = data.perk.getInstance(Perk.Type.AMMO);
                var perk = perkInstance != null ? perkInstance.perk() : null;

                ProjectileEntity projectile = new ProjectileEntity(player.level())
                        .shooter(player)
                        .damage(perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? projectileAmount * damage : damage)
                        .damage(damage)
                        .headShot(headshot)
                        .zoom(zoom)
                        .setGunItemId(stack);

                if (perk instanceof AmmoPerk ammoPerk) {
                    int level = data.perk.getLevel(perk);

                    bypassArmorRate += ammoPerk.bypassArmorRate + (perk == ModPerks.AP_BULLET.get() ? 0.05f * (level - 1) : 0);
                    projectile.setRGB(ammoPerk.rgb);

                    if (!ammoPerk.mobEffects.get().isEmpty()) {
                        int amplifier;
                        if (perk.descriptionId.equals("blade_bullet")) {
                            amplifier = level / 3;
                        } else if (perk.descriptionId.equals("bread_bullet")) {
                            amplifier = 1;
                        } else {
                            amplifier = level - 1;
                        }

                        ArrayList<MobEffectInstance> mobEffectInstances = new ArrayList<>();
                        for (MobEffect effect : ammoPerk.mobEffects.get()) {
                            mobEffectInstances.add(new MobEffectInstance(Holder.direct(effect), 70 + 30 * level, amplifier));
                        }
                        projectile.effect(mobEffectInstances);
                    }

                    if (perk.descriptionId.equals("bread_bullet")) {
                        projectile.knockback(level * 0.3f);
                        projectile.forceKnockback();
                    }
                }

                bypassArmorRate = Math.max(bypassArmorRate, 0);
                projectile.bypassArmorRate(bypassArmorRate);

                if (perk == ModPerks.SILVER_BULLET.get()) {
                    int level = data.perk.getLevel(perk);
                    projectile.undeadMultiple(1.0f + 0.5f * level);
                } else if (perk == ModPerks.BEAST_BULLET.get()) {
                    projectile.beast();
                } else if (perk == ModPerks.JHP_BULLET.get()) {
                    int level = data.perk.getLevel(perk);
                    projectile.jhpBullet(level);
                } else if (perk == ModPerks.HE_BULLET.get()) {
                    int level = data.perk.getLevel(perk);
                    projectile.heBullet(level);
                } else if (perk == ModPerks.INCENDIARY_BULLET.get()) {
                    int level = data.perk.getLevel(perk);
                    projectile.fireBullet(level, stack.is(ModTags.Items.SHOTGUN));
                }

                var dmgPerk = data.perk.get(Perk.Type.DAMAGE);
                if (dmgPerk == ModPerks.MONSTER_HUNTER.get()) {
                    int level = data.perk.getLevel(dmgPerk);
                    projectile.monsterMultiple(0.1f + 0.1f * level);
                }

                projectile.setPos(player.getX() - 0.1 * player.getLookAngle().x, player.getEyeY() - 0.1 - 0.1 * player.getLookAngle().y, player.getZ() + -0.1 * player.getLookAngle().z);
                projectile.shoot(player, player.getLookAngle().x, player.getLookAngle().y + 0.001f, player.getLookAngle().z, stack.is(ModTags.Items.SHOTGUN) && perk == ModPerks.INCENDIARY_BULLET.get() ? 4.5f : velocity, (float) spared);
                player.level().addFreshEntity(projectile);
            }
        }
    }

    public static double perkSpeed(GunData data) {
        var perk = data.perk.get(Perk.Type.AMMO);
        if (perk instanceof AmmoPerk ammoPerk) {
            return ammoPerk.speedRate;
        }
        return 1;
    }

    /**
     * 通用的武器换弹流程
     */
    private static void handleGunReload(Player player, GunData gun) {
        var stack = gun.stack();
        var gunItem = gun.item();
        var reload = gun.reload;

        // 启动换弹
        if (reload.reloadStarter.start()) {
            NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(player, stack));

            if (gunItem.isOpenBolt(stack)) {
                if (gun.ammo.get() == 0) {
                    reload.setTime(gun.defaultEmptyReloadTime() + 1);
                    reload.setState(ReloadState.EMPTY_RELOADING);
                    playGunEmptyReloadSounds(player);
                } else {
                    reload.setTime(gun.defaultNormalReloadTime() + 1);
                    reload.setState(ReloadState.NORMAL_RELOADING);
                    playGunNormalReloadSounds(player);
                }
            } else {
                reload.setTime(gun.defaultEmptyReloadTime() + 2);
                reload.setState(ReloadState.EMPTY_RELOADING);
                playGunEmptyReloadSounds(player);
            }
        }

        reload.reduce();

        // 换弹时额外行为
        var behavior = gunItem.reloadTimeBehaviors.get(reload.time());
        if (behavior != null) {
            behavior.accept(gun);
        }

        if (reload.time() == 1) {
            if (gunItem.isOpenBolt(stack)) {
                if (gun.ammo.get() == 0) {
                    playGunEmptyReload(player, gun);
                } else {
                    playGunNormalReload(player, gun);
                }
            } else {
                playGunEmptyReload(player, gun);
            }
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
                GunsTool.reload(player, stack, data, AmmoType.getType(ammoTypeInfo.value()), gunItem.hasBulletInBarrel(stack));
            }
        }
        data.reload.setState(ReloadState.NOT_RELOADING);
        NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, stack));
    }

    public static void playGunEmptyReload(Player player, GunData data) {
        ItemStack stack = data.stack();

        if (player.getInventory().hasAnyMatching(item -> item.is(ModItems.CREATIVE_AMMO_BOX.get()))) {
            data.ammo.set(data.magazine());
        } else {
            var ammoTypeInfo = data.ammoTypeInfo();
            switch (ammoTypeInfo.type()) {
                case PLAYER_AMMO -> GunsTool.reload(player, stack, data, AmmoType.getType(ammoTypeInfo.value()));
                case TAG -> {
                    data.ammo.set(1);
                    player.getInventory().clearOrCountMatchingItems(
                            p -> p.is(ItemTags.create(ResourceLocation.parse(ammoTypeInfo.value()))),
                            1,
                            player.inventoryMenu.getCraftSlots()
                    );
                }
                case ITEM -> {
                    data.ammo.set(1);
                    player.getInventory().clearOrCountMatchingItems(
                            p -> p.getItem().getDescriptionId().equals(ammoTypeInfo.value()),
                            1,
                            player.inventoryMenu.getCraftSlots()
                    );
                }
            }
        }
        data.reload.setState(ReloadState.NOT_RELOADING);
        NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, stack));
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

            SoundEvent sound1p;
            sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_reload_normal"));

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

//        player.displayClientMessage(Component.literal("prepare: " +  new DecimalFormat("##.#").format(data.reload.prepareTimer.get())
//                        + " prepare_load: " +  new DecimalFormat("##.#").format(data.reload.iterativeLoadTimer.get())
//                        + " iterative: " +  new DecimalFormat("##.#").format(tag.getDouble("IterativeLoadTime"))
//                        + " finish: " +  new DecimalFormat("##.#").format(tag.getDouble("FinishTime"))
//                        + " reload_stage: " +  new DecimalFormat("##.#").format(tag.getDouble("reload_stage"))
//        ), true);

        // 一阶段
        if (reload.singleReloadStarter.start()) {
            NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(player, stack));

            if ((data.defaultPrepareLoadTime() != 0 && data.ammo.get() == 0) || stack.is(ModItems.SECONDARY_CATACLYSM.get())) {
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

        if (stack.getItem() == ModItems.M_870.get() && reload.prepareLoadTimer.get() == 10) {
            singleLoad(player, data);
        }

        if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get() && reload.prepareLoadTimer.get() == 3) {
            singleLoad(player, data);
        }

        // 一阶段结束，检查备弹，如果有则二阶段启动，无则直接跳到三阶段
        if ((reload.prepareTimer.get() == 1 || reload.prepareLoadTimer.get() == 1)) {
            if (!InventoryTool.hasCreativeAmmoBox(player)) {
                var capability = player.getData(ModAttachments.PLAYER_VARIABLE);
                var startStage3 = false;

                var ammoTypeInfo = data.ammoTypeInfo();
                if (ammoTypeInfo.type() == GunData.AmmoConsumeType.PLAYER_AMMO) {
                    var type = AmmoType.getType(ammoTypeInfo.value());
                    assert type != null;

                    if (type.get(capability) == 0) {
                        startStage3 = true;
                    }
                }

                // TODO 优化这坨判断
                if (stack.is(ModTags.Items.LAUNCHER) && data.maxAmmo.get() == 0
                        || stack.is(ModItems.SECONDARY_CATACLYSM.get()) && data.ammo.get() >= data.magazine()
                ) {
                    startStage3 = true;
                }

                if (startStage3) {
                    reload.stage3Starter.markStart();
                } else {
                    reload.setStage(2);
                }
            } else {
                if (stack.is(ModItems.SECONDARY_CATACLYSM.get()) && data.ammo.get() >= data.magazine()) {
                    reload.stage3Starter.markStart();
                } else {
                    reload.setStage(2);
                }
            }
            // 检查备弹
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
        if ((stack.getItem() == ModItems.M_870.get()
                || stack.getItem() == ModItems.MARLIN.get())
                && reload.iterativeLoadTimer.get() == 3
        ) {
            singleLoad(player, data);
        }

        if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get() && reload.iterativeLoadTimer.get() == 16) {
            singleLoad(player, data);
        }

        if ((stack.getItem() == ModItems.K_98.get() || stack.getItem() == ModItems.MOSIN_NAGANT.get())
                && reload.iterativeLoadTimer.get() == 1
        ) {
            singleLoad(player, data);
        }

        // 二阶段结束
        if (reload.iterativeLoadTimer.get() == 1) {
            // 装满结束
            if (data.ammo.get() >= data.magazine()) {
                reload.setStage(3);
            }

            // 备弹耗尽结束
            if (!InventoryTool.hasCreativeAmmoBox(player)) {
                var capability = player.getData(ModAttachments.PLAYER_VARIABLE);

                var ammoTypeInfo = data.ammoTypeInfo();
                if (ammoTypeInfo.type() == GunData.AmmoConsumeType.PLAYER_AMMO) {
                    var type = AmmoType.getType(ammoTypeInfo.value());
                    assert type != null;

                    if (type.get(capability) == 0) {
                        reload.setStage(3);
                    }
                }
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

            NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, stack));
        }
    }

    public static void singleLoad(Player player, GunData data) {
        data.ammo.set(data.ammo.get() + 1);

        if (!InventoryTool.hasCreativeAmmoBox(player)) {
            var cap = player.getData(ModAttachments.PLAYER_VARIABLE);

            var ammoTypeInfo = data.ammoTypeInfo();
            switch (ammoTypeInfo.type()) {
                case PLAYER_AMMO -> {
                    var type = AmmoType.getType(ammoTypeInfo.value());
                    assert type != null;

                    type.add(cap, -1);
                }
                case ITEM -> player.getInventory().clearOrCountMatchingItems(
                        p -> p.getItem().getDescriptionId().equals(ammoTypeInfo.value()),
                        1,
                        player.inventoryMenu.getCraftSlots()
                );
                case TAG -> player.getInventory().clearOrCountMatchingItems(
                        p -> p.is(ItemTags.create(ResourceLocation.parse(ammoTypeInfo.value()))),
                        1,
                        player.inventoryMenu.getCraftSlots()
                );
            }

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
                    if (stack.is(ModTags.Items.SHOTGUN)) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_SHOTGUN.get(), (float) Math.max(0.75 - 0.12 * shooterHeight, 0), 1);
                    } else if (stack.is(ModTags.Items.SNIPER_RIFLE) || stack.is(ModTags.Items.HEAVY_WEAPON)) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_50CAL.get(), (float) Math.max(1 - 0.15 * shooterHeight, 0), 1);
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

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_prepare_load"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);

                double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

                Mod.queueServerWork((int) (8 + 1.5 * shooterHeight), () -> {
                    if (stack.is(ModTags.Items.SHOTGUN)) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_SHOTGUN.get(), (float) Math.max(0.75 - 0.12 * shooterHeight, 0), 1);
                    } else if (stack.is(ModTags.Items.SNIPER_RIFLE) || stack.is(ModTags.Items.HEAVY_WEAPON)) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.SHELL_CASING_50CAL.get(), (float) Math.max(1 - 0.15 * shooterHeight, 0), 1);
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
