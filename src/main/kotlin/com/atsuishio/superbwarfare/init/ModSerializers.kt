package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

object ModSerializers {
    val REGISTRY: DeferredRegister<EntityDataSerializer<*>> =
        DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, Mod.MODID)

    @JvmField
    val INT_LIST_SERIALIZER: DeferredHolder<EntityDataSerializer<*>, EntityDataSerializer<List<Int>>> =
        REGISTRY.register(
            "int_list_serializer",
            Supplier {
                EntityDataSerializer.forValueType(
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())
                )
            }
        )

    @JvmField
    val FLOAT_LIST_SERIALIZER: DeferredHolder<EntityDataSerializer<*>, EntityDataSerializer<List<Float>>> =
        REGISTRY.register(
            "float_list_serializer",
            Supplier {
                EntityDataSerializer.forValueType(
                    ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list())
                )
            }
        )

    @JvmField
    val VEHICLE_GUN_DATA_MAP_SERIALIZER: DeferredHolder<EntityDataSerializer<*>, EntityDataSerializer<Map<String, GunData>>> =
        REGISTRY.register(
            "vehicle_gun_data_map_serializer",
            Supplier {
                object : EntityDataSerializer<Map<String, GunData>> {
                    override fun codec(): StreamCodec<in RegistryFriendlyByteBuf, Map<String, GunData>> {
                        return ByteBufCodecs.map(
                            { HashMap(it) },
                            ByteBufCodecs.STRING_UTF8,
                            GunData.VEHICLE_GUN_STREAM_CODEC
                        )
                    }

                    override fun copy(map: Map<String, GunData>): Map<String, GunData> {
                        val newMap = HashMap<String, GunData>()
                        map.forEach { (key: String, value: GunData) -> newMap[key] = value.copy() }
                        return newMap
                    }
                }
            })

    @JvmField
    val SHORT_LIST_LIST_SERIALIZER: DeferredHolder<EntityDataSerializer<*>, EntityDataSerializer<List<List<Short>>>> =
        REGISTRY.register(
            "short_list_serializer",
            Supplier {
                EntityDataSerializer.forValueType(
                    ByteBufCodecs.SHORT.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list())
                )
            }
        )
}