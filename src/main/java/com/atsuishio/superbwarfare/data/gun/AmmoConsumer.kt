package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.annotation.ServerOnly
import com.atsuishio.superbwarfare.data.*
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.AmmoConsumeStrategy
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedGsonObject
import com.atsuishio.superbwarfare.tools.isSameItemStack
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.IItemHandler

@STOFactory(AmmoConsumer.AmmoConsumerInstanceBuilder::class)
@Serializable
class AmmoConsumer : DeserializeFromString, PropertyModifier<GunData, DefaultGunData> {
    @SerializedName("Ammo")
    @SerialName("Ammo")
    var ammo: String? = null

    @SerializedName("AmmoSlot")
    @SerialName("AmmoSlot")
    var ammoSlot: String = "Default"

    @ServerOnly
    @SerializedName("Projectile")
    @SerialName("Projectile")
    var projectile: StringToObject<ProjectileInfo>? = null

    @SerializedName("Override")
    @SerialName("Override")
    var override: SerializedGsonObject? = null

    @SerializedName("Icon")
    @SerialName("Icon")
    var icon: String = loc("textures/overlay/vehicle/weapon/icons/empty.png").toString()

    @SerializedName("ShouldUnload")
    @SerialName("ShouldUnload")
    var shouldUnload: Boolean = true

    @Transient
    @kotlinx.serialization.Transient
    var type: AmmoConsumeType = AmmoConsumeType.EMPTY

    @Transient
    @kotlinx.serialization.Transient
    var loadAmount: Int = 1

    @Transient
    @kotlinx.serialization.Transient
    lateinit var strategy: AmmoConsumeStrategy

    @Transient
    @kotlinx.serialization.Transient
    private var initialized = false

    @Transient
    @kotlinx.serialization.Transient
    var playerAmmoType: Ammo? = null

    @Transient
    @kotlinx.serialization.Transient
    var stack: ItemStack = ItemStack.EMPTY

    fun stack(): ItemStack {
        return this.stack
    }

    fun initialized(): Boolean {
        return this.initialized
    }

    // TODO 是否可以考虑移除这玩意了？
    enum class AmmoConsumeType {
        INVALID,
        EMPTY,
        INFINITE,

        PLAYER_AMMO,
        ITEM,
        ENERGY,
    }

    fun isAmmoItem(stack: ItemStack): Boolean {
        return isSameItemStack(stack, this.stack)
    }

    /**
     * 消耗指定弹药数量（原始数量，不包括虚拟弹药，不考虑count）
     */
    fun consume(data: GunData, shooter: Entity, count: Int): Int {
        if (!initialized) init()
        if (count <= 0 || shooter is Player && shooter.isCreative) return 0
        return strategy.consume(data, this, shooter, count)
    }

    /**
     * 消耗指定弹药数量（原始数量，不包括虚拟弹药，不考虑count）
     */
    fun consume(data: GunData, handler: IItemHandler, count: Int): Int {
        if (!initialized) init()
        if (count <= 0) return 0
        return strategy.consume(data, this, handler, count)
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     */
    fun count(data: GunData, entity: Entity?): Int {
        if (!initialized) init()
        if (entity == null) return 0
        return strategy.count(data, this, entity).coerceAtLeast(0)
    }

    /**
     * 清点不包括虚拟弹药在内的原始弹药数量
     */
    fun count(data: GunData, handler: IItemHandler?): Int {
        if (!initialized) init()
        if (handler == null) return 0
        return strategy.count(data, this, handler).coerceAtLeast(0)
    }

    /**
     * 返还指定数量的弹药
     * <br></br>
     * 注：不会实际消耗枪内弹药
     *
     * @return 成功返还的弹药数量
     */
    fun withdraw(ammoSupplier: Entity, count: Int): Int {
        if (!initialized) init()
        if (count <= 0) return 0
        return strategy.withdraw(this, ammoSupplier, count)
    }

    fun withdraw(handler: IItemHandler, count: Int): Int {
        if (!initialized) init()
        if (count <= 0) return 0
        return strategy.withdraw(this, handler, count)
    }

    @Transient
    @kotlinx.serialization.Transient
    private val jsonPropModifier = JsonPropertyModifier(GunProp.entries)

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        if (this.projectile != null) {
            modifier[GunProp.PROJECTILE] = projectile!!.value
        }

        jsonPropModifier.update(override)
        jsonPropModifier.modifyProperty(modifier)
    }

    fun init() {
        if (ammo == null) return

        val trimmed = ammo!!.trim()

        // 解析 "30 @RifleAmmo" → count=30, ammoStr="@RifleAmmo"
        val count = extractCount(trimmed)
        this.loadAmount = count

        val ammoStr = trimmed.trimStart { it.isDigit() || it.isWhitespace() }.trimEnd()
        val strategy = AmmoConsumeStrategy.match(ammoStr).create()

        this.strategy = strategy
        this.type = strategy.defaultType
        strategy.init(this, count, ammoStr)
        this.initialized = true
    }

    /**
     * 从 ammo 字符串头部提取 count 数值，无数字时默认为 1
     */
    private fun extractCount(ammo: String): Int {
        val digits = ammo.takeWhile { it.isDigit() }
        val parsed = if (digits.isEmpty()) 1 else digits.toInt()
        return if (parsed < 1) 1 else parsed
    }

    override fun deserializeFromString(str: String?) {
        this.ammo = str
        init()
    }

    object AmmoConsumerInstanceBuilder : StringInstanceBuilder<AmmoConsumer> {
        override fun fromString(value: String) = AmmoConsumer().apply {
            this.ammo = value
            init()
        }
    }

    companion object {
        val INVALID: AmmoConsumer = AmmoConsumer()
    }
}
