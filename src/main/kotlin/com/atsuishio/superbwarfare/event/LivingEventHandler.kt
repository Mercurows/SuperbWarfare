package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.api.event.PreKillEvent.Indicator
import com.atsuishio.superbwarfare.api.event.PreKillEvent.SendKillMessage
import com.atsuishio.superbwarfare.config.common.GameplayConfig
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.SoundInfo
import com.atsuishio.superbwarfare.data.gun.value.ReloadState
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.ammo.ammoBoxData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.DrawClientMessage
import com.atsuishio.superbwarfare.network.message.receive.LivingGunKillMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.DamageTypeTool.isGunDamage
import com.atsuishio.superbwarfare.tools.DamageTypeTool.isHeadshotDamage
import com.atsuishio.superbwarfare.tools.DamageTypeTool.isModDamage
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.FormatTool.format2D
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
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
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.util.TriState
import net.neoforged.neoforge.event.entity.living.*
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent
import net.neoforged.neoforge.event.level.ExplosionEvent
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent
import java.util.*
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

@EventBusSubscriber
object LivingEventHandler {
    @SubscribeEvent
    fun onLivingChangeTargetEvent(event: LivingChangeTargetEvent) {
        val entity = event.entity
        val vehicle = entity.vehicle
        if (entity is Mob && vehicle is VehicleEntity) {
            if (entity === vehicle.getNthEntity(vehicle.turretControllerIndex)) {
                if (event.newAboutToBeSetTarget != null) {
                    vehicle.aiTurretTargetUUID = event.newAboutToBeSetTarget!!.getStringUUID()
                } else {
                    vehicle.aiTurretTargetUUID = "undefined"
                }
            }

            if (entity === vehicle.getNthEntity(vehicle.passengerWeaponStationControllerIndex)) {
                if (event.newAboutToBeSetTarget != null) {
                    vehicle.aiPassengerWeaponTargetUUID = event.newAboutToBeSetTarget!!.getStringUUID()
                } else {
                    vehicle.aiPassengerWeaponTargetUUID = "undefined"
                }
            }
        }
    }

    @SubscribeEvent
    fun onEntityAttacked(event: LivingIncomingDamageEvent) {
        val source = event.source
        val entity = event.entity ?: return
        val vehicle = entity.vehicle
        if (!source.`is`(ModDamageTypes.VEHICLE_EXPLOSION) && !source.`is`(ModDamageTypes.AIR_CRASH)
            && vehicle is VehicleEntity
            && vehicle.isEnclosed(event.entity)
        ) {
            if (!source.`is`(ModTags.DamageTypes.VEHICLE_NOT_ABSORB)) {
                vehicle.hurt(source, event.amount)
            }
            event.setCanceled(true)
        }
    }

    @SubscribeEvent
    fun onEntityHurt(event: LivingIncomingDamageEvent?) {
        if (event == null) return

        handleVehicleHurt(event)
        handleGunPerksWhenHurt(event)
        renderDamageIndicator(event)
        reduceDamage(event)
        giveExpToWeapon(event)
    }

    @SubscribeEvent
    fun onEntityDeath(event: LivingDeathEvent) {
        if (event.entity == null) return

        killIndication(event)
        handleGunPerksWhenDeath(event)
        handlePlayerKillEntity(event)
        giveKillExpToWeapon(event)
    }

    fun handleVehicleHurt(event: LivingIncomingDamageEvent) {
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
                val rate = vehicle.getSeat(entity)?.damageAbsorbRate ?: 0.0f
                if (!source.`is`(ModTags.DamageTypes.VEHICLE_NOT_ABSORB)) {
                    vehicle.hurt(source, rate.coerceIn(0f, 1f) * event.amount)
                }

                event.amount *= (1 - rate).coerceIn(0f, 1f)
            }
        }
    }

    /**
     * 计算伤害减免
     */
    private fun reduceDamage(event: LivingIncomingDamageEvent) {
        val source = event.source
        val entity = event.entity
        val sourceEntity = source.entity ?: return
        if (sourceEntity.level().isClientSide) return

        val amount = event.amount.toDouble()
        var damage = amount

        val stack = if (sourceEntity is LivingEntity) sourceEntity.mainHandItem else ItemStack.EMPTY

        // 距离衰减
        if (isGunDamage(source) && stack.item is GunItem) {
            val data = GunData.from(stack)
            val distance = entity.position().distanceTo(sourceEntity.position())
            damage = reduceDamageByDistance(amount, distance, data.damageReduceRate, data.damageReduceMinDistance)
        }

        // 计算防弹插板减伤
        val armor = entity.getItemBySlot(EquipmentSlot.CHEST)

        val tag = NBTTool.getTag(armor)
        if (armor != ItemStack.EMPTY && tag.contains("ArmorPlate")) {
            val armorValue = tag.getDouble("ArmorPlate")
            tag.putDouble("ArmorPlate", max(armorValue - damage, 0.0))
            NBTTool.saveTag(armor, tag)
            damage = max(damage - armorValue, 0.0)
        }

        // 计算防弹护具减伤
        if (source.`is`(ModTags.DamageTypes.PROJECTILE) || source.`is`(DamageTypes.MOB_PROJECTILE)) {
            damage *= 1 - 0.8 * Mth.clamp(entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE), 0.0, 1.0)
        }

        if (source.`is`(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)) {
            damage *= 1 - 0.2 * Mth.clamp(entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE), 0.0, 1.0)
        }

        if (source.`is`(ModDamageTypes.PROJECTILE_EXPLOSION) || source.`is`(ModDamageTypes.MINE) || source.`is`(
                ModDamageTypes.PROJECTILE_HIT
            ) || source.`is`(ModDamageTypes.CUSTOM_EXPLOSION)
            || source.`is`(DamageTypes.EXPLOSION) || source.`is`(DamageTypes.PLAYER_EXPLOSION)
        ) {
            damage *= 1 - 0.3 * Mth.clamp(entity.getAttributeValue(ModAttributes.BULLET_RESISTANCE), 0.0, 1.0)
        }

        event.amount = damage.toFloat()

        if (entity is TargetEntity && sourceEntity is Player) {
            if (event.source.`is`(ModDamageTypes.BEAST)) {
                damage = Float.POSITIVE_INFINITY.toDouble()
            }

            sourceEntity.displayClientMessage(
                Component.translatable(
                    "tips.superbwarfare.target.damage",
                    format2D(damage),
                    format1D(entity.position().distanceTo(sourceEntity.position()), "m")
                ), false
            )
        }
    }

    private fun reduceDamageByDistance(amount: Double, distance: Double, rate: Double, minDistance: Double): Double {
        return amount / (1 + rate * max(0.0, distance - minDistance))
    }

    /**
     * 根据造成的伤害，提供武器经验
     */
    private fun giveExpToWeapon(event: LivingIncomingDamageEvent) {
        val source = event.source ?: return
        val sourceEntity = source.entity as? LivingEntity ?: return
        val stack = sourceEntity.mainHandItem
        if (stack.item !is GunItem) return
        val entity = event.entity
        if (entity.type.`is`(ModTags.EntityTypes.NO_EXPERIENCE)) return

        val data = GunData.from(stack)
        val amount = (0.5f * event.amount).coerceAtMost(entity.maxHealth).toDouble()

        // 判断是不是枪械能造成的伤害
        if (!isGunDamage(source) && !source.`is`(DamageTypes.PLAYER_ATTACK)) return

        data.exp.add(amount)

        // 提升武器等级
        var level = data.level.get()
        var exp = data.exp.get()
        var upgradeExpNeeded = 20 * level.toDouble().pow(2.0) + 160 * level + 20

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded
            level = data.level.get() + 1
            upgradeExpNeeded = 20 * level.toDouble().pow(2.0) + 160 * level + 20
            data.exp.set(exp)
            data.level.set(level)
        }
        data.save()
    }

    private fun giveKillExpToWeapon(event: LivingDeathEvent) {
        val source = event.source ?: return
        val sourceEntity = source.entity as? LivingEntity ?: return
        val stack = sourceEntity.mainHandItem
        if (stack.item !is GunItem) return
        val entity = event.entity
        if (event.entity.type.`is`(ModTags.EntityTypes.NO_EXPERIENCE)) return

        val data = GunData.from(stack)
        val amount = 20 + 2 * entity.maxHealth.toDouble()

        // 判断是不是枪械能造成的伤害
        if (isGunDamage(source) || source.`is`(DamageTypes.PLAYER_ATTACK)) {
            data.exp.add(amount)
        }

        // 提升武器等级
        var level = data.level.get()
        var exp = data.exp.get()
        var upgradeExpNeeded = 20 * level.toDouble().pow(2.0) + 160 * level + 20

        while (exp >= upgradeExpNeeded) {
            exp -= upgradeExpNeeded
            level = data.level.get() + 1
            upgradeExpNeeded = 20 * level.toDouble().pow(2.0) + 160 * level + 20
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
        if (!GameplayConfig.GLOBAL_INDICATION.get() && !isModDamage(source)) {
            return
        }

        if (!sourceEntity.level().isClientSide() && sourceEntity is ServerPlayer) {
            if (postEvent(Indicator(sourceEntity, source, event.entity)).isCanceled()) {
                return
            }

            SoundTool.playLocalSound(sourceEntity, ModSounds.TARGET_DOWN.get(), 3f, 1f)
            sendPacketTo(sourceEntity, ClientIndicatorMessage(2, 8))
        }
    }

    private fun renderDamageIndicator(event: LivingIncomingDamageEvent?) {
        if (event == null) return

        val damagesource = event.source
        val sourceEntity = damagesource.entity ?: return

        if (sourceEntity is ServerPlayer && (damagesource.`is`(DamageTypes.EXPLOSION) || damagesource.`is`(DamageTypes.PLAYER_EXPLOSION)
                    || damagesource.`is`(ModDamageTypes.MINE) || damagesource.`is`(ModDamageTypes.PROJECTILE_EXPLOSION))
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
        if (entity is Player && event.slot == EquipmentSlot.MAINHAND) {
            if (entity.level().isClientSide) return

            val oldStack = event.from
            val newStack = event.to

            if (entity is ServerPlayer) {
                if (newStack.item is GunItem) {
                    checkCopyGuns(newStack, entity)
                }

                if (newStack.item !== oldStack.item || (newStack.item is GunItem && !GunData.from(newStack)
                        .initialized())
                    || (oldStack.item is GunItem && !GunData.from(oldStack).initialized())
                    || (newStack.item is GunItem && oldStack.item is GunItem && (GunsTool.getGunUUID(
                        NBTTool.getTag(
                            newStack
                        )
                    ) != GunsTool.getGunUUID(NBTTool.getTag(oldStack))))
                ) {
                    sendPacketTo(entity, DrawClientMessage)

                    val oldGun = oldStack.item
                    if (oldGun is GunItem) {
                        val oldData = GunData.from(oldStack)

                        stopGunReloadSound(entity, oldData)
                        stopGunChargeSound(entity, oldData)

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

                    if (newStack.item is GunItem) {
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
                            instance.forEach { perk ->
                                perk.perk.onChangeSlot(
                                    newData,
                                    perk,
                                    entity
                                )
                            }
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
        val uuid = data.uuid ?: return

        for (item in player.getInventory().items) {
            if (item == stack) continue
            if (item.item is GunItem) {
                val itemData = GunData.from(item)
                if (itemData.uuid == uuid) {
                    data.update { it.copy(uuid = UUID.randomUUID()) }
                    return
                }
            }
        }
    }

    @JvmStatic
    fun stopGunReloadSound(player: ServerPlayer, data: GunData) {
        val soundInfo: SoundInfo = data.get(GunProp.SOUND_INFO)
        soundInfo.cancellableSounds.list
            .forEach { str ->
                val location = ResourceLocation.tryParse(str)
                if (location != null) {
                    player.connection.send(ClientboundStopSoundPacket(location, SoundSource.PLAYERS))
                }
            }
    }

    fun stopGunChargeSound(player: ServerPlayer, data: GunData) {
        val resource = GunResource.compute(data.stack)
        val chargeSound = resource.chargeSound
        if (chargeSound != null) {
            player.sendPacket(ClientboundStopSoundPacket(chargeSound.location, SoundSource.PLAYERS))
        }

        val dischargeSound = resource.dischargeSound
        if (dischargeSound != null) {
            player.sendPacket(ClientboundStopSoundPacket(dischargeSound.location, SoundSource.PLAYERS))
        }
    }

    /**
     * 发送击杀消息
     */
    private fun handlePlayerKillEntity(event: LivingDeathEvent) {
        val entity = event.entity
        val source = event.source

        val damageTypeResourceKey = if (source.typeHolder().unwrapKey().isPresent) source.typeHolder().unwrapKey()
            .get() else DamageTypes.GENERIC

        var attacker: LivingEntity? = null
        val sourceEntity = source.entity
        val directEntity = source.directEntity

        if (sourceEntity is LivingEntity) {
            attacker = sourceEntity
        }

        if (directEntity is Projectile && directEntity.owner is LivingEntity) {
            val owner = directEntity.owner as LivingEntity
            if (owner is ServerPlayer) {
                attacker = owner
            } else if (owner is OwnableEntity && owner.owner is ServerPlayer) {
                attacker = owner
            }
        }

        if (attacker == null) return

        if (postEvent(SendKillMessage(attacker, source, entity)).isCanceled()) {
            return
        }

        if (MiscConfig.SEND_KILL_FEEDBACK.get()) {
            if (isHeadshotDamage(source)) {
                sendPacketToAll(
                    LivingGunKillMessage(
                        attacker.id,
                        entity.id,
                        true,
                        damageTypeResourceKey
                    )
                )
            } else {
                sendPacketToAll(
                    LivingGunKillMessage(
                        attacker.id,
                        entity.id,
                        false,
                        damageTypeResourceKey
                    )
                )
            }
        }
    }

    private fun handleGunPerksWhenHurt(event: LivingIncomingDamageEvent) {
        val source = event.source
        if (!isGunDamage(source) && !source.`is`(DamageTypes.PLAYER_ATTACK)) return

        var attacker: LivingEntity? = null
        val sourceEntity = source.entity
        val directEntity = source.directEntity

        if (sourceEntity is LivingEntity) {
            attacker = sourceEntity
        }

        if (directEntity is Projectile && directEntity.owner is LivingEntity) {
            val owner = directEntity.owner as LivingEntity
            if (owner is ServerPlayer) {
                attacker = owner
            } else if (owner is OwnableEntity && owner.owner is ServerPlayer) {
                attacker = owner
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
                if (isGunDamage(source)) {
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
        if (!isGunDamage(source)) return

        var attacker: LivingEntity? = null
        val sourceEntity = source.entity
        val directEntity = source.directEntity

        if (sourceEntity is LivingEntity) {
            attacker = sourceEntity
        }

        if (directEntity is Projectile && directEntity.owner is LivingEntity) {
            val owner = directEntity.owner as LivingEntity
            if (owner is ServerPlayer) {
                attacker = owner
            } else if (owner is OwnableEntity && owner.owner is ServerPlayer) {
                attacker = owner
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
    fun onPickup(event: ItemEntityPickupEvent.Pre) {
        if (!VehicleConfig.VEHICLE_ITEM_PICKUP.get()) return
        val entity = event.player
        val vehicle = entity.vehicle as? VehicleEntity ?: return
        val pickUp = event.itemEntity
        if (!vehicle.level().isClientSide) {
            val stack = pickUp.item.copy()
            val oldCount = stack.count

            val count = InventoryTool.insertItem(vehicle.inventory.getItems(), stack)

            pickUp.discard()

            if (oldCount > count && entity is Player) {
                val item = stack.copy()
                item.count = oldCount - count
                if (!entity.addItem(item)) {
                    entity.drop(item, false)
                }
            }
        }
        event.setCanPickup(TriState.FALSE)
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

        val cap = player.getData(ModDataAttachments.PLAYER_VARIABLE)

        val drop = Ammo.entries.sumOf { it.get(cap) } > 0
        if (!drop) return

        val stack = ItemStack(ModItems.AMMO_BOX.get())

        for (type in Ammo.entries) {
            type.set(stack, type.get(cap))
            type.set(cap, 0)
        }

        stack.ammoBoxData = stack.ammoBoxData.asDrop()

        player.setData(ModDataAttachments.PLAYER_VARIABLE, cap)

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
            InventoryTool.insertItem(vehicle.inventory.getItems(), stack)

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
            event.setCanceled(true)
        }
    }

    @SubscribeEvent
    fun onKnockback(event: LivingKnockBackEvent) {
        val knockback = ICustomKnockback.getInstance(event.entity)
        if (knockback.`superbWarfare$getKnockbackStrength`() >= 0) {
            event.setStrength(knockback.`superbWarfare$getKnockbackStrength`().toFloat())
        }
    }

    @SubscribeEvent
    fun onEntityFall(event: LivingFallEvent) {
        val living = event.entity
        if (living.vehicle is VehicleEntity) {
            event.setCanceled(true)
        }
    }

    @SubscribeEvent
    fun onPreSendKillMessage(event: SendKillMessage) {
        if (event.source.directEntity is AutoAimableEntity && event.target !is Player) {
            event.setCanceled(true)
        }
    }

    @SubscribeEvent
    fun onPreIndicator(event: Indicator) {
        if (event.source.directEntity is AutoAimableEntity && event.target !is Player) {
            event.setCanceled(true)
        }
    }

    @SubscribeEvent
    fun onEffectApply(event: Applicable) {
        val entity = event.entity
        val vehicle = entity.vehicle
        if (event.effectInstance?.effect?.value()?.category == MobEffectCategory.HARMFUL
            && vehicle is VehicleEntity
            && vehicle.isEnclosed(vehicle.getSeatIndex(entity))
        ) {
            event.result = Applicable.Result.DO_NOT_APPLY
        }
    }

    @SubscribeEvent
    fun onExplosionDetonate(event: ExplosionEvent.Detonate) {
        val explosion = event.explosion as? CustomExplosion ?: return

        val iterator = event.affectedEntities.iterator()
        while (iterator.hasNext()) {
            val entity = iterator.next() as? VehicleEntity ?: continue

            iterator.remove()
            val explosionPos = explosion.position
            val explosionRadius = explosion.radius * 2.0F
            if (!entity.ignoreExplosion(explosion)) {
                val distanceRatio = sqrt(entity.distanceToSqr(explosionPos)) / explosionRadius
                if (distanceRatio <= 1.0) {
                    val dx = entity.x - explosionPos.x
                    val dy = entity.eyeY - explosionPos.y
                    val dz = entity.z - explosionPos.z
                    val distance = sqrt(dx * dx + dy * dy + dz * dz)
                    if (distance != 0.0) {
                        val visibilityFactor = if (!entity.enableAABB())
                            CustomExplosion.getSeenPercentOptimized(entity.level(), explosionPos, entity)
                        else
                            Explosion.getSeenPercent(explosionPos, entity)
                        val impactStrength = (1.0 - distanceRatio) * visibilityFactor
                        val damage = (impactStrength * impactStrength + impactStrength) / 2.0 * explosion.damage

                        entity.hurt(explosion.damageSource, damage.toFloat())
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onExplosionKnockback(event: ExplosionKnockbackEvent) {
        if (event.affectedEntity is VehicleEntity) {
            event.knockbackVelocity = Vec3.ZERO
        }
    }
}
