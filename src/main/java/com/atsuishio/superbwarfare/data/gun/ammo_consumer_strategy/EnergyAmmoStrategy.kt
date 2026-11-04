package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.AmmoSource
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.EnergyAmmoStrategy.consume
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.EnergyAmmoStrategy.count
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.EnergyAmmoStrategy.withdraw
import net.minecraft.world.entity.Entity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.items.IItemHandler
import kotlin.math.min

/**
 * 能量弹药策略 —— ammo 字符串形如 `"fe"`、`"rf"`、`"energy"`。
 *
 * 同一个 `"FE"` 弹种按「有没有弹匣 + 有没有声明换算比例」分成两种形态，两者共用本策略：
 *
 * ### 背包型（`Magazine <= 0`，如 `ql_1031`、`repair_tool`）
 * 没有弹匣，能量**在开火时**直接按 `AmmoCostPerShoot` 扣除，不参与换弹，也不能退弹。
 * 此时「1 弹药单位 == 1 FE」，[AmmoSource.loadAmount] 不参与换算。
 *
 * ### 弹匣型（`Magazine > 0` 且 `FuelPerAmmo > 0`，如改造后的 `devotion`）
 * 弹匣里存的是**发数**，能量只作为备弹：
 * - 开火只扣 `GunData.ammo`（走 `GunItem.afterShoot` 的通用弹匣分支，按 `AmmoCostPerShoot` 扣，
 *   这类武器应当写 `1`），不碰能量；
 * - 换弹时按 `FuelPerAmmo` 把能量折算成发数装进弹匣
 *   （[count] 报「能装几发」，[consume] 扣「发数 × FuelPerAmmo」）；
 * - 退弹由 `GunData.withdrawAmmo` 统一处理（那里才拿得到 `FuelPerAmmo`），
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

    /** 是否为弹匣型能量武器：有弹匣且声明了 [GunProp.FUEL_PER_AMMO] 换算比例。 */
    private fun isMagazine(data: GunData) = data.get(GunProp.MAGAZINE) > 0
            && data.get(GunProp.FUEL_PER_AMMO) > 0

    /**
     * 「1 弹药单位值多少 FE」，没声明 [GunProp.FUEL_PER_AMMO] 就是 1（纯能量武器）。
     *
     * 注意它与有没有弹匣无关：`背书连弹夹` 这类词条会把 [GunProp.MAGAZINE] 改成 0 让枪直接吃背包能量，
     * 但换算比例还在，一发仍然要按它折算出对应电量。
     */
    private fun fuelPerAmmo(data: GunData) = data.get(GunProp.FUEL_PER_AMMO).coerceAtLeast(0)

    /** 弹药单位 → FE；夹到 `Int.MAX_VALUE`，因为发数与换算比例都可能来自数据包，相乘会溢出成负数 */
    private fun unitsToEnergy(data: GunData, count: Int): Int {
        val fuel = fuelPerAmmo(data)
        if (fuel <= 0) return count
        return min(count.toLong() * fuel, Int.MAX_VALUE.toLong()).toInt()
    }

    /** FE → 弹药单位；没声明换算比例时 1 FE 就是 1 单位 */
    private fun energyToUnits(data: GunData, energy: Int): Int {
        val fuel = fuelPerAmmo(data)
        return if (fuel > 0) energy / fuel else energy
    }

    // ---------------------------------------------------------------- 消耗

    override fun consume(data: GunData, source: AmmoSource, shooter: Entity?, count: Int): Int {
        if (!isMagazine(data)) {
            // 背包型：count 是弹药单位，声明了换算比例时一发要按它折算电量
            val extracted = data.getEnergyProvider(shooter)
                .map { it.extractEnergy(unitsToEnergy(data, count), false) }
                .orElseGet { 0 }
            return energyToUnits(data, extracted)
        }

        val perRound = fuelPerAmmo(data)
        val extracted = data.getEnergyProvider(shooter)
            .map { it.extractEnergy(unitsToEnergy(data, count), false) }
            .orElseGet { 0 }

        // 返回实际装填的发数：不足一发时向下取整，不会出现「半发」
        return extracted / perRound
    }

    override fun consume(data: GunData, source: AmmoSource, handler: IItemHandler, count: Int): Int {
        if (!isMagazine(data)) {
            val extracted = data.stack.getCapability(ForgeCapabilities.ENERGY)
                .map { it.extractEnergy(unitsToEnergy(data, count), false) }
                .orElseGet { 0 }
            return energyToUnits(data, extracted)
        }

        val perRound = fuelPerAmmo(data)
        val extracted = data.stack.getCapability(ForgeCapabilities.ENERGY)
            .map { it.extractEnergy(unitsToEnergy(data, count), false) }
            .orElseGet { 0 }

        return extracted / perRound
    }

    // ---------------------------------------------------------------- 清点

    override fun count(data: GunData, source: AmmoSource, entity: Entity?): Int {
        if (entity == null) return 0
        val energy = data.getEnergyProvider(entity).map { it.energyStored }.orElseGet { 0 }
        return toAmmoUnits(data, energy)
    }

    override fun count(data: GunData, source: AmmoSource, handler: IItemHandler?): Int {
        if (handler == null) return 0
        val energy = data.stack.getCapability(ForgeCapabilities.ENERGY).map { it.energyStored }.orElseGet { 0 }
        return toAmmoUnits(data, energy)
    }

    /**
     * 把能量折算成弹药单位。
     *
     * 弹匣型下这里报的是**能装填几发**，正好对应 `GunData.reloadAmmo` 里
     * `min(ammoNeeded, countBackupAmmo(...))` 与 `tryStartReload` 的 `hasBackupAmmo` 语义；
     * 背包型下没有弹匣、换弹路径全部被 `useBackpackAmmo()` 拦截，但声明了换算比例时
     * 报的仍然是「还能打几发」，与 `AmmoCostPerShoot` 的口径对齐；没声明比例的纯能量枪
     * 则按 1 单位 = 1 FE 直接返回能量点数。
     */
    private fun toAmmoUnits(data: GunData, energy: Int): Int {
        if (!isMagazine(data) && fuelPerAmmo(data) <= 0) return energy
        return energyToUnits(data, energy)
    }

    // ---------------------------------------------------------------- 退弹

    /**
     * 退弹。
     *
     * 弹匣型的能量回流放在 `GunData.withdrawAmmo`：换算需要 `FuelPerAmmo`，
     * 而本方法签名里拿不到 [GunData]（`AmmoConsumeStrategy.withdraw` 不参与消耗口径换算），
     * 在那里处理既拿得到数据，也省掉一次「策略反查枪械状态」的绕行。
     * 这里只为背包型把关：没有弹匣就退不出东西，避免把整管能量「退回」自己。
     */
    override fun withdraw(source: AmmoSource, ammoSupplier: Entity, count: Int) = 0

    override fun withdraw(source: AmmoSource, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(source: AmmoSource) = "Energy"
}
