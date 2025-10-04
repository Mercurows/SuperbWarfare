package com.atsuishio.superbwarfare.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RisingSmokeParticle extends TextureSheetParticle {
    public static RisingSmokeParticleProvider provider(SpriteSet spriteSet) {
        return new RisingSmokeParticleProvider(spriteSet);
    }

    public static class RisingSmokeParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public RisingSmokeParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new RisingSmokeParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;

    protected RisingSmokeParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.scale(3F);
        this.setSize(0.25F, 0.25F);
        this.lifetime = this.random.nextInt(20) + 60;
        this.gravity = -0.2f;
        this.xd = vx * 0.8;
        this.yd = vy * 0.8;
        this.zd = vz * 0.8;
        this.alpha = 0.7f;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha *= 0.98f;
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(Math.min((this.age / 2) + 1, 8), 8));
        }
    }
}
