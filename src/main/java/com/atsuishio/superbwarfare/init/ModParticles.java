package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.client.particle.BulletDecalParticle;
import com.atsuishio.superbwarfare.client.particle.CustomCloudParticle;
import com.atsuishio.superbwarfare.client.particle.CustomSmokeParticle;
import com.atsuishio.superbwarfare.client.particle.FireStarParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticles {
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.FIRE_STAR.get(), FireStarParticle::provider);
        event.registerSpecial(ModParticleTypes.BULLET_DECAL.get(), new BulletDecalParticle.Provider());
        event.registerSpriteSet(ModParticleTypes.CUSTOM_CLOUD.get(), CustomCloudParticle::provider);
        event.registerSpriteSet(ModParticleTypes.CUSTOM_SMOKE.get(), CustomSmokeParticle::provider);
    }
}

