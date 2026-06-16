package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.client.particle.*
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterParticleProvidersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ModParticles {
    @SubscribeEvent
    fun registerParticles(event: RegisterParticleProvidersEvent) {
        with(event) {
            registerSpriteSet(ModParticleTypes.FIRE_STAR.get()) { FireStarParticle.provider(it) }
            registerSpriteSet(ModParticleTypes.EXPLOSION_DEBRIS.get()) { ExplosionDebrisParticle.Provider(it) }
            registerSpriteSet(ModParticleTypes.WHITE_STAR.get()) { WhiteStarParticle.provider(it) }
            registerSpriteSet(ModParticleTypes.RISING_SMOKE.get()) { RisingSmokeParticle.provider(it) }
            registerSpecial(ModParticleTypes.BULLET_DECAL.get(), BulletDecalParticle.Provider())
            registerSpriteSet(ModParticleTypes.CUSTOM_CLOUD.get()) { CustomCloudParticle.Provider(it) }
            registerSpriteSet(ModParticleTypes.CUSTOM_SMOKE.get()) { CustomSmokeParticle.Provider(it) }
            registerSpriteSet(ModParticleTypes.CANNON_MUZZLE_FLARE.get()) { CannonMuzzleFlareParticle.Provider(it) }
            registerSpriteSet(ModParticleTypes.CUSTOM_FLARE.get()) { CustomFlareParticle.Provider(it) }
        }
    }
}