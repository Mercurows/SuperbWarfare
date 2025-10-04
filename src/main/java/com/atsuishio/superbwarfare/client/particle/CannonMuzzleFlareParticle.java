package com.atsuishio.superbwarfare.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class CannonMuzzleFlareParticle extends TextureSheetParticle {
    public float fade;
    public int animationSpeed;
    public float sizeAdd;

    @OnlyIn(Dist.CLIENT)
        public record Provider(SpriteSet spriteSet) implements ParticleProvider<CannonMuzzleFlareOption> {

        public Particle createParticle(CannonMuzzleFlareOption pType, @NotNull ClientLevel pLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
                return new CannonMuzzleFlareParticle(pLevel, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet, pType.getRed(), pType.getGreen(), pType.getBlue(), pType.life(), pType.fade(), pType.getAnimationSpeed(), pType.sizeAdd());
            }
        }

    private final SpriteSet spriteSet;

    protected CannonMuzzleFlareParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, float rCol, float gCol, float bCol, int life, float fade, int animationSpeed, float sizeAdd) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.35f, 0.35f);
        this.quadSize *= 11f;
        this.lifetime = Math.max(1, life + (this.random.nextInt(1)));
        this.gravity = -0.05f;
        this.hasPhysics = false;
        this.xd = vx * 0.6;
        this.yd = vy * 0.6;
        this.zd = vz * 0.6;
        this.setSpriteFromAge(spriteSet);
        this.rCol = rCol;
        this.gCol = gCol;
        this.bCol = bCol;
        this.roll = (float)Math.random() * ((float)Math.PI * 0.01F);
        this.fade = fade;
        this.animationSpeed = animationSpeed;
        this.sizeAdd = sizeAdd;
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
        super.tick();
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(Mth.clamp((this.age / animationSpeed) % 12 + 1, 0 ,12), 12));
        }
        this.quadSize += sizeAdd;
        this.alpha *= fade;
        this.rCol *= 0.93f;
        this.gCol *= 0.93f;
        this.bCol *= 0.93f;
    }
}
