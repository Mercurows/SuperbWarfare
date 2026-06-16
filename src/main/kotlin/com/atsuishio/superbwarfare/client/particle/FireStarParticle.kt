package com.atsuishio.superbwarfare.client.particle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import net.minecraft.core.particles.SimpleParticleType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
open class FireStarParticle protected constructor(
    world: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    vx: Double,
    vy: Double,
    vz: Double,
    private val spriteSet: SpriteSet
) : TextureSheetParticle(world, x, y, z) {
    class FireStarParticleProvider(private val spriteSet: SpriteSet) : ParticleProvider<SimpleParticleType> {
        override fun createParticle(
            typeIn: SimpleParticleType,
            worldIn: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double
        ): Particle {
            return FireStarParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet)
        }
    }

    init {
        this.setSize(0.35f, 0.35f)
        this.quadSize *= 0.75f
        this.lifetime = max(1, 40 + (this.random.nextInt(40) - 20))
        this.gravity = 1f
        this.hasPhysics = true
        this.xd = vx * 0.98
        this.yd = vy * 0.98
        this.zd = vz * 0.98
        this.setSpriteFromAge(spriteSet)
    }

    public override fun getLightColor(partialTick: Float): Int {
        return 15728880
    }

    override fun getRenderType(): ParticleRenderType {
        return ParticleRenderType.PARTICLE_SHEET_LIT
    }

    override fun tick() {
        super.tick()
        if (!this.removed) {
            this.setSprite(this.spriteSet.get((this.age / 2) % 8 + 1, 8))
        }

    }

    companion object {
        fun provider(spriteSet: SpriteSet): FireStarParticleProvider {
            return FireStarParticleProvider(spriteSet)
        }
    }
}
