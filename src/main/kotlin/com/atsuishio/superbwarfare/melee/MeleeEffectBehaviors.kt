package com.atsuishio.superbwarfare.melee

import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.melee.ResolvedMeleeEffect
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage
import com.atsuishio.superbwarfare.tools.*
import net.minecraft.core.Holder
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.LivingEntity
import kotlin.jvm.optionals.getOrNull
import kotlin.math.cos
import kotlin.math.sin

/**
 * 近战额外效果的**首发行为集合**。
 *
 * 全部**只在服务端执行**，由 `MeleeEffectDispatcher` 在报文结算时调用；
 * 每个行为都从 [MeleeEffectContext.effect] 里取自己需要的字段，拿不到就安静跳过
 * （数据包写错不该让一次挥击崩掉，问题由 `DataValidator` 在 reload 时打日志）。
 *
 * 字段复用的约定（数据里只给了字段名，语义由行为解释）：
 *
 * | 字段 | 谁在用 |
 * |---|---|
 * | `Damage` | `explosion` 伤害、`extra_damage` 附加伤害、`heal` 治疗量 |
 * | `Radius` | `explosion` 半径、`screen_shake` 传送半径、`sound` 传播半径（×16 格）、`particle` 扩散 |
 * | `Duration` | `shock`/`potion` 效果时长、`screen_shake` 持续 tick |
 * | `Amplifier` | `shock`/`potion` 效果等级 |
 * | `Count` | `ammo_refund` 返还发数、`particle` 粒子数量 |
 * | `FireTime` | `ignite` 点燃 tick、`explosion` 引燃时间 |
 * | `Knockback`/`Lift` | `knockback` 水平强度 / 上挑 |
 * | `Sound`/`Particle` | `sound` / `particle` 的注册 id |
 * | `Extra` | `potion` 的药水效果注册 id |
 * | `Amplitude` | `screen_shake` 的震动幅度（三期新增的键） |
 */
object MeleeEffectBehaviors {

    /** 爆炸。位置取 [MeleeEffectContext.position]，**默认不破坏方块**。 */
    object Explosion : MeleeEffectBehavior {
        override val id = "explosion"

        /** 不写 `Radius` 时的爆炸半径 */
        const val DEFAULT_RADIUS = 3.0

        override fun apply(context: MeleeEffectContext) {
            val effect = context.effect
            val radius = (effect.radius ?: DEFAULT_RADIUS).coerceAtLeast(0.0)

            // Builder 的 directSource 就是"被排除在伤害之外的那个实体"：
            // CustomExplosion 用 `level.getEntities(entity, aabb)` 取目标，攻击者天然不在列表里，
            // 所以贴脸戳爆自己不会被自己的爆炸炸到（也不需要额外的自伤豁免）。
            CustomExplosion.Builder(context.attacker)
                .damage((effect.damage ?: 0.0).toFloat())
                .radius(radius.toFloat())
                .position(context.position)
                .destroyBlock(effect.destroyBlocks ?: false)
                .fireTime(effect.fireTime ?: 0)
                .explode()
        }
    }

    /** 直接追加一段枪械近战伤害（走 `forceHurt`，与穿甲段同一条路径）。 */
    object ExtraDamage : MeleeEffectBehavior {
        override val id = "extra_damage"

        override fun apply(context: MeleeEffectContext) {
            val target = context.target ?: return
            val damage = context.effect.damage ?: return
            if (damage <= 0) return

            val source = ModDamageTypes.causeGunMeleeDamage(
                context.level.registryAccess(), context.attacker, context.attacker,
            )
            target.forceHurt(source, damage.toFloat())
        }

        override fun validate(effect: ResolvedMeleeEffect): String? =
            if (effect.damage == null) "extra_damage needs 'Damage'" else null
    }

    /** 感电（本模组的 `shock` 效果）。 */
    object Shock : MeleeEffectBehavior {
        override val id = "shock"

        const val DEFAULT_DURATION = 60

        override fun apply(context: MeleeEffectContext) {
            val target = context.target ?: return
            val effect = context.effect
            target.forceApplyEffect(
                MobEffectInstance(
                    ModMobEffects.SHOCK,
                    (effect.duration ?: DEFAULT_DURATION).coerceAtLeast(1),
                    (effect.amplifier ?: 0).coerceAtLeast(0),
                ),
                context.attacker,
            )
        }
    }

    /** 任意药水效果；效果 id 写在 `Extra` 里（`minecraft:slowness` 之类）。 */
    object Potion : MeleeEffectBehavior {
        override val id = "potion"

        const val DEFAULT_DURATION = 100

        override fun apply(context: MeleeEffectContext) {
            val target = context.target ?: return
            val effect = context.effect
            val mobEffect = resolveMobEffect(effect.extra) ?: return

            target.forceApplyEffect(
                MobEffectInstance(
                    mobEffect,
                    (effect.duration ?: DEFAULT_DURATION).coerceAtLeast(1),
                    (effect.amplifier ?: 0).coerceAtLeast(0),
                ),
                context.attacker,
            )
        }

        override fun validate(effect: ResolvedMeleeEffect): String? = when {
            effect.extra == null -> "potion needs 'Extra' = a mob effect id (e.g. minecraft:slowness)"
            resolveMobEffect(effect.extra) == null -> "unknown mob effect '${effect.extra}'"
            else -> null
        }
    }

    /** 点燃。`FireTime` 是 **tick** 数。 */
    object Ignite : MeleeEffectBehavior {
        override val id = "ignite"

        const val DEFAULT_FIRE_TICKS = 60

        override fun apply(context: MeleeEffectContext) {
            val target = context.target ?: return
            val ticks = (context.effect.fireTime ?: DEFAULT_FIRE_TICKS).coerceAtLeast(0)
            if (ticks <= 0) return

            // 取"更长的那一个"：连续点燃不会把已经烧着的目标提前熄灭
            if (target.remainingFireTicks < ticks) {
                target.remainingFireTicks = ticks
            }
        }
    }

    /**
     * 击退 + 上挑。
     *
     * 仓库没有现成的"上挑"原语，所以水平方向用原版 `knockback`、
     * 垂直分量自己写。
     */
    object Knockback : MeleeEffectBehavior {
        override val id = "knockback"

        const val DEFAULT_STRENGTH = 0.8

        override fun apply(context: MeleeEffectContext) {
            val target = context.target ?: return
            val effect = context.effect
            val strength = (effect.knockback ?: DEFAULT_STRENGTH).coerceAtLeast(0.0)
            val lift = (effect.lift ?: 0.0).coerceAtLeast(0.0)
            if (strength <= 0 && lift <= 0) return

            val yaw = Math.toRadians(context.attacker.yRot.toDouble())
            if (strength > 0) {
                if (target is LivingEntity) {
                    // 与 MeleeAttackMessage 同一条约定：`knockback` 内部是"减去"方向比，
                    // 所以传 (sin, -cos) 才等于沿视线推出去
                    target.knockback(strength, sin(yaw), -cos(yaw))
                } else {
                    target.push(-sin(yaw) * strength, 0.0, cos(yaw) * strength)
                }
            }
            if (lift > 0) {
                target.push(0.0, lift, 0.0)
            }
        }
    }

    /** 落雷。`DestroyBlocks` 不为 `true` 时只打一道**纯视觉**闪电。 */
    object Lightning : MeleeEffectBehavior {
        override val id = "lightning"

        override fun apply(context: MeleeEffectContext) {
            val level = context.level
            val pos = context.position

            val bolt = LightningBolt(EntityType.LIGHTNING_BOLT, level)
            bolt.moveTo(pos.x, pos.y, pos.z)
            // 视觉模式 = 不伤害、不点燃；与爆炸的 DestroyBlocks 保持同一套"默认不搞破坏"的取向
            bolt.setVisualOnly(context.effect.destroyBlocks != true)
            (context.attacker as? ServerPlayer)?.let { bolt.setCause(it) }
            level.addFreshEntity(bolt)
        }
    }

    /** 治疗攻击者，数值取 `Damage`。 */
    object Heal : MeleeEffectBehavior {
        override val id = "heal"

        override fun apply(context: MeleeEffectContext) {
            val amount = context.effect.damage ?: return
            if (amount <= 0) return
            context.attacker.heal(amount.toFloat())
        }

        override fun validate(effect: ResolvedMeleeEffect): String? =
            if (effect.damage == null) "heal needs 'Damage'" else null
    }

    /**
     * 返还弹药（`Count` 发，默认 1）。
     *
     * 只补弹匣上限之内的部分：`Magazine <= 0`（背包型 / 近战枪）没有可补的弹匣，直接跳过。
     */
    object AmmoRefund : MeleeEffectBehavior {
        override val id = "ammo_refund"

        override fun apply(context: MeleeEffectContext) {
            val data = context.data
            val magazine = data.get(GunProp.MAGAZINE)
            if (magazine <= 0) return

            val count = (context.effect.count ?: 1).coerceAtLeast(0)
            if (count <= 0) return

            val current = data.ammo.get()
            if (current >= magazine) return

            data.ammo.set((current + count).coerceAtMost(magazine))
            data.invalidateProperties()
        }
    }

    /** 屏幕震动。`Radius` 传送半径、`Duration` 持续 tick、`Amplitude` 幅度。 */
    object ScreenShake : MeleeEffectBehavior {
        override val id = "screen_shake"

        const val DEFAULT_RADIUS = 8.0
        const val DEFAULT_TIME = 10
        const val DEFAULT_AMPLITUDE = 1.0

        override fun apply(context: MeleeEffectContext) {
            val effect = context.effect
            val radius = (effect.radius ?: DEFAULT_RADIUS).coerceAtLeast(0.0)
            val time = (effect.duration ?: DEFAULT_TIME).toDouble()
            val amplitude = (effect.amplitude ?: DEFAULT_AMPLITUDE).coerceAtLeast(0.0)
            if (radius <= 0 || time <= 0 || amplitude <= 0) return

            val pos = context.position
            ShakeClientMessage.sendToNearbyPlayers(context.level, pos.x, pos.y, pos.z, radius, time, amplitude)
        }
    }

    /**
     * 播放一个音效。
     *
     * `Radius` 沿用 [SoundTool.playDistantSound] 的口径 —— 它是**倍率**（×16 格），不是格数。
     */
    object Sound : MeleeEffectBehavior {
        override val id = "sound"

        const val DEFAULT_RADIUS = 4.0

        override fun apply(context: MeleeEffectContext) {
            val id = context.effect.sound ?: return
            val sound = resolveSound(id) ?: return
            val pos = context.position
            SoundTool.playDistantSound(
                context.level, sound, pos,
                (context.effect.radius ?: DEFAULT_RADIUS).toFloat(),
                1f, context.attacker,
            )
        }

        override fun validate(effect: ResolvedMeleeEffect): String? = when {
            effect.sound == null -> "sound needs 'Sound' = a sound event id"
            resolveSound(effect.sound) == null -> "unknown sound event '${effect.sound}'"
            else -> null
        }
    }

    /**
     * 生成粒子。
     *
     * 只接受**简单粒子**（`SimpleParticleType`，即 `minecraft:flame` 这类没有额外参数的粒子）；
     * 带 codec 的复杂粒子（`block` / `dust` …）需要额外参数，数据里写不出来，会被跳过。
     */
    object Particle : MeleeEffectBehavior {
        override val id = "particle"

        const val DEFAULT_COUNT = 8

        override fun apply(context: MeleeEffectContext) {
            val id = context.effect.particle ?: return
            val type = resolveParticle(id) ?: return
            val effect = context.effect
            val count = (effect.count ?: DEFAULT_COUNT).coerceAtLeast(1)
            val spread = (effect.radius ?: 0.5).coerceAtLeast(0.0)
            val pos = context.position

            ParticleTool.sendParticle(
                context.level, type,
                pos.x, pos.y, pos.z,
                count, spread, spread, spread, 0.0, true,
            )
        }

        override fun validate(effect: ResolvedMeleeEffect): String? = when {
            effect.particle == null -> "particle needs 'Particle' = a particle id"
            resolveParticle(effect.particle) == null -> "unknown or non-simple particle '${effect.particle}'"
            else -> null
        }
    }

    /** 全部首发行为（注册进 `ModMeleeEffects` 的顺序就是文档里那张表的顺序） */
    val ALL: List<MeleeEffectBehavior> = listOf(
        Explosion,
        ExtraDamage,
        Shock,
        Potion,
        Ignite,
        Knockback,
        Lightning,
        Heal,
        AmmoRefund,
        ScreenShake,
        Sound,
        Particle,
    )

    // ---------------------------------------------------------------- 解析工具

    private fun resolveMobEffect(id: String?): Holder.Reference<MobEffect>? {
        val location = id?.let { ResourceLocation.tryParse(it) } ?: return null
        return BuiltInRegistries.MOB_EFFECT.getHolder(location).getOrNull()
    }

    private fun resolveSound(id: String): SoundEvent? {
        val location = ResourceLocation.tryParse(id) ?: return null
        return BuiltInRegistries.SOUND_EVENT.get(location)
    }

    private fun resolveParticle(id: String): SimpleParticleType? {
        val location = ResourceLocation.tryParse(id) ?: return null
        return BuiltInRegistries.PARTICLE_TYPE.get(location) as? SimpleParticleType
    }
}
