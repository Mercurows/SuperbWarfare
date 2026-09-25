package com.atsuishio.superbwarfare.melee

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.melee.MeleeEffectTrigger
import com.atsuishio.superbwarfare.data.gun.melee.ResolvedMeleeAction
import com.atsuishio.superbwarfare.data.gun.subdata.Cooldown
import com.atsuishio.superbwarfare.init.ModMeleeEffects
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/**
 * 近战额外效果的**结算器**（服务端专用）。
 *
 * 用法是在一次近战报文里建一个实例，然后按实际发生的三件事调用：
 *
 * ```
 * val dispatcher = MeleeEffectDispatcher(level, attacker, data, index, action)
 * dispatcher.swing()                                     // 无论是否命中（Swing）
 * dispatcher.hit(target, position, damaged = true)        // 每个真的挨打的目标（Hit / FirstHit）
 * dispatcher.kill(target, position)                       // 目标被这一击打死（Kill）
 * ```
 *
 * 三条落地规则：
 * 1. **概率只在服务端 roll** —— 用的是 [ServerLevel.random]，客户端从不参与；
 * 2. **冷却写在枪械 NBT 上**（键 `effect:<key>`），**只有真的触发了才写**：
 *    冷却的语义是"发动过之后这段时间不再发动"，而不是"每隔 N tick 掷一次骰子"；
 * 3. `FirstHit` 每次挥击**只考虑一次**（第一个通过命中判定的目标），
 *    用 [firedOnce] 记账；`Hit`/`Kill` 则对每个目标各考虑一次。
 */
class MeleeEffectDispatcher(
    private val level: ServerLevel,
    private val attacker: Player,
    private val data: GunData,
    private val actionIndex: Int,
    private val action: ResolvedMeleeAction,
) {

    /** `FirstHit` 已经用掉的效果键 */
    private val firedOnce = HashSet<String>()

    /** `Swing`：挥击瞬间，无论是否命中。 */
    fun swing() = dispatch(MeleeEffectTrigger.SWING)

    /**
     * `Hit` / `FirstHit`：每个通过判定并真的挨打的目标各一次。
     *
     * `FirstHit` 先派发 —— 它只在**第一个**挨打的目标身上被考虑，
     * 所以必须在这次调用里就消耗掉，否则第二个目标会把它再排一次队。
     *
     * @param position 效果作用点（命中点优先；没有就退化成目标位置）
     * @param damaged 这一击是否真的造成了伤害
     */
    fun hit(target: LivingEntity, position: Vec3?, damaged: Boolean = true) {
        dispatch(MeleeEffectTrigger.FIRST_HIT, target, position, damaged = damaged)
        dispatch(MeleeEffectTrigger.HIT, target, position, damaged = damaged)
    }

    /** `Kill`：目标被这一击打死。 */
    fun kill(target: LivingEntity, position: Vec3? = null) =
        dispatch(MeleeEffectTrigger.KILL, target, position, damaged = true, killed = true)

    /**
     * 逐个检查 [ResolvedMeleeAction.effects]，命中触发时机的就执行。
     *
     * 未知行为 id 只打一条 warning 并跳过 —— 数据包写错不该让整次挥击失效。
     */
    fun dispatch(
        trigger: MeleeEffectTrigger,
        target: LivingEntity? = null,
        position: Vec3? = null,
        damaged: Boolean = false,
        killed: Boolean = false,
    ) {
        val effects = action.effects
        if (effects.isEmpty()) return

        val point = position ?: target?.position() ?: fallbackPosition()

        for (effect in effects) {
            if (effect.trigger != trigger) continue

            // FirstHit：一次挥击只考虑一次（Hit 段与 FirstHit 段是两套独立的记账）
            if (trigger == MeleeEffectTrigger.FIRST_HIT && !firedOnce.add(effect.key)) continue

            val cooldownKey = Cooldown.effectKey(effect.key)
            if (effect.cooldown > 0 && data.cooldown.isCoolingDown(cooldownKey)) {
                debug { "effect '${effect.key}' skipped: cooling down (${data.cooldown.get(cooldownKey)} ticks left)" }
                continue
            }

            if (effect.chance < 1.0 && level.random.nextDouble() >= effect.chance) {
                debug { "effect '${effect.key}' rolled out (chance=${effect.chance})" }
                continue
            }

            val behavior = ModMeleeEffects.get(effect.type)
            if (behavior == null) {
                Mod.LOGGER.warn(
                    "[MeleeEffect] unknown effect type '{}' (registered: {}), skipping",
                    effect.type, ModMeleeEffects.ids().joinToString(", "),
                )
                continue
            }

            val context = MeleeEffectContext(
                level = level,
                attacker = attacker,
                data = data,
                actionIndex = actionIndex,
                action = action,
                effect = effect,
                target = target,
                position = point,
                damaged = damaged,
                killed = killed,
            )

            try {
                behavior.apply(context)
            } catch (exception: Exception) {
                Mod.LOGGER.error("[MeleeEffect] '{}' threw while applying", effect.type, exception)
                continue
            }

            // 只有真的触发了才上冷却
            if (effect.cooldown > 0) {
                data.cooldown.set(cooldownKey, effect.cooldown)
            }
            debug {
                "effect '${effect.key}' -> ${effect.type} trigger=$trigger" +
                        (if (target != null) " target=${target.type}" else "")
            }
        }
    }

    /** 没有目标也没有命中点时的作用点：攻击者视线前方 1.5 格（枪口大致的位置）。 */
    private fun fallbackPosition(): Vec3 = attacker.eyePosition.add(attacker.lookAngle.scale(1.5))

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[MeleeEffect] {}", message())
        }
    }
}
