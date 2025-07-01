package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

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
}
