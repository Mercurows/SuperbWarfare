package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.zoomVehicle;

public class A10Entity extends GeoVehicleEntity {

    public A10Entity(EntityType<A10Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage * (getHealth() > 0.1f ? 0.4f : 0.05f));
    }

    @Override
    public void onEngine1Damaged(Vec3 pos) {
        if (level().isClientSide) {
            float random = 2 * (this.random.nextFloat() - 0.5f);
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0, level(), 0.25f, 5);
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1);
            addRandomParticle(new CustomCloudOption(1f, 0.25f, 0, (int) (240 + 40 * random), 2.5f + 0.5f * random, -0.07f, true, true), pos, 0.5f, level(), 1.5f, 1);
        }
    }

    @Override
    public void onEngine2Damaged(Vec3 pos) {
        if (level().isClientSide) {
            float random = 2 * (this.random.nextFloat() - 0.5f);
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0, level(), 0.25f, 5);
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1);
            addRandomParticle(new CustomCloudOption(1f, 0.25f, 0, (int) (240 + 40 * random), 2.5f + 0.5f * random, -0.07f, true, true), pos, 0.5f, level(), 1.5f, 1);
        }
    }

    @Override
    public double getMouseSensitivity() {
        return zoomVehicle ? 0.03 : 0.07;
    }
}
