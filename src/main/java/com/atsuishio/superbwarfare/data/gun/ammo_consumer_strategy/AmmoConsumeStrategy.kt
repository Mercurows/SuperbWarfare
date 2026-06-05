package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.AmmoConsumeStrategy.Companion.match
import net.minecraft.world.entity.Entity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.items.IItemHandler

/**
 * 弹药消耗策略抽象类。
 * 各子类通过 [match] 判断是否匹配已去除 count 前缀的 ammo 字符串，
 * 在 [Companion.match] 中按顺序尝试，取首个匹配。
 *
 * count 由 AmmoConsumer 统一解析后传入 [init]。
 */
abstract class AmmoConsumeStrategy {

    /** 对应的弹药消耗类型枚举 */
    abstract val defaultType: AmmoConsumer.AmmoConsumeType

    /**
     * 判断本策略是否匹配给定的 ammo 字符串（已去除 count 前缀）。
     * 各子类自行实现匹配逻辑（字符串比较 / 前缀判断 / 正则等）。
     */
    abstract fun match(ammo: String): Boolean

    /**
     * 初始化 AmmoConsumer 的状态。
     * @param consumer 所属 AmmoConsumer
     * @param count 已由 AmmoConsumer 统一解析的 loadAmount
     * @param matchedString 去除 count 前缀后的类型专有字符串，各子类自行手动解析
     */
    open fun init(consumer: AmmoConsumer, count: Int, matchedString: String) {}

    /**
     * 创建本策略的新实例。默认返回 this（适用于无状态单例策略）。
     * 有状态策略（如 [HealthAmmoStrategy]）应重写此方法返回新实例，
     * 以避免多消费者共享可变状态。
     */
    open fun create(): AmmoConsumeStrategy = this

    /** 从 shooter 实体消耗指定数量的弹药，返回实际消耗的弹药物品数量 */
    abstract fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int): Int

    /** 从 IItemHandler 消耗指定数量的弹药，返回实际消耗的弹药物品数量 */
    abstract fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int): Int

    /** 清点 shooter 实体拥有的原始弹药数量 */
    abstract fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?): Int

    /** 清点 IItemHandler 中的原始弹药数量 */
    abstract fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?): Int

    /** 向 ammoSupplier 返还指定数量的弹药，返回成功返还的数量 */
    abstract fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int): Int

    /** 向 IItemHandler 返还指定数量的弹药，返回成功返还的数量 */
    abstract fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int): Int

    /** 在武器 AmmoBarOverlay 上显示的弹药信息 */
    @OnlyIn(Dist.CLIENT)
    open fun getDisplayName(consumer: AmmoConsumer): String = "Invalid"

    companion object {
        /**
         * 所有弹药策略的注册列表，按优先级排序。
         * 添加新弹药类型时，在此列表的合适位置插入即可。
         */
        // TODO 优化注册方式？
        val strategies: List<AmmoConsumeStrategy> = listOf(
            EmptyAmmoStrategy,
            InfiniteAmmoStrategy,
            EnergyAmmoStrategy,
            PlayerAmmoStrategy,
            HealthAmmoStrategy(),
            HungerAmmoStrategy(),
            ExpAmmoStrategy(),
            ItemAmmoStrategy,
            InvalidAmmoStrategy,
        )

        /**
         * 按顺序尝试各策略的 [match] 方法，返回首个匹配的策略。
         * @param ammo 已去除 count 前缀的 ammo 字符串
         * @return 匹配的策略，或 null（无匹配）
         */
        fun match(ammo: String): AmmoConsumeStrategy {
            return strategies.firstOrNull { it.match(ammo) } ?: InvalidAmmoStrategy
        }
    }
}
