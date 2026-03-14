package com.atsuishio.superbwarfare.client.particle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import net.minecraft.core.particles.SimpleParticleType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
open class WhiteStarParticle protected constructor(
    world: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    vx: Double,
    vy: Double,
    vz: Double,
    spriteSet: SpriteSet
) : TextureSheetParticle(world, x, y, z) {
    class WhiteStarParticleProvider(private val spriteSet: SpriteSet) : ParticleProvider<SimpleParticleType> {
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
            return WhiteStarParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet)
        }
    }

    init {
        this.setSize(0.6f, 0.6f)
        this.quadSize *= 0.9f
        this.lifetime = max(1, 40 + (this.random.nextInt(40) - 20))
        this.gravity = 0f
        this.hasPhysics = true
        this.xd = vx * 0.95
        this.yd = vy * 0.95
        this.zd = vz * 0.95
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
        this.quadSize *= 0.95f
    }

    companion object {
        fun provider(spriteSet: SpriteSet): WhiteStarParticleProvider {
            return WhiteStarParticleProvider(spriteSet)
        }
    }
}
