package com.atsuishio.superbwarfare.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class CustomSmokeParticle extends TextureSheetParticle {

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<CustomSmokeOption> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(CustomSmokeOption pType, @NotNull ClientLevel pLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new CustomSmokeParticle(pLevel, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet, pType.getRed(), pType.getGreen(), pType.getBlue());
        }
    }

    private final SpriteSet spriteSet;

    protected CustomSmokeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, float rCol, float gCol, float bCol) {
        super(level, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.4f, 0.4f);
        this.quadSize *= 10f;
        this.lifetime = this.random.nextInt(200) + 600;
        this.gravity = 0.001f;
        this.hasPhysics = true;
        this.xd = vx * 0.5;
        this.yd = vy * 0.5;
        this.zd = vz * 0.5;
        this.setSpriteFromAge(spriteSet);
        this.rCol = rCol;
        this.gCol = gCol;
        this.bCol = bCol;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(Math.min((this.age / 8) + 1, 8), 8));
        }
        if (this.age++ < this.lifetime && !(this.alpha <= 0)) {
            if (this.age >= this.lifetime - 60 && this.alpha > 0.01F) {
                this.alpha -= 0.015F;
            }
        } else {
            this.remove();
        }
    }
}
