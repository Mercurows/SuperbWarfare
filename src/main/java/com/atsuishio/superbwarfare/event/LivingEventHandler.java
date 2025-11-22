package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.api.event.PreKillEvent;
import com.atsuishio.superbwarfare.capability.LaserCapability;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.config.common.GameplayConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.gun.Ammo;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.ReloadState;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.mixin.ExplosionAccess;
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback;
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.DrawClientMessage;
import com.atsuishio.superbwarfare.network.message.receive.LivingGunKillMessage;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.AI_PASSENGER_WEAPON_TARGET_UUID;
import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.AI_TURRET_TARGET_UUID;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber
public class LivingEventHandler {

    @SubscribeEvent
    public static void onLivingChangeTargetEvent(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.getVehicle() instanceof VehicleEntity vehicle) {
            if (mob == vehicle.getNthEntity(vehicle.getTurretControllerIndex())) {
                if (event.getNewTarget() != null) {
                    vehicle.getEntityData().set(AI_TURRET_TARGET_UUID, event.getNewTarget().getStringUUID());
                } else {
                    vehicle.getEntityData().set(AI_TURRET_TARGET_UUID, "undefined");
                }
            }

            if (mob == vehicle.getNthEntity(vehicle.getPassengerWeaponStationControllerIndex())) {
                if (event.getNewTarget() != null) {
                    vehicle.getEntityData().set(AI_PASSENGER_WEAPON_TARGET_UUID, event.getNewTarget().getStringUUID());
                } else {
                    vehicle.getEntityData().set(AI_PASSENGER_WEAPON_TARGET_UUID, "undefined");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityAttacked(LivingAttackEvent event) {
        if (!event.getSource().is(ModDamageTypes.VEHICLE_EXPLOSION)
                && event.getEntity().getVehicle() instanceof VehicleEntity vehicle
                && vehicle.isEnclosed(event.getEntity())
        ) {
            if (!event.getSource().is(ModTags.DamageTypes.VEHICLE_NOT_ABSORB)) {
                vehicle.hurt(event.getSource(), event.getAmount());
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        if (event == null || event.getEntity() == null) {
            return;
        }

        handleVehicleHurt(event);
        handleGunPerksWhenHurt(event);
        renderDamageIndicator(event);
        reduceDamage(event);
        giveExpToWeapon(event);
        handleGunLevels(event);
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event == null || event.getEntity() == null) {
            return;
        }

        killIndication(event);
        handleGunPerksWhenDeath(event);
        handlePlayerKillEntity(event);
        giveKillExpToWeapon(event);

        if (event.getEntity() instanceof Player player) {
            handlePlayerBeamReset(player);
        }
    }

    private static void handleVehicleHurt(LivingHurtEvent event) {
        var entity = event.getEntity().getVehicle();
        if (entity instanceof VehicleEntity vehicle) {
            var source = event.getSource();
            if (source.is(ModTags.DamageTypes.VEHICLE_IGNORE)) return;

            if (vehicle.isEnclosed(event.getEntity())) {
                if (!source.is(ModDamageTypes.VEHICLE_EXPLOSION)) {
                    event.setCanceled(true);
                }
            } else {
                if (!source.is(ModTags.DamageTypes.VEHICLE_NOT_ABSORB)) {
                    entity.hurt(event.getSource(), 0.7f * event.getAmount());
                }

                event.setAmount(0.3f * event.getAmount());
            }
        }
    }

    /**
     * 计算伤害减免
     */
    private static void reduceDamage(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity entity = event.getEntity();
        if (entity == null) return;
        Entity sourceEntity = source.getEntity();
        if (sourceEntity == null) return;
        if (sourceEntity.level().isClientSide) return;

        double amount = event.getAmount();
        double damage = amount;

        ItemStack stack = sourceEntity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;

        // 距离衰减
        if (DamageTypeTool.isGunDamage(source) && stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);
            double distance = entity.position().distanceTo(sourceEntity.position());
            damage = reduceDamageByDistance(amount, distance, data.getDamageReduceRate(), data.getDamageReduceMinDistance());
        }

        // 计算防弹插板减伤
        ItemStack armor = entity.getItemBySlot(EquipmentSlot.CHEST);

        if (armor != ItemStack.EMPTY && armor.getTag() != null && armor.getTag().contains("ArmorPlate")) {
            double armorValue = armor.getOrCreateTag().getDouble("ArmorPlate");
            armor.getOrCreateTag().putDouble("ArmorPlate", Math.max(armorValue - damage, 0));
            damage = Math.max(damage - armorValue, 0);
        }

        // 计算防弹护具减伤
        if (source.is(ModTags.DamageTypes.PROJECTILE) || source.is(DamageTypes.MOB_PROJECTILE)) {
            damage *= 1 - 0.8 * Mth.clamp(entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE.get()), 0, 1);
        }

        if (source.is(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)) {
            damage *= 1 - 0.2 * Mth.clamp(entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE.get()), 0, 1);
        }

        if (source.is(ModDamageTypes.PROJECTILE_EXPLOSION) || source.is(ModDamageTypes.MINE) || source.is(ModDamageTypes.PROJECTILE_HIT) || source.is(ModDamageTypes.CUSTOM_EXPLOSION)
                || source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            damage *= 1 - 0.3 * Mth.clamp(entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE.get()), 0, 1);
        }

        event.setAmount((float) damage);

        if (entity instanceof TargetEntity && sourceEntity instanceof Player player) {
            if (event.getSource().is(ModDamageTypes.BEAST)) {
                damage = Float.POSITIVE_INFINITY;
            }

            player.displayClientMessage(Component.translatable("tips.superbwarfare.target.damage",
                    FormatTool.format2D(damage),
                    FormatTool.format1D(entity.position().distanceTo(sourceEntity.position()), "m")), false);
        }
    }

    private static double reduceDamageByDistance(double amount, double distance, double rate, double minDistance) {
        return amount / (1 + rate * Math.max(0, distance - minDistance));
    }

    /**
     * 根据造成的伤害，提供武器经验
     */
    private static void giveExpToWeapon(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (source == null) return;
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        if (event.getEntity().getType().is(ModTags.EntityTypes.NO_EXPERIENCE)) return;

        var data = GunData.from(stack);
        double amount = Math.min(0.125 * event.getAmount(), event.getEntity().getMaxHealth());

        // 先处理发射器类武器或高爆弹的爆炸伤害
        if (source.is(ModDamageTypes.PROJECTILE_EXPLOSION)) {
            if (data.compute().explosionDamage > 0 || GunData.from(stack).perk.getLevel(ModPerks.HE_BULLET) > 0) {
                data.exp.set(data.exp.get() + amount);
            }
        }

        // 再判断是不是枪械能造成的伤害
        if (!DamageTypeTool.isGunDamage(source)) return;

        data.exp.set(data.exp.get() + amount);
        data.save();
    }

    private static void giveKillExpToWeapon(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (source == null) return;
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        if (event.getEntity().getType().is(ModTags.EntityTypes.NO_EXPERIENCE)) return;

        var data = GunData.from(stack);
        double amount = 20 + 2 * event.getEntity().getMaxHealth();

        // 先处理发射器类武器或高爆弹的爆炸伤害
        if (source.is(ModDamageTypes.PROJECTILE_EXPLOSION)) {
            if (data.compute().explosionDamage > 0 || GunData.from(stack).perk.getLevel(ModPerks.HE_BULLET) > 0) {
                data.exp.set(data.exp.get() + amount);
            }
        }

        // 再判断是不是枪械能造成的伤害
        if (DamageTypeTool.isGunDamage(source)) {
            data.exp.set(data.exp.get() + amount);
        }

        // 提升武器等级
        int level = data.level.get();
        double exp = data.exp.get();
        double upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded;
            level = data.level.get() + 1;
            upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;
            data.exp.set(exp);
            data.level.set(level);
            data.upgradePoint.set(data.upgradePoint.get() + 0.5);
        }
        data.save();
    }

    private static void handleGunLevels(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (source == null) return;
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        if (event.getEntity().getType().is(ModTags.EntityTypes.NO_EXPERIENCE)) return;

        var data = GunData.from(stack);
        int level = data.level.get();
        double exp = data.exp.get();
        double upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded;
            level = data.level.get() + 1;
            upgradeExpNeeded = 20 * Math.pow(level, 2) + 160 * level + 20;
            data.exp.set(exp);
            data.level.set(level);
            data.upgradePoint.set(data.upgradePoint.get() + 0.5);
        }
        data.save();
    }

    private static void killIndication(LivingDeathEvent event) {
        if (!MiscConfig.SEND_KILL_FEEDBACK.get()) return;

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
            if (MinecraftForge.EVENT_BUS.post(new PreKillEvent.Indicator(player, source, event.getEntity()))) {
                return;
            }

            SoundTool.playLocalSound(player, ModSounds.TARGET_DOWN.get(), 3f, 1f);

            NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(2, 8));
        }
    }

    private static void renderDamageIndicator(LivingHurtEvent event) {
        if (event == null || event.getEntity() == null) {
            return;
        }

        var damagesource = event.getSource();
        var sourceEntity = damagesource.getEntity();

        if (sourceEntity == null) {
            return;
        }

        if (sourceEntity instanceof ServerPlayer player && (damagesource.is(DamageTypes.EXPLOSION) || damagesource.is(DamageTypes.PLAYER_EXPLOSION)
                || damagesource.is(ModDamageTypes.MINE) || damagesource.is(ModDamageTypes.PROJECTILE_EXPLOSION))) {
            SoundTool.playLocalSound(player, ModSounds.INDICATION.get(), 1f, 1f);

            NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
        }
    }

    /**
     * 换弹时切换枪械，取消换弹音效播放
     */
    @SubscribeEvent
    public static void handleChangeSlot(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && event.getSlot() == EquipmentSlot.MAINHAND) {
            if (player.level().isClientSide) {
                return;
            }

            ItemStack oldStack = event.getFrom();
            ItemStack newStack = event.getTo();

            player.getCapability(ModCapabilities.LASER_CAPABILITY).ifPresent(LaserCapability.ILaserCapability::stop);

            if (player instanceof ServerPlayer serverPlayer) {
                if (newStack.getItem() instanceof GunItem) {
                    checkCopyGuns(newStack, player);
                }

                if (newStack.getItem() != oldStack.getItem()
                        || newStack.getTag() == null || oldStack.getTag() == null
                        || (newStack.getItem() instanceof GunItem && !GunData.from(newStack).initialized())
                        || (oldStack.getItem() instanceof GunItem && !GunData.from(oldStack).initialized())
                        || (newStack.getItem() instanceof GunItem && oldStack.getItem() instanceof GunItem && !Objects.equals(GunsTool.getGunUUID(newStack), GunsTool.getGunUUID(oldStack)))
                ) {
                    NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), DrawClientMessage.INSTANCE);

                    if (oldStack.getItem() instanceof GunItem oldGun) {
                        var oldData = GunData.from(oldStack);

                        stopGunReloadSound(serverPlayer, oldData);

                        if (oldData.compute().boltActionTime > 0) {
                            oldData.bolt.actionTimer.reset();
                        }

                        oldData.reload.setTime(0);

                        oldData.reload.setState(ReloadState.NOT_RELOADING);

                        if (oldData.compute().iterativeTime != 0) {
                            oldData.stopped.set(false);
                            oldData.forceStop.set(false);
                            oldData.reload.setStage(0);
                            oldData.reload.prepareTimer.reset();
                            oldData.reload.prepareLoadTimer.reset();
                            oldData.reload.iterativeLoadTimer.reset();
                            oldData.reload.finishTimer.reset();
                        }

                        if (oldStack.is(ModItems.SENTINEL.get())) {
                            oldData.charge.timer.reset();
                        }

                        // TODO 如何保存修改后的数据
                        oldGun.onChangeSlot(oldData, player);
                        oldData.save();
                    }

                    if (newStack.getItem() instanceof GunItem) {
                        var newData = GunData.from(newStack);

                        if (newData.compute().boltActionTime > 0) {
                            newData.bolt.actionTimer.reset();
                        }

                        newData.reload.setState(ReloadState.NOT_RELOADING);
                        newData.reload.reloadTimer.reset();

                        if (newData.compute().iterativeTime != 0) {
                            newData.forceStop.set(false);
                            newData.stopped.set(false);
                            newData.reload.setStage(0);
                            newData.reload.prepareTimer.reset();
                            newData.reload.prepareLoadTimer.reset();
                            newData.reload.iterativeLoadTimer.reset();
                            newData.reload.finishTimer.reset();
                        }

                        if (newStack.is(ModItems.SENTINEL.get())) {
                            newData.charge.timer.reset();
                        }

                        for (Perk.Type type : Perk.Type.values()) {
                            var instance = newData.perk.getInstance(type);
                            if (instance != null) {
                                instance.perk().onChangeSlot(newData, instance, player);
                            }
                        }

                        newData.save();
                    }
                }
            }
        }
    }

    private static void checkCopyGuns(ItemStack stack, Player player) {
        var data = GunData.from(stack);
        if (!data.initialized()) return;
        if (data.gunDataTag == null) return;
        var uuid = data.gunDataTag.getUUID("UUID");

        for (var item : player.getInventory().items) {
            if (item.equals(stack)) continue;
            if (item.getItem() instanceof GunItem) {
                var itemData = GunData.from(item);
                var dataTag = itemData.gunDataTag;
                if (dataTag == null) continue;
                if (!dataTag.hasUUID("UUID")) continue;
                if (dataTag.getUUID("UUID").equals(uuid)) {
                    data.gunDataTag.putUUID("UUID", UUID.randomUUID());
                    return;
                }
            }
        }
    }

    public static void stopGunReloadSound(ServerPlayer player, GunData data) {
        var soundInfo = data.compute().soundInfo;
        soundInfo.cancellableSounds.list
                .forEach(str -> {
                    var location = ResourceLocation.tryParse(str);
                    if (location != null) {
                        player.connection.send(new ClientboundStopSoundPacket(location, SoundSource.PLAYERS));
                    }
                });
    }

    /**
     * 发送击杀消息
     */
    private static void handlePlayerKillEntity(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        ResourceKey<DamageType> damageTypeResourceKey = source.typeHolder().unwrapKey().isPresent() ? source.typeHolder().unwrapKey().get() : DamageTypes.GENERIC;

        LivingEntity attacker = null;
        if (source.getEntity() instanceof LivingEntity living) {
            if (living instanceof ServerPlayer player) {
                attacker = player;
            } else {
                attacker = living;
            }
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity living) {
            if (living instanceof ServerPlayer player) {
                attacker = player;
            } else if (living instanceof OwnableEntity ownableEntity && ownableEntity.getOwner() instanceof ServerPlayer) {
                attacker = living;
            }
        }

        if (MinecraftForge.EVENT_BUS.post(new PreKillEvent.SendKillMessage(attacker, source, entity))) {
            return;
        }

        if (attacker != null && MiscConfig.SEND_KILL_FEEDBACK.get()) {
            if (DamageTypeTool.isHeadshotDamage(source)) {
                NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new LivingGunKillMessage(attacker.getId(), entity.getId(), true, damageTypeResourceKey));
            } else {
                NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new LivingGunKillMessage(attacker.getId(), entity.getId(), false, damageTypeResourceKey));
            }
        }
    }

    private static void handleGunPerksWhenHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!DamageTypeTool.isGunDamage(source) && !source.is(DamageTypes.PLAYER_ATTACK)) return;

        LivingEntity attacker = null;
        if (source.getEntity() instanceof LivingEntity living) {
            attacker = living;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity living) {
            attacker = living;
        }
        if (attacker == null) {
            return;
        }

        ItemStack stack = attacker.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }

        float damage = event.getAmount();

        GunData data = GunData.from(stack);
        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                if (DamageTypeTool.isGunDamage(source)) {
                    damage = instance.perk().getModifiedDamage(damage, data, instance, event.getEntity(), source);
                    instance.perk().onHurtEntity(damage, data, instance, event.getEntity(), source);
                } else if (source.is(DamageTypes.PLAYER_ATTACK)) {
                    instance.perk().onMeleeAttack(data, instance, event.getEntity());
                }
            }
        }

        event.setAmount(damage);
    }

    private static void handleGunPerksWhenDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (!DamageTypeTool.isGunDamage(source)) return;

        LivingEntity attacker = null;
        if (source.getEntity() instanceof LivingEntity living) {
            attacker = living;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity living) {
            attacker = living;
        }
        if (attacker == null) {
            return;
        }

        ItemStack stack = attacker.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }

        GunData data = GunData.from(stack);
        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().onKill(data, instance, event.getEntity(), source);
            }
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (!VehicleConfig.VEHICLE_ITEM_PICKUP.get()) return;
        if (event.getEntity().getVehicle() instanceof VehicleEntity vehicleEntity) {
            var pickUp = event.getItem();
            if (!vehicleEntity.level().isClientSide) {
                HopperBlockEntity.addItem(vehicleEntity, pickUp);
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        playerDropAmmoBox(event);
        vehicleCollectDrops(event);
    }

    /**
     * 开启死亡掉落 & 保留武器弹药时，玩家死亡会掉落一个弹药盒
     */
    private static void playerDropAmmoBox(LivingDropsEvent event) {
        if (!MiscConfig.DROP_AMMO_BOX.get()) return;

        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.level().getLevelData().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE).orElse(new PlayerVariable());
        cap.watch();

        boolean drop = Stream.of(Ammo.values())
                .mapToInt(type -> type.get(cap))
                .sum() > 0;
        if (!drop) return;

        var stack = new ItemStack(ModItems.AMMO_BOX.get());

        for (var type : Ammo.values()) {
            type.set(stack, type.get(cap));
            type.set(cap, 0);
        }

        stack.getOrCreateTag().putBoolean("All", true);
        stack.getOrCreateTag().putBoolean("IsDrop", true);

        cap.sync(player);
        event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY() + 1, player.getZ(), stack));
    }

    /**
     * 载具撞死生物时自动收集掉落物
     */
    private static void vehicleCollectDrops(LivingDropsEvent event) {
        if (!VehicleConfig.COLLECT_DROPS_BY_CRASHING.get()) return;

        DamageSource source = event.getSource();
        if (source == null) return;
        if (!source.is(ModDamageTypes.VEHICLE_STRIKE)) return;

        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            var drops = event.getDrops();
            var removed = new ArrayList<ItemEntity>();

            drops.forEach(itemEntity -> {
                ItemStack stack = itemEntity.getItem();

                InventoryTool.insertItem(vehicle.getItemStacks(), stack);

                if (stack.getCount() <= 0) {
                    player.drop(stack, false);
                    removed.add(itemEntity);
                }
            });

            drops.removeAll(removed);
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null) return;

        if (player.getVehicle() instanceof VehicleEntity) {
            player.giveExperiencePoints(event.getDroppedExperience());
            event.setCanceled(true);
        }
    }

    public static void handlePlayerBeamReset(Player player) {
        player.getCapability(ModCapabilities.LASER_CAPABILITY).ifPresent(LaserCapability.ILaserCapability::end);
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
        if (event.getSource().getDirectEntity() instanceof AutoAimableEntity && !(event.getTarget() instanceof Player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPreIndicator(PreKillEvent.Indicator event) {
        if (event.getSource().getDirectEntity() instanceof AutoAimableEntity && !(event.getTarget() instanceof Player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEffectApply(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.HARMFUL
                && event.getEntity().getVehicle() instanceof VehicleEntity vehicle
                && vehicle.isEnclosed(vehicle.getSeatIndex(event.getEntity()))
        ) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * 取消原版爆炸对载具的影响，改为单独计算
     * Code based on YWZJ-Vehicle
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        var explosion = event.getExplosion();
        if (explosion instanceof CustomExplosion) return;

        Iterator<Entity> iterator = event.getAffectedEntities().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity instanceof VehicleEntity) {
                iterator.remove();

                Vec3 explosionPos = explosion.getPosition();
                float explosionRadius = ((ExplosionAccess) explosion).superbwarfare$getRadius() * 2.0F;
                if (!entity.ignoreExplosion()) {
                    double distanceRatio = Math.sqrt(entity.distanceToSqr(explosionPos)) / explosionRadius;
                    if (distanceRatio <= 1.0D) {
                        double dx = entity.getX() - explosionPos.x;
                        double dy = entity.getEyeY() - explosionPos.y;
                        double dz = entity.getZ() - explosionPos.z;
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (distance != 0.0D) {
                            double visibilityFactor = Explosion.getSeenPercent(explosionPos, entity);
                            double impactStrength = (1.0D - distanceRatio) * visibilityFactor;
                            float damage = (float) ((int) ((impactStrength * impactStrength + impactStrength) / 2.0D * 7.0D * explosionRadius + 1.0D));
                            entity.hurt(explosion.getDamageSource(), damage);
                        }
                    }
                }
            }
        }
    }
}
