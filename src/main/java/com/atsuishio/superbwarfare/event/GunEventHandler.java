package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.event.events.ReloadEvent;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.AttachmentType;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.ReloadState;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.AmmoType;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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

        if (stack.is(ModTags.Items.GUN)) {
            var data = GunData.from(stack);

            handleGunBolt(player, data);
            handleGunReload(player, data);
            handleGunSingleReload(player, data);
            handleSentinelCharge(player, data);

            data.save();
        }
    }

    /**
     * 拉大栓
     */
    private static void handleGunBolt(Player player, GunData data) {
        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModTags.Items.NORMAL_GUN)) {
            var tag = data.tag();

            if (data.bolt.actionTime() > 0) {
                data.bolt.reduceActionTime();
            }

            if (stack.getItem() == ModItems.MARLIN.get() && data.bolt.actionTime() == 9) {
                tag.remove("IsEmpty");
            }

            if (data.bolt.actionTime() == 1) {
                data.bolt.markNeedless();
                if (stack.is(ModTags.Items.REVOLVER)) {
                    tag.putBoolean("canImmediatelyShoot", true);
                }
            }
        }
    }

    /**
     * 根据武器的注册名来寻找音效并播放
     */
    public static void playGunSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;
        var data = GunData.from(stack);
        var tag = data.tag();

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            if (stack.getItem() == ModItems.SENTINEL.get()) {
                var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);

                if (cap != null && cap.getEnergyStored() > 0) {
                    float soundRadius = (float) data.soundRadius();

                    SoundEvent sound3p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc("sentinel_charge_fire_3p"));
                    if (sound3p != null) {
                        player.playSound(sound3p, soundRadius * 0.4f, 1f);
                    }

                    SoundEvent soundFar = BuiltInRegistries.SOUND_EVENT.get(Mod.loc("sentinel_charge_far"));
                    if (soundFar != null) {
                        player.playSound(soundFar, soundRadius * 0.7f, 1f);
                    }

                    SoundEvent soundVeryFar = BuiltInRegistries.SOUND_EVENT.get(Mod.loc("sentinel_charge_veryfar"));
                    if (soundVeryFar != null) {
                        player.playSound(soundVeryFar, soundRadius, 1f);
                    }
                    return;
                }
            }

            var perk = PerkHelper.getPerkByType(tag, Perk.Type.AMMO);
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
        if (!stack.is(ModTags.Items.GUN)) return;
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

                Mod.queueServerWork((int) (data.bolt.defaultActionTime() / 2 + 1.5 * shooterHeight), () -> {
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

    public static void gunShoot(Player player, final CompoundTag tag, double spared) {
        ItemStack stack = player.getMainHandItem();
        var data = GunData.from(stack);

        if (!player.level().isClientSide()) {
            float headshot = (float) data.headshot();
            float damage = (float) data.damage();
            float velocity = (float) ((data.velocity() + GunsTool.getGunDoubleTag(tag, "CustomVelocity")) * perkSpeed(tag));
            int projectileAmount = data.projectileAmount();
            float bypassArmorRate = (float) data.bypassArmor();
            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            boolean zoom = cap != null && cap.zoom;
            var perk = PerkHelper.getPerkByType(tag, Perk.Type.AMMO);

            if (perk != null && perk.descriptionId.equals("butterfly_bullet")) {
                if (handleButterflyBullet(perk, stack, player)) return;
            }

            ProjectileEntity projectile = new ProjectileEntity(player.level())
                    .shooter(player)
                    .damage(perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? projectileAmount * damage : damage)
                    .damage(damage)
                    .headShot(headshot)
                    .zoom(zoom);

            if (perk instanceof AmmoPerk ammoPerk) {
                int level = PerkHelper.getItemPerkLevel(perk, tag);

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
                int level = PerkHelper.getItemPerkLevel(perk, tag);
                projectile.undeadMultiple(1.0f + 0.5f * level);
            } else if (perk == ModPerks.BEAST_BULLET.get()) {
                projectile.beast();
            } else if (perk == ModPerks.JHP_BULLET.get()) {
                int level = PerkHelper.getItemPerkLevel(perk, tag);
                projectile.jhpBullet(level);
            } else if (perk == ModPerks.HE_BULLET.get()) {
                int level = PerkHelper.getItemPerkLevel(perk, tag);
                projectile.heBullet(level);
            } else if (perk == ModPerks.INCENDIARY_BULLET.get()) {
                int level = PerkHelper.getItemPerkLevel(perk, tag);
                projectile.fireBullet(level, stack.is(ModTags.Items.SHOTGUN));
            }

            var dmgPerk = PerkHelper.getPerkByType(tag, Perk.Type.DAMAGE);
            if (dmgPerk == ModPerks.MONSTER_HUNTER.get()) {
                int level = PerkHelper.getItemPerkLevel(dmgPerk, tag);
                projectile.monsterMultiple(0.1f + 0.1f * level);
            }

            projectile.setPos(player.getX() - 0.1 * player.getLookAngle().x, player.getEyeY() - 0.1 - 0.1 * player.getLookAngle().y, player.getZ() + -0.1 * player.getLookAngle().z);
            projectile.shoot(player, player.getLookAngle().x, player.getLookAngle().y + 0.001f, player.getLookAngle().z, stack.is(ModTags.Items.SHOTGUN) && perk == ModPerks.INCENDIARY_BULLET.get() ? 4.5f : velocity, (float) spared);
            player.level().addFreshEntity(projectile);
        }
    }

    public static double perkSpeed(final CompoundTag tag) {
        var perk = PerkHelper.getPerkByType(tag, Perk.Type.AMMO);
        if (perk instanceof AmmoPerk ammoPerk) {
            return ammoPerk.speedRate;
        }
        return 1;
    }

    // TODO 这还有联动的必要吗（
    private static boolean handleButterflyBullet(Perk perk, ItemStack heldItem, Player player) {
        return true;
//        int perkLevel = PerkHelper.getItemPerkLevel(perk, tag);
//
//        var entityType = CompatHolder.VRC_RAIN_SHOWER_BUTTERFLY;
//        if (entityType != null) {
//            Projectile projectile = entityType.create(player.level());
//
//            float inaccuracy = Math.max(0.0f, 1.1f - perkLevel * .1f);
//            projectile.setOwner(player);
//            projectile.setPos(player.getX() - 0.1 * player.getLookAngle().x,
//                    player.getEyeY() - 0.1 - 0.1 * player.getLookAngle().y, player.getZ() + -0.1 * player.getLookAngle().z);
//
//            Vec3 vec3 = (new Vec3(player.getLookAngle().x, player.getLookAngle().y + 0.001f, player.getLookAngle().z)).normalize().scale(1.2).
//                    add(player.level().random.triangle(0.0D, 0.0172275D * (double) inaccuracy),
//                            player.level().random.triangle(0.0D, 0.0172275D * (double) inaccuracy),
//                            player.level().random.triangle(0.0D, 0.0172275D * (double) inaccuracy)).
//                    add(player.getDeltaMovement().x, player.onGround() ? 0.0 : 0.05 * player.getDeltaMovement().y, player.getDeltaMovement().z).
//                    scale(5.0f);
//            projectile.setDeltaMovement(vec3);
//            projectile.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)));
//            projectile.setXRot((float) (Mth.atan2(vec3.y, vec3.horizontalDistance()) * (double) (180F / (float) Math.PI)));
//            projectile.yRotO = projectile.getYRot();
//            projectile.xRotO = projectile.getXRot();
//
//            projectile.setNoGravity(true);
//            player.level().addFreshEntity(projectile);
//            return true;
//        }
//
//        return false;
    }

    /**
     * 通用的武器换弹流程
     */
    private static void handleGunReload(Player player, GunData gunData) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;

        var tag = gunData.tag();
        var data = gunData.data();

        // 启动换弹
        if (gunData.reload.shouldStart()) {

            NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(player, stack));
            if (gunItem.isOpenBolt(stack)) {
                if (gunData.ammo() == 0) {
                    gunData.reload.setTime(gunData.emptyReloadTime() + 1);
                    gunData.reload.setState(ReloadState.EMPTY_RELOADING);
                    playGunEmptyReloadSounds(player);
                } else {
                    gunData.reload.setTime(gunData.normalReloadTime() + 1);
                    gunData.reload.setState(ReloadState.NORMAL_RELOADING);
                    playGunNormalReloadSounds(player);
                }
            } else {
                gunData.reload.setTime(gunData.emptyReloadTime() + 2);
                gunData.reload.setState(ReloadState.EMPTY_RELOADING);
                playGunEmptyReloadSounds(player);
            }
            gunData.reload.markStarted();
        }

        gunData.reload.reduce();

        if (stack.getItem() == ModItems.RPG.get()) {
            if (gunData.reload.time() == 84) {
                tag.remove("IsEmpty");
            }
            if (gunData.reload.time() == 9) {
                data.remove("CloseHammer");
            }
        }

        if (stack.getItem() == ModItems.MK_14.get() && gunData.reload.time() == 18) {
            data.remove("HoldOpen");
        }

        if (stack.getItem() == ModItems.SVD.get() && gunData.reload.time() == 17) {
            data.remove("HoldOpen");
        }

        if (stack.getItem() == ModItems.SKS.get() && gunData.reload.time() == 14) {
            data.remove("HoldOpen");
        }

        if (stack.getItem() == ModItems.M_60.get() && gunData.reload.time() == 55) {
            data.remove("HideBulletChain");
        }

        if (stack.getItem() == ModItems.GLOCK_17.get()
                || stack.getItem() == ModItems.GLOCK_18.get()
                || stack.getItem() == ModItems.M_1911.get()
                || stack.getItem() == ModItems.MP_443.get()
        ) {
            if (gunData.reload.time() == 9) {
                data.remove("HoldOpen");
            }
        }

        if (stack.getItem() == ModItems.QBZ_95.get() && gunData.reload.time() == 14) {
            data.remove("HoldOpen");
        }

        if (gunData.reload.time() == 1) {
            if (gunItem.isOpenBolt(stack)) {
                if (gunData.ammo() == 0) {
                    playGunEmptyReload(player, gunData);
                } else {
                    playGunNormalReload(player, gunData);
                }
            } else {
                playGunEmptyReload(player, gunData);
            }
            gunData.reload.markStarted();
        }
    }

    public static void playGunNormalReload(Player player, GunData gunData) {
        var stack = gunData.stack();
        var gunItem = gunData.item();

        if (player.getInventory().hasAnyMatching(item -> item.is(ModItems.CREATIVE_AMMO_BOX.get()))) {
            gunData.setAmmo(gunData.magazine() + (gunItem.hasBulletInBarrel(stack) ? 1 : 0));
        } else {
            if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.SHOTGUN, gunItem.hasBulletInBarrel(stack));
            } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.SNIPER, true);
            } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.HANDGUN, true);
            } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.RIFLE, gunItem.hasBulletInBarrel(stack));
            } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.HEAVY, gunItem.hasBulletInBarrel(stack));
            }
        }
        gunData.reload.setState(ReloadState.NOT_RELOADING);
        NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, stack));
    }

    public static void playGunEmptyReload(Player player, GunData gunData) {
        ItemStack stack = gunData.stack();

        if (player.getInventory().hasAnyMatching(item -> item.is(ModItems.CREATIVE_AMMO_BOX.get()))) {
            gunData.setAmmo(gunData.magazine());
        } else {
            if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.SHOTGUN);
            } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.SNIPER);
            } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.HANDGUN);
            } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.RIFLE);
            } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
                GunsTool.reload(player, stack, gunData, AmmoType.HEAVY);
            } else if (stack.getItem() == ModItems.TASER.get()) {
                gunData.setAmmo(1);
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.TASER_ELECTRODE.get(), 1, player.inventoryMenu.getCraftSlots());
            } else if (stack.getItem() == ModItems.M_79.get()) {
                gunData.setAmmo(1);
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.GRENADE_40MM.get(), 1, player.inventoryMenu.getCraftSlots());
            } else if (stack.getItem() == ModItems.RPG.get()) {
                gunData.setAmmo(1);
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.ROCKET.get(), 1, player.inventoryMenu.getCraftSlots());
            } else if (stack.getItem() == ModItems.JAVELIN.get()) {
                gunData.setAmmo(1);
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.JAVELIN_MISSILE.get(), 1, player.inventoryMenu.getCraftSlots());
            }
        }
        gunData.reload.setState(ReloadState.NOT_RELOADING);
        NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, stack));
    }

    public static void playGunEmptyReloadSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) {
            return;
        }

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
        if (!stack.is(ModTags.Items.GUN)) {
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
    private static void handleGunSingleReload(Player player, GunData gunData) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var tag = gunData.tag();

        // 换弹流程计时器
        if (tag.getDouble("PrepareTime") > 0) {
            tag.putDouble("PrepareTime", tag.getDouble("PrepareTime") - 1);
        }
        if (tag.getDouble("PrepareLoadTime") > 0) {
            tag.putDouble("PrepareLoadTime", tag.getDouble("PrepareLoadTime") - 1);
        }
        if (tag.getDouble("IterativeLoadTime") > 0) {
            tag.putDouble("IterativeLoadTime", tag.getDouble("IterativeLoadTime") - 1);
        }
        if (tag.getDouble("FinishTime") > 0) {
            tag.putDouble("FinishTime", tag.getDouble("FinishTime") - 1);
        }

//        player.displayClientMessage(Component.literal("prepare: " +  new DecimalFormat("##.#").format(tag.getDouble("PrepareTime"))
//                        + " prepare_load: " +  new DecimalFormat("##.#").format(tag.getDouble("PrepareLoadTime"))
//                        + " iterative: " +  new DecimalFormat("##.#").format(tag.getDouble("IterativeLoadTime"))
//                        + " finish: " +  new DecimalFormat("##.#").format(tag.getDouble("FinishTime"))
//                        + " reload_stage: " +  new DecimalFormat("##.#").format(tag.getDouble("reload_stage"))
//        ), true);

        // 一阶段
        if (tag.getBoolean("StartSingleReload")) {
            NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(player, stack));

            if ((gunData.prepareLoadTime() != 0 && gunData.ammo() == 0) || stack.is(ModItems.SECONDARY_CATACLYSM.get())) {
                // 此处判断空仓换弹的时候，是否在准备阶段就需要装填一发，如M870
                playGunPrepareLoadReloadSounds(player);
                int prepareLoadTime = gunData.prepareLoadTime();
                tag.putInt("PrepareLoadTime", prepareLoadTime + 1);
                player.getCooldowns().addCooldown(stack.getItem(), prepareLoadTime);
            } else if (gunData.prepareEmptyTime() != 0 && gunData.ammo() == 0) {
                // 此处判断空仓换弹，如莫辛纳甘
                playGunEmptyPrepareSounds(player);
                int prepareEmptyTime = gunData.prepareEmptyTime();
                tag.putInt("PrepareTime", prepareEmptyTime + 1);
                player.getCooldowns().addCooldown(stack.getItem(), prepareEmptyTime);
            } else {
                playGunPrepareReloadSounds(player);
                int prepareTime = gunData.prepareTime();
                tag.putInt("PrepareTime", prepareTime + 1);
                player.getCooldowns().addCooldown(stack.getItem(), prepareTime);
            }

            tag.remove("ForceStop");
            tag.remove("Stopped");
            gunData.reload.setStage(1);
            gunData.reload.setState(ReloadState.NORMAL_RELOADING);
            tag.remove("StartSingleReload");
        }

        if (stack.getItem() == ModItems.M_870.get() && tag.getInt("PrepareLoadTime") == 10) {
            singleLoad(player, gunData);
        }

        if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get() && tag.getInt("PrepareLoadTime") == 3) {
            singleLoad(player, gunData);
        }

        // 一阶段结束，检查备弹，如果有则二阶段启动，无则直接跳到三阶段
        if ((tag.getDouble("PrepareTime") == 1 || tag.getDouble("PrepareLoadTime") == 1)) {
            if (!InventoryTool.hasCreativeAmmoBox(player)) {
                var capability = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
                if (capability == null) capability = new PlayerVariable();

                if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO) && capability.shotgunAmmo == 0) {
                    tag.putBoolean("ForceStartStage3", true);
                } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO) && capability.sniperAmmo == 0) {
                    tag.putBoolean("ForceStartStage3", true);
                } else if ((stack.is(ModTags.Items.USE_HANDGUN_AMMO) || stack.is(ModTags.Items.SMG)) && capability.handgunAmmo == 0) {
                    tag.putBoolean("ForceStartStage3", true);
                } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO) && capability.rifleAmmo == 0) {
                    tag.putBoolean("ForceStartStage3", true);
                } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO) && capability.heavyAmmo == 0) {
                    tag.putBoolean("ForceStartStage3", true);
                } else if (stack.is(ModTags.Items.LAUNCHER) && GunsTool.getGunIntTag(tag, "MaxAmmo") == 0) {
                    tag.putBoolean("ForceStartStage3", true);
                } else if (stack.is(ModItems.SECONDARY_CATACLYSM.get()) && gunData.ammo() >= gunData.magazine()) {
                    tag.putBoolean("ForceStartStage3", true);
                } else {
                    gunData.reload.setStage(2);
                }
            } else {
                if (stack.is(ModItems.SECONDARY_CATACLYSM.get()) && gunData.ammo() >= gunData.magazine()) {
                    tag.putBoolean("ForceStartStage3", true);
                } else {
                    gunData.reload.setStage(2);
                }
            }
            // 检查备弹
        }

        // 强制停止换弹，进入三阶段
        if (tag.getBoolean("ForceStop") && gunData.reload.stage() == 2 && tag.getInt("IterativeLoadTime") > 0) {
            tag.putBoolean("Stopped", true);
        }

        // 二阶段
        if ((tag.getDouble("PrepareTime") == 0 || tag.getDouble("PrepareLoadTime") == 0)
                && gunData.reload.stage() == 2
                && tag.getInt("IterativeLoadTime") == 0
                && !tag.getBoolean("Stopped")
                && gunData.ammo() < gunData.magazine()
        ) {

            playGunLoopReloadSounds(player);
            int iterativeTime = gunData.iterativeTime();
            tag.putDouble("IterativeLoadTime", iterativeTime);
            player.getCooldowns().addCooldown(stack.getItem(), iterativeTime);
            // 动画播放nbt
            if (tag.getDouble("LoadIndex") == 1) {
                tag.putDouble("LoadIndex", 0);
            } else {
                tag.putDouble("LoadIndex", 1);
            }
        }

        // 装填
        if ((stack.getItem() == ModItems.M_870.get()
                || stack.getItem() == ModItems.MARLIN.get())
                && tag.getInt("IterativeLoadTime") == 3
        ) {
            singleLoad(player, gunData);
        }

        if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get() && tag.getInt("IterativeLoadTime") == 16) {
            singleLoad(player, gunData);
        }

        if ((stack.getItem() == ModItems.K_98.get()
                || stack.getItem() == ModItems.MOSIN_NAGANT.get())
                && tag.getInt("IterativeLoadTime") == 1
        ) {
            singleLoad(player, gunData);
        }

        // 二阶段结束
        if (tag.getInt("IterativeLoadTime") == 1) {
            // 装满结束
            if (gunData.ammo() >= gunData.magazine()) {
                gunData.reload.setStage(3);
            }

            // 备弹耗尽结束
            if (!InventoryTool.hasCreativeAmmoBox(player)) {
                var capability = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
                if (capability == null) capability = new PlayerVariable();

                if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO) && capability.shotgunAmmo == 0) {
                    gunData.reload.setStage(3);
                } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO) && capability.sniperAmmo == 0) {
                    gunData.reload.setStage(3);
                } else if ((stack.is(ModTags.Items.USE_HANDGUN_AMMO) || stack.is(ModTags.Items.SMG)) && capability.handgunAmmo == 0) {
                    gunData.reload.setStage(3);
                } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO) && capability.rifleAmmo == 0) {
                    gunData.reload.setStage(3);
                } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO) && capability.heavyAmmo == 0) {
                    gunData.reload.setStage(3);
                }
            }

            // 强制结束
            if (tag.getBoolean("Stopped")) {
                gunData.reload.setStage(3);
                tag.remove("ForceStop");
                tag.remove("Stopped");
            }
        }

        // 三阶段
        if ((tag.getInt("IterativeLoadTime") == 1 && gunData.reload.stage() == 3) || tag.getBoolean("ForceStartStage3")) {
            gunData.reload.setStage(3);
            tag.remove("ForceStartStage3");
            int finishTime = gunData.finishTime();
            tag.putInt("FinishTime", finishTime + 2);
            player.getCooldowns().addCooldown(stack.getItem(), finishTime + 2);
            playGunEndReloadSounds(player);
        }

        if (stack.getItem() == ModItems.MARLIN.get() && tag.getInt("FinishTime") == 10) {
            tag.remove("IsEmpty");
        }

        // 三阶段结束
        if (tag.getInt("FinishTime") == 1) {
            gunData.reload.setStage(0);
            if (gunData.bolt.defaultActionTime() > 0) {
                gunData.bolt.markNeedless();
            }
            gunData.reload.setState(ReloadState.NOT_RELOADING);
            tag.remove("StartSingleReload");

            NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, stack));
        }
    }

    public static void singleLoad(Player player, GunData data) {
        data.setAmmo(data.ammo() + 1);

        if (!InventoryTool.hasCreativeAmmoBox(player)) {
            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            if (cap == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
                AmmoType.SHOTGUN.add(cap, -1);
            } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
                AmmoType.SNIPER.add(cap, -1);
            } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
                AmmoType.HANDGUN.add(cap, -1);
            } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
                AmmoType.RIFLE.add(cap, -1);
            } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
                AmmoType.HEAVY.add(cap, -1);
            } else if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get()) {
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.GRENADE_40MM.get(), 1, player.inventoryMenu.getCraftSlots());
            }

            cap.syncPlayerVariables(player);
        }
    }

    public static void playGunPrepareReloadSounds(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;

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
        if (!stack.is(ModTags.Items.GUN)) return;
        var data = GunData.from(stack);

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_prepare_empty"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);

                double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

                Mod.queueServerWork((int) (data.prepareEmptyTime() / 2 + 3 + 1.5 * shooterHeight), () -> {
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
        if (!stack.is(ModTags.Items.GUN)) return;

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
        if (!stack.is(ModTags.Items.GUN)) {
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
        if (!stack.is(ModTags.Items.GUN)) return;

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
        if (!(player.getMainHandItem().getItem() instanceof GunItem)) return;

        // 启动充能
        if (data.charge.shouldStartCharge()) {
            data.charge.setTime(127);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc("sentinel_charge"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 2f, 1f);
            }

            data.charge.markStarted();
        }

        if (data.charge.time() > 0) {
            data.charge.reduce();
        }

        if (data.charge.time() == 17) {
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
