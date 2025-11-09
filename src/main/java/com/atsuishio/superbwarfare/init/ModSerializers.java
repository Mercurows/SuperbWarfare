package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModSerializers {

    public static final DeferredRegister<EntityDataSerializer<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, Mod.MODID);

    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<List<Integer>>> INT_LIST_SERIALIZER = REGISTRY.register("int_list_serializer",
            () -> EntityDataSerializer.forValueType(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()))
    );
    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<List<Float>>> FLOAT_LIST_SERIALIZER = REGISTRY.register("float_list_serializer",
            () -> EntityDataSerializer.forValueType(ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()))
    );

    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Map<String, GunData>>> VEHICLE_GUN_DATA_MAP_SERIALIZER = REGISTRY.register("vehicle_gun_data_map_serializer",
            () -> new EntityDataSerializer<>() {
                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, Map<String, GunData>> codec() {
                    return ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, GunData.VEHICLE_GUN_STREAM_CODEC);
                }

                public @NotNull Map<String, GunData> copy(@NotNull Map<String, GunData> map) {
                    var newMap = new HashMap<String, GunData>();
                    map.forEach((key, value) -> newMap.put(key, value.copy()));
                    return newMap;
                }
            });
}
