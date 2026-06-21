package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.overlay.GPWSOverlay.FORWARD_LOOK_DISTANCE
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo.Aircraft
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.localPlayer
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.HitResult
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import kotlin.math.max

/**
 * GPWS (Ground Proximity Warning System) 近地警告系统
 *
 * 为直升机和固定翼飞行器提供近地警告，在屏幕中央以红色文字显示警告类型，
 * 并预留对应警告的音效接口。
 * 警告触发条件针对MC短视距特点进行了合理调整。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
object GPWSOverlay : CommonOverlay("gpws") {

    /** 起飞后警告抑制时间 (tick) */
    private const val TAKEOFF_GRACE_TICKS = 100  // 5 秒

    /** 前方地形检测距离 */
    private const val FORWARD_LOOK_DISTANCE = 120.0

    /** 前方紧急碰撞距离 */
    private const val FORWARD_CRITICAL_DISTANCE = 70.0

    /** 前方检测时忽略的高度差（避免地面误报） */
    private const val FORWARD_VERTICAL_TOLERANCE = 6.0

    /**
     * GPWS 警告类型，按严重程度排序（priority 越高越严重）
     */
    enum class GPWSWarning(val priority: Int, val text: String) {
        NONE(0, ""),
        TOO_LOW_TERRAIN(1, "TOO LOW\nTERRAIN"),
        TOO_LOW_GEAR(2, "TOO LOW\nGEAR"),
        TERRAIN(3, "TERRAIN"),
        TERRAIN_AHEAD(4, "TERRAIN\nAHEAD"),
        SINK_RATE(5, "SINK RATE"),
        PULL_UP(6, "PULL UP")
    }

    // 闪烁计时器
    private var blinkTick = 0

    // 当前警告状态
    private var lastWarning = GPWSWarning.NONE

    // 起飞抑制计时器（离地后倒计时，期间抑制低优先级警告）
    private var takeoffGraceTicks = 0

    // 前一次是否在地面上
    private var wasOnGround = false

    // 音效冷却计时器 (tick)
    private val soundCooldowns = mutableMapOf<GPWSWarning, Int>()

    // 前方碰撞距离缓存（供渲染使用）
    private var forwardCollisionDistance = -1.0

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) return
        blinkTick++

        val player = localPlayer ?: return
        val vehicle = player.vehicle
        if (vehicle !is VehicleEntity) {
            resetState()
            return
        }
        if (!isAircraft(vehicle)) {
            resetState()
            return
        }

        // --- 起落 / 起飞抑制 ---
        val onGround = vehicle.onGround()
        if (onGround) {
            // 在地面时，持续重置抑制计时器
            takeoffGraceTicks = TAKEOFF_GRACE_TICKS
            forwardCollisionDistance = -1.0
        } else if (wasOnGround) {
            // 刚离地瞬间，开始倒计时
            takeoffGraceTicks = TAKEOFF_GRACE_TICKS
        } else if (takeoffGraceTicks > 0) {
            takeoffGraceTicks--
        }
        wasOnGround = onGround

        // 在地面上时，不发出任何警告
        if (onGround) {
            lastWarning = GPWSWarning.NONE
            return
        }

        // 更新音效冷却计时器
        soundCooldowns.forEach { (key, value) ->
            if (value > 0) soundCooldowns[key] = value - 1
        }

        // 前方地形碰撞检测
        forwardCollisionDistance = checkForwardTerrainCollision(vehicle)

        // 评估当前警告
        val currentWarning = evaluateWarning(vehicle, forwardCollisionDistance)
        lastWarning = currentWarning

        // 触发音效
        if (currentWarning != GPWSWarning.NONE) {
            triggerWarningSound(player, currentWarning)
        }
    }

    override fun RenderContext.render() {
        val vehicle = player.vehicle
        if (vehicle !is VehicleEntity) return
        if (!isAircraft(vehicle)) return
        if (vehicle.onGround()) return  // 地面不渲染

        val warning = evaluateWarning(vehicle, forwardCollisionDistance)
        if (warning == GPWSWarning.NONE) return

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        )
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        // 闪烁效果：高级别警告间歇显示
        val shouldRender = when (warning) {
            GPWSWarning.PULL_UP -> blinkTick % 8 < 6     // 75% 时间显示
            GPWSWarning.SINK_RATE -> blinkTick % 10 < 7  // 70% 时间显示
            GPWSWarning.TERRAIN_AHEAD -> blinkTick % 12 < 9 // 75% 时间显示
            GPWSWarning.TERRAIN -> blinkTick % 20 < 15   // 75% 时间显示
            else -> true
        }

        if (shouldRender) {
            val textColor = when (warning) {
                GPWSWarning.PULL_UP -> if (blinkTick % 8 < 4) 0xFFFF0000.toInt() else 0xFFAA0000.toInt()
                GPWSWarning.SINK_RATE -> if (blinkTick % 10 < 5) 0xFFFF0000.toInt() else 0xFFCC0000.toInt()
                GPWSWarning.TERRAIN_AHEAD -> if (blinkTick % 12 < 6) 0xFFFF6600.toInt() else 0xFFFF0000.toInt()
                else -> 0xFFFF0000.toInt()
            }

            // 渲染警告文字（支持多行）
            val lines = warning.text.split("\n")
            poseStack.pushPose()
            poseStack.translate(centerX.toDouble(), centerY.toDouble(), 0.0)
            poseStack.scale(1.25f, 1.25f, 1f)

            val font = mc.font
            val lineHeight = 10
            val totalHeight = lines.size * lineHeight
            val startY = -totalHeight / 2f + lineHeight / 2f

            for ((index, line) in lines.withIndex()) {
                val textWidth = font.width(line).toFloat()
                val scaledX = -textWidth / 2f
                val scaledY = startY + index * lineHeight

                guiGraphics.drawString(
                    font,
                    Component.literal(line),
                    scaledX.toInt(),
                    scaledY.toInt(),
                    textColor,
                    false
                )
            }

            poseStack.popPose()
        }

        poseStack.popPose()
    }

    override fun shouldRender(): Boolean {
        if (!super.shouldRender()) return false
        val player = localPlayer ?: return false
        val vehicle = player.vehicle ?: return false
        return vehicle is VehicleEntity && isAircraft(vehicle)
    }

    /**
     * 判断是否为飞行器（直升机或固定翼）
     */
    private fun isAircraft(vehicle: VehicleEntity): Boolean {
        val engineType = vehicle.computed().engineType
        return engineType == EngineType.HELICOPTER || engineType == EngineType.AIRCRAFT
    }

    /**
     * 判断是否为固定翼飞机且有起落架
     */
    private fun isFixedWingWithGear(vehicle: VehicleEntity): Boolean {
        if (vehicle.computed().engineType != EngineType.AIRCRAFT) return false
        val engineInfo = vehicle.engineInfo ?: return false
        return engineInfo is Aircraft && engineInfo.hasGear
    }

    /**
     * 获取离地高度（Above Ground Level）
     */
    fun getHeightAboveGround(vehicle: VehicleEntity): Double {
        val level = vehicle.level()
        val chunkX = vehicle.blockX shr 4
        val chunkZ = vehicle.blockZ shr 4
        if (!level.hasChunk(chunkX, chunkZ)) return Double.MAX_VALUE
        val groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, vehicle.blockX, vehicle.blockZ).toDouble()
        return max(0.0, vehicle.y - groundY)
    }

    /**
     * 前方地形碰撞检测
     *
     * 沿飞行器当前速度方向发射射线，检测前方 [FORWARD_LOOK_DISTANCE] 米内
     * 是否会撞上地形。只报告大致与飞行器同高度的障碍物。
     *
     * @return 碰撞距离（米），-1 表示无碰撞
     */
    private fun checkForwardTerrainCollision(vehicle: VehicleEntity): Double {
        val velocity = vehicle.deltaMovement
        val speed = velocity.length()
        if (speed < 2.0) return -1.0  // 速度太低时不检测（如悬停）

        val direction = velocity.normalize()
        // 如果飞行器在爬升，降低灵敏度（通常能飞越地形）
        val isClimbing = direction.y > 0.15

        val startPos = vehicle.position().add(0.0, vehicle.eyeHeight * 0.5, 0.0)
        val endPos = startPos.add(direction.scale(FORWARD_LOOK_DISTANCE))

        val clipContext = ClipContext(
            startPos, endPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            vehicle
        )
        val hitResult = vehicle.level().clip(clipContext)

        if (hitResult.type == HitResult.Type.BLOCK) {
            val hitPos = hitResult.location
            val distance = startPos.distanceTo(hitPos)

            // 忽略正下方的地面（障碍物必须大致在飞行器同高度或上方）
            val hitRelativeY = hitPos.y - vehicle.y
            if (hitRelativeY < -FORWARD_VERTICAL_TOLERANCE) {
                return -1.0  // 障碍物太低，是地面，不警告
            }

            // 爬升时，仅警告明显高于飞行器的障碍物
            if (isClimbing && hitRelativeY < 2.0) {
                return -1.0  // 爬升中足以飞越
            }

            return distance
        }

        return -1.0
    }

    /**
     * 评估当前应触发的警告
     */
    fun evaluateWarning(vehicle: VehicleEntity, forwardDist: Double = -1.0): GPWSWarning {
        val onGround = vehicle.onGround()
        if (onGround) return GPWSWarning.NONE

        val agl = getHeightAboveGround(vehicle)
        if (agl > 200) return GPWSWarning.NONE  // 安全高度

        // 起落架放下且姿态稳定（俯仰、滚转均 ≤15°）时不显示任何警告
        if (isStableLandingApproach(vehicle)) return GPWSWarning.NONE

        val verticalSpeed = vehicle.deltaMovement.y  // 负值 = 下降
        val isDescending = verticalSpeed < -0.5
        val isClimbing = verticalSpeed > 0.2
        val isLandingConfig = isInLandingConfig(vehicle)
        val inGracePeriod = takeoffGraceTicks > 0

        // ─── 1. PULL UP — 最高优先级 ───
        // 条件 A: 前方即将撞山
        if (forwardDist > 0 && forwardDist < FORWARD_CRITICAL_DISTANCE && !isClimbing) {
            return GPWSWarning.PULL_UP
        }
        // 条件 B: 极低高度 + 快速下降
        if ((agl < 120 && verticalSpeed < -1) || (agl < 12 && isDescending)) {
            return GPWSWarning.PULL_UP
        }

        // ─── 2. SINK RATE — 下降率过高 ───
        if (verticalSpeed < -1.5) {
            return GPWSWarning.SINK_RATE
        }

        // ─── 3. TERRAIN AHEAD — 前方地形警告 ───
        if (forwardDist > 0 && forwardDist < FORWARD_LOOK_DISTANCE && !isClimbing) {
            return GPWSWarning.TERRAIN_AHEAD
        }

        // ─── 4. TERRAIN — 未处于着陆构型且接近地面（起飞抑制期间跳过） ───
        if (!inGracePeriod && agl < 16 && !isLandingConfig && isDescending) {
            return GPWSWarning.TERRAIN
        }

        // ─── 5. TOO LOW, GEAR — 仅固定翼，起落架未放下（起飞抑制期间跳过） ───
        if (!inGracePeriod && isFixedWingWithGear(vehicle) && agl < 8 && vehicle.gearUp && !isClimbing) {
            return GPWSWarning.TOO_LOW_GEAR
        }

        // ─── 6. TOO LOW, TERRAIN — 最低优先级（起飞抑制/着陆构型时跳过） ───
        if (!inGracePeriod && agl < 8 && !isClimbing && !isLandingConfig) {
            return GPWSWarning.TOO_LOW_TERRAIN
        }

        return GPWSWarning.NONE
    }

    /**
     * 判断是否处于着陆构型：
     * - 固定翼：起落架已放下 (gearUp == false)
     * - 直升机：悬停模式开启 (hoverMode == true)
     */
    fun isInLandingConfig(vehicle: VehicleEntity): Boolean {
        return when (vehicle.computed().engineType) {
            EngineType.AIRCRAFT -> !vehicle.gearUp
            EngineType.HELICOPTER -> vehicle.hoverMode
            else -> true
        }
    }

    /**
     * 判断是否处于稳定进近状态（起落架放下 + 姿态稳定），
     * 此状态下应抑制所有近地警告，避免正常着陆时产生干扰。
     *
     * 条件：
     * 1. 仅对固定翼飞机生效
     * 2. 起落架已放下 (gearUp == false)
     * 3. 俯仰角 (xRot) 绝对值 ≤ 15°
     * 4. 滚转角 (roll) 绝对值 ≤ 15°
     */
    private fun isStableLandingApproach(vehicle: VehicleEntity): Boolean {
        if (!isFixedWingWithGear(vehicle)) return false
        if (vehicle.gearUp) return false
        if (kotlin.math.abs(vehicle.xRot) > 15f) return false
        if (kotlin.math.abs(vehicle.roll) > 15f) return false
        return true
    }

    // ==================== 音效接口（预留） ====================

    /**
     * 触发警告音效
     */
    private fun triggerWarningSound(player: Player, warning: GPWSWarning) {
        val cooldown = soundCooldowns[warning] ?: 0
        if (cooldown > 0) return

        when (warning) {
            GPWSWarning.PULL_UP -> {
                playPullUpSound(player)
                soundCooldowns[warning] = 30  // 1.5秒冷却
            }
            GPWSWarning.SINK_RATE -> {
                playSinkRateSound(player)
                soundCooldowns[warning] = 40
            }
            GPWSWarning.TERRAIN_AHEAD -> {
                playTerrainAheadSound(player)
                soundCooldowns[warning] = 50
            }
            GPWSWarning.TERRAIN -> {
                playTerrainSound(player)
                soundCooldowns[warning] = 60
            }
            GPWSWarning.TOO_LOW_GEAR -> {
                playTooLowGearSound(player)
                soundCooldowns[warning] = 60
            }
            GPWSWarning.TOO_LOW_TERRAIN -> {
                playTooLowTerrainSound(player)
                soundCooldowns[warning] = 60
            }
            else -> {}
        }
    }

    /** PULL UP 警告音效 — 最紧急 */
    private fun playPullUpSound(player: Player) {
         player.playSound(ModSounds.GPWS_PULL_UP.get(), 2.0f, 1.0f)
    }

    /** SINK RATE 警告音效 — 下降率过高 */
    private fun playSinkRateSound(player: Player) {
         player.playSound(ModSounds.GPWS_SINK_RATE.get(), 2.0f, 1.0f)
    }

    /** TERRAIN AHEAD 警告音效 — 前方地形 */
    private fun playTerrainAheadSound(player: Player) {
         player.playSound(ModSounds.GPWS_TERRAIN_AHEAD.get(), 2.0f, 1.0f)
    }

    /** TERRAIN 地形警告音效 */
    private fun playTerrainSound(player: Player) {
         player.playSound(ModSounds.GPWS_TERRAIN.get(), 2.0f, 1.0f)
    }

    /** TOO LOW GEAR 起落架警告音效 */
    private fun playTooLowGearSound(player: Player) {
         player.playSound(ModSounds.GPWS_TOO_LOW_GEAR.get(), 2.0f, 1.0f)
    }

    /** TOO LOW TERRAIN 过低警告音效 */
    private fun playTooLowTerrainSound(player: Player) {
         player.playSound(ModSounds.GPWS_TOO_LOW_TERRAIN.get(), 2.0f, 1.0f)
    }

    private fun resetState() {
        lastWarning = GPWSWarning.NONE
        takeoffGraceTicks = 0
        wasOnGround = false
        forwardCollisionDistance = -1.0
        soundCooldowns.clear()
    }

    init {
        GPWSWarning.entries.forEach { soundCooldowns[it] = 0 }
    }
}
