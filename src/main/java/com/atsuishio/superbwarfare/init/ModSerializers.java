package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

public class ModSerializers {

    public static final DeferredRegister<EntityDataSerializer<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, Mod.MODID);

    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<List<Integer>>> INT_LIST_SERIALIZER = REGISTRY.register("int_list_serializer",
            () -> EntityDataSerializer.forValueType(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()))
    );
}
