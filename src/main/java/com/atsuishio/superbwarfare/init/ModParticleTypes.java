package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.particle.BulletDecalOption;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Mod.MODID);


    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_STAR = REGISTRY.register("fire_star", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, ParticleType<BulletDecalOption>> BULLET_DECAL = REGISTRY.register("bullet_decal",
            () -> createOptions(BulletDecalOption.CODEC, BulletDecalOption.STREAM_CODEC));
    public static final DeferredHolder<ParticleType<?>, ParticleType<CustomSmokeOption>> CUSTOM_SMOKE = REGISTRY.register("custom_smoke", () -> createOptions(CustomSmokeOption.CODEC, CustomSmokeOption.STREAM_CODEC));
    public static final DeferredHolder<ParticleType<?>, ParticleType<CustomCloudOption>> CUSTOM_CLOUD = REGISTRY.register("custom_cloud", () -> createOptions(CustomCloudOption.CODEC, CustomCloudOption.STREAM_CODEC));

    public static <T extends ParticleOptions> ParticleType<T> createOptions(MapCodec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return new ParticleType<>(false) {
            public @NotNull MapCodec<T> codec() {
                return codec;
            }

            public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodec;
            }
        };
    }
}

