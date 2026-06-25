package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.particle.*
import com.mojang.serialization.Codec
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.SimpleParticleType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModParticleTypes {
    val REGISTRY: DeferredRegister<ParticleType<*>> = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Mod.MODID)

    @JvmField
    val FIRE_STAR = registerSimpleParticle("fire_star")

    @JvmField
    val EXPLOSION_DEBRIS: RegistryObject<ParticleType<ExplosionDebrisOption>> =
        REGISTRY.register("explosion_debris") {
            createOptions(
                ExplosionDebrisOption.CODEC,
                true,
                ExplosionDebrisOption.DESERIALIZER
            )
        }

    @JvmField
    val WHITE_STAR = registerSimpleParticle("white_star")

    @JvmField
    val RISING_SMOKE = registerSimpleParticle("rising_smoke")

    @JvmField
    val BULLET_DECAL: RegistryObject<ParticleType<BulletDecalOption>> =
        REGISTRY.register("bullet_decal") {
            createOptions(
                BulletDecalOption.CODEC,
                true,
                BulletDecalOption.DESERIALIZER
            )
        }

    @JvmField
    val CUSTOM_SMOKE: RegistryObject<ParticleType<CustomSmokeOption>> =
        REGISTRY.register("custom_smoke") {
            createOptions(
                CustomSmokeOption.CODEC,
                true,
                CustomSmokeOption.DESERIALIZER
            )
        }

    @JvmField
    val CANNON_MUZZLE_FLARE: RegistryObject<ParticleType<CannonMuzzleFlareOption>> =
        REGISTRY.register("cannon_muzzle_flare") {
            createOptions(
                CannonMuzzleFlareOption.CODEC,
                true,
                CannonMuzzleFlareOption.DESERIALIZER
            )
        }

    @JvmField
    val CUSTOM_FLARE: RegistryObject<ParticleType<CustomFlareOption>> =
        REGISTRY.register("custom_flare") {
            createOptions(
                CustomFlareOption.CODEC,
                true,
                CustomFlareOption.DESERIALIZER
            )
        }

    @JvmField
    val CUSTOM_CLOUD: RegistryObject<ParticleType<CustomCloudOption>> =
        REGISTRY.register("custom_cloud") {
            createOptions(
                CustomCloudOption.CODEC,
                true,
                CustomCloudOption.DESERIALIZER
            )
        }

    @Suppress("DEPRECATION")
    fun <T : ParticleOptions> createOptions(
        codec: Codec<T>,
        pOverrideLimiter: Boolean,
        deserializer: ParticleOptions.Deserializer<T>
    ): ParticleType<T> {
        return object : ParticleType<T>(pOverrideLimiter, deserializer) {
            override fun codec(): Codec<T> {
                return codec
            }
        }
    }

    fun registerSimpleParticle(name: String, limit: Boolean = true): RegistryObject<SimpleParticleType> {
        return REGISTRY.register(name) { SimpleParticleType(limit) }
    }
}