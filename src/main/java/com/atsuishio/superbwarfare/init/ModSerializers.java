package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModSerializers {

    public static final DeferredRegister<EntityDataSerializer<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, Mod.MODID);

    public static final RegistryObject<EntityDataSerializer<IntList>> INT_LIST_SERIALIZER = REGISTRY.register("int_list_serializer",
            () -> EntityDataSerializer.simple(FriendlyByteBuf::writeIntIdList, FriendlyByteBuf::readIntIdList));
    public static final RegistryObject<EntityDataSerializer<List<Float>>> FLOAT_LIST_SERIALIZER = REGISTRY.register("float_list_serializer",
            () -> EntityDataSerializer.simple((buf, list) -> {
                buf.writeVarInt(list.size());
                for (Float v : list) {
                    buf.writeFloat(v);
                }
            }, buf -> {
                var length = buf.readVarInt();
                var list = new ArrayList<Float>();
                for (int i = 0; i < length; i++) {
                    list.add(buf.readFloat());
                }
                return list;
            }));


    public static final RegistryObject<EntityDataSerializer<Map<String, GunData>>> GUN_DATA_MAP_SERIALIZER = REGISTRY.register("gun_data_map_serializer",
            () -> EntityDataSerializer.simple((buf, map) -> {
                buf.writeVarInt(map.size());
                for (var kv : map.entrySet()) {
                    buf.writeUtf(kv.getKey());
                    buf.writeItem(kv.getValue().stack);
                }
            }, buf -> {
                var length = buf.readVarInt();
                var map = new HashMap<String, GunData>();
                for (int i = 0; i < length; i++) {
                    map.put(buf.readUtf(), GunData.from(buf.readItem()));
                }

                return map;
            }));
}
