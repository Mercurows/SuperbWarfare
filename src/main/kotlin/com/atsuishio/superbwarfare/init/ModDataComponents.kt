package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.item.ammo.AmmoBoxInfo
import com.atsuishio.superbwarfare.item.misc.FiringParametersItem
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
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