package com.atsuishio.superbwarfare.client.particle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
open class ExplosionDebrisParticle protected constructor(
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
    size: Float,
    animationSpeed: Int,
    sizeAdd: Float
) : TextureSheetParticle(world, x, y, z) {
    var fade: Float
    var animationSpeed: Int
    var sizeAdd: Float
    var targetR: Float
    var targetG: Float
    var targetB: Float
    var life: Int
    var size: Float

    @OnlyIn(Dist.CLIENT)
    class Provider(private val spriteSet: SpriteSet) : ParticleProvider<ExplosionDebrisOption> {
        override fun createParticle(
            pType: ExplosionDebrisOption,
            pLevel: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double
        ): Particle {
            return ExplosionDebrisParticle(
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
                pType.size,
                pType.animationSpeed,
                pType.sizeAdd
            )
        }
    }

    init {
        this.setSize(0.35f, 0.35f)
        this.quadSize *= 0.75f
        this.lifetime = max(1, life + (this.random.nextInt(1)))
        this.gravity = 1f
        this.hasPhysics = true
        this.xd = vx
        this.yd = vy
        this.zd = vz
        this.setSpriteFromAge(spriteSet)

        this.targetR = rCol
        this.targetG = gCol
        this.targetB = bCol
        this.rCol = 1f
        this.gCol = 1f
        this.bCol = 1f
        this.fade = fade
        this.animationSpeed = animationSpeed
        this.sizeAdd = sizeAdd
        this.life = life
        this.size = size

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
            this.setSprite(this.spriteSet.get((this.age / 2) % 8 + 1, 8))
        }
        if (this.onGround) {
            this.remove()
            return
        }

        this.targetR *= 0.92f
        this.targetG *= 0.92f
        this.targetB *= 0.92f
        this.fade *= 0.994f
        this.size *= 0.99f
        this.quadSize *= 0.99f

        val velocity = Vec3(xd, yd, zd)
        val l = velocity.length()
        var i = 0.0
        while (i < l) {
            val startPos = Vec3(xo, yo + bbHeight / 2, zo)
            val pos = startPos.add(velocity.normalize().scale(-i))
            val offset = 2 * (random.nextFloat() - 0.5f)
            level.addParticle(
                CustomFlareOption(
                    this.targetR,
                    this.targetG,
                    this.targetB,
                    this.life,
                    this.fade,
                    this.animationSpeed,
                    this.sizeAdd,
                    size = this.size
                ), pos.x + offset * 0.05, pos.y + offset * 0.05, pos.z + offset * 0.05, 0.0, 0.0, 0.0
            )
            i += size * 3
        }

        xd *= 0.96
        yd *= 0.96
        zd *= 0.99
    }
}
