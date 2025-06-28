package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.particle.BulletDecalOption;
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

    public static final RegistryObject<SimpleParticleType> FIRE_STAR = REGISTRY.register("fire_star", () -> new SimpleParticleType(false));
    public static final RegistryObject<ParticleType<BulletDecalOption>> BULLET_DECAL = REGISTRY.register("bullet_decal",
            () -> new ModParticleType<>(false, BulletDecalOption.DESERIALIZER, BulletDecalOption.CODEC));
    public static final RegistryObject<SimpleParticleType> CUSTOM_CLOUD = REGISTRY.register("custom_cloud", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CUSTOM_SMOKE = REGISTRY.register("custom_smoke", () -> new SimpleParticleType(false));

    @SuppressWarnings("deprecation")
    private static class ModParticleType<T extends ParticleOptions> extends ParticleType<T> {
        private final Codec<T> codec;

        public ModParticleType(boolean overrideLimiter, ParticleOptions.Deserializer<T> deserializer, Codec<T> codec) {
            super(overrideLimiter, deserializer);
            this.codec = codec;
        }

        @Override
        public @NotNull Codec<T> codec() {
            return this.codec;
        }
    }
}

