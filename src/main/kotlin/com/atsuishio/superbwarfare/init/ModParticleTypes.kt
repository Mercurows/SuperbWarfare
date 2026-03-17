package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.particle.*
import com.mojang.serialization.MapCodec
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModParticleTypes {
    val REGISTRY: DeferredRegister<ParticleType<*>> =
        DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Mod.MODID)

    @JvmField
    val FIRE_STAR = registerSimpleParticle("fire_star")

    @JvmField
    val WHITE_STAR = registerSimpleParticle("white_star")

    @JvmField
    val RISING_SMOKE = registerSimpleParticle("rising_smoke")

    @JvmField
    val BULLET_DECAL: DeferredHolder<ParticleType<*>, ParticleType<BulletDecalOption>> =
        REGISTRY.register(
            "bullet_decal",
            Supplier { createOptions(BulletDecalOption.CODEC, true, BulletDecalOption.STREAM_CODEC) }
        )

    @JvmField
    val CUSTOM_SMOKE: DeferredHolder<ParticleType<*>, ParticleType<CustomSmokeOption>> =
        REGISTRY.register(
            "custom_smoke",
            Supplier { createOptions(CustomSmokeOption.CODEC, true, CustomSmokeOption.STREAM_CODEC) }
        )

    @JvmField
    val CANNON_MUZZLE_FLARE: DeferredHolder<ParticleType<*>, ParticleType<CannonMuzzleFlareOption>> =
        REGISTRY.register(
            "cannon_muzzle_flare",
            Supplier { createOptions(CannonMuzzleFlareOption.CODEC, true, CannonMuzzleFlareOption.STREAM_CODEC) }
        )

    @JvmField
    val CUSTOM_CLOUD: DeferredHolder<ParticleType<*>, ParticleType<CustomCloudOption>> =
        REGISTRY.register(
            "custom_cloud",
            Supplier { createOptions(CustomCloudOption.CODEC, true, CustomCloudOption.STREAM_CODEC) }
        )

    fun <T : ParticleOptions> createOptions(
        codec: MapCodec<T>,
        overrideLimiter: Boolean,
        streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>
    ): ParticleType<T> {
        return object : ParticleType<T>(overrideLimiter) {
            override fun codec() = codec

            override fun streamCodec() = streamCodec
        }
    }

    fun registerSimpleParticle(
        name: String,
        limit: Boolean = true
    ): DeferredHolder<ParticleType<*>, out SimpleParticleType> {
        return REGISTRY.register(name, Supplier { SimpleParticleType(limit) })
    }
}