package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeThermalRenderer
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.resource.gun.GunResource
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max

/** 枪械专用的短时渲染光源；不写入方块光照，也不触发区块更新。 */
@OnlyIn(Dist.CLIENT)
object GunMuzzleLightHandler {

    private const val MAX_SOURCES = 64
    private const val WALL_CLEARANCE = 0.12

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
    ) {
        var requestedPosition: Vec3 = position
        var sampledAt = Double.NEGATIVE_INFINITY
        var blocked = false

        fun intensity(time: Double): Float = strength * envelope(time - start, duration)
    }

    private var world: ClientLevel? = null
    private val lights = linkedMapOf<SourceKey, Light>()
    private val samples = linkedMapOf<SourceKey, MuzzleSample>()

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

    /** 复用材质 shader 的光照输入，兼容原版手臂、SBM 枪身及光影包材质。 */
    @JvmStatic
    fun firstPersonLight(packedLight: Int): Int {
        val mc = Minecraft.getInstance()
        if (!DisplayConfig.ENABLE_FIRE_FLASH_LIGHT.get() || !mc.options.cameraType.isFirstPerson ||
            ShaderCompat.isRenderingShadowPass() || ScopeThermalRenderer.isFullScreenActive()) return packedLight
        val player = mc.player ?: return packedLight
        if (player.level() !== world) return packedLight
        val active = ActiveGun.stackOf(player)
        if (active.isEmpty) return packedLight
        val light = lights[SourceKey(player.id, identity(active))] ?: return packedLight
        val amount = intensity(light).coerceIn(0f, 1f)
        if (amount <= 0f) return packedLight
        return LightTexture.pack(max(LightTexture.block(packedLight), (amount * 13f).toInt()), LightTexture.sky(packedLight))
    }

    @JvmStatic
    fun clear() {
        lights.clear()
        samples.clear()
        world = null
    }
}
