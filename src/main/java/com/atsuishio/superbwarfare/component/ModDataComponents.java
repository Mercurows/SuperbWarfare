package com.atsuishio.superbwarfare.component;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.item.common.ammo.box.AmmoBoxInfo;
import com.atsuishio.superbwarfare.tools.AmmoType;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AmmoBoxInfo>> AMMO_BOX_INFO = register(
            "ammo_box_type",
            builder -> builder.persistent(AmmoBoxInfo.CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> GUN_DATA = register(
            "gun_data",
            builder -> builder.persistent(CompoundTag.CODEC)
    );

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        for (var type : AmmoType.values()) {
            type.dataComponent = register("ammo_" + type.name.toLowerCase(), builder -> builder.persistent(Codec.INT));
        }

        DATA_COMPONENT_TYPES.register(eventBus);
    }

}
