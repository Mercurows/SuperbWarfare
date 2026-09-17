package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.AmmoConsumeStrategy
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.InvalidAmmoStrategy
import com.atsuishio.superbwarfare.tools.isSameItemStack
import kotlinx.serialization.Transient
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler

/**
 * 单个弹药来源的运行时状态（不参与序列化）。
 *
 * 一个 [AmmoConsumer] 可以同时持有多个来源：
 * - 第一个是**主来源**，进弹匣，负责装填、备弹显示与图标；
 * - 其余是**附加来源**，不装填，开火前检查数量是否足够，开火时按数量直接扣除。
 *
 * ammo 字符串头部都可以带数量前缀，但两种来源的含义不同：
 * - 主来源：前缀表示「每个装载单位能提供多少弹药」，如 `"30 @RifleAmmo"` = 1 发步枪弹提供 30 点弹药；
 * - 附加来源：前缀表示「每开火一次消耗多少」，如 `"400 fe"` = 每发消耗 400 FE。
 */
class AmmoSource {

    /** 对应的弹药消耗类型枚举 */
    @Transient
    var type: AmmoConsumer.AmmoConsumeType = AmmoConsumer.AmmoConsumeType.EMPTY

    /** 主来源为「每个装载单位提供的弹药数」，附加来源为「每发消耗量」 */
    @Transient
    var loadAmount: Int = 1

    @Transient
    var strategy: AmmoConsumeStrategy = InvalidAmmoStrategy

    @Transient
    var playerAmmoType: Ammo? = null

    @Transient
    var stack: ItemStack = ItemStack.EMPTY

    /** 原始 ammo 字符串，仅用于日志 */
    @Transient
    var ammo: String = ""

    fun stack(): ItemStack {
        return this.stack
    }

    /**
     * 解析形如 `"30 @RifleAmmo"`、`"400 fe"`、`"minecraft:arrow"` 的来源字符串
     */
    fun init(ammo: String) {
        this.ammo = ammo

        val trimmed = ammo.trim()

        // 解析 "30 @RifleAmmo" → count=30, ammoStr="@RifleAmmo"
        val count = extractCount(trimmed)
        this.loadAmount = count

        val ammoStr = trimmed.trimStart { it.isDigit() || it.isWhitespace() }.trimEnd()
        val strategy = AmmoConsumeStrategy.match(ammoStr).create()

        this.strategy = strategy
        this.type = strategy.defaultType
        strategy.init(this, count, ammoStr)
    }

    fun isAmmoItem(stack: ItemStack): Boolean {
        return isSameItemStack(stack, this.stack)
    }

    /**
     * 消耗指定数量（原始数量，不包括虚拟弹药，不考虑 count）
     */
    fun consume(data: GunData, shooter: Entity?, count: Int): Int {
        if (count <= 0 || data.hasInfiniteBackupAmmo(shooter)) return 0
        return strategy.consume(data, this, shooter, count)
    }

    /**
     * 消耗指定数量（原始数量，不包括虚拟弹药，不考虑 count）
     */
    fun consume(data: GunData, handler: IItemHandler, count: Int): Int {
        if (count <= 0) return 0
        return strategy.consume(data, this, handler, count)
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     *
     * entity 为 null 时由各策略自行判断（物品类来源返回 0，能量类来源仍会读取枪械自身电量）
     */
    fun count(data: GunData, entity: Entity?): Int {
        return strategy.count(data, this, entity).coerceAtLeast(0)
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     */
    fun count(data: GunData, handler: IItemHandler?): Int {
        if (handler == null) return 0
        return strategy.count(data, this, handler).coerceAtLeast(0)
    }

    /**
     * 开火前的可用性检查。主来源由弹匣负责，附加来源则要求原始数量不少于每发消耗量。
     */
    fun hasEnough(data: GunData, entity: Entity?): Boolean {
        return count(data, entity) >= this.loadAmount
    }

    /**
     * 返还指定数量的弹药
     * <br></br>
     * 注：不会实际消耗枪内弹药
     *
     * @return 成功返还的弹药数量
     */
    fun withdraw(ammoSupplier: Entity, count: Int): Int {
        if (count <= 0) return 0
        return strategy.withdraw(this, ammoSupplier, count)
    }

    fun withdraw(handler: IItemHandler, count: Int): Int {
        if (count <= 0) return 0
        return strategy.withdraw(this, handler, count)
    }

    companion object {
        /**
         * 从 ammo 字符串头部提取 count 数值，无数字时默认为 1
         */
        fun extractCount(ammo: String): Int {
            val digits = ammo.takeWhile { it.isDigit() }
            val parsed = if (digits.isEmpty()) 1 else digits.toInt()
            return if (parsed < 1) 1 else parsed
        }
    }
}
