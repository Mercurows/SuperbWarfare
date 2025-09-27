package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.particle.BulletDecalOption;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.client.particle.CustomSmokeOption;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class ModParticleTypes {

    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Mod.MODID);

    public static final RegistryObject<SimpleParticleType> FIRE_STAR = REGISTRY.register("fire_star", () -> new SimpleParticleType(true));
    public static final RegistryObject<ParticleType<BulletDecalOption>> BULLET_DECAL = REGISTRY.register("bullet_decal",
            () -> createOptions(BulletDecalOption.CODEC, true, BulletDecalOption.DESERIALIZER));
    public static final RegistryObject<ParticleType<CustomSmokeOption>> CUSTOM_SMOKE = REGISTRY.register("custom_smoke",
            () -> createOptions(CustomSmokeOption.CODEC, true, CustomSmokeOption.DESERIALIZER));

    public static final RegistryObject<ParticleType<CustomCloudOption>> CUSTOM_CLOUD = REGISTRY.register("custom_cloud",
            () -> createOptions(CustomCloudOption.CODEC, true, CustomCloudOption.DESERIALIZER));

    @SuppressWarnings("deprecation")
    public static <T extends ParticleOptions> ParticleType<T> createOptions(Codec<T> codec, boolean pOverrideLimiter, ParticleOptions.Deserializer<T> deserializer) {
        return new ParticleType<>(pOverrideLimiter, deserializer) {
            public @NotNull Codec<T> codec() {
                return codec;
            }
        };
    }
}

