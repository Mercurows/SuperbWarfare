package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModSerializers {

    // TODO serializers
    public static final DeferredRegister<EntityDataSerializer<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, ModUtils.MODID);

//    public static final DeferredHolder<EntityDataSerializer<IntList>, EntityDataSerializer<IntList>> INT_LIST_SERIALIZER = REGISTRY.register("int_list_serializer",
//            () -> EntityDataSerializer.simple(FriendlyByteBuf::writeIntIdList, FriendlyByteBuf::readIntIdList));
}
