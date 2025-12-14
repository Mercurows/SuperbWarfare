package com.atsuishio.superbwarfare.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class PrismaticBoltParticle extends TextureSheetParticle {

    private float randomAngle = (float) (((Math.random() * 2) - 1) * 45);

    public static PrismaticBoltParticleProvider provider(SpriteSet spriteSet) {
        return new PrismaticBoltParticleProvider(spriteSet);
    }

    public static class PrismaticBoltParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public PrismaticBoltParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @ParametersAreNonnullByDefault
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new PrismaticBoltParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;

    protected PrismaticBoltParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.6f, 0.6f);
        this.quadSize *= 40f;
        this.lifetime = 6;
        this.gravity = 0;
        this.hasPhysics = false;
        float angle = -45 + randomAngle;
        this.oRoll = angle * Mth.DEG_TO_RAD;
        this.roll = angle * Mth.DEG_TO_RAD;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        this.setSprite(this.spriteSet.get(Mth.clamp(this.age + 1, 1, 6), 6));
        this.alpha *= 0.95f;
    }
}
