package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.AttachmentLightState
import com.atsuishio.superbwarfare.data.attachment.FlashlightInfo
import com.atsuishio.superbwarfare.data.attachment.FlashlightMode
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.SetAttachmentLightModeMessage
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

    private const val MAX_LIGHTS = 64

    /** 超过该距离的其他玩家手电不渲染（格）。 */
    private const val MAX_DISTANCE = 128.0
    private const val WALL_CLEARANCE = 0.15
    private const val MIN_FROM_EYE = 0.05

    private data class Sample(val origin: Vec3, val direction: Vec3, val time: Double)

    /** key = 玩家 UUID + 槽位；每件配件各自一份采样。 */
    private val samples = mutableMapOf<String, Sample>()

    /** 每件配件上一帧的档位，用于检测切换（开灯眩光 / 爆闪相位起点）。 */
    private val lastModes = mutableMapOf<String, FlashlightMode>()
    private val flashStartedAt = mutableMapOf<String, Double>()
    private val modeStartedAt = mutableMapOf<String, Double>()

    private const val TICKS_PER_SECOND = 20.0

    private fun key(uuid: String, slot: AttachmentType): String = "$uuid|${slot.name}"

    private fun flashBoost(info: FlashlightInfo, start: Double, now: Double): Float {
        val duration = info.safeFlashTime * TICKS_PER_SECOND
        val age = now - start
        if (age < 0.0) return info.safeFlashPeak
        if (age >= duration) return 1f
        val remaining = (1.0 - age / duration).toFloat()
        return 1f + (info.safeFlashPeak - 1f) * remaining * remaining * remaining
    }

    private fun strobeFactor(info: FlashlightInfo, elapsedTicks: Double): Float {
        val period = TICKS_PER_SECOND / info.safeStrobeHz
        if (period <= 0.0) return 1f
        val phase = ((elapsedTicks % period) / period).toFloat().coerceIn(0f, 0.999f)
        val duty = 0.4f
        val edge = 0.1f
        val rise = (phase / edge).coerceIn(0f, 1f)
        val fall = ((duty + edge - phase) / edge).coerceIn(0f, 1f)
        val pulse = minOf(rise, fall)
        return pulse * pulse * (3f - 2f * pulse)
    }

    private fun time(): Double {
        val mc = Minecraft.getInstance()
        return (mc.level?.gameTime ?: 0L).toDouble() + mc.frameTime
    }

    private fun gunOf(stack: ItemStack): GunData? =
        if (GunItem.isHeldWeapon(stack)) GunData.from(stack) else null

    @JvmStatic
    fun hasAnyEmitter(stack: ItemStack): Boolean {
        val gun = gunOf(stack) ?: return false
        return AttachmentDefinition.findFlashlights(gun).isNotEmpty() ||
            AttachmentDefinition.findLaserEmitters(gun).isNotEmpty()
    }

    @JvmStatic
    fun cycleAllLocal(): Boolean {
        val player = localPlayer ?: return false
        val stack = player.mainHandItem
        val gun = gunOf(stack) ?: return false
        if (!hasAnyEmitter(stack)) return false
        sendPacketToServer(
            ToggleFlashlightMessage(
                ToggleFlashlightMessage.ACTION_CYCLE_ALL,
                player.inventory.selected,
                gun.uuid?.toString(),
            )
        )
        return true
    }

    @JvmStatic
    fun turnOffAllLocal(): Boolean {
        val player = localPlayer ?: return false
        val stack = player.mainHandItem
        val gun = gunOf(stack) ?: return false
        if (!hasAnyEmitter(stack)) return false
        sendPacketToServer(
            ToggleFlashlightMessage(
                ToggleFlashlightMessage.ACTION_TURN_OFF_ALL,
                player.inventory.selected,
                gun.uuid?.toString(),
            )
        )
        return true
    }

    @JvmStatic
    fun setSlotModeLocal(slot: AttachmentType, mode: FlashlightMode) {
        val player = localPlayer ?: return
        val stack = player.mainHandItem
        val gun = gunOf(stack) ?: return
        if (!hasAnyEmitter(stack)) return
        sendPacketToServer(
            SetAttachmentLightModeMessage(
                slot.ordinal,
                mode.ordinal,
                player.inventory.selected,
                gun.uuid?.toString(),
            )
        )
    }

    @JvmStatic
    fun sample(holder: LivingEntity, stack: ItemStack, slot: AttachmentType, origin: Vec3, direction: Vec3) {
        val gun = gunOf(stack) ?: return
        val mode = AttachmentLightState.modeOf(gun, slot)
        if (mode != FlashlightMode.ON && mode != FlashlightMode.STROBE) return
        val dir = direction.normalize()
        if (dir.lengthSqr() < 1e-6) return
        samples[key(holder.stringUUID, slot)] = Sample(origin, dir, time())
    }

    @JvmStatic
    fun clear() {
        samples.clear()
        lastModes.clear()
        flashStartedAt.clear()
        modeStartedAt.clear()
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
            val gun = gunOf(stack) ?: continue
            val emitters = AttachmentDefinition.findFlashlights(gun)
            if (emitters.isEmpty()) continue

            // 多手电联动增益：同一把枪上每多开一个手电（ON/STROBE），所有手电射程 +50%。
            // 只算"开着"的手电，激光档/关闭档不参与计数。
            val modes = emitters.associateWith { AttachmentLightState.modeOf(gun, it.slot.type) }
            val activeCount = modes.values.count {
                it == FlashlightMode.ON || it == FlashlightMode.STROBE
            }
            val rangeMultiplier = 1f + 0.5f * (activeCount - 1).coerceAtLeast(0)

            for ((emitter, mode) in modes) {
                val uuid = player.stringUUID
                val slot = emitter.slot.type
                val k = key(uuid, slot)
                val info = emitter.info
                if (info.safeIntensity <= 0f) continue

                val lightOn = mode == FlashlightMode.ON || mode == FlashlightMode.STROBE
                val previous = lastModes.put(k, mode) ?: mode
                val previousLightOn = previous == FlashlightMode.ON || previous == FlashlightMode.STROBE
                if (previous != mode) modeStartedAt[k] = now
                if (lightOn && !previousLightOn) flashStartedAt[k] = now
                if (!lightOn) {
                    flashStartedAt.remove(k)
                    continue
                }

                val flashStart = flashStartedAt[k]
                val boost = if (flashStart == null) 1f else {
                    val value = flashBoost(info, flashStart, now)
                    if (value <= 1.0001f) flashStartedAt.remove(k)
                    value
                }
                val strobe = if (mode == FlashlightMode.STROBE) {
                    strobeFactor(info, now - (modeStartedAt[k] ?: now))
                } else 1f

                if (strobe <= 0.001f) continue

                val sample = samples[k]?.takeIf { now - it.time in 0.0..1.0 }
                val rawOrigin = sample?.origin ?: player.eyePosition.add(player.lookAngle.scale(0.6))
                val origin = clampOrigin(player, rawOrigin)
                val direction = sample?.direction ?: player.lookAngle.normalize()

                val shadowThickness = 1000f
                val selfMask = 1.5f

                lights += GunMuzzleLightHandler.Light(
                    player,
                    0.0,
                    1.0,
                    info.safeIntensity * boost * strobe,
                    info.safeRange * rangeMultiplier,
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
        }

        return lights.sortedBy { it.position.distanceToSqr(camera) }.take(MAX_LIGHTS)
    }
}
