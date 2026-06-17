package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.client.renderer.ModParticleRenderTypes
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.*
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.LightLayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
open class CustomFlareParticle protected constructor(
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

    @OnlyIn(Dist.CLIENT)
    class Provider(private val spriteSet: SpriteSet) : ParticleProvider<CustomFlareOption> {
        override fun createParticle(
            pType: CustomFlareOption,
            pLevel: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double
        ): Particle {
            return CustomFlareParticle(
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
        this.setSize(0.45f, 0.45f)
        this.quadSize *= size * 14
        this.lifetime = max(1, life + (this.random.nextInt(1)))
        this.gravity = -0.05f
        this.hasPhysics = false
        this.xd = vx * 0.6
        this.yd = vy * 0.6
        this.zd = vz * 0.6
        this.setSpriteFromAge(spriteSet)
        this.targetR = rCol
        this.targetG = gCol
        this.targetB = bCol
        this.rCol = 1f
        this.gCol = 1f
        this.bCol = 1f
        this.roll = Math.random().toFloat() * (Math.PI.toFloat() * 0.01f)
        this.fade = fade
        this.animationSpeed = animationSpeed
        this.sizeAdd = sizeAdd
    }

    public override fun getLightColor(partialTick: Float): Int {
        val blockPos = BlockPos.containing(this.x, this.y, this.z)
        val worldBlockLight: Int
        val worldSkyLight: Int
        if (this.level.hasChunkAt(blockPos)) {
            worldBlockLight = this.level.getBrightness(LightLayer.BLOCK, blockPos)
            worldSkyLight = this.level.getBrightness(LightLayer.SKY, blockPos)
        } else {
            worldBlockLight = 0
            worldSkyLight = 15
        }

        val blockLight = Mth.lerp(alpha, worldBlockLight.toFloat(), 15f).toInt().coerceIn(0, 15)
        val skyLight = Mth.lerp(alpha, worldSkyLight.toFloat(), 15f).toInt().coerceIn(0, 15)
        return (blockLight shl 4) or (skyLight shl 20)
    }

    override fun getRenderType(): ParticleRenderType {
        return ModParticleRenderTypes.PARTICLE_SHEET_SOFT_TRANSLUCENT
    }

    override fun tick() {
        super.tick()
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(((this.age / animationSpeed) % 12 + 1).coerceIn(0, 12), 12))
        }
        this.quadSize += sizeAdd
        this.alpha *= fade
        this.rCol = Mth.lerp(0.1f, rCol, targetR)
        this.gCol = Mth.lerp(0.15f, gCol, targetG)
        this.bCol = Mth.lerp(0.2f, bCol, targetB)
    }
}
