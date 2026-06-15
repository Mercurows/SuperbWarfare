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

        //TODO 封存的绝密技术
//        val velocity = Vec3(xd, yd, zd)
//        val l = velocity.length()
//        var i = 0.0
//        while (i < l) {
//            val startPos = Vec3(xo, yo + bbHeight / 2, zo)
//            val pos = startPos.add(velocity.normalize().scale(-i))
//            val offset = 2 * (random.nextFloat() - 0.5f)
//            level.addParticle(
//                CustomFlareOption(
//                    0.5f,
//                    0.43f,
//                    0.36f,
//                    700,
//                    0.985f,
//                    (10 + 8 * offset).toInt(),
//                    0.03f
//                ), pos.x + offset * 0.25, pos.y + offset * 0.25, pos.z + offset * 0.25, 0.0, 0.0, 0.0
//            )
//            i += 2.0
//        }

    }

    companion object {
        fun provider(spriteSet: SpriteSet): FireStarParticleProvider {
            return FireStarParticleProvider(spriteSet)
        }
    }
}
