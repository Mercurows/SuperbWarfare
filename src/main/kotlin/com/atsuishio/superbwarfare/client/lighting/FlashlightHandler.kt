package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.config.client.DisplayConfig
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
import net.minecraft.Util
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
    private const val ANTI_FLASH_FADE_MILLIS = 200f
    private const val ANTI_FLASH_STROBE_SCALE = 0.25f
    private const val ANTI_FLASH_STROBE_MAX_INTENSITY = 0.5f

    data class Emitter(val key: String, val showEffects: Boolean)

    private data class Sample(val gun: Any, val origin: Vec3, val direction: Vec3)

    private val worldSamples = mutableMapOf<String, Sample>()
    private var handSamples = mutableMapOf<String, Sample>()
    private var previousHandSamples = mutableMapOf<String, Sample>()

    /** 每件配件上一帧的档位，用于检测切换（开灯眩光 / 爆闪相位起点）。 */
    private val lastModes = mutableMapOf<String, FlashlightMode>()
    private val flashStartedAt = mutableMapOf<String, Double>()
    private val modeStartedAt = mutableMapOf<String, Double>()

    private class BrightnessTransition(val gun: Any, val info: FlashlightInfo, now: Long) {
        private var start = 0f
        private var target = 0f
        private var startedAt = now
        var seenFrame = 0L

        fun intensity(nextTarget: Float, now: Long): Float {
            val progress = ((now - startedAt) / ANTI_FLASH_FADE_MILLIS).coerceIn(0f, 1f)
            val fraction = progress * progress * (3f - 2f * progress)
            val current = start + (target - start) * fraction
            if (target != nextTarget) {
                start = current
                target = nextTarget
                startedAt = now
            }
            return current
        }
    }

    private val brightnessTransitions = mutableMapOf<String, BrightnessTransition>()
    private var brightnessFrame = 0L

    private const val TICKS_PER_SECOND = 20.0

    private fun key(uuid: String, slot: AttachmentType): String = "$uuid|${slot.name}"

    private fun FlashlightMode.emitsLight(): Boolean = this == FlashlightMode.ON || this == FlashlightMode.STROBE

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

    private fun regularIntensity(key: String, mode: FlashlightMode, info: FlashlightInfo, now: Double): Float {
        val previous = lastModes.put(key, mode) ?: mode
        if (previous != mode || key !in modeStartedAt) modeStartedAt[key] = now
        if (!mode.emitsLight()) {
            flashStartedAt.remove(key)
            return 0f
        }
        if (!previous.emitsLight()) flashStartedAt[key] = now

        val flashStart = flashStartedAt[key]
        val boost = if (flashStart == null) 1f else {
            val value = flashBoost(info, flashStart, now)
            if (value <= 1.0001f) flashStartedAt.remove(key)
            value
        }
        val strobe = if (mode == FlashlightMode.STROBE) {
            strobeFactor(info, now - (modeStartedAt[key] ?: now))
        } else 1f
        return if (strobe > 0.001f) info.safeIntensity * boost * strobe else 0f
    }

    private fun antiFlashIntensity(
        key: String, gun: Any, mode: FlashlightMode, info: FlashlightInfo, now: Long,
    ): Float {
        val target = when (mode) {
            FlashlightMode.ON -> info.safeIntensity
            FlashlightMode.STROBE -> minOf(info.safeIntensity * ANTI_FLASH_STROBE_SCALE, ANTI_FLASH_STROBE_MAX_INTENSITY)
            else -> 0f
        }
        val previous = brightnessTransitions[key]?.takeIf { it.gun == gun && it.info == info }
        val transition = previous ?: if (target > 0f) {
            BrightnessTransition(gun, info, now).also { brightnessTransitions[key] = it }
        } else return 0f
        transition.seenFrame = brightnessFrame
        val intensity = transition.intensity(target, now)
        if (target == 0f && intensity == 0f) brightnessTransitions.remove(key)
        return intensity
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
    fun sample(
        holder: LivingEntity, stack: ItemStack, slot: AttachmentType, origin: Vec3, direction: Vec3,
        firstPerson: Boolean,
    ) {
        val gun = gunOf(stack) ?: return
        val mode = AttachmentLightState.modeOf(gun, slot)
        val key = key(holder.stringUUID, slot)
        val gunIdentity = gun.uuid ?: stack
        if (!mode.emitsLight() && brightnessTransitions[key]?.gun != gunIdentity) return
        val dir = direction.normalize()
        if (dir.lengthSqr() < 1e-6) return
        val samples = if (firstPerson) handSamples else worldSamples
        samples[key] = Sample(gunIdentity, origin, dir)
    }

    fun beginFrame() {
        worldSamples.clear()
        val reusable = previousHandSamples
        previousHandSamples = handSamples
        handSamples = reusable
        handSamples.clear()
    }

    @JvmStatic
    fun clear() {
        worldSamples.clear()
        handSamples.clear()
        previousHandSamples.clear()
        lastModes.clear()
        flashStartedAt.clear()
        modeStartedAt.clear()
        brightnessTransitions.clear()
    }

    private fun colorVec(rgb: Int): Vec3 = Vec3(
        ((rgb shr 16) and 0xFF) / 255.0,
        ((rgb shr 8) and 0xFF) / 255.0,
        (rgb and 0xFF) / 255.0,
    )

    private fun eyePosition(holder: LivingEntity): Vec3 {
        val mc = Minecraft.getInstance()
        return if (isLocalHand(holder, mc)) {
            mc.gameRenderer.mainCamera.position
        } else {
            holder.getEyePosition(mc.frameTime)
        }
    }

    private fun isLocalHand(holder: LivingEntity, mc: Minecraft): Boolean =
        holder === mc.player && holder === mc.cameraEntity && mc.options.cameraType.isFirstPerson

    private fun clampOrigin(holder: LivingEntity, desired: Vec3): Vec3 {
        val level = Minecraft.getInstance().level ?: return desired
        val eye = eyePosition(holder)
        val length = eye.distanceTo(desired)
        if (length <= MIN_FROM_EYE) return desired

        val hit = level.clip(
            ClipContext(eye, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, holder)
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
        val antiFlash = DisplayConfig.ANTI_FLASH_MODE.get()
        val fadeTime = if (antiFlash) Util.getMillis() else 0L
        if (antiFlash) {
            brightnessFrame++
            lastModes.clear()
            flashStartedAt.clear()
            modeStartedAt.clear()
        } else {
            brightnessTransitions.clear()
        }
        val lights = ArrayList<GunMuzzleLightHandler.Light>(MAX_LIGHTS)

        for (player in level.players()) {
            if (player.position().distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) continue

            val stack = player.mainHandItem
            val gun = gunOf(stack) ?: continue
            val gunIdentity = gun.uuid ?: stack
            val firstPerson = isLocalHand(player, mc)
            val emitters = AttachmentDefinition.findFlashlights(gun)
            if (emitters.isEmpty()) continue

            // 多手电联动增益：同一把枪上每多开一个手电（ON/STROBE），所有手电射程 +50%。
            // 只算"开着"的手电，激光档/关闭档不参与计数。
            val modes = emitters.associateWith { AttachmentLightState.modeOf(gun, it.slot.type) }
            val activeCount = modes.values.count { it.emitsLight() }
            val rangeMultiplier = 1f + 0.5f * (activeCount - 1).coerceAtLeast(0)

            for ((emitter, mode) in modes) {
                val uuid = player.stringUUID
                val slot = emitter.slot.type
                val k = key(uuid, slot)
                val info = emitter.info
                if (info.safeIntensity <= 0f) continue

                val intensity = if (antiFlash) antiFlashIntensity(k, gunIdentity, mode, info, fadeTime)
                else regularIntensity(k, mode, info, now)
                if (intensity <= 0f) continue

                val sample = if (firstPerson) handSamples[k] ?: previousHandSamples[k] else worldSamples[k]
                val mount = sample?.takeIf { it.gun == gunIdentity }
                val look = player.getViewVector(mc.frameTime)
                val rawOrigin = mount?.origin ?: eyePosition(player).add(look.scale(0.6))
                val origin = clampOrigin(player, rawOrigin)

                lights += GunMuzzleLightHandler.Light(
                    player,
                    0.0,
                    1.0,
                    intensity,
                    info.safeRange * rangeMultiplier,
                    origin,
                    mount?.direction ?: look.normalize(),
                    colorVec(info.colorRgb()),
                    info.outerCos(),
                    info.innerCos(),
                    steady = true,
                    flashlight = Emitter(k, !firstPerson && mount != null && origin == rawOrigin),
                )
            }
        }

        brightnessTransitions.values.removeIf { it.seenFrame != brightnessFrame }
        return lights.sortedBy { it.position.distanceToSqr(camera) }.take(MAX_LIGHTS)
    }
}
