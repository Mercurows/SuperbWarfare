package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModSerializers {
    val REGISTRY: DeferredRegister<EntityDataSerializer<*>> =
        DeferredRegister.create(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, Mod.MODID)

    @JvmField
    val INT_LIST_SERIALIZER: RegistryObject<EntityDataSerializer<MutableList<Int>>> =
        REGISTRY.register("int_list_serializer") {
            EntityDataSerializer.simple({ buf, list ->
                buf.writeVarInt(list.size)
                for (v in list) {
                    buf.writeVarInt(v)
                }
            }, { buf ->
                val length = buf.readVarInt()
                val list = arrayListOf<Int>()
                repeat(length) {
                    list.add(buf.readVarInt())
                }
                list
            })
        }

    @JvmField
    val FLOAT_LIST_SERIALIZER: RegistryObject<EntityDataSerializer<MutableList<Float>>> =
        REGISTRY.register("float_list_serializer") {
            EntityDataSerializer.simple({ buf, list ->
                buf.writeVarInt(list.size)
                for (v in list) {
                    buf.writeFloat(v)
                }
            }, { buf ->
                val length = buf.readVarInt()
                val list = arrayListOf<Float>()
                repeat(length) {
                    list.add(buf.readFloat())
                }
                list
            })
        }

    @JvmField
    val VEHICLE_GUN_DATA_MAP_SERIALIZER: RegistryObject<EntityDataSerializer<MutableMap<String, GunData>>> =
        REGISTRY.register("vehicle_gun_data_map_serializer") {
            EntityDataSerializer.simple({ buf, map ->
                buf.writeVarInt(map.size)
                for (kv in map.entries) {
                    buf.writeUtf(kv.key)
                    buf.writeNbt(kv.value.stack.shareTag)
                }
            }, { buf ->
                val length = buf.readVarInt()
                val map = hashMapOf<String, GunData>()
                repeat(length) {
                    val weaponName = buf.readUtf()

                    val tag = buf.readNbt()
                    val gunItemStack = ItemStack(ModItems.VEHICLE_GUN.get())
                    gunItemStack.setTag(tag)

                    map[weaponName] = GunData.from(gunItemStack)
                }
                map
            })
        }
}