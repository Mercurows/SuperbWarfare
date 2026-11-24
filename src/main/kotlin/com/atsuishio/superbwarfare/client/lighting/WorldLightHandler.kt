package com.atsuishio.superbwarfare.client.lighting

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
 * 通用的世界动态光源（炮弹曳光、爆炸闪光、载具开火/燃烧等）。
 *
 * 这些光源不再往客户端区块光照引擎里塞"假方块光"，而是直接生成 [GunMuzzleLightHandler.Light]，
 * 交给 [GunMuzzleLightRenderer] 用屏幕空间锥形光 pass 渲染，与枪口焰/手电走同一套管线。
 *
 * [put] 以 [key] 为单位刷新：同一颗炮弹/同一辆燃烧载具每 tick 用同一个 key 更新位置与亮度，
 * 持续时间到期后自动移除；也可以每发爆炸用一个新的 key 注册一次性的闪光。
 */
@OnlyIn(Dist.CLIENT)
object WorldLightHandler {
    private const val MAX_LIGHTS = 24

    /** 超过该距离的动态光不再提交给渲染器。 */
    private const val MAX_DISTANCE = 256.0

    private val WARM = Vec3(1.0, 0.64, 0.28)
    private val EXPLOSION = Vec3(1.0, 0.82, 0.55)

    private class Entry(
        var start: Double,
        var duration: Double,
        var strength: Float,
        var radius: Float,
        var position: Vec3,
        var direction: Vec3,
        var color: Vec3,
        var coneOuter: Float,
        var coneInner: Float,
        var shadowThickness: Float,
        var selfMask: Float,
        /** 非空时每个渲染帧用实体的插值位置（`getPosition(partialTick)`）替代 [position]，避免 20Hz tick 跳动。 */
        val follow: Entity? = null,
        val followOffset: Vec3 = Vec3.ZERO,
    )

    private val entries = LinkedHashMap<Any, Entry>()

    private fun time(): Double {
        val mc = Minecraft.getInstance()
        return (mc.level?.gameTime ?: 0L).toDouble() + mc.frameTime
    }

    /**
     * 注册/刷新一个动态光源。位置由调用方每 tick 更新；[durationTicks] 为这次注册的存活时间。
     * 圆锥光传入 cosine 值 [coneOuter]/[coneInner]（与 [GunMuzzleLightHandler.Light] 一致，点光两者都传 -2）。
     */
    @JvmStatic
    @JvmOverloads
    fun put(
        key: Any,
        position: Vec3,
        direction: Vec3 = Vec3.ZERO,
        strength: Float,
        radius: Float,
        durationTicks: Int,
        color: Vec3 = WARM,
        coneOuter: Float = -2f,
        coneInner: Float = -2f,
        /** 屏幕空间遮挡步进厚度，与枪口焰默认值一致；0 = 关闭遮挡。 */
        shadowThickness: Float = 0.9f,
        selfMask: Float = 0f,
        /** 非空时每帧按实体的插值位置跟随，避免只在客户端 tick 更新造成的跳变。 */
        follow: Entity? = null,
        followOffset: Vec3 = Vec3.ZERO,
    ) {
        if (strength <= 0f || radius <= 0f || durationTicks <= 0) return
        val dir = if (direction.lengthSqr() > 1e-8) direction.normalize() else Vec3.ZERO
        if (key !in entries && entries.size >= MAX_LIGHTS) {
            entries.remove(entries.keys.first())
        }
        entries[key] = Entry(
            // Light 的 start/duration 都是 tick 单位（与 GunMuzzleLightHandler.envelope、gameTime 一致），
            // 不能把 durationTicks 换算成秒，否则光会在下一 tick 立刻过期，几乎完全看不见。
            time(), durationTicks.toDouble(), strength.coerceIn(0f, 4f), radius,
            position, dir, color, coneOuter, coneInner, shadowThickness, selfMask,
            follow, followOffset,
        )
    }

    @JvmStatic
    fun remove(key: Any) {
        entries.remove(key)
    }

    @JvmStatic
    fun clear() {
        entries.clear()
    }

    @JvmStatic
    fun visibleLights(): List<GunMuzzleLightHandler.Light> {
        if (entries.isEmpty()) return emptyList()
        val mc = Minecraft.getInstance()
        if (mc.level == null) return emptyList()
        val now = time()
        val partialTick = mc.frameTime
        val camera = mc.gameRenderer.mainCamera.position
        entries.entries.removeIf { (_, entry) ->
            entry.follow?.isRemoved == true ||
                now - entry.start >= entry.duration ||
                entry.position.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE
        }
        if (entries.isEmpty()) return emptyList()
        val holder = mc.player ?: return emptyList()
        return entries.values.take(MAX_LIGHTS).map { entry ->
            val position = entry.follow?.getPosition(partialTick)?.add(entry.followOffset) ?: entry.position
            GunMuzzleLightHandler.Light(
                holder,
                entry.start,
                entry.duration,
                entry.strength,
                entry.radius,
                position,
                entry.direction,
                entry.color,
                entry.coneOuter,
                entry.coneInner,
                steady = false,
                shadowThickness = entry.shadowThickness,
                selfMask = entry.selfMask,
            )
        }
    }

    /** 预置的爆炸闪光颜色。 */
    @JvmStatic
    fun explosionColor(): Vec3 = EXPLOSION

    /** 预置的燃烧/枪口暖色。 */
    @JvmStatic
    fun warmColor(): Vec3 = WARM
}
