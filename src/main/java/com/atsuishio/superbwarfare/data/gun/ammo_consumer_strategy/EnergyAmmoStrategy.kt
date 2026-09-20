package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.AmmoSource
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.EnergyAmmoStrategy.consume
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.EnergyAmmoStrategy.count
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.EnergyAmmoStrategy.withdraw
import net.minecraft.world.entity.Entity
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import kotlin.math.min

/**
 * 能量弹药策略 —— ammo 字符串形如 `"fe"`、`"rf"`、`"energy"`。
 *
 * 同一个 `"FE"` 弹种按 `Magazine` 分成两种截然不同的形态，两者共用本策略，
 * 具体走哪条分支由 `Magazine > 0` 决定：
 *
 * ### 背包型（`Magazine <= 0`，如 `ql_1031`、`repair_tool`）
 * 没有弹匣，能量**在开火时**直接按 `AmmoCostPerShoot` 扣除，不参与换弹，也不能退弹。
 * 此时「1 弹药单位 == 1 FE」，[AmmoSource.loadAmount] 不参与换算。
 *
 * ### 弹匣型（`Magazine > 0`，如改造后的 `devotion`）
 * 弹匣里存的是**发数**，能量只作为备弹：
 * - 开火只扣 `GunData.ammo`（走 `GunItem.afterShoot` 的通用弹匣分支），不碰能量；
 * - 换弹时按 `AmmoCostPerShoot` 把能量折算成发数装进弹匣
 *   （[count] 报「能装几发」，[consume] 扣「发数 × 每发 FE」）；
 * - 退弹由 `GunData.withdrawAmmo` 统一处理（那里才拿得到 `AmmoCostPerShoot`），
 *   本策略的 [withdraw] 只负责背包型，见该方法的说明。
 *
 * 之所以把「发数 ↔ FE」的换算收在本策略内部，是因为换弹链路
 * （`GunData.reloadAmmo` → `countBackupAmmo` / `consumeBackupAmmo` → `AmmoConsumer.consume`）
 * 本来就按「弹药单位」结算，换算放在这里通用管线一行都不用改。
 *
 * 作为附加来源时（如泰瑟枪的 `"400 fe"`），前缀即每发消耗的 FE 数量，与上述两种形态无关。
 */
object EnergyAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.ENERGY

    override fun match(ammo: String) = ammo.lowercase() in setOf("fe", "rf", "energy")

    // ---------------------------------------------------------------- 形态判定

    /**
     * 是否为弹匣型能量武器。
     *
     * 用 [GunData.getDefault] 而不是 `get(MAGAZINE)`：本策略会在 PMC 计算流水线内部被读取
     * （`AmmoConsumer` 的覆盖层），而重入的 `get()` 受 `GunData.rebuilding` 保护会提前返回
     * **未完成**的结果。`Magazine` 与 `AmmoCostPerShoot` 都不参与 Perk / 配件改写，读基线即正确值。
     */
    private fun isMagazine(data: GunData) = (data.getDefault().magazine.firstOrNull() ?: 0) > 0

    /** 每发（背包型下为每次开火）折算的 FE，至少为 1 以免除零 */
    private fun energyPerRound(data: GunData) = data.getDefault().ammoCostPerShoot.coerceAtLeast(1)

    /**
     * 把弹药单位折算成 FE。
     *
     * 夹到 `Int.MAX_VALUE`：发数与每发消耗都可能来自数据包，直接相乘会溢出成负数，
     * 而负数传给 `extractEnergy` 会被当成「请求 0」而静默不扣能量。
     */
    private fun toEnergy(data: GunData, count: Int) =
        min(count.toLong() * energyPerRound(data), Int.MAX_VALUE.toLong()).toInt()

    // ---------------------------------------------------------------- 消耗

    override fun consume(data: GunData, source: AmmoSource, shooter: Entity?, count: Int): Int {
        if (!isMagazine(data)) {
            // 背包型：count 就是 FE 点数
            return data.getEnergyProvider(shooter)?.extractEnergy(count, false) ?: 0
        }

        val perRound = energyPerRound(data)
        val extracted = data.getEnergyProvider(shooter)
            ?.extractEnergy(toEnergy(data, count), false)
            ?: 0

        // 返回实际装填的发数：不足一发时向下取整，不会出现「半发」
        return extracted / perRound
    }

    override fun consume(data: GunData, source: AmmoSource, handler: IItemHandler, count: Int): Int {
        if (!isMagazine(data)) {
            val energyStorage = data.stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return 0
            return energyStorage.extractEnergy(count, false)
        }

        val perRound = energyPerRound(data)
        val extracted = data.stack.getCapability(Capabilities.EnergyStorage.ITEM)
            ?.extractEnergy(toEnergy(data, count), false)
            ?: 0

        return extracted / perRound
    }

    // ---------------------------------------------------------------- 清点

    override fun count(data: GunData, source: AmmoSource, entity: Entity?): Int {
        if (entity == null) return 0
        val energy = data.getEnergyProvider(entity)?.energyStored ?: 0
        return toAmmoUnits(data, energy)
    }

    override fun count(data: GunData, source: AmmoSource, handler: IItemHandler?): Int {
        if (handler == null) return 0
        val energy = data.stack.getCapability(Capabilities.EnergyStorage.ITEM)?.energyStored ?: 0
        return toAmmoUnits(data, energy)
    }

    /**
     * 把能量折算成弹药单位。
     *
     * 弹匣型下这里报的是**能装填几发**，正好对应 `GunData.reloadAmmo` 里
     * `min(ammoNeeded, countBackupAmmo(...))` 与 `tryStartReload` 的 `hasBackupAmmo` 语义；
     * 背包型下直接返回能量点数（换弹路径全部被 `useBackpackAmmo()` 拦截，不会走到这里）。
     */
    private fun toAmmoUnits(data: GunData, energy: Int): Int {
        if (!isMagazine(data)) return energy
        return energy / energyPerRound(data)
    }

    // ---------------------------------------------------------------- 退弹

    /**
     * 退弹。
     *
     * 弹匣型的能量回流放在 `GunData.withdrawAmmo`：换算需要 `AmmoCostPerShoot`，
     * 而本方法签名里拿不到 [GunData]（`AmmoConsumeStrategy.withdraw` 不参与消耗口径换算），
     * 在那里处理既拿得到数据，也省掉一次「策略反查枪械状态」的绕行。
     * 这里只为背包型把关：没有弹匣就退不出东西，避免把整管能量「退回」自己。
     */
    override fun withdraw(source: AmmoSource, ammoSupplier: Entity, count: Int) = 0

    override fun withdraw(source: AmmoSource, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(source: AmmoSource) = "Energy"
}
