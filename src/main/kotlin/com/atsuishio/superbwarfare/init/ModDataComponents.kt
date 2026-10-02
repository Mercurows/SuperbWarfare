package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModDataComponents.ITEM_TAG
import com.atsuishio.superbwarfare.item.ammo.AmmoBoxInfo
import com.atsuishio.superbwarfare.item.misc.FiringParametersItem
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Function
import java.util.function.UnaryOperator

object ModDataComponents {
    @JvmField
    val DATA_COMPONENT_TYPES: DeferredRegister<DataComponentType<*>> =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mod.MODID)

    @JvmField
    val FIRING_PARAMETERS: DeferredHolder<DataComponentType<*>, DataComponentType<FiringParametersItem.Parameters>> =
        register("firing_parameters") {
            it.persistent(RecordCodecBuilder.create { instance ->
                instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(FiringParametersItem.Parameters::pos),
                    Codec.INT.fieldOf("radius").forGetter(FiringParametersItem.Parameters::radius),
                    Codec.BOOL.fieldOf("is_depressed").forGetter(FiringParametersItem.Parameters::isDepressed)
                ).apply(instance, FiringParametersItem::Parameters)
            })
        }

    @JvmField
    val ENERGY: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        register("energy") { it.persistent(Codec.INT) }

    @JvmField
    val TRANSCRIPT_SCORE: DeferredHolder<DataComponentType<*>, DataComponentType<List<Pair<Int, Double>>>> =
        register("transcript_score") {
            it.persistent(
                Codec.pair(
                    Codec.INT.fieldOf("score").codec(),
                    Codec.DOUBLE.fieldOf("distance").codec()
                ).listOf()
            )
        }

    @JvmField
    val AMMO_BOX_INFO: DeferredHolder<DataComponentType<*>, DataComponentType<AmmoBoxInfo>> =
        register("ammo_box_info") { it.persistent(AmmoBoxInfo.CODEC) }

    @JvmField
    val DOG_TAG_IMAGE: DeferredHolder<DataComponentType<*>, DataComponentType<List<List<Short>>>> =
        register("dog_tag_image") { it.persistent(Codec.SHORT.listOf().listOf()) }

    /**
     * 枪械 / 物品的根 compound（1.21 的 `ItemStack` 没有物品 NBT 了）。
     *
     * 包一层 [ItemTag] 而不直接放 `CompoundTag` 是**为了同步**：tag 是就地改的，而"这个槽位变了没有"
     * 靠 `ItemStack.matches` 比对组件值 —— `CompoundTag` 是内容判等，就地改完和上一份快照永远相等，
     * 服务端就不会把变化同步给客户端（弹药不减、装填没反应、副武器切不动）。
     * [ItemTag] 只做身份判等，所以"内容变了"靠换一个新实例表达。
     *
     * ⚠ 不能开 `cacheEncoding()`：tag 是就地改的，缓存下来的编码会一直是旧内容。
     */
    @JvmField
    val ITEM_TAG: DeferredHolder<DataComponentType<*>, DataComponentType<ItemTag>> =
        register("item_tag") {
            it.persistent(CompoundTag.CODEC.xmap({ tag -> ItemTag(tag) }, ItemTag::tag))
                .networkSynchronized(ByteBufCodecs.COMPOUND_TAG.map({ tag -> ItemTag(tag) }, ItemTag::tag))
        }

    /**
     * 根 tag 的组件值。
     *
     * `equals` / `hashCode` 是**身份**判等（不是内容判等）—— 见 [ITEM_TAG]。
     *
     * @param tag 活引用；`GunData` 与副武器运行时全程共用这一个对象
     * @param written 上一次落盘时那份内容。用来回答"自那次写回之后有没有人改过这份 tag"：
     *   副武器的状态住在主武器 tag 的子 compound 里，主武器自己一个字段都没写、内容也会变，
     *   只比对当前内容是看不出来的
     */
    class ItemTag(@JvmField val tag: CompoundTag, written: CompoundTag? = null) {
        @JvmField
        val written: CompoundTag = written ?: tag.copy()

        // 组件值必须自己实现 equals / hashCode（NeoForge 在 IDE 里会校验，普通类没有豁免）。
        // 要的是**身份**语义：tag 是就地改的，内容判等永远相等，"变了"只能靠换新实例表达。
        override fun equals(other: Any?): Boolean = this === other

        override fun hashCode(): Int = System.identityHashCode(this)
    }

    private fun <T> register(
        name: String,
        builderOperator: UnaryOperator<DataComponentType.Builder<T>>
    ): DeferredHolder<DataComponentType<*>, DataComponentType<T>> {
        return DATA_COMPONENT_TYPES.register(
            name,
            Function { builderOperator.apply(DataComponentType.builder()).build() }
        )
    }

    fun register(eventBus: IEventBus) {
        for (type in Ammo.entries) {
            type.dataComponent = register("ammo_" + type.name) { it.persistent(Codec.INT) }
        }
        DATA_COMPONENT_TYPES.register(eventBus)
    }
}