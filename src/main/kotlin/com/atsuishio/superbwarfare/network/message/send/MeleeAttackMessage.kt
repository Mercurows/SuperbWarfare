package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.MeleeSound
import com.atsuishio.superbwarfare.data.gun.melee.MeleeEffectDispatcher
import com.atsuishio.superbwarfare.data.gun.melee.ResolvedMeleeAction
import com.atsuishio.superbwarfare.data.gun.subdata.Cooldown
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.perk.MeleeAttackContext
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.damage.InvisibleBladeShapeness
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.MeleeQuery
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketTo
import kotlinx.serialization.Serializable
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.ForgeHooks
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 近战命中报文
 */
@RegisterPacket
@Serializable
data class MeleeAttackMessage(
    /** 只认 `MAIN`（主武器近战）。未知来源一律拒掉 */
    val source: String,
    /** 客户端锁存的连招下标；服务端只做越界校验 */
    val actionIndex: Int,
    val targets: List<TargetPayload>,
    /** 这一击是不是刺刀冲锋撞人那一下（伤害基数改走 `BayonetCharge.StrikeDamage`） */
    val chargeStrike: Boolean = false,
) : ServerPacketPayload() {

    @Serializable
    data class TargetPayload(
        val uuid: SerializedUUID,
        /**
         * **命中区域判定点**：客户端算的"准星射线到该目标 AABB 的最近点"
         */
        val hitX: Double,
        val hitY: Double,
        val hitZ: Double,
        val distance: Double = 0.0,
        /**
         * 是否是**准星正对**的那一个目标
         */
        val aimed: Boolean = false,
    )

    override fun PayloadContext.handler() {
        val player = sender()

        if (player.isSpectator) return
        if (!player.isAlive) return

        val stack = player.mainHandItem
        val item = stack.item
        if (item !is GunItem || !GunItem.isHeldWeapon(stack)) return
        if (item.isBroken(stack)) return

        if (source.isNotEmpty() && source != SOURCE_MAIN) return

        val data = GunData.from(stack)
        if (chargeStrike && !data.get(GunProp.BAYONET_CHARGE).canCharge(data)) return
        if (data.reloading() || data.bolt.actionTimer.get() > 0) return

        val operated = ActiveGun.dataOf(data, false)
        if (operated !== data && (operated.reloading() || operated.bolt.actionTimer.get() > 0)) return

        val actions = data.meleeActions()
        val index = ((actionIndex % actions.size) + actions.size) % actions.size

        val bladeCharge = if (!chargeStrike && actions[index].isHeavy()) {
            InvisibleBladeShapeness.charge(data)
        } else {
            0.0
        }
        val action = data.resolveMeleeAction(index, bladeCharge)

        if (action.cooldown > 0 && data.cooldown.isCoolingDown(Cooldown.meleeKey(index))) return

        val meleeSound = data.get(GunProp.MELEE_SOUND)

        val context = MeleeAttackContext.Entry(
            actionIndex = index,
            source = source.ifEmpty { MeleeAttackContext.SOURCE_MAIN },
            action = action,
        )
        MeleeAttackContext.put(player, context)

        for (type in Perk.Type.entries) {
            val instances = data.perk.getInstances(type)
            instances.forEach {
                it.perk.onMeleeSwing(data, it, player, context)
                it.perk.onMeleeSwing(data, it, player)
            }
        }

        // 近战额外效果
        val level = player.level() as? ServerLevel
        val dispatcher = level?.let { MeleeEffectDispatcher(it, player, data, index, action) }
        dispatcher?.swing()

        var hitLanded = false
        try {
            if (targets.isNotEmpty() && dispatcher != null) {
                hitLanded = attack(
                    player,
                    data,
                    action.resolvedHeadshot(data.get(GunProp.MELEE_HEADSHOT)),
                    action.resolvedLegshot(data.get(GunProp.MELEE_LEGSHOT)),
                    action,
                    meleeSound,
                    targets,
                    dispatcher,
                    chargeStrike,
                )
            }
        } finally {
            // 伤害是在上面的调用栈里打出去的，事件已经跑完，上下文不该留到下一 tick
            MeleeAttackContext.remove(player.uuid)
        }

        // 命中（含"只吃到效果、没真的掉血"的目标，比如免伤的载具）后消耗；创造模式不消耗
        if (hitLanded && data.get(GunProp.CONSUME_ON_MELEE_HIT) && !player.isCreative) {
            stack.shrink(1)
        }

        if (hitLanded && action.durability > 0 && data.get(GunProp.MAX_DURABILITY) > 0) {
            // 本段每次命中消耗的耐久（`MeleeAction.Durability`，不写 = 1）
            stack.hurtAndBreak(action.durability, player) { p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND) }
        }
    }

    private fun attack(
        attacker: Player,
        data: GunData,
        headshotMultiplier: Double,
        legshotMultiplier: Double,
        action: ResolvedMeleeAction,
        meleeSound: MeleeSound?,
        targets: List<TargetPayload>,
        effects: MeleeEffectDispatcher,
        chargeStrike: Boolean,
    ): Boolean {
        val level = attacker.level()
        // 刺刀冲锋那一击的伤害基数由 `BayonetCharge.StrikeDamage` 给（绝对值），不写就还是那一段的伤害
        val chargeDamage = data.get(GunProp.BAYONET_CHARGE).strikeDamage
        val strength = attacker.getEffect(MobEffects.DAMAGE_BOOST)
        val strengthBonus = if (strength != null) 3.0 * (strength.amplifier + 1) else 0.0
        val baseDamage =
            (if (chargeStrike && chargeDamage > 0) chargeDamage else action.damage) + strengthBonus
        var hurtCount = 0
        var hitCount = 0
        var landedHit = false
        var headshotHit = false
        var vehicleHit = false
        var playedHitSound = false
        var playedNoDamageSound = false

        // 隐之刃凛：轻击打折并把折掉的部分存进蓄力槽；重击的蓄力已经在 handler 里
        // 垫进了 `action.damage`（面板伤害层），这里只负责"命中后清空"
        val blade = InvisibleBladeShapeness.instanceOf(data)
        val bladeRate = blade?.let { InvisibleBladeShapeness.rate(it.level.toInt()) } ?: 0.0
        var storedCharge = if (blade != null) InvisibleBladeShapeness.charge(data) else 0.0
        var chargeSpent = false

        for ((order, payload) in targets.withIndex()) {
            val target = EntityFindUtil.findEntity(level, payload.uuid.toString()) ?: continue
            if (!ForgeHooks.onPlayerAttackTarget(attacker, target)) continue
            if (!target.isAttackable) continue
            if (target.skipAttackInteraction(attacker)) continue

            val wasAlive = (target as? LivingEntity)?.isAlive == true

            val zonePos = Vec3(payload.hitX, payload.hitY, payload.hitZ)
            // 效果落在判定的那个点上（不是目标中心）；命中点是客户端给的，越出碰撞箱就贴回箱面
            val effectPos = MeleeQuery.closestPointInBox(target.boundingBox.inflate(0.5), zonePos)

            // 命中区域 → 伤害倍率
            val headshot = payload.aimed && MeleeQuery.isHeadshot(target, zonePos)
            val legshot = !headshot && MeleeQuery.isLegshot(target, zonePos)
            val zoneMultiplier = when {
                headshot -> headshotMultiplier
                legshot -> legshotMultiplier
                else -> 1.0
            }


            val falloff = if (order == 0) 1.0 else (1.0 - order * action.falloff).coerceAtLeast(0.1)
            var damage = baseDamage * zoneMultiplier * falloff
            if (blade != null && !action.heavy) damage *= bladeRate
            if (damage <= 0) continue

            val damageSource = ModDamageTypes.causeDamageType(
                level.registryAccess(),
                data.get(if (headshot) GunProp.MELEE_HEADSHOT_DAMAGE_TYPE else GunProp.MELEE_DAMAGE_TYPE),
                attacker,
                attacker,
            ) ?: if (headshot) {
                ModDamageTypes.causeGunMeleeHeadshotDamage(level.registryAccess(), attacker, attacker)
            } else {
                ModDamageTypes.causeGunMeleeDamage(level.registryAccess(), attacker, attacker)
            }

            // 原版无敌帧会吞掉紧跟其后的多段出伤或下一次挥击，结算前先清掉
            if (target is LivingEntity) {
                target.invulnerableTime = 0
            }

            val currentHealth = (target as? LivingEntity)?.health ?: 0.0F
            val canHurt = hurtWithBypass(target, damageSource, damage, action.bypassesArmor)

            if (!canHurt) {
                if (!playedNoDamageSound) {
                    level.playSound(
                        null, attacker.x, attacker.y, attacker.z,
                        SoundEvents.PLAYER_ATTACK_NODAMAGE, attacker.soundSource, 1.0f, 1.0f
                    )
                    playedNoDamageSound = true
                }

                // 载具这类非生物目标的 hurt 恒返回 false，伤害被拒不等于没戳到，效果照常触发
                effects.hit(target, effectPos, damaged = false)
                hitCount++
                landedHit = true
                if (target is VehicleEntity) vehicleHit = true
                continue
            }

            hurtCount++
            landedHit = true
            if (headshot) headshotHit = true

            if (blade != null) {
                if (action.heavy) {
                    if (!chargeStrike && !chargeSpent && storedCharge > 0.0) chargeSpent = true
                } else {
                    InvisibleBladeShapeness.addCharge(data, damage * bladeRate)
                }
            }

            val attackerMotion = attacker.deltaMovement

            var knockback = action.knockback + attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK)
            if (attacker.isSprinting) knockback += 1.0
            if (knockback > 0) {
                level.playSound(
                    null, attacker.x, attacker.y, attacker.z,
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, attacker.soundSource, 1.0f, 1.0f
                )

                if (target is LivingEntity) {
                    target.knockback(
                        knockback * 0.5,
                        sin(attacker.yRot * Math.PI / 180.0),
                        -cos(attacker.yRot * Math.PI / 180.0),
                    )
                } else {
                    target.push(
                        -sin(attacker.yRot * Math.PI / 180.0) * knockback / 2.0,
                        0.1,
                        cos(attacker.yRot * Math.PI / 180.0) * knockback / 2.0,
                    )
                }

                attacker.deltaMovement = attackerMotion.multiply(0.6, 1.0, 0.6)

                // 不能在这里 `attacker.isSprinting = false`
            }

            if (target is ServerPlayer && target.hurtMarked) {
                sendPacketTo(target, ClientboundSetEntityMotionPacket(target))
                target.hurtMarked = false
            }

            // 命中音效：每次挥击只播一次，位置在**第一个真正受伤的目标**身上
            if (!playedHitSound) {
                level.playSound(
                    null,
                    target,
                    action.hit ?: meleeSound?.hit ?: ModSounds.MELEE_HIT.get(),
                    SoundSource.PLAYERS,
                    1f,
                    ((2 * Random.nextDouble() - 1) * 0.1f + 1.0f).toFloat(),
                )
                attacker.crit(target)
                playedHitSound = true
            }

            attacker.setLastHurtMob(target)
            if (target is LivingEntity) {
                EnchantmentHelper.doPostHurtEffects(target, attacker)
            }
            EnchantmentHelper.doPostDamageEffects(attacker, target)

            if (target is LivingEntity) {
                attacker.awardStat(Stats.DAMAGE_DEALT, ((currentHealth - target.health) * 10.0F).roundToInt())
            }

            // 近战额外效果。放在伤害之后：效果自己打出伤害（`extra_damage`/`explosion`）时，
            // 上面的原版收尾已经跑完，不会被后续的 `setLastHurtMob`/`awardStat` 覆盖掉
            effects.hit(target, effectPos)
            hitCount++
            if (target is LivingEntity && wasAlive && target.isDeadOrDying) {
                effects.kill(target, effectPos)
            }
        }

        if (chargeSpent) InvisibleBladeShapeness.setCharge(data, 0.0)

        if (hurtCount > 0) {
            attacker.sweepAttack()
        }

        // 命中/爆头提示：一次挥击只发一次，爆头优先，其次载具
        if (attacker is ServerPlayer) {
            if (headshotHit) {
                sendPacketTo(attacker, ClientIndicatorMessage(1, 5))
            } else if (vehicleHit) {
                sendPacketTo(attacker, ClientIndicatorMessage(3, 5))
            } else if (landedHit) {
                sendPacketTo(attacker, ClientIndicatorMessage(0, 5))
            }
        }

        return hitCount > 0
    }

    /**
     * 按 `BypassesArmor` 拆成「护甲部分 + 穿甲部分」两段伤害
     */
    private fun hurtWithBypass(
        target: Entity,
        source: DamageSource,
        damage: Double,
        bypassesArmor: Double,
    ): Boolean {
        val bypassRate = bypassesArmor.coerceIn(0.0, 1.0)
        val byPassed = damage * bypassRate
        val armored = damage - byPassed

        var hurt = false
        if (armored > 0) {
            hurt = target.hurt(source, armored.toFloat())
        }
        if (byPassed > 0) {
            // 护甲部分刚刚写入了无敌帧，不清掉的话穿甲部分会被当成重复伤害吞掉
            if (target is LivingEntity) {
                target.invulnerableTime = 0
            }
            hurt = target.forceHurt(source, byPassed.toFloat()) || hurt
        }
        return hurt
    }

    companion object {
        const val SOURCE_MAIN: String = "MAIN"
    }
}
