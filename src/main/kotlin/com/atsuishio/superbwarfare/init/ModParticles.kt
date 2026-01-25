package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.client.particle.*
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ModParticles {
    @SubscribeEvent
    fun registerParticles(event: RegisterParticleProvidersEvent) {
        event.registerSpriteSet(
            ModParticleTypes.FIRE_STAR.get()
        ) { FireStarParticle.provider(it) }
        event.registerSpriteSet(
            ModParticleTypes.WHITE_STAR.get()
        ) { WhiteStarParticle.provider(it) }
        event.registerSpriteSet(
            ModParticleTypes.RISING_SMOKE.get()
        ) { RisingSmokeParticle.provider(it) }
        event.registerSpecial(ModParticleTypes.BULLET_DECAL.get(), BulletDecalParticle.Provider())
        event.registerSpriteSet(
            ModParticleTypes.CUSTOM_CLOUD.get()
        ) { CustomCloudParticle.Provider(it) }
        event.registerSpriteSet(
            ModParticleTypes.CUSTOM_SMOKE.get()
        ) { CustomSmokeParticle.Provider(it) }
        event.registerSpriteSet(
            ModParticleTypes.CANNON_MUZZLE_FLARE.get()
        ) { CannonMuzzleFlareParticle.Provider(it) }
    }
}