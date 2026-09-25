package com.atsuishio.superbwarfare.melee

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.melee.ResolvedMeleeAction
import com.atsuishio.superbwarfare.data.gun.melee.ResolvedMeleeEffect
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/**
 * 一次近战效果触发的上下文。
 *
 * 所有行为都**只在服务端**执行（概率 `Chance` 用的是服务端权威的 `level.random`），
 * 所以这里直接给 [ServerLevel] / [Player]，行为里不必再判端。
 *
 * @param level 服务端世界
 * @param attacker 攻击者
 * @param data 结算用的枪械数据（主武器或副武器；冷却表写在它身上）
 * @param actionIndex 本次锁存的连招下标（调试与 Perk 上下文用）
 * @param action 本段动作（完全展开）
 * @param effect 本次要执行的效果（已展开预设与内联覆盖）
 * @param target 触发目标；`Swing` 触发时为 `null`
 * @param position 效果的作用点：命中点 ?: 目标位置 ?: 攻击者视线前方
 * @param damaged 这次攻击是否真的对这个目标造成了伤害（`Hit`/`Kill` 才有意义）
 * @param killed 这个目标是否被这一击打死（`Kill` 才有意义）
 */
class MeleeEffectContext(
    val level: ServerLevel,
    val attacker: Player,
    val data: GunData,
    val actionIndex: Int,
    val action: ResolvedMeleeAction,
    val effect: ResolvedMeleeEffect,
    val target: LivingEntity?,
    val position: Vec3,
    val damaged: Boolean = false,
    val killed: Boolean = false,
)

/**
 * 一种近战额外行为（`sbw` 数据里 `Type` 指向的那个东西）。
 *
 * 实现放在 `MeleeEffectBehaviors` 里，注册进 `ModMeleeEffects`。
 * **只在服务端调用**：`MeleeEffectDispatcher` 已经从报文链路保证这一点。
 */
interface MeleeEffectBehavior {

    /** 行为 id，小写、不带命名空间（`explosion` / `extra_damage` / …） */
    val id: String

    /** 执行一次。**不允许抛异常**：拿不到的参数就安静地跳过，别把整次挥击弄崩 */
    fun apply(context: MeleeEffectContext)

    /**
     * 数据校验：返回问题描述（`null` = 合法）。
     *
     * 只检查"写了但明显不对"的情况（缺必填字段、数值越界），
     * 由 `DataValidator` 在数据包 reload 后统一打日志。
     */
    fun validate(effect: ResolvedMeleeEffect): String? = null
}
