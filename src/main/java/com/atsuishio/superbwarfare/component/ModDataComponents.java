package com.atsuishio.superbwarfare.component;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.item.common.ammo.box.AmmoBoxInfo;
import com.atsuishio.superbwarfare.tools.Ammo;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FiringParameters.Parameters>> FIRING_PARAMETERS = register(
            "firing_parameters",
            builder -> builder.persistent(RecordCodecBuilder.create(instance ->
                    instance.group(
                            BlockPos.CODEC.fieldOf("pos").forGetter(FiringParameters.Parameters::pos),
                            Codec.BOOL.fieldOf("is_depressed").forGetter(FiringParameters.Parameters::isDepressed)
                    ).apply(instance, FiringParameters.Parameters::new)
            ))
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = register(
            "energy",
            builder -> builder.persistent(Codec.INT)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Pair<Integer, Double>>>> TRANSCRIPT_SCORE = register(
            "transcript_score",
            builder -> builder.persistent(Codec.pair(
                    Codec.INT.fieldOf("score").codec(),
                    Codec.DOUBLE.fieldOf("distance").codec()
            ).listOf())
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AmmoBoxInfo>> AMMO_BOX_INFO = register(
            "ammo_box_info",
            builder -> builder.persistent(AmmoBoxInfo.CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Short>>> DOG_TAG_IMAGE = register(
            "dog_tag_image",
            builder -> builder.persistent(Codec.SHORT.listOf())
    );

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        for (var type : Ammo.values()) {
            type.dataComponent = register("ammo_" + type.name, builder -> builder.persistent(Codec.INT));
        }

        DATA_COMPONENT_TYPES.register(eventBus);
    }

}
