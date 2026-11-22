package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.ToggleFlashlightMessage
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object FlashlightHandler {

    const val NBT_KEY = "FlashlightOn"
    private const val MAX_LIGHTS = 4

    /** 超过该距离的其他玩家手电不渲染（格）。 */
    private const val MAX_DISTANCE = 96.0
    private const val WALL_CLEARANCE = 0.15
    private const val MIN_FROM_EYE = 0.05

    private data class Sample(val origin: Vec3, val direction: Vec3, val time: Double)
    private val samples = mutableMapOf<String, Sample>()

    private fun time(): Double {
        val mc = Minecraft.getInstance()
        return (mc.level?.gameTime ?: 0L).toDouble() + mc.frameTime
    }

    private fun gunOf(stack: ItemStack): GunData? =
        if (GunItem.isHeldWeapon(stack)) GunData.from(stack) else null
    @JvmStatic
    fun hasFlashlight(stack: ItemStack): Boolean {
        val gun = gunOf(stack) ?: return false
        return AttachmentDefinition.findFlashlights(gun).isNotEmpty()
    }
    @JvmStatic
    fun isOn(stack: ItemStack): Boolean {
        val gun = gunOf(stack) ?: return false
        return AttachmentDefinition.findFlashlights(gun).isNotEmpty() && gun.data().getBoolean(NBT_KEY)
    }
    @JvmStatic
    fun toggleLocal(): Boolean? {
        val player = localPlayer ?: return null
        val stack = player.mainHandItem
        if (!hasFlashlight(stack)) return null
        val expected = !isOn(stack)
        sendPacketToServer(ToggleFlashlightMessage)
        return expected
    }
    @JvmStatic
    fun sample(holder: LivingEntity, stack: ItemStack, origin: Vec3, direction: Vec3) {
        if (!isOn(stack)) return
        val dir = direction.normalize()
        if (dir.lengthSqr() < 1e-6) return
        samples[holder.stringUUID] = Sample(origin, dir, time())
    }

    @JvmStatic
    fun clear() {
        samples.clear()
    }

    private fun colorVec(rgb: Int): Vec3 = Vec3(
        ((rgb shr 16) and 0xFF) / 255.0,
        ((rgb shr 8) and 0xFF) / 255.0,
        (rgb and 0xFF) / 255.0,
    )

    private fun eyePosition(holder: LivingEntity): Vec3 {
        val mc = Minecraft.getInstance()
        return if (holder === mc.player && mc.options.cameraType.isFirstPerson) {
            mc.gameRenderer.mainCamera.position
        } else {
            holder.getEyePosition(mc.frameTime)
        }
    }

    private fun clampOrigin(holder: LivingEntity, desired: Vec3): Vec3 {
        val level = Minecraft.getInstance().level ?: return desired
        val eye = eyePosition(holder)
        val length = eye.distanceTo(desired)
        if (length <= MIN_FROM_EYE) return desired

        val hit = level.clip(
            ClipContext(eye, desired, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, holder)
        )
        if (hit.type == HitResult.Type.MISS) return desired

        val safeDistance = (eye.distanceTo(hit.location) - WALL_CLEARANCE)
            .coerceIn(MIN_FROM_EYE, length)
        return eye.add(desired.subtract(eye).scale(safeDistance / length))
    }

    @JvmStatic
    fun visibleLights(): List<GunMuzzleLightHandler.Light> {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return emptyList()
        val camera = mc.gameRenderer.mainCamera.position
        val now = time()
        val lights = ArrayList<GunMuzzleLightHandler.Light>(MAX_LIGHTS)

        for (player in level.players()) {
            if (player.position().distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) continue

            val stack = player.mainHandItem
            if (!isOn(stack)) continue
            val emitter = AttachmentDefinition.findFlashlights(GunData.from(stack)).firstOrNull() ?: continue
            val info = emitter.info
            if (info.safeIntensity <= 0f) continue

            val sample = samples[player.stringUUID]?.takeIf { now - it.time in 0.0..1.0 }
            val rawOrigin = sample?.origin ?: player.eyePosition.add(player.lookAngle.scale(0.6))
            val origin = clampOrigin(player, rawOrigin)
            val direction = sample?.direction ?: player.lookAngle.normalize()
            // 手电的遮挡使用完整世界深度（方块 + 实体），与枪口火光一致；
            // selfMask 让 shader 忽略"光源后方 + 前方 1.5m 内"的深度——第三人称里那正是玩家自己的
            // 枪身/握把/枪管/手臂/身体，不排除会在光斑里留下锯齿状假阴影。
            // 更远的墙体/柱子/其他实体仍然照常遮挡。
            val shadowThickness = 1000f
            val selfMask = 1.5f

            lights += GunMuzzleLightHandler.Light(
                player,
                0.0,
                1.0,
                info.safeIntensity,
                info.safeRange,
                origin,
                direction,
                colorVec(info.colorRgb()),
                info.outerCos(),
                info.innerCos(),
                steady = true,
                shadowThickness = shadowThickness,
                selfMask = selfMask,
            )
        }

        return lights.sortedBy { it.position.distanceToSqr(camera) }.take(MAX_LIGHTS)
    }
}
