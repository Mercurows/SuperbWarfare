package com.atsuishio.superbwarfare.component;

import com.atsuishio.superbwarfare.ModUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ModUtils.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> BLOCK_POS = register(
            "coordinates",
            builder -> builder.persistent(BlockPos.CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = register(
            "energy",
            builder -> builder.persistent(Codec.INT)
    );

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }

}
