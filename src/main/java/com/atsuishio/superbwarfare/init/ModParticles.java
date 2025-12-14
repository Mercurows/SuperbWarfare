package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.client.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticles {

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.FIRE_STAR.get(), FireStarParticle::provider);
        event.registerSpriteSet(ModParticleTypes.WHITE_STAR.get(), WhiteStarParticle::provider);
        event.registerSpriteSet(ModParticleTypes.PRISMATIC_BOLT.get(), PrismaticBoltParticle::provider);
        event.registerSpriteSet(ModParticleTypes.RISING_SMOKE.get(), RisingSmokeParticle::provider);
        event.registerSpecial(ModParticleTypes.BULLET_DECAL.get(), new BulletDecalParticle.Provider());
        event.registerSpriteSet(ModParticleTypes.CUSTOM_CLOUD.get(), CustomCloudParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.CUSTOM_SMOKE.get(), CustomSmokeParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.CANNON_MUZZLE_FLARE.get(), CannonMuzzleFlareParticle.Provider::new);
    }
}

