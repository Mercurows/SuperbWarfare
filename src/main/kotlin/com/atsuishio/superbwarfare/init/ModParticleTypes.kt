package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.particle.*
import com.atsuishio.superbwarfare.tools.createStreamCodec
import com.atsuishio.superbwarfare.tools.generateMapCodec
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
    val EXPLOSION_DEBRIS = registerParticle("explosion_debris", true, ExplosionDebrisOption.CODEC)

    @JvmField
    val WHITE_STAR = registerSimpleParticle("white_star")

    @JvmField
    val RISING_SMOKE = registerSimpleParticle("rising_smoke")

    @JvmField
    val BULLET_DECAL = registerParticle("bullet_decal", true, BulletDecalOption.CODEC)

    @JvmField
    val CUSTOM_SMOKE = registerParticle<CustomSmokeOption>("custom_smoke")

    @JvmField
    val CANNON_MUZZLE_FLARE = registerParticle<CannonMuzzleFlareOption>("cannon_muzzle_flare")

    @JvmField
    val CUSTOM_FLARE = registerParticle<CustomFlareOption>("custom_flare")

    @JvmField
    val CUSTOM_CLOUD = registerParticle<CustomCloudOption>("custom_cloud")

    /** Registers a data particle type with fully explicit codecs. */
    inline fun <reified T : ParticleOptions> registerParticle(
        name: String,
        overrideLimiter: Boolean = true,
        codec: MapCodec<T> = generateMapCodec<T>(),
        streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T> = createStreamCodec<T>(),
    ): DeferredHolder<ParticleType<*>, ParticleType<T>> =
        REGISTRY.register(name, Supplier { createOptions(overrideLimiter, codec, streamCodec) })

    fun <T : ParticleOptions> createOptions(
        overrideLimiter: Boolean,
        codec: MapCodec<T>,
        streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>,
    ) = object : ParticleType<T>(overrideLimiter) {
        override fun codec() = codec

        override fun streamCodec() = streamCodec
    }

    fun registerSimpleParticle(
        name: String,
        limit: Boolean = true
    ): DeferredHolder<ParticleType<*>, out SimpleParticleType> {
        return REGISTRY.register(name, Supplier { SimpleParticleType(limit) })
    }
}
