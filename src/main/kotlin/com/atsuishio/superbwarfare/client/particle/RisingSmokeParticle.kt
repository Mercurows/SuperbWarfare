package com.atsuishio.superbwarfare.client.particle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import net.minecraft.core.particles.SimpleParticleType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
open class RisingSmokeParticle protected constructor(
    world: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    vx: Double,
    vy: Double,
    vz: Double,
    private val spriteSet: SpriteSet
) : TextureSheetParticle(world, x, y, z) {
    class RisingSmokeParticleProvider(private val spriteSet: SpriteSet) : ParticleProvider<SimpleParticleType> {
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
            return RisingSmokeParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet)
        }
    }

    init {
        this.scale(3f)
        this.setSize(0.25f, 0.25f)
        this.lifetime = this.random.nextInt(20) + 60
        this.gravity = -0.2f
        this.xd = vx * 0.8
        this.yd = vy * 0.8
        this.zd = vz * 0.8
        this.alpha = 0.7f
        this.setSpriteFromAge(spriteSet)
    }

    override fun getRenderType(): ParticleRenderType {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
    }

    override fun tick() {
        super.tick()
        this.alpha *= 0.98f
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(min((this.age / 2) + 1, 8), 8))
        }
    }

    companion object {
        fun provider(spriteSet: SpriteSet): RisingSmokeParticleProvider {
            return RisingSmokeParticleProvider(spriteSet)
        }
    }
}
