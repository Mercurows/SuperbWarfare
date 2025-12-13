package com.atsuishio.superbwarfare.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WhiteStarParticle extends TextureSheetParticle {
    public static WhiteStarParticleProvider provider(SpriteSet spriteSet) {
        return new WhiteStarParticleProvider(spriteSet);
    }

    public static class WhiteStarParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public WhiteStarParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new WhiteStarParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;

    protected WhiteStarParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.6f, 0.6f);
        this.quadSize *= 0.9f;
        this.lifetime = Math.max(1, 40 + (this.random.nextInt(40) - 20));
        this.gravity = 0;
        this.hasPhysics = true;
        this.xd = vx * 0.95;
        this.yd = vy * 0.95;
        this.zd = vz * 0.95;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    @Override
    public void tick() {
        super.tick();
        this.quadSize *= 0.95f;
    }
}
