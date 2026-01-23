package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.api.event.PreKillEvent
import com.atsuishio.superbwarfare.capability.LaserCapability
import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.config.common.GameplayConfig
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.value.ReloadState
import com.atsuishio.superbwarfare.entity.TargetEntity
import com.atsuishio.superbwarfare.entity.mixin.ExplosionAccess
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.DrawClientMessage
import com.atsuishio.superbwarfare.network.message.receive.LivingGunKillMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.*
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraftforge.event.entity.living.*
import net.minecraftforge.event.entity.player.EntityItemPickupEvent
import net.minecraftforge.event.level.ExplosionEvent
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

@Mod.EventBusSubscriber
object LivingEventHandler {
    @SubscribeEvent
    fun onLivingChangeTargetEvent(event: LivingChangeTargetEvent) {
        val entity = event.entity
        val vehicle = entity.vehicle
        if (entity is Mob && vehicle is VehicleEntity) {
            if (entity == vehicle.getNthEntity(vehicle.turretControllerIndex)) {
                if (event.newTarget != null) {
                    vehicle.aiTurretTargetUUID = event.newTarget.getStringUUID()
                } else {
                    vehicle.aiTurretTargetUUID = "undefined"
                }
            }

            if (entity == vehicle.getNthEntity(vehicle.passengerWeaponStationControllerIndex)) {
                if (event.newTarget != null) {
                    vehicle.aiPassengerWeaponTargetUUID = event.newTarget.getStringUUID()
                } else {
                    vehicle.aiPassengerWeaponTargetUUID = "undefined"
                }
            }
        }
    }

    @SubscribeEvent
    fun onEntityAttacked(event: LivingAttackEvent) {
        val source = event.source
        val entity = event.entity ?: return
        val vehicle = entity.vehicle
        if (!source.`is`(ModDamageTypes.VEHICLE_EXPLOSION) && !source.`is`(ModDamageTypes.AIR_CRASH)
            && vehicle is VehicleEntity
            && vehicle.isEnclosed(entity)
        ) {
            if (!source.`is`(ModTags.DamageTypes.VEHICLE_NOT_ABSORB)) {
                vehicle.hurt(source, event.amount)
            }
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onEntityHurt(event: LivingHurtEvent) {
        if (event.entity == null) return

        handleVehicleHurt(event)
        handleGunPerksWhenHurt(event)
        renderDamageIndicator(event)
        reduceDamage(event)
        giveExpToWeapon(event)
        handleGunLevels(event)
    }

    @SubscribeEvent
    fun onEntityDeath(event: LivingDeathEvent) {
        val entity = event.entity ?: return

        killIndication(event)
        handleGunPerksWhenDeath(event)
        handlePlayerKillEntity(event)
        giveKillExpToWeapon(event)

        if (entity is Player) {
            handlePlayerBeamReset(entity)
        }
    }

    fun handleVehicleHurt(event: LivingHurtEvent) {
        val entity = event.entity
        val vehicle = entity.vehicle
        if (vehicle is VehicleEntity) {
            val source = event.source
            if (source.`is`(ModTags.DamageTypes.VEHICLE_IGNORE)) return

            if (vehicle.isEnclosed(entity)) {
                if (!source.`is`(ModDamageTypes.VEHICLE_EXPLOSION) && !source.`is`(ModDamageTypes.AIR_CRASH)) {
                    event.isCanceled = true
                }
            } else {
                if (!source.`is`(ModTags.DamageTypes.VEHICLE_NOT_ABSORB)) {
                    entity.hurt(source, 0.7f * event.amount)
                }

                event.amount *= 0.3f
            }
        }
    }

    /**
     * 计算伤害减免
     */
    private fun reduceDamage(event: LivingHurtEvent) {
        val source = event.source
        val entity = event.entity ?: return
        val sourceEntity = source.entity ?: return
        if (sourceEntity.level().isClientSide) return

        val amount = event.amount
        var damage = amount

        val stack = if (sourceEntity is LivingEntity) sourceEntity.mainHandItem else ItemStack.EMPTY

        // 距离衰减
        if (DamageTypeTool.isGunDamage(source) && stack.item is GunItem) {
            val data = GunData.from(stack)
            val distance = entity.position().distanceTo(sourceEntity.position())
            damage = reduceDamageByDistance(
                amount.toDouble(),
                distance,
                data.damageReduceRate,
                data.damageReduceMinDistance
            ).toFloat()
        }

        // 计算防弹插板减伤
        val armor = entity.getItemBySlot(EquipmentSlot.CHEST)
        val tag = armor.tag
        if (armor != ItemStack.EMPTY && tag != null && tag.contains("ArmorPlate")) {
            val armorValue = armor.getOrCreateTag().getDouble("ArmorPlate")
            armor.getOrCreateTag().putDouble("ArmorPlate", (armorValue - damage).coerceAtLeast(0.0))
            damage = (damage - armorValue).coerceAtLeast(0.0).toFloat()
        }

        // 计算防弹护具减伤
        if (source.`is`(ModTags.DamageTypes.PROJECTILE) || source.`is`(DamageTypes.MOB_PROJECTILE)) {
            damage *= 1 - 0.8f * entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE.get()).coerceIn(0.0, 1.0)
                .toFloat()
        }

        if (source.`is`(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)) {
            damage *= 1 - 0.2f * entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE.get()).coerceIn(0.0, 1.0)
                .toFloat()
        }

        if (source.`is`(ModDamageTypes.PROJECTILE_EXPLOSION) || source.`is`(ModDamageTypes.MINE)
            || source.`is`(ModDamageTypes.PROJECTILE_HIT) || source.`is`(ModDamageTypes.CUSTOM_EXPLOSION)
            || source.`is`(DamageTypes.EXPLOSION) || source.`is`(DamageTypes.PLAYER_EXPLOSION)
        ) {
            damage *= 1 - 0.3f * entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE.get()).coerceIn(0.0, 1.0)
                .toFloat()
        }

        event.amount = damage

        if (entity is TargetEntity && sourceEntity is Player) {
            if (source.`is`(ModDamageTypes.BEAST)) {
                damage = Float.POSITIVE_INFINITY
            }

            sourceEntity.displayClientMessage(
                Component.translatable(
                    "tips.superbwarfare.target.damage",
                    FormatTool.format2D(damage.toDouble()),
                    FormatTool.format1D(entity.position().distanceTo(sourceEntity.position()), "m")
                ), false
            )
        }
    }

    private fun reduceDamageByDistance(amount: Double, distance: Double, rate: Double, minDistance: Double): Double {
        return amount / (1 + rate * 0.0.coerceAtLeast(distance - minDistance))
    }

    /**
     * 根据造成的伤害，提供武器经验
     */
    private fun giveExpToWeapon(event: LivingHurtEvent) {
        val source = event.source ?: return
        val sourceEntity = source.entity as? Player ?: return
        val stack = sourceEntity.mainHandItem
        if (stack.item !is GunItem) return
        val entity = event.entity
        if (entity.type.`is`(ModTags.EntityTypes.NO_EXPERIENCE)) return

        val data = GunData.from(stack)
        val amount = (0.125f * event.amount).coerceAtMost(entity.maxHealth)

        // 先处理发射器类武器或高爆弹的爆炸伤害
        if (source.`is`(ModDamageTypes.PROJECTILE_EXPLOSION)) {
            if (data.get(GunProp.EXPLOSION_DAMAGE) > 0 || GunData.from(stack).perk.getLevel(ModPerks.HE_BULLET) > 0) {
                data.exp.set(data.exp.get() + amount)
            }
        }

        // 再判断是不是枪械能造成的伤害
        if (!DamageTypeTool.isGunDamage(source)) return

        data.exp.set(data.exp.get() + amount)
        data.save()
    }

    private fun giveKillExpToWeapon(event: LivingDeathEvent) {
        val source = event.source ?: return
        val sourceEntity = source.entity as? Player ?: return
        val stack = sourceEntity.mainHandItem
        if (stack.item !is GunItem) return
        val entity = event.entity
        if (entity.type.`is`(ModTags.EntityTypes.NO_EXPERIENCE)) return

        val data = GunData.from(stack)
        val amount = 20 + 2 * entity.maxHealth.toDouble()

        // 先处理发射器类武器或高爆弹的爆炸伤害
        if (source.`is`(ModDamageTypes.PROJECTILE_EXPLOSION)) {
            if (data.get(GunProp.EXPLOSION_DAMAGE) > 0 || GunData.from(stack).perk.getLevel(ModPerks.HE_BULLET) > 0) {
                data.exp.add(amount)
            }
        }

        // 再判断是不是枪械能造成的伤害
        if (DamageTypeTool.isGunDamage(source)) {
            data.exp.add(amount)
        }

        // 提升武器等级
        var level = data.level.get()
        var exp = data.exp.get()
        var upgradeExpNeeded = 20 * level.toDouble().pow(2) + 160 * level + 20

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded
            level = data.level.get() + 1
            upgradeExpNeeded = 20 * level.toDouble().pow(2) + 160 * level + 20
            data.exp.set(exp)
            data.level.set(level)
        }
        data.save()
    }

    private fun handleGunLevels(event: LivingHurtEvent) {
        val source = event.source ?: return
        val sourceEntity = source.entity as? Player ?: return
        val stack = sourceEntity.mainHandItem
        if (stack.item !is GunItem) return
        val entity = event.entity
        if (entity.type.`is`(ModTags.EntityTypes.NO_EXPERIENCE)) return

        val data = GunData.from(stack)
        var level = data.level.get()
        var exp = data.exp.get()
        var upgradeExpNeeded = 20 * level.toDouble().pow(2) + 160 * level + 20

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded
            level = data.level.get() + 1
            upgradeExpNeeded = 20 * level.toDouble().pow(2) + 160 * level + 20
            data.exp.set(exp)
            data.level.set(level)
        }
        data.save()
    }

    private fun killIndication(event: LivingDeathEvent) {
        if (!MiscConfig.SEND_KILL_FEEDBACK.get()) return

        val source = event.source
        val sourceEntity = source.entity ?: return

        // 如果配置不选择全局伤害提示，则只在伤害类型为mod添加的时显示指示器
        if (!GameplayConfig.GLOBAL_INDICATION.get() && !DamageTypeTool.isModDamage(source)) {
            return
        }

        if (!sourceEntity.level().isClientSide() && sourceEntity is ServerPlayer) {
            if (postEvent(PreKillEvent.Indicator(sourceEntity, source, event.entity))) return

            SoundTool.playLocalSound(sourceEntity, ModSounds.TARGET_DOWN.get(), 3f, 1f)
            sendPacketTo(sourceEntity, ClientIndicatorMessage(2, 8))
        }
    }

    private fun renderDamageIndicator(event: LivingHurtEvent) {
        if (event.entity == null) return
        val source = event.source
        val sourceEntity = source.entity ?: return

        if (sourceEntity is ServerPlayer &&
            (source.`is`(DamageTypes.EXPLOSION) || source.`is`(DamageTypes.PLAYER_EXPLOSION)
                    || source.`is`(ModDamageTypes.MINE) || source.`is`(ModDamageTypes.PROJECTILE_EXPLOSION))
        ) {
            SoundTool.playLocalSound(sourceEntity, ModSounds.INDICATION.get(), 1f, 1f)
            sendPacketTo(sourceEntity, ClientIndicatorMessage(0, 5))
        }
    }

    /**
     * 换弹时切换枪械，取消换弹音效播放
     */
    @SubscribeEvent
    fun handleChangeSlot(event: LivingEquipmentChangeEvent) {
        val entity = event.entity
        val slot = event.slot
        if (entity is Player && slot == EquipmentSlot.MAINHAND) {
            if (entity.level().isClientSide) {
                return
            }

            val oldStack = event.from
            val newStack = event.to

            entity.getCapability(ModCapabilities.LASER_CAPABILITY).ifPresent(LaserCapability.ILaserCapability::stop)

            if (entity is ServerPlayer) {
                if (newStack.item is GunItem) {
                    checkCopyGuns(newStack, entity)
                }

                if (newStack.item != oldStack.item
                    || newStack.tag == null || oldStack.tag == null
                    || (newStack.item is GunItem && !GunData.from(newStack).initialized())
                    || (oldStack.item is GunItem && !GunData.from(oldStack).initialized())
                    || (newStack.item is GunItem && oldStack.item is GunItem && !Objects.equals(
                        GunsTool.getGunUUID(newStack),
                        GunsTool.getGunUUID(oldStack)
                    ))
                ) {
                    sendPacketTo(entity, DrawClientMessage)

                    val oldGun = oldStack.item
                    if (oldGun is GunItem) {
                        val oldData = GunData.from(oldStack)

                        stopGunReloadSound(entity, oldData)

                        if (oldData.get(GunProp.BOLT_ACTION_TIME) > 0) {
                            oldData.bolt.actionTimer.reset()
                        }

                        oldData.reload.setTime(0)

                        oldData.reload.setState(ReloadState.NOT_RELOADING)

                        if (oldData.get(GunProp.ITERATIVE_TIME) != 0) {
                            oldData.stopped.set(false)
                            oldData.forceStop.set(false)
                            oldData.reload.setStage(0)
                            oldData.reload.prepareTimer.reset()
                            oldData.reload.prepareLoadTimer.reset()
                            oldData.reload.iterativeLoadTimer.reset()
                            oldData.reload.finishTimer.reset()
                        }

                        if (oldStack.`is`(ModItems.SENTINEL.get())) {
                            oldData.charge.timer.reset()
                        }

                        // TODO 如何保存修改后的数据
                        oldGun.onChangeSlot(oldData, entity)
                        oldData.save()
                    }

                    val newItem = newStack.item
                    if (newItem is GunItem) {
                        val newData = GunData.from(newStack)

                        if (newData.get(GunProp.BOLT_ACTION_TIME) > 0) {
                            newData.bolt.actionTimer.reset()
                        }

                        newData.reload.setState(ReloadState.NOT_RELOADING)
                        newData.reload.reloadTimer.reset()

                        if (newData.get(GunProp.ITERATIVE_TIME) != 0) {
                            newData.forceStop.set(false)
                            newData.stopped.set(false)
                            newData.reload.setStage(0)
                            newData.reload.prepareTimer.reset()
                            newData.reload.prepareLoadTimer.reset()
                            newData.reload.iterativeLoadTimer.reset()
                            newData.reload.finishTimer.reset()
                        }

                        if (newStack.`is`(ModItems.SENTINEL.get())) {
                            newData.charge.timer.reset()
                        }

                        for (type in Perk.Type.entries) {
                            val instance = newData.perk.getInstances(type)
                            instance.forEach { it.perk.onChangeSlot(newData, it, entity) }
                        }

                        newData.save()
                    }
                }
            }
        }
    }

    private fun checkCopyGuns(stack: ItemStack, player: Player) {
        val data = GunData.from(stack)
        if (!data.initialized()) return
        val uuid = data.gunDataTag.getUUID("UUID")

        for (item in player.inventory.items) {
            if (item.equals(stack)) continue
            if (item.item is GunItem) {
                val itemData = GunData.from(item)
                val dataTag = itemData.gunDataTag
                if (!dataTag.hasUUID("UUID")) continue
                if (dataTag.getUUID("UUID").equals(uuid)) {
                    data.gunDataTag.putUUID("UUID", UUID.randomUUID())
                    return
                }
            }
        }
    }

    @JvmStatic
    fun stopGunReloadSound(player: ServerPlayer, data: GunData) {
        val soundInfo = data.get(GunProp.SOUND_INFO)
        soundInfo.cancellableSounds.list
            .forEach {
                val location = ResourceLocation.tryParse(it) ?: return@forEach
                player.sendPacket(ClientboundStopSoundPacket(location, SoundSource.PLAYERS))
            }
    }

    /**
     * 发送击杀消息
     */
    private fun handlePlayerKillEntity(event: LivingDeathEvent) {
        val entity = event.entity
        val source = event.source

        val damageTypeResourceKey = if (source.typeHolder().unwrapKey().isPresent) {
            source.typeHolder().unwrapKey().get()
        } else DamageTypes.GENERIC

        var attacker: LivingEntity? = null
        val sourceEntity = source.entity
        val directEntity = source.directEntity

        if (sourceEntity is LivingEntity) {
            attacker = sourceEntity
        }

        if (directEntity is Projectile && directEntity.owner is LivingEntity) {
            val owner = directEntity.owner
            if (owner is ServerPlayer) {
                attacker = owner
            } else if (owner is OwnableEntity && owner.owner is ServerPlayer) {
                attacker = owner.owner
            }
        }

        if (attacker == null) return

        if (postEvent(PreKillEvent.SendKillMessage(attacker, source, entity))) {
            return
        }

        if (MiscConfig.SEND_KILL_FEEDBACK.get()) {
            sendPacketToAll(
                LivingGunKillMessage(
                    attacker.id,
                    entity.id,
                    DamageTypeTool.isHeadshotDamage(source),
                    damageTypeResourceKey
                )
            )
        }
    }

    private fun handleGunPerksWhenHurt(event: LivingHurtEvent) {
        val source = event.source
        if (!DamageTypeTool.isGunDamage(source) && !source.`is`(DamageTypes.PLAYER_ATTACK)) return

        var attacker: LivingEntity? = null
        val sourceEntity = source.entity
        val directEntity = source.directEntity

        if (sourceEntity is LivingEntity) {
            attacker = sourceEntity
        }

        if (directEntity is Projectile && directEntity.owner is LivingEntity) {
            val owner = directEntity.owner
            if (owner is ServerPlayer) {
                attacker = owner
            } else if (owner is OwnableEntity && owner.owner is ServerPlayer) {
                attacker = owner.owner
            }
        }

        if (attacker == null) return

        val stack = attacker.mainHandItem
        if (stack.item !is GunItem) return

        var damage = event.amount

        val data = GunData.from(stack)
        for (type in Perk.Type.entries) {
            val instance = data.perk.getInstances(type)

            instance.forEach {
                if (DamageTypeTool.isGunDamage(source)) {
                    damage = it.perk.getModifiedDamage(damage, data, it, event.entity, source)
                    it.perk.onHurtEntity(damage, data, it, event.entity, source)
                } else if (source.`is`(DamageTypes.PLAYER_ATTACK)) {
                    it.perk.onMeleeAttack(data, it, event.entity, source)
                }
            }
        }

        event.amount = damage
    }

    private fun handleGunPerksWhenDeath(event: LivingDeathEvent) {
        val source = event.source
        if (!DamageTypeTool.isGunDamage(source)) return

        var attacker: LivingEntity? = null
        val sourceEntity = source.entity
        val directEntity = source.directEntity

        if (sourceEntity is LivingEntity) {
            attacker = sourceEntity
        }

        if (directEntity is Projectile && directEntity.owner is LivingEntity) {
            val owner = directEntity.owner
            if (owner is ServerPlayer) {
                attacker = owner
            } else if (owner is OwnableEntity && owner.owner is ServerPlayer) {
                attacker = owner.owner
            }
        }

        if (attacker == null) return

        val stack = attacker.mainHandItem
        if (stack.item !is GunItem) return

        val data = GunData.from(stack)
        for (type in Perk.Type.entries) {
            val instance = data.perk.getInstances(type)
            instance.forEach { it.perk.onKill(data, it, event.entity, source) }
        }
    }

    @SubscribeEvent
    fun onPickup(event: EntityItemPickupEvent) {
        if (!VehicleConfig.VEHICLE_ITEM_PICKUP.get()) return
        val vehicle = event.entity.vehicle
        if (vehicle is VehicleEntity) {
            val pickUp = event.item
            if (!vehicle.level().isClientSide) {
                HopperBlockEntity.addItem(vehicle, pickUp)
            }
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onLivingDrops(event: LivingDropsEvent) {
        playerDropAmmoBox(event)
        vehicleCollectDrops(event)
    }

    /**
     * 开启死亡掉落 & 保留武器弹药时，玩家死亡会掉落一个弹药盒
     */
    private fun playerDropAmmoBox(event: LivingDropsEvent) {
        val player = event.entity as? Player ?: return
        if (!MiscConfig.DROP_AMMO_BOX.get()) return

        val cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE).orElse(PlayerVariable())
        cap.watch()

        val drop = Ammo.entries.sumOf { it.get(cap) } > 0
        if (!drop) return

        val stack = ItemStack(ModItems.AMMO_BOX.get())

        for (type in Ammo.entries) {
            type.set(stack, type.get(cap))
            type.set(cap, 0)
        }

        stack.getOrCreateTag().putBoolean("All", true)
        stack.getOrCreateTag().putBoolean("IsDrop", true)

        cap.sync(player)
        event.drops += ItemEntity(player.level(), player.x, player.y + 1, player.z, stack)
    }

    /**
     * 载具撞死生物时自动收集掉落物
     */
    private fun vehicleCollectDrops(event: LivingDropsEvent) {
        if (!VehicleConfig.COLLECT_DROPS_BY_CRASHING.get()) return

        val source = event.source ?: return
        if (!source.`is`(ModDamageTypes.VEHICLE_STRIKE)) return

        val player = source.entity as? Player ?: return
        val vehicle = player.vehicle as? VehicleEntity ?: return

        val drops = event.drops
        val removed = arrayListOf<ItemEntity>()

        drops.forEach {
            val stack = it.item
            InventoryTool.insertItem(vehicle.itemStacks, stack)

            if (stack.count <= 0) {
                player.drop(stack, false)
                removed.add(it)
            }
        }

        drops -= removed.toSet()
    }

    @SubscribeEvent
    fun onLivingExperienceDrop(event: LivingExperienceDropEvent) {
        val player = event.attackingPlayer ?: return
        if (player.vehicle is VehicleEntity) {
            player.giveExperiencePoints(event.droppedExperience)
            event.isCanceled = true
        }
    }

    fun handlePlayerBeamReset(player: Player) {
        player.getCapability(ModCapabilities.LASER_CAPABILITY).ifPresent(LaserCapability.ILaserCapability::end)
    }

    @SubscribeEvent
    fun onKnockback(event: LivingKnockBackEvent) {
        val knockback = ICustomKnockback.getInstance(event.entity)
        if (knockback.`superbWarfare$getKnockbackStrength`() >= 0) {
            event.strength = knockback.`superbWarfare$getKnockbackStrength`().toFloat()
        }
    }

    @SubscribeEvent
    fun onEntityFall(event: LivingFallEvent) {
        val living = event.entity
        if (living.vehicle is VehicleEntity) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onPreSendKillMessage(event: PreKillEvent.SendKillMessage) {
        if (event.source.directEntity is AutoAimableEntity && event.target !is Player) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onPreIndicator(event: PreKillEvent.Indicator) {
        if (event.source.directEntity is AutoAimableEntity && event.target !is Player) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onEffectApply(event: MobEffectEvent.Applicable) {
        val entity = event.entity
        val vehicle = entity.vehicle
        if (event.effectInstance.effect.category == MobEffectCategory.HARMFUL
            && vehicle is VehicleEntity
            && vehicle.isEnclosed(vehicle.getSeatIndex(entity))
        ) {
            event.result = Event.Result.DENY
        }
    }

    /**
     * 取消原版爆炸对载具的影响，改为单独计算
     * Code based on YWZJ-Vehicle
     */
    @SubscribeEvent
    fun onExplosionDetonate(event: ExplosionEvent.Detonate) {
        val explosion = event.explosion as? CustomExplosion ?: return

        val iterator = event.affectedEntities.iterator()
        while (iterator.hasNext()) {
            val entity = iterator.next() as? VehicleEntity ?: continue

            iterator.remove()
            val explosionPos = explosion.position
            val explosionRadius = (explosion as ExplosionAccess).`superbwarfare$getRadius`() * 2.0F
            if (!entity.ignoreExplosion()) {
                val distanceRatio = sqrt(entity.distanceToSqr(explosionPos)) / explosionRadius
                if (distanceRatio <= 1.0) {
                    val dx = entity.x - explosionPos.x
                    val dy = entity.eyeY - explosionPos.y
                    val dz = entity.z - explosionPos.z
                    val distance = sqrt(dx * dx + dy * dy + dz * dz)
                    if (distance != 0.0) {
                        val visibilityFactor = Explosion.getSeenPercent(explosionPos, entity)
                        val impactStrength = (1.0 - distanceRatio) * visibilityFactor
                        val damage =
                            (impactStrength * impactStrength + impactStrength) / 2.0 * 7.0 * explosionRadius + 1.0

                        entity.hurt(explosion.damageSource, damage.toFloat())
                    }
                }
            }
        }
    }
}