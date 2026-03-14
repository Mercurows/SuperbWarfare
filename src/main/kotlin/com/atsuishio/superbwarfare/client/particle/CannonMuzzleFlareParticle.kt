package com.atsuishio.superbwarfare.client.particle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
open class CannonMuzzleFlareParticle protected constructor(
    world: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    vx: Double,
    vy: Double,
    vz: Double,
    private val spriteSet: SpriteSet,
    rCol: Float,
    gCol: Float,
    bCol: Float,
    life: Int,
    fade: Float,
    animationSpeed: Int,
    sizeAdd: Float
) : TextureSheetParticle(world, x, y, z) {
    var fade: Float
    var animationSpeed: Int
    var sizeAdd: Float

    @OnlyIn(Dist.CLIENT)
    class Provider(private val spriteSet: SpriteSet) : ParticleProvider<CannonMuzzleFlareOption> {
        override fun createParticle(
            pType: CannonMuzzleFlareOption,
            pLevel: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double
        ): Particle {
            return CannonMuzzleFlareParticle(
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
                pType.blue,
                pType.life,
                pType.fade,
                pType.animationSpeed,
                pType.sizeAdd
            )
        }
    }

    init {
        this.setSize(0.35f, 0.35f)
        this.quadSize *= 11f
        this.lifetime = max(1, life + (this.random.nextInt(1)))
        this.gravity = -0.05f
        this.hasPhysics = false
        this.xd = vx * 0.6
        this.yd = vy * 0.6
        this.zd = vz * 0.6
        this.setSpriteFromAge(spriteSet)
        this.rCol = rCol
        this.gCol = gCol
        this.bCol = bCol
        this.roll = Math.random().toFloat() * (Math.PI.toFloat() * 0.01f)
        this.fade = fade
        this.animationSpeed = animationSpeed
        this.sizeAdd = sizeAdd
    }

    public override fun getLightColor(partialTick: Float): Int {
        return 15728880
    }

    override fun getRenderType(): ParticleRenderType {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
    }

    override fun tick() {
        super.tick()
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(((this.age / animationSpeed) % 12 + 1).coerceIn(0, 12), 12))
        }
        this.quadSize += sizeAdd
        this.alpha *= fade
        this.rCol *= 0.93f
        this.gCol *= 0.93f
        this.bCol *= 0.93f
    }
}
