package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeThermalRenderer
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 枪械专用的短时渲染光源；不写入方块光照，也不触发区块更新。 */
@OnlyIn(Dist.CLIENT)
object GunMuzzleLightHandler {

    private const val MAX_SOURCES = 64
    private const val WALL_CLEARANCE = 0.12
    private const val MODEL_LIGHT_RADIUS = 2.5
    private const val NEAR_SHADOW_TESTS = 72
    private const val FAR_SHADOW_TESTS = 16
    private const val MODEL_HIGHLIGHT_GAIN = 3.2
    /** 被自身遮挡/背光的表面保留的受光比例：压低它来制造明暗对比。 */
    private const val MODEL_SHADOW_VISIBILITY = 0.0

    private data class SourceKey(val holder: Int, val barrel: Any)
    private data class MuzzleSample(val position: Vec3, val direction: Vec3, val time: Double)

    class Light(
        val holder: LivingEntity,
        val start: Double,
        val duration: Double,
        val strength: Float,
        val radius: Float,
        var position: Vec3,
        var direction: Vec3,
        val color: Vec3 = Vec3(1.0, 0.64, 0.28),
        val coneCosOuter: Float = -2f,
        val coneCosInner: Float = -2f,
        val steady: Boolean = false,
        val shadowThickness: Float = 0.65f,
        /**
         * 自身遮挡深度窗口（米）：遮挡步进会忽略"光源后方 + 光源前方该距离内"的深度，
         * 用于排除发光者自己的枪械/握把/手臂/身体；0 = 关闭。
         */
        val selfMask: Float = 0f,
    ) {
        var requestedPosition: Vec3 = position
        var sampledAt = Double.NEGATIVE_INFINITY
        var blocked = false

        fun intensity(time: Double): Float =
            if (steady) strength else strength * envelope(time - start, duration)
    }

    private var world: ClientLevel? = null
    private val lights = linkedMapOf<SourceKey, Light>()
    private val samples = linkedMapOf<SourceKey, MuzzleSample>()
    private var firstPersonShot: Pair<SourceKey, Light>? = null
    private var modelLight: ModelLight? = null
    /** 同一枪管连射刷新同一个脉冲，亮度不会随弹丸或渲染次数累加。时间单位为 tick。 */
    @JvmStatic
    fun envelope(age: Double, duration: Double): Float {
        if (age < 0.0 || duration <= 0.0 || age >= duration) return 0f
        // 短暂保持峰值后再衰减，避免在常见帧率下只有一帧很弱的环境光。
        val hold = minOf(duration * 0.2, 0.3)
        val remaining = (1.0 - ((age - hold) / (duration - hold)).coerceIn(0.0, 1.0)).toFloat()
        return remaining * remaining * (3f - 2f * remaining)
    }

    private fun identity(stack: ItemStack): Any = GunData.from(stack).uuid ?: stack

    private fun time(): Double {
        val mc = Minecraft.getInstance()
        return (mc.level?.gameTime ?: 0L).toDouble() + mc.frameTime
    }

    private fun syncWorld() {
        val current = Minecraft.getInstance().level
        if (world !== current) {
            lights.clear()
            samples.clear()
            firstPersonShot = null
            world = current
        }
    }

    fun recordShot(holder: LivingEntity, barrel: ItemStack) {
        syncWorld()
        if (!DisplayConfig.ENABLE_FIRE_FLASH_LIGHT.get() || holder.level() !== world) return
        // 修理工具的持续焊接光仍由旧工具光效入口管理，避免重复照明。
        if (barrel.`is`(ModItems.REPAIR_TOOL.get())) return
        val params = MuzzleFlashHelper.calculateFromStack(barrel) ?: return
        val muzzleScale = GunData.from(barrel).attachment.id(AttachmentType.MUZZLE)
            ?.let(AttachmentDefinition::from)?.muzzleFlashScale ?: 1f
        if (muzzleScale <= 0f) return
        val resource = GunResource.compute(barrel)
        if (resource.muzzleFx?.flash?.enabled == false) return
        val now = time()
        lights.entries.removeIf { it.value.intensity(now) <= 0f }
        val key = SourceKey(holder.id, identity(barrel))
        val previous = lights[key]
        if (previous == null && lights.size >= MAX_SOURCES) {
            // 本地枪口永远优先；远处射手不会挤掉近处的光源。
            val camera = Minecraft.getInstance().gameRenderer.mainCamera.position
            val furthest = lights.maxByOrNull { it.value.position.distanceToSqr(camera) } ?: return
            if (holder !== Minecraft.getInstance().player &&
                holder.position().distanceToSqr(camera) >= furthest.value.position.distanceToSqr(camera)) return
            lights.remove(furthest.key)
        }
        val level = params.maxLevel.coerceIn(1, 15)
        val sample = samples[key]?.takeIf { now - it.time in 0.0..1.0 }
        val direction = sample?.direction ?: holder.lookAngle.normalize()
        val light = Light(holder, now,
            // 环境补光比枪口焰贴图稍长；QBZ 的 41ms 火焰不应限制房间照明的可见时间。
            ((resource.muzzleFx?.flash?.duration?.toDouble() ?: 0.045) + 0.055).coerceIn(0.09, 0.14) * 20.0,
            (level / 12f * muzzleScale.coerceAtMost(1f)).coerceIn(0.02f, 1f), (2f + level * 0.25f),
            sample?.position ?: holder.eyePosition.add(direction.scale(0.6)), direction)
        lights[key] = light
        if (holder === Minecraft.getInstance().player) firstPersonShot = key to light
        updatePosition(light, light.position, direction)
        light.sampledAt = sample?.time ?: Double.NEGATIVE_INFINITY
    }

    /** 模型渲染只更新挂点，不生成新光源。阴影 pass 不允许覆盖可见场景的挂点。 */
    fun sample(holder: LivingEntity, barrel: ItemStack, position: Vec3, direction: Vec3) {
        if (ShaderCompat.isRenderingShadowPass()) return
        syncWorld()
        if (holder.level() !== world || !DisplayConfig.ENABLE_FIRE_FLASH_LIGHT.get() || barrel.isEmpty) return
        val key = SourceKey(holder.id, identity(barrel))
        if (!samples.containsKey(key) && samples.size >= MAX_SOURCES) samples.remove(samples.keys.first())
        samples[key] = MuzzleSample(position, direction, time())
        val light = lights[key] ?: return
        if (light.intensity(time()) <= 0f) return
        updatePosition(light, position, direction)
        light.sampledAt = time()
    }

    private fun updatePosition(light: Light, position: Vec3, direction: Vec3) {
        val level = world ?: return
        light.requestedPosition = position
        light.direction = direction.normalize()
        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        val firstPerson = light.holder === mc.player && camera.entity === light.holder && mc.options.cameraType.isFirstPerson
        val eye = if (firstPerson) camera.position else light.holder.getEyePosition(mc.frameTime)
        val viewDirection = if (firstPerson) Vec3(camera.lookVector).normalize()
            else light.holder.getViewVector(mc.frameTime).normalize()
        val muzzleDepth = position.subtract(eye).dot(viewDirection)
        val viewHit = OverlayTraceHandler.traceViewBlocks(light.holder, eye, viewDirection, muzzleDepth)
        val limited = clampToViewDistance(eye, position, viewDirection, viewHit)
        // 视线与偏在一侧的模型枪口不在同一条射线上，缩回之后仍需检查侧墙。
        val hit = level.clip(ClipContext(eye, limited, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, light.holder))
        val candidate = if (hit.type == HitResult.Type.MISS) limited else offsetFromWall(eye, hit)
        // 射线只验证中心；再验证光源周围的体积，覆盖斜墙、墙角与半砖边缘。
        val point = retreatToClearPosition(eye, candidate) { center ->
            val box = AABB(center.x - WALL_CLEARANCE, center.y - WALL_CLEARANCE, center.z - WALL_CLEARANCE,
                center.x + WALL_CLEARANCE, center.y + WALL_CLEARANCE, center.z + WALL_CLEARANCE)
            !level.getBlockCollisions(light.holder, box).iterator().hasNext()
        }
        light.blocked = point == null
        if (point != null) light.position = point
    }

    /** 按视线深度取最小值；侧向和高度偏移同比缩回，保持原枪口方向上的位置关系。 */
    private fun clampToViewDistance(eye: Vec3, muzzle: Vec3, viewDirection: Vec3, hit: BlockHitResult): Vec3 {
        if (hit.type == HitResult.Type.MISS) return muzzle
        val offset = muzzle.subtract(eye)
        val muzzleDepth = offset.dot(viewDirection)
        if (muzzleDepth <= 0.0) return muzzle
        val wallDepth = hit.location.subtract(eye).dot(viewDirection)
        val depth = minOf(muzzleDepth, (wallDepth - WALL_CLEARANCE).coerceAtLeast(0.0))
        var fraction = depth / muzzleDepth
        // 斜视墙面时，视线距离不等于垂直墙面的距离；侧向偏移也必须受命中平面约束。
        val normal = Vec3.atLowerCornerOf(hit.direction.normal)
        val towardsWall = -offset.dot(normal)
        if (towardsWall > 1e-8) {
            val clearance = eye.subtract(hit.location).dot(normal)
            fraction = minOf(fraction, ((clearance - WALL_CLEARANCE) / towardsWall).coerceIn(0.0, 1.0))
        }
        return if (fraction >= 1.0) muzzle else eye.add(offset.scale(fraction))
    }

    /** 只沿已经通过射线检测的线段缩回；找不到安全眼位时跳过这一帧环境光。 */
    private fun retreatToClearPosition(eye: Vec3, desired: Vec3, isClear: (Vec3) -> Boolean): Vec3? {
        if (isClear(desired)) return desired
        if (!isClear(eye)) return null
        var safe = 0.0
        var blocked = 1.0
        val offset = desired.subtract(eye)
        repeat(8) {
            val middle = (safe + blocked) * 0.5
            if (isClear(eye.add(offset.scale(middle)))) safe = middle else blocked = middle
        }
        return eye.add(offset.scale(safe))
    }

    /** 沿命中面的法线留出间隙，避免斜着贴墙时沿射线回退仍然太靠近表面。 */
    private fun offsetFromWall(eye: Vec3, hit: BlockHitResult): Vec3 {
        if (hit.isInside) return eye
        val clearance = minOf(WALL_CLEARANCE, eye.distanceTo(hit.location))
        return hit.location.add(Vec3.atLowerCornerOf(hit.direction.normal).scale(clearance))
    }

    fun visibleLights(): List<Light> {
        syncWorld()
        if (!DisplayConfig.ENABLE_FIRE_FLASH_LIGHT.get()) {
            lights.clear()
            return emptyList()
        }
        val mc = Minecraft.getInstance()
        val now = time()
        lights.entries.removeIf { (_, light) -> light.holder.isRemoved || light.intensity(now) <= 0f }
        val camera = mc.gameRenderer.mainCamera.position
        return lights.values.sortedBy { if (it.holder === mc.player) -1.0 else it.position.distanceToSqr(camera) }
            .take(DisplayConfig.MUZZLE_LIGHT_LIMIT.get())
            .onEach {
                if (now - it.sampledAt > 1.0) updatePosition(it,
                    it.holder.eyePosition.add(it.holder.lookAngle.scale(0.6)), it.holder.lookAngle)
                else updatePosition(it, it.requestedPosition, it.direction)
            }
            .filterNot { it.blocked }
    }

    fun intensity(light: Light): Float = light.intensity(time()) * DisplayConfig.MUZZLE_LIGHT_STRENGTH.get().toFloat()

    @JvmStatic
    private fun firstPersonStrength(): Float {
        val mc = Minecraft.getInstance()
        if (!DisplayConfig.ENABLE_FIRE_FLASH_LIGHT.get() || !mc.options.cameraType.isFirstPerson ||
            ShaderCompat.isRenderingShadowPass() || ScopeThermalRenderer.isFullScreenActive()) return 0f
        val player = mc.player ?: return 0f
        if (mc.cameraEntity !== player || player.level() !== world) return 0f
        val (key, light) = firstPersonShot ?: return 0f
        val age = time() - light.start
        // 枪身补光比枪口焰贴图略长（5 tick / 250ms），否则截图/低帧率下经常只剩粒子而枪身已经暗回去。
        if (age < 0.0 || age >= 3.0) return 0f
        val active = ActiveGun.stackOf(player)
        if (active.isEmpty || key != SourceKey(player.id, identity(active))) return 0f
        // 枪身补光略晚于火焰贴图熄灭，夜间也能看清受光的前端零件。
        val fade = ((3.0 - age) / 2.35).coerceIn(0.0, 1.0).toFloat()
        return light.strength * fade * fade * (3f - 2f * fade) * DisplayConfig.MUZZLE_LIGHT_STRENGTH.get().toFloat()
    }

    @JvmStatic
    fun beginFirstPersonModelLight(model: TreeModelInstance, pose: Matrix4f, anchor: Matrix4f?): AutoCloseable {
        val previous = modelLight
        modelLight = null
        val strength = firstPersonStrength()
        if (anchor != null && strength > 0f) {
            val muzzle = Matrix4f(pose).mul(anchor)
            val tip = muzzle.getTranslation(Vector3f())
            val forward = muzzle.transformDirection(0f, 0f, -1f, Vector3f())
            if (forward.lengthSquared() > 1e-8f) tip.add(forward.normalize(0.055f))
            // 第一人称模型补光：真实枪口经常偏在屏幕一侧，只有前端受光。
            // 把光源的相机 x/y 移到屏幕中心（保留枪口深度），让枪身受光分布更接近"枪口火球"而不是点光源。
            val centered = Vector3f(0f, 0f, tip.z)
            modelLight = ModelLight(Vec3(centered), strength).also { it.addOccluder(model, pose) }
        }
        return AutoCloseable {
            modelLight = previous
        }
    }

    @JvmStatic
    fun renderLitModel(
        source: MultiBufferSource, model: TreeModelInstance?, pose: Matrix4f,
        texture: ResourceLocation, first: RenderType, second: RenderType,
        immediate: Boolean = false,
        draw: (MultiBufferSource) -> Unit,
    ) {
        val light = modelLight
        if (light == null) {
            draw(source)
            return
        }
        if (model != null) light.addOccluder(model, pose)

        val consumers = mutableMapOf<RenderType, ModelLightConsumer>()
        val litSource = MultiBufferSource { type ->
            val original = source.getBuffer(type)
            val mode = type.mode()
            if ((type !== first && type !== second) || type.format() != DefaultVertexFormat.NEW_ENTITY ||
                (mode != VertexFormat.Mode.QUADS && mode != VertexFormat.Mode.TRIANGLES)
            ) {
                return@MultiBufferSource original
            }
            val consumer = consumers.getOrPut(type) {
                ModelLightConsumer(light)
            }
            consumer.target = original
            consumer
        }
        draw(litSource)
    }

    /** 保留旧调用点；灯光贴图方案不需要额外 flush。 */
    @JvmStatic
    fun flushFirstPersonModelLight() = Unit

    private class ModelOccluder(val model: TreeModelInstance, val pose: Matrix4f)

    private class ModelLight(private val muzzle: Vec3, private val strength: Float) {
        private val occluders = mutableListOf<ModelOccluder>()
        private val shadows = Long2DoubleOpenHashMap()
        private var nearTests = 0
        private var farTests = 0

        fun addOccluder(model: TreeModelInstance, pose: Matrix4f) {
            if (occluders.none { it.model === model && it.pose == pose }) {
                occluders.add(ModelOccluder(model, Matrix4f(pose)))
            }
        }

        fun exposure(x: Double, y: Double, z: Double, nx: Float, ny: Float, nz: Float): Double {
            val dx = muzzle.x - x
            val dy = muzzle.y - y
            val dz = muzzle.z - z
            val distanceSquared = dx * dx + dy * dy + dz * dz
            if (distanceSquared >= MODEL_LIGHT_RADIUS * MODEL_LIGHT_RADIUS) return 0.0
            val normalSquared = nx * nx + ny * ny + nz * nz
            if (normalSquared < 1e-6f) return 0.0
            val distance = sqrt(distanceSquared)
            val facing = (nx * dx + ny * dy + nz * dz) / (distance * sqrt(normalSquared)).coerceAtLeast(0.001)
            val angular = ((facing + 0.25) / 1.25).coerceIn(0.0, 1.0)
            val range = 1.0 - distance / MODEL_LIGHT_RADIUS
            // 局部光：只照亮朝向枪口、距离足够近的面；不额外加环境项，让背光面保持暗，形成对比。
            // 用 range^1.5 而不是 range²：枪口附近保持更亮，同时中段不会掉得太快。
            return strength * angular * range * sqrt(range) * 8.0
        }

        fun visibility(x: Double, y: Double, z: Double, nx: Float, ny: Float, nz: Float): Double {
            val near = muzzle.distanceToSqr(x, y, z) < 0.8 * 0.8
            val cell = BlockPos.asLong((x * 6).roundToInt(), (y * 6).roundToInt(), (z * 6).roundToInt())
            val previous = shadows.getOrDefault(cell, -1.0)
            if (previous >= 0.0) return previous
            val used = if (near) nearTests else farTests
            val limit = if (near) NEAR_SHADOW_TESTS else FAR_SHADOW_TESTS
            // 额度按实际模型检测计数，整把枪、配件和双臂共用。
            if (used + occluders.size > limit) return 0.5
            val normal = Vec3(nx.toDouble(), ny.toDouble(), nz.toDouble()).normalize()
            val start = Vec3(x, y, z).add(normal.scale(0.018))
            val edge = muzzle.add(normal.scale(0.15))
            for (occluder in occluders) {
                if (near) nearTests++ else farTests++
                val hit = occluder.model.rayTrace(occluder.pose, Vec3.ZERO, start, edge)?.t()
                if (hit != null && hit in 0.04..0.97) {
                    shadows.put(cell, MODEL_SHADOW_VISIBILITY)
                    return MODEL_SHADOW_VISIBILITY
                }
            }
            shadows.put(cell, 1.0)
            return 1.0
        }
    }

    /**
     * 把枪口局部光照写进**灯光贴图 UV2**，而不是再画一层加色 overlay：
     * 受光面按“距离/朝向/遮挡”提升 block light，背光面保持原亮度，因此对比度来自模型本身的明暗。
     */
    private class ModelLightConsumer(
        private val light: ModelLight,
    ) : VertexConsumer {
        lateinit var target: VertexConsumer
        private var x = 0.0
        private var y = 0.0
        private var z = 0.0
        private var nx = 0f
        private var ny = 0f
        private var nz = 0f
        private var blockLight = 0
        private var skyLight = 0

        override fun vertex(
            x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float,
            u: Float, v: Float, overlay: Int, packedLight: Int, nx: Float, ny: Float, nz: Float,
        ) {
            target.vertex(x.toDouble(), y.toDouble(), z.toDouble())
            target.color(r, g, b, a)
            target.uv(u, v)
            target.overlayCoords(overlay)
            this.x = x.toDouble()
            this.y = y.toDouble()
            this.z = z.toDouble()
            this.nx = nx
            this.ny = ny
            this.nz = nz
            blockLight = LightTexture.block(packedLight) * 16
            skyLight = LightTexture.sky(packedLight) * 16
            finishVertex()
        }

        override fun vertex(x: Double, y: Double, z: Double): VertexConsumer {
            target.vertex(x, y, z)
            this.x = x
            this.y = y
            this.z = z
            return this
        }

        override fun color(r: Int, g: Int, b: Int, a: Int): VertexConsumer {
            target.color(r, g, b, a)
            return this
        }

        override fun uv(u: Float, v: Float): VertexConsumer {
            target.uv(u, v)
            return this
        }

        override fun overlayCoords(u: Int, v: Int): VertexConsumer {
            target.overlayCoords(u, v)
            return this
        }

        override fun uv2(u: Int, v: Int): VertexConsumer {
            // 先缓存原灯光，等 endVertex 时再把枪口光照合并进去。
            blockLight = u
            skyLight = v
            return this
        }

        override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
            nx = x
            ny = y
            nz = z
            return this
        }

        override fun endVertex() {
            finishVertex()
        }

        private fun finishVertex() {
            val exposure = light.exposure(x, y, z, nx, ny, nz)
            val visibility = if (exposure > 1e-5) {
                light.visibility(x, y, z, nx, ny, nz)
            } else 0.0
            val level = (sqrt((exposure * visibility * MODEL_HIGHLIGHT_GAIN).coerceIn(0.0, 1.0)) * 15.0).roundToInt()
            target.uv2(maxOf(blockLight, level * 16), skyLight)
            target.normal(nx, ny, nz)
            target.endVertex()
        }

        override fun defaultColor(r: Int, g: Int, b: Int, a: Int) {
            target.defaultColor(r, g, b, a)
        }

        override fun unsetDefaultColor() {
            target.unsetDefaultColor()
        }
    }

    @JvmStatic
    fun clear() {
        lights.clear()
        samples.clear()
        firstPersonShot = null
        modelLight = null
        world = null
    }
}
