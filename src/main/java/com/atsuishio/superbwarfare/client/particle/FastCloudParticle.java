package com.atsuishio.superbwarfare.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class FastCloudParticle extends TextureSheetParticle {

    protected boolean cooldown;
    protected boolean light;

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<FastCloudOption> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(FastCloudOption pType, @NotNull ClientLevel pLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new FastCloudParticle(pLevel, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet, pType.getRed(), pType.getGreen(), pType.getBlue(), pType.getLife(), pType.getSize(), pType.getCooldown(), pType.getLight());
        }
    }

    private final SpriteSet spriteSet;

    protected FastCloudParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, float rCol, float gCol, float bCol, int life, int size, boolean cooldown, boolean light) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.4f, 0.4f);
        this.quadSize *= size;
        this.lifetime = Math.max(1, life + (this.random.nextInt(life) - (int) (0.1 * life)));
        this.gravity = 0;
        this.hasPhysics = false;
        this.xd = vx * 0.01;
        this.yd = vy * 0.01;
        this.zd = vz * 0.01;
        this.setSpriteFromAge(spriteSet);
        this.rCol = rCol;
        this.gCol = gCol;
        this.bCol = bCol;
        this.cooldown = cooldown;
        this.light = light;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return light ? ParticleRenderType.PARTICLE_SHEET_LIT : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (cooldown) {
            this.rCol *= 0.985f;
            this.gCol *= 0.985f;
            this.bCol *= 0.985f;
        }
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(Math.min((this.age / 8) + 1, 8), 8));
        }
        if (this.age++ < this.lifetime && !(this.alpha <= 0.0F)) {
            alpha = 1 - ((float) age / lifetime);
        } else {
            this.remove();
        }
    }
}
