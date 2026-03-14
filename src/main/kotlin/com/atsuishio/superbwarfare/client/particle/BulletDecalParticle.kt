package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.TextureSheetParticle
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.inventory.InventoryMenu
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Vector3f
import kotlin.math.max

/**
 * @author Forked from MrCrayfish, continued by Timeless devs
 * Code based on TaC-Z
 */
class BulletDecalParticle @JvmOverloads constructor(
    level: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    direction: Direction,
    pos: BlockPos,
    rCol: Float = 0f,
    gCol: Float = 0f,
    bCol: Float = 0f
) : TextureSheetParticle(level, x, y, z) {
    private val direction: Direction
    private val pos: BlockPos
    private var uOffset = 0
    private var vOffset = 0
    private var textureDensity = 0f

    init {
        this.setSprite(this.getSprite(pos)!!)
        this.direction = direction
        this.pos = pos
        this.lifetime = 200
        this.hasPhysics = false
        this.gravity = 0f
        this.quadSize = 0.05f

        if (shouldRemove()) {
            this.remove()
        }

        this.rCol = rCol
        this.gCol = gCol
        this.bCol = bCol

        this.alpha = 0.9f
    }

    override fun setSprite(sprite: TextureAtlasSprite) {
        super.setSprite(sprite)
        this.uOffset = this.random.nextInt(16)
        this.vOffset = this.random.nextInt(16)
        this.textureDensity = (sprite.u1 - sprite.u0) / 16f
    }

    private fun getSprite(pos: BlockPos): TextureAtlasSprite? {
        val clientLevel = clientLevel
        if (clientLevel != null) {
            val state = clientLevel.getBlockState(pos)
            return mc.blockRenderer.blockModelShaper.getTexture(state, clientLevel, pos)
        }
        return mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(MissingTextureAtlasSprite.getLocation())
    }

    override fun getU0(): Float {
        return this.sprite.u0 + this.uOffset * this.textureDensity
    }

    override fun getV0(): Float {
        return this.sprite.v0 + this.vOffset * this.textureDensity
    }

    override fun getU1(): Float {
        return this.u0 + this.textureDensity
    }

    override fun getV1(): Float {
        return this.v0 + this.textureDensity
    }

    override fun tick() {
        super.tick()
        if (shouldRemove()) {
            this.remove()
        }
    }

    override fun render(buffer: VertexConsumer, renderInfo: Camera, partialTicks: Float) {
        val view = renderInfo.position
        val particleX = (Mth.lerp(partialTicks.toDouble(), this.xo, this.x) - view.x()).toFloat()
        val particleY = (Mth.lerp(partialTicks.toDouble(), this.yo, this.y) - view.y()).toFloat()
        val particleZ = (Mth.lerp(partialTicks.toDouble(), this.zo, this.z) - view.z()).toFloat()
        val quaternion = this.direction.rotation
        val points = arrayOf( // Y 值稍微大一点点，防止 z-fight
            Vector3f(-1f, 0.01f, -1f),
            Vector3f(-1f, 0.01f, 1f),
            Vector3f(1f, 0.01f, 1f),
            Vector3f(1f, 0.01f, -1f)
        )
        val scale = this.getQuadSize(partialTicks)

        for (i in 0..3) {
            val vector3f = points[i]
            vector3f.rotate(quaternion)
            vector3f.mul(scale)
            vector3f.add(particleX, particleY, particleZ)
        }

        // UV 坐标
        val u0 = this.u0
        val u1 = this.u1
        val v0 = this.v0
        val v1 = this.v1

        // 0 - 30 tick 内，从 15 亮度到 0 亮度
        val light = max(15 - this.age / 2, 0)
        val lightColor = LightTexture.FULL_BRIGHT

        // 颜色，逐渐渐变到 0 0 0，也就是黑色
        val colorPercent = light / 15.0f
        val red = this.rCol * colorPercent
        val green = this.gCol * colorPercent
        val blue = this.bCol * colorPercent

        // 透明度，逐渐变成 0，也就是透明
        val threshold = 0.98 * this.lifetime
        val fade = 1.0f - (max(this.age - threshold, 0.0) / (this.lifetime - threshold)).toFloat()
        val alphaFade = this.alpha * fade

        buffer.addVertex(points[0].x(), points[0].y(), points[0].z()).setUv(u1, v1)
            .setColor(red, green, blue, alphaFade).setLight(lightColor)
        buffer.addVertex(points[1].x(), points[1].y(), points[1].z()).setUv(u1, v0)
            .setColor(red, green, blue, alphaFade).setLight(lightColor)
        buffer.addVertex(points[2].x(), points[2].y(), points[2].z()).setUv(u0, v0)
            .setColor(red, green, blue, alphaFade).setLight(lightColor)
        buffer.addVertex(points[3].x(), points[3].y(), points[3].z()).setUv(u0, v1)
            .setColor(red, green, blue, alphaFade).setLight(lightColor)
    }

    private fun shouldRemove(): Boolean {
        val blockState = this.level.getBlockState(this.pos)
        if (blockState.isAir) {
            return true
        } else {
            // 阻止弹孔在与方块不构成有效附着时继续渲染
            val shape = blockState.getCollisionShape(this.level, this.pos)
            if (shape.isEmpty) {
                return true
            }
            val baseBlockBoundingBox = shape.bounds()
            val blockBoundingBox = baseBlockBoundingBox.move(this.pos)
            return !blockBoundingBox.intersects(
                this.x - 0.1, this.y - 0.1, this.z - 0.1,
                this.x + 0.1, this.y + 0.1, this.z + 0.1
            )
        }
    }

    override fun getRenderType(): ParticleRenderType {
        return ParticleRenderType.TERRAIN_SHEET
    }

    @OnlyIn(Dist.CLIENT)
    class Provider : ParticleProvider<BulletDecalOption> {
        override fun createParticle(
            option: BulletDecalOption,
            level: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double
        ): Particle {
            return BulletDecalParticle(
                level,
                x,
                y,
                z,
                option.direction,
                option.pos,
                option.red,
                option.green,
                option.blue
            )
        }
    }
}