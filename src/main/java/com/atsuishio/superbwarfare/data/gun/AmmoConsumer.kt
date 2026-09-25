package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.data.*
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer.Companion.INVALID
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.AmmoConsumeStrategy
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.InvalidAmmoStrategy
import com.atsuishio.superbwarfare.data.mob_guns.MobGunState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.items.IItemHandler

/**
 * 一个可切换的弹种。
 *
 * [ammo] 允许写成单个字符串或字符串列表：
 * - 单个字符串即只有一条弹药来源；
 * - 字符串列表表示**同时消耗**的多条来源，第一条是主来源（进弹匣、负责装填与备弹显示），
 *   其余是附加来源（不装填，开火前检查数量是否足够，开火时按前缀数量直接扣除）。
 *
 * ```json
 * "AmmoType": {
 *   "Ammo": ["superbwarfare:taser_electrode", "400 fe"]
 * }
 * ```
 *
 * 泰瑟枪即「1 个电极（进弹匣）+ 每发 400 FE」。
 *
 * [sources] 是运行时解析结果，不参与序列化：声明为不可变之后改为 lazy 派生，
 * 因此不再需要 `init()` / `initialized()` 那套手工初始化。
 */
@StringOrObjectFactory(AmmoConsumer.AmmoConsumerInstanceBuilder::class)
@Serializable
data class AmmoConsumer(
    /** 本弹种的弹药来源列表，首项为主来源，其余为每发附加消耗的来源 */
    @SerialName("Ammo")
    val ammo: SingleOrList<String> = SingleOrList(),

    @SerialName("AmmoSlot")
    val ammoSlot: String = "Default",

    @SerialName("Projectile")
    val projectile: StringOrObject<ProjectileInfo>? = null,

    /**
     * Bones of the model that draw this ammo type, overriding [DefaultGunData.projectileBone].
     * A single name or a list of them, so one ammo type can draw several bones at once.
     * Renderer-only, see [com.atsuishio.superbwarfare.client.model.gun.GeoGunModel.showProjectileBone].
     */
    @SerialName("ProjectileBone")
    val projectileBone: SingleOrList<String> = SingleOrList(),

    @SerialName("Override")
    val override: JsonObject? = null,

    @SerialName("Icon")
    val icon: String = loc("textures/overlay/vehicle/weapon/icons/empty.png").toString(),

    @SerialName("ShouldUnload")
    val shouldUnload: Boolean = true,
) : PropertyModifier<GunData, DefaultGunData> {

    /**
     * 解析出的弹药来源。用显式 [Lazy] 字段而不是 `by lazy`：委托属性没法带字段注解，
     * 会被 kotlinx 序列化当成普通属性处理。
     */
    @Transient
    @kotlinx.serialization.Transient
    private val sourcesLazy: Lazy<List<AmmoSource>> = lazy { ammo.map { AmmoSource().apply { init(it) } } }

    // TODO 是否可以考虑移除这玩意了？
    enum class AmmoConsumeType {
        INVALID,
        EMPTY,
        INFINITE,

        PLAYER_AMMO,
        ITEM,
        ENERGY,
    }

    // ---------------------------------------------------------------- 主来源

    /** 全部弹药来源（首项为主来源） */
    val sources: List<AmmoSource>
        get() = sourcesLazy.value

    /** 主来源：进弹匣，负责装填、备弹显示与图标 */
    val primary: AmmoSource
        get() = sources.firstOrNull() ?: INVALID_SOURCE

    /** 附加来源：每次开火额外消耗的来源 */
    val extraSources: List<AmmoSource>
        get() = if (sources.size <= 1) emptyList() else sources.subList(1, sources.size)

    /** 主来源的弹药消耗类型 */
    val type: AmmoConsumeType
        get() = primary.type

    /** 主来源的装载换算：每个装载单位提供多少弹药 */
    val loadAmount: Int
        get() = primary.loadAmount

    /** 主来源的消耗策略 */
    val strategy: AmmoConsumeStrategy
        get() = primary.strategy

    /** 主来源的玩家弹药类型 */
    val playerAmmoType: Ammo?
        get() = primary.playerAmmoType

    fun stack(): ItemStack {
        return primary.stack()
    }

    /** 是否所有来源都解析成功 */
    fun isValid(): Boolean {
        return sources.isNotEmpty() && sources.none { it.type == AmmoConsumeType.INVALID }
    }

    fun isAmmoItem(stack: ItemStack): Boolean {
        return primary.isAmmoItem(stack)
    }

    /**
     * 消耗指定弹药数量（原始数量，不包括虚拟弹药，不考虑count）
     */
    fun consume(data: GunData, shooter: Entity?, count: Int): Int {
        return primary.consume(data, shooter, count)
    }

    /**
     * 消耗指定弹药数量（原始数量，不包括虚拟弹药，不考虑count）
     */
    fun consume(data: GunData, handler: IItemHandler, count: Int): Int {
        return primary.consume(data, handler, count)
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     */
    fun count(data: GunData, entity: Entity?): Int {
        return primary.count(data, entity)
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     */
    fun count(data: GunData, handler: IItemHandler?): Int {
        return primary.count(data, handler)
    }

    /**
     * 返还指定数量的弹药
     * <br></br>
     * 注：不会实际消耗枪内弹药
     *
     * @return 成功返还的弹药数量
     */
    fun withdraw(ammoSupplier: Entity, count: Int): Int {
        return primary.withdraw(ammoSupplier, count)
    }

    fun withdraw(handler: IItemHandler, count: Int): Int {
        return primary.withdraw(handler, count)
    }

    // ---------------------------------------------------------------- 附加来源

    /**
     * 附加来源每发的总消耗量。
     *
     * 前缀对附加来源的意义就是「每开火一次消耗多少」，所以这些量必须由消耗方另付
     * （生物由弹药池代付时也按这个数扣）。
     */
    fun extraAmmoCost(): Int {
        if (extraSources.isEmpty()) return 0
        return extraSources.sumOf { it.loadAmount }.coerceAtLeast(0)
    }

    /**
     * 开火前检查所有附加来源是否充足。
     * 创造模式、创造模式弹药盒、无限弹药等情况下直接视为充足；
     * **生物**（身上有弹药池）没有物品栏也没有电量，附加来源改由池子支付。
     */
    fun hasEnoughExtraAmmo(data: GunData, ammoSupplier: Entity?): Boolean {
        if (extraSources.isEmpty()) return true
        if (data.hasInfiniteBackupAmmo(ammoSupplier)) return true

        val pool = MobGunState.ammo(ammoSupplier ?: return false)
        if (pool > 0) return pool >= extraAmmoCost()

        return extraSources.all { it.hasEnough(data, ammoSupplier) }
    }

    /**
     * 开火后消耗所有附加来源，每个来源扣除自身前缀声明的每发消耗量。
     *
     * 生物（有弹药池）由池子一次性扣掉附加来源的总消耗；池子空了则退回原逻辑，
     * 让枪械因为附加来源不足而停下（与原版行为一致）。
     */
    fun consumeExtraAmmo(data: GunData, ammoSupplier: Entity?) {
        if (extraSources.isEmpty()) return
        if (data.hasInfiniteBackupAmmo(ammoSupplier)) return

        if (ammoSupplier != null) {
            val cost = extraAmmoCost()
            if (cost > 0 && MobGunState.ammo(ammoSupplier) > 0) {
                MobGunState.addAmmo(ammoSupplier, -cost)
                return
            }
        }

        for (source in extraSources) {
            source.consume(data, ammoSupplier, source.loadAmount)
        }
    }

    /** 在武器 AmmoBarOverlay 上显示的弹药名称（取主来源） */
    @OnlyIn(Dist.CLIENT)
    fun getDisplayName(): String {
        val source = primary
        return source.strategy.getDisplayName(source)
    }

    @Transient
    @kotlinx.serialization.Transient
    private val jsonPropModifier = JsonOverrideApplier(GunProp.entries)

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        if (this.projectile != null) {
            modifier[GunProp.PROJECTILE] = projectile.value
        }

        if (this.projectileBone.isNotEmpty()) {
            modifier[GunProp.PROJECTILE_BONE] = projectileBone
        }

        jsonPropModifier.update(override)
        jsonPropModifier.modifyProperty(modifier)
    }

    object AmmoConsumerInstanceBuilder : StringInstanceBuilder<AmmoConsumer> {
        override fun fromString(value: String) = AmmoConsumer(
            ammo = SingleOrList(value)
        )
    }

    companion object {
        val INVALID: AmmoConsumer = AmmoConsumer()

        /** [sources] 为空时（如 [INVALID]）使用的占位来源，只能读取，不应被修改 */
        private val INVALID_SOURCE = AmmoSource().apply {
            type = AmmoConsumeType.EMPTY
            strategy = InvalidAmmoStrategy
        }
    }
}
