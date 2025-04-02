package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.config.common.GameplayConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.ICustomKnockback;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ContainerMobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.events.PreKillEvent;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.DrawClientMessage;
import com.atsuishio.superbwarfare.network.message.receive.PlayerGunKillMessage;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;

@EventBusSubscriber
public class LivingEventHandler {

    @SubscribeEvent
    public static void onEntityAttacked(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(ModDamageTypes.VEHICLE_EXPLOSION) && event.getEntity().getVehicle() instanceof VehicleEntity vehicle) {
            if (event.getEntity().getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.hidePassenger(event.getEntity())) {
                if (!(event.getSource().is(DamageTypes.EXPLOSION)
                        || event.getSource().is(DamageTypes.PLAYER_EXPLOSION)
                        || event.getSource().is(ModDamageTypes.CUSTOM_EXPLOSION)
                        || event.getSource().is(ModDamageTypes.MINE)
                        || event.getSource().is(ModDamageTypes.PROJECTILE_BOOM))) {
                    vehicle.hurt(event.getSource(), event.getContainer().getOriginalDamage());
                }
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingIncomingDamageEvent event) {
        if (event == null) return;

        handleVehicleHurt(event);
        handleGunPerksWhenHurt(event);
        renderDamageIndicator(event);
        reduceBulletDamage(event);
        giveExpToWeapon(event);
        handleGunLevels(event);
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event == null) return;

        killIndication(event);
        handleGunPerksWhenDeath(event);
        handlePlayerKillEntity(event);
        handlePlayerDeathDropAmmo(event.getEntity());
        giveKillExpToWeapon(event);

        if (event.getEntity() instanceof Player player) {
            handlePlayerBeamReset(player);
        }
    }

    private static void handleVehicleHurt(LivingIncomingDamageEvent event) {
        var vehicle = event.getEntity().getVehicle();
        if (vehicle instanceof VehicleEntity) {
            if (vehicle instanceof ArmedVehicleEntity iArmedVehicle) {
                if (iArmedVehicle.hidePassenger(event.getEntity())) {
                    if (!event.getSource().is(ModDamageTypes.VEHICLE_EXPLOSION)) {
                        event.setCanceled(true);
                    }
                } else {
                    if (!(event.getSource().is(DamageTypes.EXPLOSION)
                            || event.getSource().is(DamageTypes.PLAYER_EXPLOSION)
                            || event.getSource().is(ModDamageTypes.CUSTOM_EXPLOSION)
                            || event.getSource().is(ModDamageTypes.MINE)
                            || event.getSource().is(ModDamageTypes.PROJECTILE_BOOM))) {
                        vehicle.hurt(event.getSource(), 0.7f * event.getAmount());
                    }

                    event.setAmount(0.3f * event.getAmount());
                }
            }
        }
    }

    /**
     * 计算子弹伤害衰减
     */
    private static void reduceBulletDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity entity = event.getEntity();
        Entity sourceEntity = source.getEntity();
        if (sourceEntity == null) return;

        double amount = event.getAmount();
        double damage = amount;

        ItemStack stack = sourceEntity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;

        final var tag = NBTTool.getTag(stack);
        var perk = PerkHelper.getPerkByType(tag, Perk.Type.AMMO);

        // 距离衰减
        if (DamageTypeTool.isGunDamage(source)) {
            double distance = entity.position().distanceTo(sourceEntity.position());

            if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
                if (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug) {
                    damage = reduceDamageByDistance(amount, distance, 0.015, 30);
                } else {
                    damage = reduceDamageByDistance(amount, distance, 0.05, 15);
                }
            } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO)) {
                damage = reduceDamageByDistance(amount, distance, 0.001, 150);
            } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO)) {
                damage = reduceDamageByDistance(amount, distance, 0.0007, 250);
            } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
                damage = reduceDamageByDistance(amount, distance, 0.03, 40);
            } else if (stack.is(ModTags.Items.SMG)) {
                damage = reduceDamageByDistance(amount, distance, 0.02, 50);
            } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO) || stack.getItem() == ModItems.BOCEK.get()) {
                damage = reduceDamageByDistance(amount, distance, 0.007, 100);
            }
        }

        // 计算防弹插板减伤
        ItemStack armor = entity.getItemBySlot(EquipmentSlot.CHEST);

        if (armor != ItemStack.EMPTY && tag.contains("ArmorPlate")) {
            double armorValue;
            armorValue = tag.getDouble("ArmorPlate");
            tag.putDouble("ArmorPlate", Math.max(tag.getDouble("ArmorPlate") - damage, 0));
            damage = Math.max(damage - armorValue, 0);
        }

        // 计算防弹护具减伤
        if (source.is(ModTags.DamageTypes.PROJECTILE) || source.is(DamageTypes.MOB_PROJECTILE)) {
            damage *= 1 - 0.8 * Mth.clamp(entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE), 0, 1);
        }

        if (source.is(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)) {
            damage *= 1 - 0.2 * Mth.clamp(entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE), 0, 1);
        }

        if (source.is(ModDamageTypes.PROJECTILE_BOOM) || source.is(ModDamageTypes.MINE) || source.is(ModDamageTypes.CANNON_FIRE) || source.is(ModDamageTypes.CUSTOM_EXPLOSION)
                || source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            damage *= 1 - 0.3 * Mth.clamp(entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE), 0, 1);
        }

        event.setAmount((float) damage);

        if (entity instanceof TargetEntity && sourceEntity instanceof Player player) {
            player.displayClientMessage(Component.translatable("tips.superbwarfare.target.damage",
                    FormatTool.format2D(damage),
                    FormatTool.format1D(entity.position().distanceTo(sourceEntity.position())), "m"), false);
        }
    }

    private static double reduceDamageByDistance(double amount, double distance, double rate, double minDistance) {
        return amount / (1 + rate * Math.max(0, distance - minDistance));
    }

    /**
     * 根据造成的伤害，提供武器经验
     */
    private static void giveExpToWeapon(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;
        if (event.getEntity() instanceof TargetEntity) return;

        double amount = Math.min(0.125 * event.getAmount(), event.getEntity().getMaxHealth());
        final var tag = NBTTool.getTag(stack);

        // 先处理发射器类武器或高爆弹的爆炸伤害
        if (source.is(ModDamageTypes.PROJECTILE_BOOM)) {
            if (stack.is(ModTags.Items.LAUNCHER) || PerkHelper.getItemPerkLevel(ModPerks.HE_BULLET.get(), tag) > 0) {
                GunsTool.setGunDoubleTag(tag, "Exp", GunsTool.getGunDoubleTag(tag, "Exp") + amount);
            }
        }

        // 再判断是不是枪械能造成的伤害
        if (!DamageTypeTool.isGunDamage(source)) return;

        GunsTool.setGunDoubleTag(tag, "Exp", GunsTool.getGunDoubleTag(tag, "Exp") + amount);

        NBTTool.saveTag(stack, tag);
    }

    private static void giveKillExpToWeapon(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;
        if (event.getEntity() instanceof TargetEntity) return;

        double amount = 20 + 2 * event.getEntity().getMaxHealth();
        final var tag = NBTTool.getTag(stack);

        // 先处理发射器类武器或高爆弹的爆炸伤害
        if (source.is(ModDamageTypes.PROJECTILE_BOOM)) {
            if (stack.is(ModTags.Items.LAUNCHER) || PerkHelper.getItemPerkLevel(ModPerks.HE_BULLET.get(), tag) > 0) {
                GunsTool.setGunDoubleTag(tag, "Exp", GunsTool.getGunDoubleTag(tag, "Exp") + amount);
            }
        }

        // 再判断是不是枪械能造成的伤害
        if (DamageTypeTool.isGunDamage(source)) {
            GunsTool.setGunDoubleTag(tag, "Exp", GunsTool.getGunDoubleTag(tag, "Exp") + amount);
        }

        // 提升武器等级
        int level = GunsTool.getGunIntTag(tag, "Level");
        double exp = GunsTool.getGunDoubleTag(tag, "Exp");
        double upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded;
            level = GunsTool.getGunIntTag(tag, "Level") + 1;
            upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;
            GunsTool.setGunDoubleTag(tag, "Exp", exp);
            GunsTool.setGunIntTag(tag, "Level", level);
            GunsTool.setGunDoubleTag(tag, "UpgradePoint", GunsTool.getGunDoubleTag(tag, "UpgradePoint") + 0.5);
        }
        NBTTool.saveTag(stack, tag);
    }

    private static void handleGunLevels(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;
        if (event.getEntity() instanceof TargetEntity) return;

        final var tag = NBTTool.getTag(stack);
        int level = GunsTool.getGunIntTag(tag, "Level");
        double exp = GunsTool.getGunDoubleTag(tag, "Exp");
        double upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded;
            level = GunsTool.getGunIntTag(tag, "Level") + 1;
            upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;
            GunsTool.setGunDoubleTag(tag, "Exp", exp);
            GunsTool.setGunIntTag(tag, "Level", level);
            GunsTool.setGunDoubleTag(tag, "UpgradePoint", GunsTool.getGunDoubleTag(tag, "UpgradePoint") + 0.5);
        }
        NBTTool.saveTag(stack, tag);
    }

    private static void killIndication(LivingDeathEvent event) {
        DamageSource source = event.getSource();

        var sourceEntity = source.getEntity();
        if (sourceEntity == null) {
            return;
        }

        // 如果配置不选择全局伤害提示，则只在伤害类型为mod添加的时显示指示器
        if (!GameplayConfig.GLOBAL_INDICATION.get() && !DamageTypeTool.isModDamage(source)) {
            return;
        }

        if (!sourceEntity.level().isClientSide() && sourceEntity instanceof ServerPlayer player) {
            // TODO 判断 pre kill event 结果
//            if (NeoForge.EVENT_BUS.post(new PreKillEvent.Indicator(player, source, event.getEntity()))) {
//                return;
//            }

            SoundTool.playLocalSound(player, ModSounds.TARGET_DOWN.get(), 3f, 1f);
            PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(2, 8));
        }
    }

    private static void renderDamageIndicator(LivingIncomingDamageEvent event) {
        if (event == null) return;

        var damagesource = event.getSource();
        var sourceEntity = damagesource.getEntity();

        if (sourceEntity == null) return;

        if (sourceEntity instanceof ServerPlayer player && (damagesource.is(DamageTypes.EXPLOSION) || damagesource.is(DamageTypes.PLAYER_EXPLOSION)
                || damagesource.is(ModDamageTypes.MINE) || damagesource.is(ModDamageTypes.PROJECTILE_BOOM))) {
            SoundTool.playLocalSound(player, ModSounds.INDICATION.get(), 1f, 1f);
            PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
        }
    }

    /**
     * 换弹时切换枪械，取消换弹音效播放
     */
    @SubscribeEvent
    public static void handleChangeSlot(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && event.getSlot() == EquipmentSlot.MAINHAND) {
            if (player.level().isClientSide) return;

            ItemStack oldStack = event.getFrom();
            ItemStack newStack = event.getTo();

            var laserCap = player.getCapability(ModCapabilities.LASER_CAPABILITY);
            if (laserCap != null) laserCap.stop();

            var oldTag = NBTTool.getTag(oldStack);
            var newTag = NBTTool.getTag(newStack);
            if (player instanceof ServerPlayer serverPlayer
                    && (newStack.getItem() != oldStack.getItem()
                    || (newStack.is(ModTags.Items.GUN) && !GunsTool.getGunData(newTag).hasUUID("UUID"))
                    || (oldStack.is(ModTags.Items.GUN) && !GunsTool.getGunData(oldTag).hasUUID("UUID"))
                    || (newStack.is(ModTags.Items.GUN) && oldStack.is(ModTags.Items.GUN) && !Objects.equals(GunsTool.getGunUUID(newTag), GunsTool.getGunUUID(oldTag)))
            )) {
                if (oldStack.getItem() instanceof GunItem oldGun) {
                    stopGunReloadSound(serverPlayer, oldGun);

                    CompoundTag data = oldTag.getCompound("GunData");

                    if (GunsTool.getGunDoubleTag(oldTag, "BoltActionTime") > 0) {
                        data.putInt("BoltActionTick", 0);
                    }

                    data.putInt("ReloadTime", 0);
                    oldTag.put("GunData", data);

                    oldTag.putBoolean("is_normal_reloading", false);
                    oldTag.putBoolean("is_empty_reloading", false);

                    if (GunsTool.getGunIntTag(oldTag, "IterativeTime") != 0) {
                        oldTag.putBoolean("force_stop", false);
                        oldTag.putBoolean("stop", false);
                        oldTag.putInt("reload_stage", 0);
                        data.putBoolean("Reloading", false);
                        oldTag.putDouble("prepare", 0);
                        oldTag.putDouble("prepare_load", 0);
                        oldTag.putDouble("iterative", 0);
                        oldTag.putDouble("finish", 0);
                    }

                    if (oldStack.is(ModItems.SENTINEL.get())) {
                        data.putBoolean("Charging", false);
                        data.putInt("ChargeTime", 0);
                    }

                    var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
                    if (cap != null) {
                        cap.edit = false;
                        cap.syncPlayerVariables(player);
                    }
                }

                if (newStack.getItem() instanceof GunItem) {
                    player.getPersistentData().putDouble("noRun", 40);
                    newTag.putBoolean("draw", true);
                    if (GunsTool.getGunIntTag(newTag, "BoltActionTime") > 0) {
                        GunsTool.setGunIntTag(newTag, "BoltActionTick", 0);
                    }
                    newTag.putBoolean("is_normal_reloading", false);
                    newTag.putBoolean("is_empty_reloading", false);

                    CompoundTag data = newTag.getCompound("GunData");
                    data.putInt("ReloadTime", 0);
                    newTag.put("GunData", data);

                    if (GunsTool.getGunIntTag(newTag, "IterativeTime") != 0) {
                        newTag.putBoolean("force_stop", false);
                        newTag.putBoolean("stop", false);
                        newTag.putInt("reload_stage", 0);
                        GunsTool.setGunBooleanTag(newTag, "Reloading", false);
                        newTag.putDouble("prepare", 0);
                        newTag.putDouble("prepare_load", 0);
                        newTag.putDouble("iterative", 0);
                        newTag.putDouble("finish", 0);
                    }

                    if (newStack.is(ModItems.SENTINEL.get())) {
                        GunsTool.setGunBooleanTag(newTag, "Charging", false);
                        GunsTool.setGunIntTag(newTag, "ChargeTime", 0);
                    }
                    NBTTool.saveTag(newStack, newTag);

                    int level = PerkHelper.getItemPerkLevel(ModPerks.KILLING_TALLY.get(), newTag);
                    if (level != 0) {
                        GunsTool.setPerkIntTag(newTag, "KillingTally", 0);
                    }

                    if (player.level() instanceof ServerLevel) {
                        PacketDistributor.sendToPlayer(serverPlayer, new DrawClientMessage(true));
                    }

                    var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
                    if (cap != null) {
                        cap.tacticalSprint = false;
                        cap.syncPlayerVariables(player);
                    }
                }
                NBTTool.saveTag(oldStack, oldTag);
                NBTTool.saveTag(newStack, newTag);
            }
        }
    }

    private static void stopGunReloadSound(ServerPlayer player, GunItem gun) {
        gun.getReloadSound().forEach(sound -> {
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(sound.getLocation(), SoundSource.PLAYERS);
            player.connection.send(clientboundstopsoundpacket);
        });
    }

    /**
     * 发送击杀消息
     */
    private static void handlePlayerKillEntity(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        ResourceKey<DamageType> damageTypeResourceKey = source.typeHolder().unwrapKey().isPresent() ? source.typeHolder().unwrapKey().get() : DamageTypes.GENERIC;

        ServerPlayer attacker = null;
        if (source.getEntity() instanceof ServerPlayer player) {
            attacker = player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            attacker = player;
        }

        // TODO pre kill event
//        if (NeoForge.EVENT_BUS.post(new PreKillEvent.SendKillMessage(attacker, source, entity))) {
//            return;
//        }

        if (attacker != null && MiscConfig.SEND_KILL_FEEDBACK.get()) {
            if (DamageTypeTool.isHeadshotDamage(source)) {
                PacketDistributor.sendToAllPlayers(new PlayerGunKillMessage(attacker.getId(), entity.getId(), true, damageTypeResourceKey));
            } else {
                PacketDistributor.sendToAllPlayers(new PlayerGunKillMessage(attacker.getId(), entity.getId(), false, damageTypeResourceKey));
            }
        }
    }

    private static void handleGunPerksWhenHurt(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        Player attacker = null;
        if (source.getEntity() instanceof Player player) {
            attacker = player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            attacker = player;
        }

        if (attacker == null) {
            return;
        }

        ItemStack stack = attacker.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) {
            return;
        }

        if (DamageTypeTool.isGunDamage(source) || source.is(ModDamageTypes.PROJECTILE_BOOM)) {
            handleKillClipDamage(stack, event);
            handleVorpalWeaponDamage(stack, event);
        }

        if (DamageTypeTool.isGunFireDamage(source) && source.getDirectEntity() instanceof ProjectileEntity projectile && projectile.isZoom()) {
            handleGutshotStraightDamage(stack, event);
        }

        if (DamageTypeTool.isGunDamage(source)) {
            handleKillingTallyDamage(stack, event);
        }

        if (DamageTypeTool.isGunFireDamage(source)) {
            handleHeadSeekerTime(stack);
        }

        if (source.getDirectEntity() instanceof ProjectileEntity projectile) {
            final var tag = NBTTool.getTag(stack);
            if (PerkHelper.getItemPerkLevel(ModPerks.FOURTH_TIMES_CHARM.get(), tag) > 0) {
                float bypassArmorRate = projectile.getBypassArmorRate();
                if (bypassArmorRate >= 1.0f && source.is(ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE)) {
                    handleFourthTimesCharm(stack);
                } else if (source.is(ModDamageTypes.GUN_FIRE_HEADSHOT)) {
                    handleFourthTimesCharm(stack);
                }
            }

            if (!projectile.isZoom()) {
                handleFieldDoctor(stack, event, attacker);
            }
        }

        if (DamageTypeTool.isHeadshotDamage(source)) {
            handleHeadSeekerDamage(stack, event);
        }
    }

    private static void handleGunPerksWhenDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();

        Player attacker = null;
        if (source.getEntity() instanceof Player player) {
            attacker = player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            attacker = player;
        }

        if (attacker == null) {
            return;
        }

        ItemStack stack = attacker.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) {
            return;
        }

        if (DamageTypeTool.isGunDamage(source) || source.is(ModDamageTypes.PROJECTILE_BOOM)) {
            handleClipPerks(stack);
        }

        if (DamageTypeTool.isGunDamage(source)) {
            handleKillingTallyAddCount(stack);
            handleSubsistence(stack, attacker);
        }

        if (DamageTypeTool.isHeadshotDamage(source)) {
            handleDesperado(stack);
        }
    }

    private static void handleClipPerks(ItemStack stack) {
        final var tag = NBTTool.getTag(stack);
        int healClipLevel = PerkHelper.getItemPerkLevel(ModPerks.HEAL_CLIP.get(), tag);
        if (healClipLevel != 0) {
            GunsTool.setPerkIntTag(tag, "HealClipTime", 80 + healClipLevel * 20);
        }

        int killClipLevel = PerkHelper.getItemPerkLevel(ModPerks.KILL_CLIP.get(), tag);
        if (killClipLevel != 0) {
            GunsTool.setPerkIntTag(tag, "KillClipReloadTime", 80);
        }
        NBTTool.saveTag(stack, tag);
    }

    private static void handleKillClipDamage(ItemStack stack, LivingIncomingDamageEvent event) {
        final var tag = NBTTool.getTag(stack);
        if (GunsTool.getPerkIntTag(tag, "KillClipTime") > 0) {
            int level = PerkHelper.getItemPerkLevel(ModPerks.KILL_CLIP.get(), tag);
            if (level == 0) {
                return;
            }

            event.setAmount(event.getAmount() * (1.2f + 0.05f * level));
        }
    }

    private static void handleGutshotStraightDamage(ItemStack stack, LivingIncomingDamageEvent event) {
        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.GUTSHOT_STRAIGHT.get(), tag);
        if (level == 0) return;

        event.setAmount(event.getAmount() * (1.15f + 0.05f * level));
    }

    private static void handleKillingTallyDamage(ItemStack stack, LivingIncomingDamageEvent event) {
        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.KILLING_TALLY.get(), tag);
        if (level == 0) return;

        int killTally = GunsTool.getPerkIntTag(tag, "KillingTally");
        if (killTally == 0) {
            return;
        }

        event.setAmount(event.getAmount() * (1.0f + (0.1f * level) * killTally));
    }

    private static void handleKillingTallyAddCount(ItemStack stack) {
        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.KILLING_TALLY.get(), tag);
        if (level != 0) {
            GunsTool.setPerkIntTag(tag, "KillingTally", Math.min(3, GunsTool.getPerkIntTag(tag, "KillingTally") + 1));
            NBTTool.saveTag(stack, tag);
        }
    }

    private static void handleFourthTimesCharm(ItemStack stack) {
        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.FOURTH_TIMES_CHARM.get(), tag);
        if (level == 0) return;

        int fourthTimesCharmTick = GunsTool.getPerkIntTag(tag, "FourthTimesCharmTick");
        if (fourthTimesCharmTick <= 0) {
            GunsTool.setPerkIntTag(tag, "FourthTimesCharmTick", 40 + 10 * level);
            GunsTool.setPerkIntTag(tag, "FourthTimesCharmCount", 1);
        } else {
            int count = GunsTool.getPerkIntTag(tag, "FourthTimesCharmCount");
            if (count < 4) {
                GunsTool.setPerkIntTag(tag, "FourthTimesCharmCount", Math.min(4, count + 1));
            }
        }
        NBTTool.saveTag(stack, tag);
    }

    private static void handleSubsistence(ItemStack stack, Player player) {
        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.SUBSISTENCE.get(), tag);
        if (level == 0) return;

        float rate = level * 0.1f + (stack.is(ModTags.Items.SMG) || stack.is(ModTags.Items.RIFLE) ? 0.07f : 0f);

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return;

        int mag = GunsTool.getGunIntTag(tag, "Magazine") + GunsTool.getGunIntTag(tag, "CustomMagazine");
        int ammo = GunsTool.getGunIntTag(tag, "Ammo");
        int ammoReload = (int) Math.min(mag, mag * rate);
        int ammoNeed = Math.min(mag - ammo, ammoReload);

        boolean flag = InventoryTool.hasCreativeAmmoBox(player);

        if (stack.is(ModTags.Items.USE_RIFLE_AMMO)) {
            int ammoFinal = Math.min(cap.rifleAmmo, ammoNeed);
            if (flag) {
                ammoFinal = ammoNeed;
            } else {
                cap.rifleAmmo -= ammoFinal;
            }
            GunsTool.setGunIntTag(tag, "Ammo", Math.min(mag, ammo + ammoFinal));
        } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
            int ammoFinal = Math.min(cap.handgunAmmo, ammoNeed);
            if (flag) {
                ammoFinal = ammoNeed;
            } else {
                cap.handgunAmmo -= ammoFinal;
            }
            GunsTool.setGunIntTag(tag, "Ammo", Math.min(mag, ammo + ammoFinal));
        }
        NBTTool.saveTag(stack, tag);
        cap.syncPlayerVariables(player);
    }


    private static void handleFieldDoctor(ItemStack stack, LivingIncomingDamageEvent event, Player player) {
        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.FIELD_DOCTOR.get(), tag);
        if (level == 0) return;

        if (event.getEntity().isAlliedTo(player)) {
            event.getEntity().heal(event.getAmount() * Math.min(1.0f, 0.25f + 0.05f * level));
            event.setCanceled(true);
        }
    }

    private static void handleHeadSeekerTime(ItemStack stack) {
        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.HEAD_SEEKER.get(), tag);
        if (level == 0) return;

        GunsTool.setPerkIntTag(tag, "HeadSeeker", 11 + level * 2);
        NBTTool.saveTag(stack, tag);
    }

    private static void handleHeadSeekerDamage(ItemStack stack, LivingIncomingDamageEvent event) {
        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.HEAD_SEEKER.get(), tag);
        if (level == 0) return;

        if (GunsTool.getPerkIntTag(tag, "HeadSeeker") > 0) {
            event.setAmount(event.getAmount() * (1.095f + 0.0225f * level));
        }
    }

    private static void handleDesperado(ItemStack stack) {
        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.DESPERADO.get(), tag);
        if (level == 0) return;

        GunsTool.setPerkIntTag(tag, "DesperadoTime", 90 + level * 10);
        NBTTool.saveTag(stack, tag);
    }

    /**
     * 开启死亡掉落时掉落一个弹药盒
     */
    private static void handlePlayerDeathDropAmmo(LivingEntity entity) {
        if (!entity.level().getLevelData().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && entity instanceof Player player) {
            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            if (cap == null) cap = new PlayerVariable();

            boolean drop = cap.rifleAmmo + cap.handgunAmmo + cap.shotgunAmmo + cap.sniperAmmo + cap.heavyAmmo > 0;

            if (drop) {
                ItemStack stack = new ItemStack(ModItems.AMMO_BOX.get());
                CompoundTag tag = NBTTool.getTag(stack);

                for (var type : AmmoType.values()) {
                    type.set(tag, type.get(cap));
                    type.set(cap, 0);
                }
                tag.putBoolean("IsDrop", true);
                NBTTool.saveTag(stack, tag);
                cap.syncPlayerVariables(player);

                if (player.level() instanceof ServerLevel level) {
                    ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 1, player.getZ(), stack);
                    itemEntity.setPickUpDelay(10);
                    level.addFreshEntity(itemEntity);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!VehicleConfig.VEHICLE_ITEM_PICKUP.get()) return;
        if (event.getPlayer().getVehicle() instanceof ContainerMobileVehicleEntity containerMobileVehicleEntity) {
            var pickUp = event.getItemEntity();
            if (!containerMobileVehicleEntity.level().isClientSide) {
                HopperBlockEntity.addItem(containerMobileVehicleEntity, pickUp);
            }
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();

        if (player.getVehicle() instanceof ContainerMobileVehicleEntity containerMobileVehicleEntity && source.is(ModDamageTypes.VEHICLE_STRIKE)) {
            var drops = event.getDrops();
            drops.forEach(itemEntity -> {
                ItemStack item = itemEntity.getItem();
                if (!HopperBlockEntity.addItem(containerMobileVehicleEntity, itemEntity)) {
                    player.drop(item, false);
                }
            });
            event.setCanceled(true);
            return;
        }


        final var tag = NBTTool.getTag(stack);
        if (stack.is(ModTags.Items.GUN) && PerkHelper.getItemPerkLevel(ModPerks.POWERFUL_ATTRACTION.get(), tag) > 0 && (DamageTypeTool.isGunDamage(source) || DamageTypeTool.isExplosionDamage(source))) {
            var drops = event.getDrops();
            drops.forEach(itemEntity -> {
                ItemStack item = itemEntity.getItem();
                if (!player.addItem(item)) {
                    player.drop(item, false);
                }
            });
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null) return;

        if (player.getVehicle() instanceof ArmedVehicleEntity) {
            player.giveExperiencePoints(event.getDroppedExperience());
            event.setCanceled(true);
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;

        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.POWERFUL_ATTRACTION.get(), tag);
        if (level > 0) {
            player.giveExperiencePoints((int) (event.getDroppedExperience() * (0.8f + 0.2f * level)));

            event.setCanceled(true);
        }
    }

    public static void handlePlayerBeamReset(Player player) {
        var cap = player.getCapability(ModCapabilities.LASER_CAPABILITY);
        if (cap != null) {
            cap.end();
        }
    }

    private static void handleVorpalWeaponDamage(ItemStack stack, LivingIncomingDamageEvent event) {
        var entity = event.getEntity();

        final var tag = NBTTool.getTag(stack);
        int level = PerkHelper.getItemPerkLevel(ModPerks.VORPAL_WEAPON.get(), tag);
        if (level <= 0) return;
        if (entity.getHealth() < 100.0f) return;

        event.setAmount((float) (event.getAmount() + entity.getHealth() * 0.00002f * Math.pow(level, 2)));
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        ICustomKnockback knockback = ICustomKnockback.getInstance(event.getEntity());
        if (knockback.superbWarfare$getKnockbackStrength() >= 0) {
            event.setStrength((float) knockback.superbWarfare$getKnockbackStrength());
        }
    }

    @SubscribeEvent
    public static void onEntityFall(LivingFallEvent event) {
        LivingEntity living = event.getEntity();
        if (living.getVehicle() instanceof VehicleEntity) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPreSendKillMessage(PreKillEvent.SendKillMessage event) {
        if (event.getSource().getDirectEntity() instanceof LaserTowerEntity && !(event.getTarget() instanceof Player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPreIndicator(PreKillEvent.Indicator event) {
        if (event.getSource().getDirectEntity() instanceof LaserTowerEntity && !(event.getTarget() instanceof Player)) {
            event.setCanceled(true);
        }
    }
}
