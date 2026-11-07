package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.lighting.MuzzleFlashHelper
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.gun.MuzzleFlashRenderer.MAX_VISIBLE_TIME
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.isBarrelSilenced
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.resource.gun.DefaultGunResource
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.WeakHashMap
import kotlin.collections.ArrayDeque
import kotlin.collections.getOrPut
import kotlin.collections.isNotEmpty
import kotlin.math.abs
import kotlin.random.Random

object MuzzleFlashRenderer {

    private val FLARE_TEXTURE = Mod.loc("textures/particle/flare.png")

    private const val FLARE_BONE = "flare"
    private const val MUZZLE_FLASH_BONE = "muzzle_flash"
    private const val MAX_VISIBLE_TIME = 0.3

    /**
     * 开火窗口（枪口焰可见的那一小段）：主武器看 `fireRotTimer`，副武器看
     * `subWeaponFireRotTimer`，阈值共用 [MAX_VISIBLE_TIME]（见各自的 KDoc），两者都归 0 时不在窗口里。
     *
     * 子弹的"虚拟出膛点"也按这个窗口采样 —— 只有刚开过枪的那一小段里，枪口才代表弹体该出现的位置。
     */
    fun isFiring(): Boolean {
        val timer = if (ClientEventHandler.subWeaponFireRotTimer > 0.0) {
            ClientEventHandler.subWeaponFireRotTimer
        } else {
            ClientEventHandler.fireRotTimer
        }
        return timer > 0.0 && timer < MAX_VISIBLE_TIME
    }

    fun resolveFlareTransform(
        model: GeoGunModel,
        stack: ItemStack,
        attachmentMuzzleTransform: Matrix4f? = null,
        subWeaponFlareTransform: Matrix4f? = null,
        subWeapon: Boolean = ClientEventHandler.subWeaponFireRotTimer > 0.0,
    ): Matrix4f? {
        // 副武器开火：挂点只能是**副武器模型自己的** `flare`。解析不到就返回 `null`（调用方什么都别画），
        // 绝不退回主武器的枪口（榴弹从下挂筒里出去，枪管前端不该喷火），也不叠 `FlarePosition`
        if (subWeapon) {
            return subWeaponFlareTransform?.let { Matrix4f(it) }
        }

        val flarePosition = GunResource.compute(stack).flarePosition
        val flareBone = model.getBone(FLARE_BONE) ?: model.getBone(MUZZLE_FLASH_BONE)
        if (attachmentMuzzleTransform == null && flareBone == null && flarePosition == null) return null

        // 拷贝一份再叠 `FlarePosition`：SBM 目前每次调用都新建矩阵，但别赌它以后不改
        val transform = attachmentMuzzleTransform?.let { Matrix4f(it) }
            ?: flareBone?.let { Matrix4f(model.getGlobalTransform(it.index())) }
            ?: Matrix4f()
        flarePosition?.let {
            transform.translate(it.x.toFloat(), (it.y + 0.02).toFloat(), (-it.z).toFloat())
        }
        return transform
    }

    private val atlas = Mod.loc("textures/particle/muzzle/flash.png")
    private val shots = WeakHashMap<LivingEntity, ArrayDeque<Shot>>()

    private class Shot(val start: Double, val host: Any, val barrel: Any, val firstPerson: Boolean) {
        val variant = Random.nextInt(8)
        val angle = Random.nextFloat() * 6.283185f
        val size = 0.8f + Random.nextFloat() * 0.4f
        val sparks = Random.nextFloat()
        var presented = false
    }

    private fun identity(stack: ItemStack): Any = GunData.from(stack).uuid ?: stack

    fun hasShot(holder: LivingEntity): Boolean {
        val queue = shots[holder] ?: return false
        if (holder.tickCount - queue.last().start <= 2) return true
        shots.remove(holder)
        return false
    }

    fun clear() = shots.clear()

    fun recordShot(holder: LivingEntity, barrel: ItemStack) {
        val mc = Minecraft.getInstance()
        val queue = shots.getOrPut(holder) { ArrayDeque() }
        val time = holder.tickCount.toDouble() + mc.frameTime
        while (queue.isNotEmpty() && (time - queue.first().start > 2 || queue.size >= 8)) queue.removeFirst()
        queue.addLast(Shot(time, identity(holder.mainHandItem), identity(barrel),
            holder === mc.player && mc.options.cameraType.isFirstPerson))
    }

    fun remoteShot(id: Int, weapon: String, subWeapon: Boolean) {
        val mc = Minecraft.getInstance()
        val holder = mc.level?.getEntity(id) as? LivingEntity ?: return
        if (holder === mc.player || mc.gameRenderer.mainCamera.position.distanceToSqr(holder.position()) > 96.0 * 96.0) return
        val active = ActiveGun.stackOf(holder)
        if (active.isEmpty || active.item !is GunItem) return
        val data = GunData.from(active)
        if (data.id != weapon || ActiveGun.isSubWeapon(data) != subWeapon) return
        recordShot(holder, active)
        MuzzleFlashHelper.calculateFromStack(active)?.let { MuzzleFlashHelper.spawnFlashCone(holder.eyePosition, holder.lookAngle, it) }
    }

    fun renderShot(
        holder: LivingEntity, host: ItemStack, firstPerson: Boolean, poseStack: PoseStack,
        buffers: MultiBufferSource, anchor: Matrix4f?, flashScale: Float, partialTick: Float,
        emit: (DefaultGunResource, Boolean, Boolean) -> Unit,
    ) {
        val queue = shots[holder] ?: return
        if (anchor == null || ShaderCompat.isRenderingShadowPass()) return
        val now = holder.tickCount.toDouble() + partialTick
        while (queue.isNotEmpty() && now - queue.first().start > 2.0) queue.removeFirst()
        if (queue.isEmpty()) {
            shots.remove(holder)
            return
        }
        val barrel = ActiveGun.stackOf(holder)
        if (barrel.isEmpty) return
        val hostId = identity(host)
        val barrelId = identity(barrel)
        val resource = GunResource.compute(barrel)
        val fx = resource.muzzleFx
        val duration = (fx?.flash?.duration ?: 0.045f).coerceIn(0.015f, 0.09f)
        val silenced = GunData.from(barrel).isBarrelSilenced()
        val matrix = Matrix4f(poseStack.last().pose()).mul(anchor)
        for (shot in queue) {
            if (shot.firstPerson != firstPerson || shot.host != hostId || shot.barrel != barrelId) continue
            val age = ((now - shot.start) * 0.05).coerceAtLeast(0.0).toFloat()
            if (age > duration && (shot.presented || age > 0.065f)) continue
            val lateFirstFrame = !shot.presented && age >= duration
            if (!shot.presented) {
                shot.presented = true
                emit(resource, !firstPerson || fx?.trigger == "shot",
                    !silenced && shot.sparks < (fx?.sparks?.probability ?: 0f).coerceIn(0f, 1f))
            }
            if (silenced || fx?.flash?.enabled == false || flashScale <= 0f) continue
            val phase = if (lateFirstFrame) 0.45f else (age / duration).coerceIn(0f, 1f)
            drawFlash(buffers, matrix, resource, flashScale, shot, phase)
        }
    }

    private fun drawFlash(
        buffers: MultiBufferSource, matrix: Matrix4f, resource: DefaultGunResource,
        flashScale: Float, shot: Shot, phase: Float,
    ) {
        val fx = resource.muzzleFx
        val size = resource.flareSize * shot.size * flashScale * (1f - 0.35f * phase)
        if (size <= 0f) return
        val alpha = ((1f - phase) * 255).toInt()
        val legacy = fx == null && !resource.hasSmoke
        val consumer = buffers.getBuffer(ModRenderTypes.MUZZLE_FLASH_TYPE.apply(if (legacy) FLARE_TEXTURE else atlas))
        val index = shot.variant % (fx?.flash?.variants ?: 8).coerceIn(1, 8)
        val u = if (legacy) 0f else index / 8f
        val du = if (legacy) 1f else 0.125f
        val center = matrix.getTranslation(Vector3f())
        val radius = size * matrix.getScale(Vector3f()).x * (if (legacy) 0.5f else 0.26f)
        val core = Matrix4f().translation(center).rotateZ(shot.angle).scale(radius)
        fun quadPoint(m: Matrix4f, x: Float, y: Float, z: Float, tx: Float, ty: Float, opacity: Int) {
            consumer.vertex(m, x, y, z).color(255, 255, 255, opacity).uv(tx, ty).endVertex()
        }
        val v = if (legacy) 1f else 0.5f
        quadPoint(core, -1f, -1f, 0f, u, v, alpha)
        quadPoint(core, 1f, -1f, 0f, u + du, v, alpha)
        quadPoint(core, 1f, 1f, 0f, u + du, 0f, alpha)
        quadPoint(core, -1f, 1f, 0f, u, 0f, alpha)
        if (legacy) return
        val length = size * (fx?.flash?.length ?: 0.85f).coerceIn(0f, 3f)
        val facing = Vector3f(center).negate()
        if (facing.lengthSquared() < 1e-8f) return
        facing.normalize()
        repeat(2) { i ->
            val tongue = Matrix4f(matrix).rotateZ(shot.angle + i * 1.5707963f)
            val side = tongue.transformDirection(1f, 0f, 0f, Vector3f())
            val axis = tongue.transformDirection(0f, 0f, -1f, Vector3f())
            val normal = side.cross(axis)
            if (normal.lengthSquared() < 1e-10f) return@repeat
            val fade = ((abs(normal.normalize().dot(facing)) - 0.08f) / 0.32f).coerceIn(0f, 1f)
            val opacity = (alpha * fade * 0.88f).toInt()
            if (opacity == 0) return@repeat
            val w = size * 0.20f
            quadPoint(tongue, -w, 0f, 0f, u, 1f, opacity)
            quadPoint(tongue, w, 0f, 0f, u + du, 1f, opacity)
            quadPoint(tongue, w, 0f, -length, u + du, 0.5f, opacity)
            quadPoint(tongue, -w, 0f, -length, u, 0.5f, opacity)
        }
    }
}
