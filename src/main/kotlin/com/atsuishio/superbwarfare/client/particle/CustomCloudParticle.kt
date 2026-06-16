package com.atsuishio.superbwarfare.client.particle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.core.BlockPos
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
open class CustomCloudParticle protected constructor(
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
    size: Float,
    gravity: Float,
    cooldown: Boolean,
    light: Boolean
) : TextureSheetParticle(world, x, y, z) {
    protected var cooldown: Boolean
    protected var light: Boolean

    @OnlyIn(Dist.CLIENT)
    class Provider(private val spriteSet: SpriteSet) : ParticleProvider<CustomCloudOption> {
        override fun createParticle(
            pType: CustomCloudOption,
            pLevel: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double
        ): Particle {
            return CustomCloudParticle(
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
                pType.size,
                pType.gravity,
                pType.cooldown,
                pType.light
            )
        }
    }

    init {
        this.setSize(0.4f, 0.4f)
        this.quadSize *= size
        this.lifetime = max(1, life + (this.random.nextInt(life) - (0.1 * life).toInt()))
        this.gravity = gravity
        this.hasPhysics = false
        this.xd = vx
        this.yd = vy
        this.zd = vz
        this.setSpriteFromAge(spriteSet)
        this.rCol = rCol
        this.gCol = gCol
        this.bCol = bCol
        this.cooldown = cooldown
        this.light = light
    }

    public override fun getLightColor(partialTick: Float): Int {
        val blockpos = BlockPos.containing(this.x, this.y + 1, this.z)
        val lightLevel = if (this.level.isLoaded(blockpos)) LevelRenderer.getLightColor(this.level, blockpos) else 0
        return if (light) 15728880 else lightLevel
    }

    override fun getRenderType(): ParticleRenderType {
        return if (light) ParticleRenderType.PARTICLE_SHEET_LIT else ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
    }

    override fun tick() {
        super.tick()
        if (cooldown) {
            this.rCol *= 0.985f
            this.gCol *= 0.985f
            this.bCol *= 0.985f
        }
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(min((this.age / 8) + 1, 8), 8))
        }
        if (this.age++ < this.lifetime && !(this.alpha <= 0)) {
            alpha = 1 - (age.toFloat() / lifetime)
        } else {
            this.remove()
        }

        xd *= 0.85
        yd *= 0.85
        zd *= 0.85
    }
}
