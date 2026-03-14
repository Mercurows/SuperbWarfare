package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
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
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
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
        this.setSprite(this.getSprite(pos))
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

    private fun getSprite(pos: BlockPos): TextureAtlasSprite {
        val clientLevel = clientLevel
        if (clientLevel != null) {
            val state = clientLevel.getBlockState(pos)
            return mc.blockRenderer.blockModelShaper.getTexture(state, clientLevel, pos)
        }
        return mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation())
    }

    override fun getU0(): Float = this.sprite.u0 + this.uOffset * this.textureDensity

    override fun getV0(): Float = this.sprite.v0 + this.vOffset * this.textureDensity

    override fun getU1(): Float = this.u0 + this.textureDensity

    override fun getV1(): Float = this.v0 + this.textureDensity

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
        val quaternion = this.direction.getRotation()
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

        buffer.vertex(points[0].x().toDouble(), points[0].y().toDouble(), points[0].z().toDouble()).uv(u1, v1)
            .color(red, green, blue, alphaFade).uv2(lightColor).endVertex()
        buffer.vertex(points[1].x().toDouble(), points[1].y().toDouble(), points[1].z().toDouble()).uv(u1, v0)
            .color(red, green, blue, alphaFade).uv2(lightColor).endVertex()
        buffer.vertex(points[2].x().toDouble(), points[2].y().toDouble(), points[2].z().toDouble()).uv(u0, v0)
            .color(red, green, blue, alphaFade).uv2(lightColor).endVertex()
        buffer.vertex(points[3].x().toDouble(), points[3].y().toDouble(), points[3].z().toDouble()).uv(u0, v1)
            .color(red, green, blue, alphaFade).uv2(lightColor).endVertex()
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

    override fun getRenderType(): ParticleRenderType = ParticleRenderType.TERRAIN_SHEET

    @OnlyIn(Dist.CLIENT)
    class Provider : ParticleProvider<BulletDecalOption> {
        override fun createParticle(
            option: BulletDecalOption,
            world: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            pXSpeed: Double,
            pYSpeed: Double,
            pZSpeed: Double
        ): BulletDecalParticle {
            return BulletDecalParticle(
                world,
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