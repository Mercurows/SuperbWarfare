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
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

import java.text.DecimalFormat;
import java.util.ArrayList;

@EventBusSubscriber(modid = Mod.MODID)
public class GunEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);

        if (stack.is(ModTags.Items.GUN)) {
            handleGunBolt(player, tag);
            handleGunReload(player, tag);
            handleGunSingleReload(player, tag);
            handleSentinelCharge(player, tag);
        }

        NBTTool.saveTag(stack, tag);
    }

    /**
     * 拉大栓
     */
    private static void handleGunBolt(Player player, final CompoundTag tag) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        if (stack.is(ModTags.Items.NORMAL_GUN)) {
            if (GunsTool.getGunIntTag(tag, "BoltActionTick") > 0) {
                tag.getCompound("GunData").putInt("BoltActionTick", GunsTool.getGunIntTag(tag, "BoltActionTick") - 1);
            }
            if (stack.getItem() == ModItems.MARLIN.get() && GunsTool.getGunIntTag(tag, "BoltActionTick") == 9) {
                tag.putBoolean("empty", false);
            }

            if (GunsTool.getGunIntTag(tag, "BoltActionTick") == 1) {
                tag.getCompound("GunData").putBoolean("NeedBoltAction", false);
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
        final var tag = NBTTool.getTag(stack);

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            if (stack.getItem() == ModItems.SENTINEL.get()) {
                var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);

                if (cap != null && cap.getEnergyStored() > 0) {
                    float soundRadius = (float) GunsTool.getGunDoubleTag(tag, "SoundRadius");

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

            float soundRadius = (float) (GunsTool.getGunDoubleTag(tag, "SoundRadius") * GunsTool.getGunDoubleTag(tag, "CustomSoundRadius", 1));
            int barrelType = GunsTool.getAttachmentType(tag, GunsTool.AttachmentType.BARREL);

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
        final var tag = NBTTool.getTag(stack);

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_bolt"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 2f, 1f);

                double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

                if (stack.is(ModTags.Items.REVOLVER)) return;

                Mod.queueServerWork((int) (GunsTool.getGunDoubleTag(tag, "BoltActionTime", 0) / 2 + 1.5 * shooterHeight), () -> {
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

        if (!player.level().isClientSide()) {
            float headshot = (float) GunsTool.getGunDoubleTag(tag, "Headshot", 0);
            float damage = (float) (GunsTool.getGunDoubleTag(tag, "Damage", 0) +
                    GunsTool.getGunDoubleTag(tag, "ChargedDamage", 0)) * (float) perkDamage(tag);
            float velocity = (float) ((GunsTool.getGunDoubleTag(tag, "Velocity", 0) + GunsTool.getGunDoubleTag(tag, "CustomVelocity", 0)) * perkSpeed(tag));
            int projectileAmount = GunsTool.getGunIntTag(tag, "ProjectileAmount", 1);
            float bypassArmorRate = (float) GunsTool.getGunDoubleTag(tag, "BypassesArmor", 0);

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

    public static double perkDamage(final CompoundTag tag) {
        var perk = PerkHelper.getPerkByType(tag, Perk.Type.AMMO);
        if (perk instanceof AmmoPerk ammoPerk) {
            return ammoPerk.damageRate;
        }
        return 1;
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
    private static void handleGunReload(Player player, final CompoundTag tag) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;

        final CompoundTag data = tag.getCompound("GunData");
        // 启动换弹
        if (GunsTool.getGunBooleanTag(tag, "StartReload")) {

            NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(player, stack));
            if (gunItem.isOpenBolt(stack)) {
                if (GunsTool.getGunIntTag(tag, "Ammo", 0) == 0) {
                    data.putInt("ReloadTime", data.getInt("EmptyReloadTime") + 1);
                    tag.putBoolean("is_empty_reloading", true);
                    playGunEmptyReloadSounds(player);
                } else {
                    data.putInt("ReloadTime", data.getInt("NormalReloadTime") + 1);
                    tag.putBoolean("is_normal_reloading", true);
                    playGunNormalReloadSounds(player);
                }
            } else {
                data.putInt("ReloadTime", data.getInt("EmptyReloadTime") + 2);
                tag.putBoolean("is_empty_reloading", true);
                playGunEmptyReloadSounds(player);
            }
            data.putBoolean("StartReload", false);
        }

        if (data.getInt("ReloadTime") > 0) {
            data.putInt("ReloadTime", data.getInt("ReloadTime") - 1);
        }

        if (stack.getItem() == ModItems.RPG.get()) {
            if (data.getInt("ReloadTime") == 84) {
                tag.putBoolean("empty", false);
            }
            if (data.getInt("ReloadTime") == 9) {
                data.putBoolean("CloseHammer", false);
            }
        }

        if (stack.getItem() == ModItems.MK_14.get()) {
            if (data.getInt("ReloadTime") == 18) {
                data.putBoolean("HoldOpen", false);
            }
        }

        if (stack.getItem() == ModItems.SVD.get()) {
            if (data.getInt("ReloadTime") == 17) {
                data.putBoolean("HoldOpen", false);
            }
        }

        if (stack.getItem() == ModItems.SKS.get()) {
            if (data.getInt("ReloadTime") == 14) {
                data.putBoolean("HoldOpen", false);
            }
        }

        if (stack.getItem() == ModItems.M_60.get()) {
            if (data.getInt("ReloadTime") == 55) {
                data.putBoolean("HideBulletChain", false);
            }
        }

        if (stack.getItem() == ModItems.GLOCK_17.get() || stack.getItem() == ModItems.GLOCK_18.get() || stack.getItem() == ModItems.M_1911.get() || stack.getItem() == ModItems.MP_443.get()) {
            if (data.getInt("ReloadTime") == 9) {
                data.putBoolean("HoldOpen", false);
            }
        }

        if (stack.getItem() == ModItems.QBZ_95.get()) {
            if (data.getInt("ReloadTime") == 14) {
                data.putBoolean("HoldOpen", false);
            }
        }

        if (data.getInt("ReloadTime") == 1) {
            if (gunItem.isOpenBolt(stack)) {
                if (GunsTool.getGunIntTag(tag, "Ammo", 0) == 0) {
                    playGunEmptyReload(player, tag);
                } else {
                    playGunNormalReload(player, tag);
                }
            } else {
                playGunEmptyReload(player, tag);
            }
            data.putBoolean("StartReload", false);
        }

        tag.put("GunData", data);
    }

    public static void playGunNormalReload(Player player, final CompoundTag tag) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;

        if (player.getInventory().hasAnyMatching(item -> item.is(ModItems.CREATIVE_AMMO_BOX.get()))) {
            var data = tag.getCompound("GunData");
            data.putInt("Ammo", data.getInt("Magazine") + data.getInt("CustomMagazine") + (gunItem.hasBulletInBarrel(stack) ? 1 : 0));
        } else {
            if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.SHOTGUN, gunItem.hasBulletInBarrel(stack));
            } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.SNIPER, true);
            } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.HANDGUN, true);
            } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.RIFLE, gunItem.hasBulletInBarrel(stack));
            } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.HEAVY, gunItem.hasBulletInBarrel(stack));
            }
        }
        tag.putBoolean("is_normal_reloading", false);
        tag.putBoolean("is_empty_reloading", false);

        NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, stack));
    }

    public static void playGunEmptyReload(Player player, final CompoundTag tag) {
        ItemStack stack = player.getMainHandItem();
        var data = tag.getCompound("GunData");

        if (player.getInventory().hasAnyMatching(item -> item.is(ModItems.CREATIVE_AMMO_BOX.get()))) {
            data.putInt("Ammo", data.getInt("Magazine") + data.getInt("CustomMagazine"));
        } else {
            if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.SHOTGUN);
            } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.SNIPER);
            } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.HANDGUN);
            } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.RIFLE);
            } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
                GunsTool.reload(player, stack, tag, AmmoType.HEAVY);
            } else if (stack.getItem() == ModItems.TASER.get()) {
                data.putInt("Ammo", 1);
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.TASER_ELECTRODE.get(), 1, player.inventoryMenu.getCraftSlots());
            } else if (stack.getItem() == ModItems.M_79.get()) {
                data.putInt("Ammo", 1);
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.GRENADE_40MM.get(), 1, player.inventoryMenu.getCraftSlots());
            } else if (stack.getItem() == ModItems.RPG.get()) {
                data.putInt("Ammo", 1);
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.ROCKET.get(), 1, player.inventoryMenu.getCraftSlots());
            } else if (stack.getItem() == ModItems.JAVELIN.get()) {
                data.putInt("Ammo", 1);
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ModItems.JAVELIN_MISSILE.get(), 1, player.inventoryMenu.getCraftSlots());
            }
        }

        tag.putBoolean("is_normal_reloading", false);
        tag.putBoolean("is_empty_reloading", false);

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
    private static void handleGunSingleReload(Player player, final CompoundTag tag) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        // 换弹流程计时器
        if (tag.getDouble("prepare") > 0) {
            tag.putDouble("prepare", tag.getDouble("prepare") - 1);
        }
        if (tag.getDouble("prepare_load") > 0) {
            tag.putDouble("prepare_load", tag.getDouble("prepare_load") - 1);
        }
        if (tag.getDouble("iterative") > 0) {
            tag.putDouble("iterative", tag.getDouble("iterative") - 1);
        }
        if (tag.getDouble("finish") > 0) {
            tag.putDouble("finish", tag.getDouble("finish") - 1);
        }

        player.displayClientMessage(Component.literal("prepare: " + new DecimalFormat("##.#").format(tag.getDouble("prepare"))
                + " prepare_load: " + new DecimalFormat("##.#").format(tag.getDouble("prepare_load"))
                + " iterative: " + new DecimalFormat("##.#").format(tag.getDouble("iterative"))
                + " finish: " + new DecimalFormat("##.#").format(tag.getDouble("finish"))
                + " reload_stage: " + new DecimalFormat("##.#").format(tag.getDouble("reload_stage"))
        ), true);

        // 一阶段
        if (tag.getBoolean("start_single_reload")) {
            NeoForge.EVENT_BUS.post(new ReloadEvent.Pre(player, stack));

            if ((GunsTool.getGunIntTag(tag, "PrepareLoadTime", 0) != 0
                    && GunsTool.getGunIntTag(tag, "Ammo", 0) == 0)
                    || stack.is(ModItems.SECONDARY_CATACLYSM.get())
            ) {
                // 此处判断空仓换弹的时候，是否在准备阶段就需要装填一发，如M870
                playGunPrepareLoadReloadSounds(player);
                int prepareLoadTime = GunsTool.getGunIntTag(tag, "PrepareLoadTime", 0);
                tag.putInt("prepare_load", prepareLoadTime + 1);
                player.getCooldowns().addCooldown(stack.getItem(), prepareLoadTime);
            } else if (GunsTool.getGunIntTag(tag, "PrepareEmptyTime", 0) != 0 && GunsTool.getGunIntTag(tag, "Ammo", 0) == 0) {
                // 此处判断空仓换弹，如莫辛纳甘
                playGunEmptyPrepareSounds(player);
                int prepareEmptyTime = GunsTool.getGunIntTag(tag, "PrepareEmptyTime", 0);
                tag.putInt("prepare", prepareEmptyTime + 1);
                player.getCooldowns().addCooldown(stack.getItem(), prepareEmptyTime);
            } else {
                playGunPrepareReloadSounds(player);
                int prepareTime = GunsTool.getGunIntTag(tag, "PrepareTime", 0);
                tag.putInt("prepare", prepareTime + 1);
                player.getCooldowns().addCooldown(stack.getItem(), prepareTime);
            }

            tag.putBoolean("force_stop", false);
            tag.putBoolean("stop", false);
            tag.putInt("reload_stage", 1);
            tag.getCompound("GunData").putBoolean("Reloading", true);
            tag.putBoolean("start_single_reload", false);
        }

        if (stack.getItem() == ModItems.M_870.get() && tag.getInt("prepare_load") == 10) {
            singleLoad(player, tag);
        }

        if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get() && tag.getInt("prepare_load") == 3) {
            singleLoad(player, tag);
        }

        // 一阶段结束，检查备弹，如果有则二阶段启动，无则直接跳到三阶段
        if ((tag.getDouble("prepare") == 1 || tag.getDouble("prepare_load") == 1)) {
            if (!InventoryTool.hasCreativeAmmoBox(player)) {
                var capability = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
                if (capability == null) capability = new PlayerVariable();

                if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO) && capability.shotgunAmmo == 0) {
                    tag.putBoolean("force_stage3_start", true);
                } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO) && capability.sniperAmmo == 0) {
                    tag.putBoolean("force_stage3_start", true);
                } else if ((stack.is(ModTags.Items.USE_HANDGUN_AMMO) || stack.is(ModTags.Items.SMG)) && capability.handgunAmmo == 0) {
                    tag.putBoolean("force_stage3_start", true);
                } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO) && capability.rifleAmmo == 0) {
                    tag.putBoolean("force_stage3_start", true);
                } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO) && capability.heavyAmmo == 0) {
                    tag.putBoolean("force_stage3_start", true);
                } else if (stack.is(ModTags.Items.LAUNCHER) && GunsTool.getGunIntTag(tag, "MaxAmmo") == 0) {
                    tag.putBoolean("force_stage3_start", true);
                } else if (stack.is(ModItems.SECONDARY_CATACLYSM.get()) && GunsTool.getGunIntTag(tag, "Ammo", 0) >= GunsTool.getGunIntTag(tag, "Magazine", 0)) {
                    tag.putBoolean("force_stage3_start", true);
                } else {
                    tag.putInt("reload_stage", 2);
                }
            } else {
                if (stack.is(ModItems.SECONDARY_CATACLYSM.get()) && GunsTool.getGunIntTag(tag, "Ammo", 0) >= GunsTool.getGunIntTag(tag, "Magazine", 0)) {
                    tag.putBoolean("force_stage3_start", true);
                } else {
                    tag.putInt("reload_stage", 2);
                }
            }
            // 检查备弹
        }

        // 强制停止换弹，进入三阶段
        if (tag.getBoolean("force_stop") && tag.getInt("reload_stage") == 2 && tag.getInt("iterative") > 0) {
            tag.putBoolean("stop", true);
        }

        // 二阶段
        if ((tag.getDouble("prepare") == 0 || tag.getDouble("prepare_load") == 0)
                && tag.getInt("reload_stage") == 2
                && tag.getInt("iterative") == 0
                && !tag.getBoolean("stop")
                && GunsTool.getGunIntTag(tag, "Ammo", 0) < GunsTool.getGunIntTag(tag, "Magazine", 0)
                + GunsTool.getGunIntTag(tag, "CustomMagazine", 0)) {

            playGunLoopReloadSounds(player);
            int iterativeTime = GunsTool.getGunIntTag(tag, "IterativeTime", 0);
            tag.putDouble("iterative", iterativeTime);
            player.getCooldowns().addCooldown(stack.getItem(), iterativeTime);
            // 动画播放nbt
            if (tag.getDouble("load_index") == 1) {
                tag.putDouble("load_index", 0);
            } else {
                tag.putDouble("load_index", 1);
            }
        }

        // 装填
        if (stack.getItem() == ModItems.M_870.get()
                || stack.getItem() == ModItems.MARLIN.get()
                && tag.getInt("iterative") == 3
        ) {
            singleLoad(player, tag);
        }

        if (stack.getItem() == ModItems.SECONDARY_CATACLYSM.get() && tag.getInt("iterative") == 16) {
            singleLoad(player, tag);
        }

        if (stack.getItem() == ModItems.K_98.get()
                || stack.getItem() == ModItems.MOSIN_NAGANT.get()
                || tag.getInt("iterative") == 1) {
            singleLoad(player, tag);
        }

        // 二阶段结束
        if (tag.getInt("iterative") == 1) {
            // 装满结束
            if (GunsTool.getGunIntTag(tag, "Ammo", 0) >= GunsTool.getGunIntTag(tag, "Magazine", 0)
                    + GunsTool.getGunIntTag(tag, "CustomMagazine", 0)) {
                tag.putInt("reload_stage", 3);
            }

            // 备弹耗尽结束
            if (!InventoryTool.hasCreativeAmmoBox(player)) {
                var capability = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
                if (capability == null) capability = new PlayerVariable();

                if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO) && capability.shotgunAmmo == 0) {
                    tag.putInt("reload_stage", 3);
                } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO) && capability.sniperAmmo == 0) {
                    tag.putInt("reload_stage", 3);
                } else if ((stack.is(ModTags.Items.USE_HANDGUN_AMMO) || stack.is(ModTags.Items.SMG)) && capability.handgunAmmo == 0) {
                    tag.putInt("reload_stage", 3);
                } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO) && capability.rifleAmmo == 0) {
                    tag.putInt("reload_stage", 3);
                } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO) && capability.heavyAmmo == 0) {
                    tag.putInt("reload_stage", 3);
                }
            }

            // 强制结束
            if (tag.getBoolean("stop")) {
                tag.putInt("reload_stage", 3);
                tag.putBoolean("force_stop", false);
                tag.putBoolean("stop", false);
            }
        }

        // 三阶段
        if ((tag.getInt("iterative") == 1 && tag.getInt("reload_stage") == 3) || tag.getBoolean("force_stage3_start")) {
            tag.putInt("reload_stage", 3);
            tag.putBoolean("force_stage3_start", false);
            int finishTime = GunsTool.getGunIntTag(tag, "FinishTime", 0);
            tag.putInt("finish", finishTime + 2);
            player.getCooldowns().addCooldown(stack.getItem(), finishTime + 2);
            playGunEndReloadSounds(player);
        }

        if (stack.getItem() == ModItems.MARLIN.get() && tag.getInt("finish") == 10) {
            tag.putBoolean("empty", false);
        }

        // 三阶段结束
        if (tag.getInt("finish") == 1) {
            tag.putInt("reload_stage", 0);
            if (GunsTool.getGunIntTag(tag, "BoltActionTime", 0) > 0) {
                GunsTool.setGunBooleanTag(tag, "NeedBoltAction", false);
            }
            GunsTool.setGunBooleanTag(tag, "Reloading", false);
            tag.putBoolean("start_single_reload", false);

            NeoForge.EVENT_BUS.post(new ReloadEvent.Post(player, stack));
        }
    }

    public static void singleLoad(Player player, final CompoundTag tag) {
        final var data = tag.getCompound("GunData");
        data.putInt("Ammo", data.getInt("Ammo") + 1);

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
        final var tag = NBTTool.getTag(stack);

        if (!player.level().isClientSide) {
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc(name + "_prepare_empty"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 10f, 1f);

                double shooterHeight = player.getEyePosition().distanceTo((Vec3.atLowerCornerOf(player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(new Vec3(0, -1, 0).scale(10)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos())));

                Mod.queueServerWork((int) (GunsTool.getGunIntTag(tag, "PrepareEmptyTime", 0) / 2 + 3 + 1.5 * shooterHeight), () -> {
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
    private static void handleSentinelCharge(Player player, final CompoundTag tag) {
        if (!(player.getMainHandItem().getItem() instanceof GunItem)) return;
        final var data = tag.getCompound("GunData");

        // 启动换弹
        if (GunsTool.getGunBooleanTag(tag, "StartCharge")) {
            data.putInt("ChargeTime", 127);
            data.putBoolean("Charging", true);

            SoundEvent sound1p = BuiltInRegistries.SOUND_EVENT.get(Mod.loc("sentinel_charge"));
            if (sound1p != null && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, sound1p, 2f, 1f);
            }

            data.putBoolean("StartCharge", true);
        }

        if (GunsTool.getGunIntTag(tag, "ChargeTime", 0) > 0) {
            data.putInt("ChargeTime", data.getInt("ChargeTime") - 1);
        }

        if (GunsTool.getGunIntTag(tag, "ChargeTime", 0) == 17) {
            for (var cell : player.getInventory().items) {
                if (cell.is(ModItems.CELL.get())) {
                    var stackStorage = cell.getCapability(Capabilities.EnergyStorage.ITEM);
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

        if (GunsTool.getGunIntTag(tag, "ChargeTime", 0) == 1) {
            data.putBoolean("Charging", false);
        }
    }

    // TODO missing mapping event
//    @SubscribeEvent
//    public static void onMissingMappings(MissingMappingsEvent event) {
//        for (MissingMappingsEvent.Mapping<Item> mapping : event.getAllMappings(Registries.ITEM)) {
//            if (ModUtils.MODID.equals(mapping.getKey().getNamespace()) && mapping.getKey().getPath().equals("abekiri")) {
//                mapping.remap(ModItems.HOMEMADE_SHOTGUN.get());
//            }
//        }
//    }
}
