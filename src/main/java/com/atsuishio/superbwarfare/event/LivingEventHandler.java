package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.component.ModDataComponents;
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
import com.atsuishio.superbwarfare.item.common.ammo.box.AmmoBoxInfo;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.ReloadState;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.DrawClientMessage;
import com.atsuishio.superbwarfare.network.message.receive.PlayerGunKillMessage;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.*;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
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

        // 距离衰减
        if (DamageTypeTool.isGunDamage(source) && stack.getItem() instanceof GunItem) {
            double distance = entity.position().distanceTo(sourceEntity.position());
            var data = GunData.from(stack);

            if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO)) {
                var perk = data.perk.get(Perk.Type.AMMO);

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
            NBTTool.saveTag(stack, tag);
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

        var data = GunData.from(stack);
        double amount = Math.min(0.125 * event.getAmount(), event.getEntity().getMaxHealth());

        // 先处理发射器类武器或高爆弹的爆炸伤害
        if (source.is(ModDamageTypes.PROJECTILE_BOOM)) {
            if (stack.is(ModTags.Items.LAUNCHER) || data.perk.getLevel(ModPerks.HE_BULLET) > 0) {
                data.setExp(data.exp() + amount);
            }
        }

        // 再判断是不是枪械能造成的伤害
        if (!DamageTypeTool.isGunDamage(source)) return;

        data.setExp(data.exp() + amount);
        data.save();
    }

    private static void giveKillExpToWeapon(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;
        if (event.getEntity() instanceof TargetEntity) return;

        var data = GunData.from(stack);
        double amount = 20 + 2 * event.getEntity().getMaxHealth();

        // 先处理发射器类武器或高爆弹的爆炸伤害
        if (source.is(ModDamageTypes.PROJECTILE_BOOM)) {
            if (stack.is(ModTags.Items.LAUNCHER) || data.perk.getLevel(ModPerks.HE_BULLET) > 0) {
                data.setExp(data.exp() + amount);
            }
        }

        // 再判断是不是枪械能造成的伤害
        if (DamageTypeTool.isGunDamage(source)) {
            data.setExp(data.exp() + amount);
        }

        // 提升武器等级
        int level = data.level();
        double exp = data.exp();
        double upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded;
            level = data.level() + 1;
            upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;
            data.setExp(exp);
            data.setLevel(level);
            data.setUpgradePoint(data.upgradePoint() + 0.5);
        }
        data.save();
    }

    private static void handleGunLevels(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;
        if (event.getEntity() instanceof TargetEntity) return;

        var data = GunData.from(stack);
        int level = data.level();
        double exp = data.exp();
        double upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded;
            level = data.level() + 1;
            upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;
            data.setExp(exp);
            data.setLevel(level);
            data.setUpgradePoint(data.upgradePoint() + 0.5);
        }
        data.save();
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
            if (NeoForge.EVENT_BUS.post(new PreKillEvent.Indicator(player, source, event.getEntity())).isCanceled()) {
                return;
            }

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

                    var oldData = GunData.from(oldStack);
                    oldTag = oldData.tag();
                    var data = oldData.data();

                    if (oldData.bolt.defaultActionTime() > 0) {
                        oldData.bolt.setActionTime(0);
                    }

                    oldData.reload.setTime(0);
                    oldTag.put("GunData", data);

                    oldData.reload.setState(ReloadState.NOT_RELOADING);

                    if (oldData.iterativeTime() != 0) {
                        oldTag.remove("ForceStop");
                        oldTag.remove("Stopped");
                        oldData.reload.setStage(0);
                        oldTag.remove("PrepareTime");
                        oldTag.remove("PrepareLoadTime");
                        oldTag.remove("IterativeLoadTime");
                        oldTag.remove("FinishTime");
                    }

                    if (oldStack.is(ModItems.SENTINEL.get())) {
                        oldData.charge.reset();
                    }

                    var cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch();
                    cap.edit = false;
                    player.setData(ModAttachments.PLAYER_VARIABLE, cap);
                    cap.sync(player);

                    oldData.save();
                }

                if (newStack.getItem() instanceof GunItem) {
                    var newData = GunData.from(newStack);
                    newTag = newData.tag();

                    player.getPersistentData().putDouble("noRun", 40);
                    newTag.putBoolean("draw", true);
                    if (newData.bolt.defaultActionTime() > 0) {
                        newData.bolt.setActionTime(0);
                    }

                    newData.reload.setState(ReloadState.NOT_RELOADING);

                    var data = newData.data();
                    newData.reload.setTime(0);
                    newTag.put("GunData", data);

                    if (newData.iterativeTime() != 0) {
                        newTag.remove("ForceStop");
                        newTag.remove("Stopped");
                        newData.reload.setStage(0);
                        newTag.remove("PrepareTime");
                        newTag.remove("PrepareLoadTime");
                        newTag.remove("IterativeLoadTime");
                        newTag.remove("FinishTime");
                    }

                    if (newStack.is(ModItems.SENTINEL.get())) {
                        newData.charge.reset();
                    }

                    int level = newData.perk.getLevel(ModPerks.KILLING_TALLY);
                    if (level != 0) {
                        GunsTool.setPerkIntTag(newTag, "KillingTally", 0);
                    }

                    if (player.level() instanceof ServerLevel) {
                        PacketDistributor.sendToPlayer(serverPlayer, new DrawClientMessage(true));
                    }

                    var cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch();
                    cap.tacticalSprint = false;
                    player.setData(ModAttachments.PLAYER_VARIABLE, cap);
                    cap.sync(player);

                    newData.save();
                }
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

        if (NeoForge.EVENT_BUS.post(new PreKillEvent.SendKillMessage(attacker, source, entity)).isCanceled()) {
            return;
        }

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
        if (!stack.is(ModTags.Items.GUN)) return;
        var data = GunData.from(stack);

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
            if (data.perk.getLevel(ModPerks.FOURTH_TIMES_CHARM) > 0) {
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
        var data = GunData.from(stack);
        int healClipLevel = data.perk.getLevel(ModPerks.HEAL_CLIP);
        var tag = data.perk.getTag(ModPerks.HEAL_CLIP);
        if (healClipLevel != 0) {
            tag.putInt("HealClipTime", 80 + healClipLevel * 20);
        }

        int killClipLevel = data.perk.getLevel(ModPerks.KILL_CLIP);
        if (killClipLevel != 0) {
            tag.putInt("KillClipReloadTime", 80);
        }
        data.save();
    }

    private static void handleKillClipDamage(ItemStack stack, LivingIncomingDamageEvent event) {
        var data = GunData.from(stack);
        final var tag = data.perk.getTag(ModPerks.KILL_CLIP);
        if (tag.getInt("KillClipTime") > 0) {
            int level = data.perk.getLevel(ModPerks.KILL_CLIP);
            if (level == 0) return;

            event.setAmount(event.getAmount() * (1.2f + 0.05f * level));
        }
    }

    private static void handleGutshotStraightDamage(ItemStack stack, LivingIncomingDamageEvent event) {
        var data = GunData.from(stack);
        int level = data.perk.getLevel(ModPerks.GUTSHOT_STRAIGHT);
        if (level == 0) return;

        event.setAmount(event.getAmount() * (1.15f + 0.05f * level));
    }

    private static void handleKillingTallyDamage(ItemStack stack, LivingIncomingDamageEvent event) {
        var data = GunData.from(stack);
        int level = data.perk.getLevel(ModPerks.KILLING_TALLY);
        if (level == 0) return;

        int killTally = data.perk.getTag(ModPerks.KILLING_TALLY).getInt("KillingTally");
        if (killTally == 0) {
            return;
        }

        event.setAmount(event.getAmount() * (1.0f + (0.1f * level) * killTally));
    }

    private static void handleKillingTallyAddCount(ItemStack stack) {
        var data = GunData.from(stack);
        int level = data.perk.getLevel(ModPerks.KILLING_TALLY);
        if (level != 0) {
            var tag = data.perk.getTag(ModPerks.KILLING_TALLY);
            tag.putInt("KillingTally", Math.min(3, tag.getInt("KillingTally") + 1));
            data.save();
        }
    }

    private static void handleFourthTimesCharm(ItemStack stack) {
        var data = GunData.from(stack);
        int level = data.perk.getLevel(ModPerks.FOURTH_TIMES_CHARM);
        if (level == 0) return;
        final var tag = data.perk.getTag(ModPerks.FOURTH_TIMES_CHARM);

        int fourthTimesCharmTick = tag.getInt("FourthTimesCharmTick");
        if (fourthTimesCharmTick <= 0) {
            tag.putInt("FourthTimesCharmTick", 40 + 10 * level);
            tag.putInt("FourthTimesCharmCount", 1);
        } else {
            int count = tag.getInt("FourthTimesCharmCount");
            if (count < 4) {
                tag.putInt("FourthTimesCharmCount", Math.min(4, count + 1));
            }
        }

        data.save();
    }

    private static void handleSubsistence(ItemStack stack, Player player) {
        var data = GunData.from(stack);
        int level = data.perk.getLevel(ModPerks.SUBSISTENCE);
        if (level == 0) return;

        float rate = level * 0.1f + (stack.is(ModTags.Items.SMG) || stack.is(ModTags.Items.RIFLE) ? 0.07f : 0f);

        var cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch();

        int mag = data.magazine();
        int ammo = data.ammo();
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
            data.setAmmo(Math.min(mag, ammo + ammoFinal));
        } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO)) {
            int ammoFinal = Math.min(cap.handgunAmmo, ammoNeed);
            if (flag) {
                ammoFinal = ammoNeed;
            } else {
                cap.handgunAmmo -= ammoFinal;
            }
            data.setAmmo(Math.min(mag, ammo + ammoFinal));
        }
        data.save();
        player.setData(ModAttachments.PLAYER_VARIABLE, cap);
        cap.sync(player);
    }


    private static void handleFieldDoctor(ItemStack stack, LivingIncomingDamageEvent event, Player player) {
        var data = GunData.from(stack);
        int level = data.perk.getLevel(ModPerks.FIELD_DOCTOR);
        if (level == 0) return;

        if (event.getEntity().isAlliedTo(player)) {
            event.getEntity().heal(event.getAmount() * Math.min(1.0f, 0.25f + 0.05f * level));
            event.setCanceled(true);
        }
    }

    private static void handleHeadSeekerTime(ItemStack stack) {
        var data = GunData.from(stack);
        int level = data.perk.getLevel(ModPerks.HEAD_SEEKER);
        if (level == 0) return;

        data.perk.getTag(ModPerks.HEAD_SEEKER).putInt("HeadSeeker", 11 + level * 2);
        data.save();
    }

    private static void handleHeadSeekerDamage(ItemStack stack, LivingIncomingDamageEvent event) {
        var data = GunData.from(stack);
        int level = data.perk.getLevel(ModPerks.HEAD_SEEKER);
        if (level == 0) return;

        var tag = data.perk.getTag(ModPerks.HEAD_SEEKER);
        if (tag.getInt("HeadSeeker") > 0) {
            event.setAmount(event.getAmount() * (1.095f + 0.0225f * level));
        }
    }

    private static void handleDesperado(ItemStack stack) {
        var data = GunData.from(stack);
        int level = data.perk.getLevel(ModPerks.DESPERADO);
        if (level == 0) return;

        var tag = data.perk.getTag(ModPerks.DESPERADO);
        tag.putInt("DesperadoTime", 90 + level * 10);
        data.save();
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
        // 死亡掉落弹药盒
        if (event.getEntity() instanceof Player player && !player.level().getLevelData().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            var cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch();

            boolean drop = cap.rifleAmmo + cap.handgunAmmo + cap.shotgunAmmo + cap.sniperAmmo + cap.heavyAmmo > 0;

            if (drop) {
                var stack = new ItemStack(ModItems.AMMO_BOX.get());

                for (var type : AmmoType.values()) {
                    type.set(stack, type.get(cap));
                    type.set(cap, 0);
                }

                var info = new AmmoBoxInfo("All", true);
                stack.set(ModDataComponents.AMMO_BOX_INFO, info);

                player.setData(ModAttachments.PLAYER_VARIABLE, cap);
                cap.sync(player);

                event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY() + 1, player.getZ(), stack));
            }
        }

        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack mainHandItem = player.getMainHandItem();

        // 创生物收集掉落物
        if (player.getVehicle() instanceof ContainerMobileVehicleEntity containerMobileVehicleEntity && source.is(ModDamageTypes.VEHICLE_STRIKE)) {
            var drops = event.getDrops();
            var removed = new ArrayList<ItemEntity>();

            drops.forEach(itemEntity -> {
                ItemStack stack = itemEntity.getItem();

                InventoryTool.insertItem(containerMobileVehicleEntity.getItemStacks(), stack);

                if (stack.getCount() <= 0) {
                    player.drop(stack, false);
                    removed.add(itemEntity);
                }
            });

            drops.removeAll(removed);
            return;
        }

        // 磁吸Perk
        if (mainHandItem.is(ModTags.Items.GUN)
                && GunData.from(mainHandItem).perk.has(ModPerks.POWERFUL_ATTRACTION.get())
                && (DamageTypeTool.isGunDamage(source) || DamageTypeTool.isExplosionDamage(source))
        ) {
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
        var data = GunData.from(stack);

        int level = data.perk.getLevel(ModPerks.POWERFUL_ATTRACTION);
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
        var data = GunData.from(stack);

        int level = data.perk.getLevel(ModPerks.VORPAL_WEAPON);
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
