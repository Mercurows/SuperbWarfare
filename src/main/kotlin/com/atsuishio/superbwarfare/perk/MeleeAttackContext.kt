package com.atsuishio.superbwarfare.perk

import com.atsuishio.superbwarfare.data.gun.melee.ResolvedMeleeAction
import com.atsuishio.superbwarfare.perk.MeleeAttackContext.SOURCE_MAIN
import com.atsuishio.superbwarfare.perk.MeleeAttackContext.subWeapon
import net.minecraft.server.level.ServerPlayer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 一次近战的上下文：**本段动作 + 来源（主武器 / 副武器槽位）**。
 *
 * 设计上 `Perk.onMeleeSwing` / `onMeleeAttack` 原来的签名只有 `(data, instance, entity/target, source)`，
 * 拿不到"这是第几段动作、是主武器还是副武器打的"。这里用一个轻量的**同 tick 传递**补上：
 *
 * - 写入：`MeleeAttackMessage` 在服务端处理时，此时就是服务端 tick 内；
 * - 读取：紧接着的 `LivingHurtEvent` / `LivingDeathEvent`（伤害是在同一次调用栈里打出去的）；
 * - 清理：对应的 perk 钩子跑完就移除，避免残留。
 *
 * **刻意不做成长生命周期状态**：近战结算与伤害事件在同一个 tick 内因果相连，
 * 所以"存一个 tick 就对"；存久了反而会在漏掉清理时串到下一位玩家的下一次攻击上。
 */
object MeleeAttackContext {

    /** 主武器近战 */
    const val SOURCE_MAIN: String = "MAIN"

    /** 副武器的近战形态（三期），`SUB:<slot>` */
    fun subWeapon(slot: String): String = "SUB:$slot"

    /**
     * @param actionIndex 本段动作在 `MeleeActions` 里的下标
     * @param source      [SOURCE_MAIN] 或 [subWeapon]
     * @param action      解析后的本段动作（伤害/倍率/耐久等已是最终值）
     */
    data class Entry(
        val actionIndex: Int,
        val source: String,
        val action: ResolvedMeleeAction,
    )

    private val entries = ConcurrentHashMap<UUID, Entry>()

    fun put(player: ServerPlayer, entry: Entry) {
        entries[player.uuid] = entry
    }

    fun get(player: UUID): Entry? = entries[player]

    fun remove(player: UUID): Entry? = entries.remove(player)

    fun clear() = entries.clear()
}
