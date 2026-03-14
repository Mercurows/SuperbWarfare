package com.atsuishio.superbwarfare.client.particle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
open class CustomSmokeParticle protected constructor(
    level: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    vx: Double,
    vy: Double,
    vz: Double,
    private val spriteSet: SpriteSet,
    rCol: Float,
    gCol: Float,
    bCol: Float
) : TextureSheetParticle(level, x, y, z) {
    @OnlyIn(Dist.CLIENT)
    class Provider(private val spriteSet: SpriteSet) : ParticleProvider<CustomSmokeOption> {
        override fun createParticle(
            pType: CustomSmokeOption,
            pLevel: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double
        ): Particle {
            return CustomSmokeParticle(
                pLevel,
                x,
                y,
                z,
                xSpeed,
                ySpeed,
                zSpeed,
                this.spriteSet,
                pType.red,
                pType.green,
                pType.blue
            )
        }
    }

    init {
        this.setSize(0.4f, 0.4f)
        this.quadSize *= 10f
        this.lifetime = this.random.nextInt(200) + 600
        this.gravity = 0.001f
        this.hasPhysics = true
        this.xd = vx * 0.5
        this.yd = vy * 0.5
        this.zd = vz * 0.5
        this.setSpriteFromAge(spriteSet)
        this.rCol = rCol
        this.gCol = gCol
        this.bCol = bCol
    }

    override fun getRenderType(): ParticleRenderType {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
    }

    override fun tick() {
        super.tick()
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(min((this.age / 8) + 1, 8), 8))
        }
        if (this.age++ < this.lifetime && !(this.alpha <= 0)) {
            if (this.age >= this.lifetime - 60 && this.alpha > 0.01f) {
                this.alpha -= 0.015f
            }
        } else {
            this.remove()
        }
    }
}
