package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.MeleeSound
import com.atsuishio.superbwarfare.data.gun.melee.ResolvedMeleeAction
import com.atsuishio.superbwarfare.data.gun.subdata.Cooldown
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.perk.MeleeAttackContext
import com.atsuishio.superbwarfare.perk.Perk
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
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.CommonHooks
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 近战命中报文（客户端 → 服务端）。
 *
 * **不做距离/角度/视线复核**（与现状相同的信任模型）：几何判定在客户端算，
 * 服务端只做**动作级健壮性校验**（越界下标、主手是不是枪、自己是不是在换弹/拉栓），
 * 不是反作弊。
 *
 * 相对旧版（只有 `uuidList`）的三处关键变化：
 * 1. 带上 `actionIndex`——伤害/倍率/冷却全部按**数据字段**算，不再靠「客户端列表下标」（缺陷 8）；
 * 2. 带上每个目标的 `hitPos`——打头/打腿由服务端用同一套阈值判定，客户端与服务端不再各算一遍；
 * 3. `source` 预留 `SUB:<slot>`——三期的副武器近战形态复用同一条链路（§6.1）。
 */
@RegisterPacket
@Serializable
data class MeleeAttackMessage(
    /** `MAIN`（主武器近战）或 `SUB:<slot>`（副武器的近战形态，三期） */
    val source: String,
    /** 客户端锁存的连招下标；服务端只做越界校验 */
    val actionIndex: Int,
    val targets: List<TargetPayload>,
) : ServerPacketPayload() {

    @Serializable
    data class TargetPayload(
        val uuid: SerializedUUID,
        val hitX: Double,
        val hitY: Double,
        val hitZ: Double,
        val distance: Double = 0.0,
    )

    override fun PayloadContext.handler() {
        val player = sender()
        if (player.isSpectator) return

        val stack = player.mainHandItem
        val item = stack.item
        // 缺陷 7：主手不是枪时不该继续往下走（旧实现在这里只判了 `isNotEmpty`）
        if (item !is GunItem || !GunItem.isHeldWeapon(stack)) return

        val data = GunData.from(stack)

        // 服务端一层廉价检查：自己这边正在换弹/拉栓就拒掉（健壮性，不是反作弊）
        if (data.reloading() || data.bolt.actionTimer.get() > 0) return

        // 越界健壮性：下标按当前动作表大小取模，而不是直接信任客户端
        val actions = data.meleeActions()
        val index = ((actionIndex % actions.size) + actions.size) % actions.size
        val action = data.resolveMeleeAction(index)

        // 本段冷却没走完就拒掉（客户端也会拦，这里是双保险）
        if (action.cooldown > 0 && data.cooldown.isCoolingDown(Cooldown.meleeKey(index))) return

        val meleeSound = data.get(GunProp.MELEE_SOUND)

        // 本段动作 + 来源交给 perk 钩子（同 tick 传递，见 `MeleeAttackContext`）
        val context = MeleeAttackContext.Entry(
            actionIndex = index,
            source = source.ifEmpty { MeleeAttackContext.SOURCE_MAIN },
            action = action,
        )
        MeleeAttackContext.put(player, context)

        // 缺陷 3：`player.swing` 只在客户端调一次就够了；服务端这里不再调，
        // 否则一次近战会触发两次 `onEntitySwing`（`swing` 双端各一次）。
        for (type in Perk.Type.entries) {
            val instances = data.perk.getInstances(type)
            instances.forEach {
                it.perk.onMeleeSwing(data, it, player, context)
                it.perk.onMeleeSwing(data, it, player)
            }
        }

        try {
            if (targets.isNotEmpty()) {
                attack(player, action.resolvedHeadshot(data.get(GunProp.HEADSHOT)), action, meleeSound, targets)
            }
        } finally {
            // 伤害是在上面的调用栈里打出去的，事件已经跑完，上下文不该留到下一 tick
            MeleeAttackContext.remove(player.uuid)
        }

        if (action.durability > 0 && data.get(GunProp.MAX_DURABILITY) > 0) {
            // 本段自带的耐久消耗（`MeleeAction.Durability`）
            stack.hurtAndBreak(action.durability, player, EquipmentSlot.MAINHAND)
        }
    }

    /**
     * 结算。
     *
     * 全部数值读自 [action]（本段数据），不再从「客户端列表下标」推衰减（缺陷 8）。
     *
     * @param headshotMultiplier 打头倍率（已按「本段覆盖 ?: 枪的 `Headshot`」解析完）
     */
    private fun attack(
        attacker: Player,
        headshotMultiplier: Double,
        action: ResolvedMeleeAction,
        meleeSound: MeleeSound?,
        targets: List<TargetPayload>,
    ) {
        val level = attacker.level()
        var hurtCount = 0
        var playedHitSound = false
        var playedNoDamageSound = false

        for ((order, payload) in targets.withIndex()) {
            val target = EntityFindUtil.findEntity(level, payload.uuid.toString()) ?: continue
            if (!CommonHooks.onPlayerAttackTarget(attacker, target)) continue
            if (!target.isAttackable) continue
            if (target.skipAttackInteraction(attacker)) continue

            val hitPos = Vec3(payload.hitX, payload.hitY, payload.hitZ)

            // 命中区域 → 伤害倍率（复用投射物已验证的阈值；服务端算，客户端不再算一遍）
            val headshot = MeleeQuery.isHeadshot(target, hitPos)
            val legshot = !headshot && MeleeQuery.isLegshot(target, hitPos)
            val zoneMultiplier = when {
                headshot -> headshotMultiplier
                legshot -> action.resolvedLegshot()
                else -> 1.0
            }

            // 衰减：第 order 个目标乘 max(1 - order * Falloff, 0.1)
            val falloff = if (order == 0) 1.0 else (1.0 - order * action.falloff).coerceAtLeast(0.1)

            // 伤害数值**直接读** `MeleeDamage`（§6.2-7）；`ATTACK_DAMAGE` 属性加成保留在
            // `GunItem.getAttributeModifiers` 里，但不再参与这里的结算——否则 `MeleeDamage`
            // 会被"属性加成"二次放大，与 `MeleeAction.Damage` 的语义打架。
            val damage = action.damage * zoneMultiplier * falloff
            if (damage <= 0) continue

            val damageSource = if (headshot) {
                ModDamageTypes.causeGunMeleeHeadshotDamage(level.registryAccess(), attacker, attacker)
            } else {
                ModDamageTypes.causeGunMeleeDamage(level.registryAccess(), attacker, attacker)
            }

            val currentHealth = (target as? LivingEntity)?.health ?: 0.0F
            val canHurt = hurtWithBypass(target, damageSource, damage, action.bypassesArmor)

            if (!canHurt) {
                // 缺陷 5：未命中的"无伤害"反馈只该响一次，不该对每个目标都响
                if (!playedNoDamageSound) {
                    level.playSound(
                        null, attacker.x, attacker.y, attacker.z,
                        SoundEvents.PLAYER_ATTACK_NODAMAGE, attacker.soundSource, 1.0f, 1.0f
                    )
                    playedNoDamageSound = true
                }
                continue
            }

            hurtCount++

            // 缺陷 6：旧实现把**受害者的**速度设成了攻击者的动量。
            //   正确写法是保留攻击者自己的动量，只在需要时把受击者的新速度同步给客户端。
            val attackerMotion = attacker.deltaMovement

            // 缺陷 5：击退音效只在该目标真的被击退时播一次
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
                attacker.isSprinting = false
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
            val level = attacker.level()
            if (level is ServerLevel) {
                val source = attacker.damageSources().playerAttack(attacker)
                if (target is LivingEntity) {
                    EnchantmentHelper.doPostAttackEffects(level, target, source)
                }
                // TODO 该source是否正确
                EnchantmentHelper.doPostAttackEffects(level, attacker, source)
            }

            if (target is LivingEntity) {
                attacker.awardStat(Stats.DAMAGE_DEALT, ((currentHealth - target.health) * 10.0F).roundToInt())
            }
        }

        // 缺陷 4：`sweepAttack()` 曾在 `forEachIndexed` 循环体内，命中 N 个目标就挥 N 次。
        //   挪到循环外，一次挥击只挥一次。
        if (hurtCount > 0) {
            attacker.sweepAttack()
        }
    }

    /**
     * 按 `BypassesArmor` 拆成「护甲部分 + 穿甲部分」两段伤害。
     *
     * 两段都走 `LivingEntity` 原生的受伤流程（无敌帧 / 护甲 / 减伤 / 死亡处理都还在，
     * 也保留了原版语义），只是穿甲段额外叠一层 [forceHurt] 兜底：
     * 被护甲/无敌帧挡掉时仍然生效，这正是"穿甲"该有的样子。
     *
     * 之所以不用一个 `DamageSource` 打两遍：`hurt()` 自带 10 tick 无敌帧，
     * 第二段会被 `damage <= lastHurt` 判掉，两段就只剩第一段生效了。
     *
     * @return 是否至少有一段真的造成了伤害
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
            hurt = target.forceHurt(source, byPassed.toFloat()) || hurt
        }
        return hurt
    }

    companion object {
        /** 主武器近战 */
        const val SOURCE_MAIN: String = "MAIN"
    }
}