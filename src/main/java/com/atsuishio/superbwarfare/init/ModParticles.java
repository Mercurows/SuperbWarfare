package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.client.particle.BulletHoleParticle;
import com.atsuishio.superbwarfare.client.particle.CustomCloudParticle;
import com.atsuishio.superbwarfare.client.particle.CustomSmokeParticle;
import com.atsuishio.superbwarfare.client.particle.FireStarParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticles {
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.FIRE_STAR.get(), FireStarParticle::provider);
        event.registerSpriteSet(ModParticleTypes.BULLET_HOLE.get(), BulletHoleParticle::provider);
        event.registerSpriteSet(ModParticleTypes.CUSTOM_CLOUD.get(), CustomCloudParticle::provider);
        event.registerSpriteSet(ModParticleTypes.CUSTOM_SMOKE.get(), CustomSmokeParticle::provider);
    }
}

