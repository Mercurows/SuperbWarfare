package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.subdata.Cooldown.Companion.COOLDOWN
import net.minecraft.nbt.CompoundTag

/**
 * 枪械自带的自定义冷却表。
 *
 * **为什么不用原版物品冷却**：`ItemCooldowns.addCooldown(item, ticks)` 是**物品级**冷却，
 * 会连带锁住整把枪的原版使用（挥击/交互），而且同型号的枪互相污染。
 * **也不用全局 `Map<UUID, Int>`**：那会让"换一把同型号的枪"绕过冷却，还要处理登出清理与重启丢失。
 *
 * 存储位置：`gunDataTag` 下的子 compound [COOLDOWN]，结构是 `冷却键 → 剩余 tick`；
 * 写在枪械 NBT 上，因此**随主武器持久化**，无需新存档字段。
 *
 * 递减由**服务端**在 gun tick 内完成（[com.atsuishio.superbwarfare.event.GunEventHandler.gunTickInternal]），
 * 归零即删键；判定在结算前读一次，`> 0` 则跳过。
 *
 * 键命名约定（见 [Companion]）：
 * - `melee:<actionIndex>`：某一段近战动作的冷却（§3.5 `MeleeAction.Cooldown`）；
 * - `effect:<id>`：某个近战额外效果的冷却（§3.8 `MeleeEffectSpec.Cooldown`）；
 * - `sub:<slot>`：某个副武器槽位的触发冷却（§9.2，三期使用）。
 *
 * @param gunDataTag 枪械自身的 `GunData` 子 tag（[com.atsuishio.superbwarfare.data.gun.GunData.gunDataTag]）
 */
class Cooldown(private val gunDataTag: CompoundTag) {

    private val table: CompoundTag
        get() = gunDataTag.getCompound(COOLDOWN)

    /** 该键剩余的冷却 tick；没有该键时为 0（= 不在冷却中） */
    fun get(key: String): Int = table.getInt(key)

    /** 是否仍在冷却中 */
    fun isCoolingDown(key: String): Boolean = get(key) > 0

    /** 写入冷却（取值与 0 取大；写入 0 等同清除，因为 [reduce] 会把归零的键删掉） */
    fun set(key: String, ticks: Int) {
        if (ticks <= 0) {
            clear(key)
            return
        }
        table.putInt(key, ticks)
    }

    fun clear(key: String) {
        if (!gunDataTag.contains(COOLDOWN)) return
        val tag = table
        tag.remove(key)
        if (tag.isEmpty) gunDataTag.remove(COOLDOWN)
    }

    fun clearAll() {
        gunDataTag.remove(COOLDOWN)
    }

    /** 全部 `冷却键 → 剩余 tick`（调试 / 工具用） */
    fun entries(): Map<String, Int> {
        if (!gunDataTag.contains(COOLDOWN)) return emptyMap()
        val tag = table
        return tag.allKeys.associateWith { tag.getInt(it) }.filterValues { it > 0 }
    }

    /**
     * 服务端逐 tick 递减整张表，归零删键。
     *
     * @param ticks 本次递减多少（固定 1；留着是为了将来能接「冷却速率」类加成）
     */
    fun tick(ticks: Int = 1) {
        if (!gunDataTag.contains(COOLDOWN)) return
        val tag = table
        if (tag.isEmpty) {
            gunDataTag.remove(COOLDOWN)
            return
        }

        for (key in tag.allKeys.toList()) {
            val next = tag.getInt(key) - ticks
            if (next <= 0) tag.remove(key) else tag.putInt(key, next)
        }

        if (tag.isEmpty) gunDataTag.remove(COOLDOWN)
    }

    companion object {
        /** 冷却表的子 tag 名 */
        const val COOLDOWN: String = "MeleeCooldown"

        /** 近战动作冷却键 */
        fun meleeKey(actionIndex: Int) = "melee:$actionIndex"

        /** 近战额外效果冷却键 */
        fun effectKey(effectId: String) = "effect:$effectId"

        /** 副武器槽位冷却键 */
        fun subWeaponKey(slot: String) = "sub:$slot"
    }
}
