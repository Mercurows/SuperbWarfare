package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay
import com.atsuishio.superbwarfare.client.shader.ThermalShaderHandler
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.*
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.item.gun.launcher.SuperStarShooterItem
import com.atsuishio.superbwarfare.item.misc.MonitorItem
import com.atsuishio.superbwarfare.network.message.send.*
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData
import net.minecraft.ChatFormatting
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.npc.AbstractVillager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.level.block.CrossCollisionBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.client.event.*
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import net.neoforged.neoforge.common.util.TriState
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW
import software.bernie.geckolib.animation.AnimationProcessor
import software.bernie.geckolib.cache.`object`.GeoBone
import top.theillusivec4.curios.api.CuriosApi
import java.util.*
import kotlin.experimental.or
import kotlin.math.*

@EventBusSubscriber(Dist.CLIENT)
object ClientEventHandler {
    @JvmField
    var zoomTime: Double = 0.0

    @JvmField
    var zoomPos: Double = 0.0

    @JvmField
    var zoomPosZ: Double = 0.0

    @JvmField
    var swayTime: Double = 0.0

    @JvmField
    var swayX: Double = 0.0

    @JvmField
    var swayY: Double = 0.0

    @JvmField
    var moveTime: Double = 0.0

    @JvmField
    var sprintTime: Double = 0.0

    @JvmField
    var movePosX: Double = 0.0

    @JvmField
    var movePosY: Double = 0.0

    @JvmField
    var moveRotZ: Double = 0.0

    @JvmField
    var sprintBasicRotX: Double = 0.0

    @JvmField
    var sprintBasicRotY: Double = 0.0

    @JvmField
    var sprintBasicRotZ: Double = 0.0

    @JvmField
    var sprintPosX: Double = 0.0

    @JvmField
    var sprintPosY: Double = 0.0

    @JvmField
    var sprintBasicPosX: Double = 0.0

    @JvmField
    var sprintBasicPosY: Double = 0.0

    @JvmField
    var sprintBasicPosZ: Double = 0.0

    @JvmField
    var movePosHorizon: Double = 0.0

    @JvmField
    var velocityY: Double = 0.0

    @JvmField
    var turnRot = doubleArrayOf(0.0, 0.0, 0.0)

    @JvmField
    var cameraRot = doubleArrayOf(0.0, 0.0, 0.0)

    @JvmField
    var fireRecoilTime: Double = 0.0

    @JvmField
    var firePosTimer: Double = 0.0

    @JvmField
    var fireRotTimer: Double = 0.0

    @JvmField
    var boltMove: Double = 0.0

    @JvmField
    var firePosZ: Double = 0.0

    @JvmField
    var customAnimSpeed: Double = 1.0

    @JvmField
    var recoilHorizon: Double = 0.0

    @JvmField
    var recoilY: Double = 0.0

    @JvmField
    var recoilForce: Double = 0.0

    @JvmField
    var droneFov: Double = 1.0

    @JvmField
    var droneFovLerp: Double = 1.0

    @JvmField
    var currentFov: Double = 0.0

    @JvmField
    var bowPullTimer: Double = 0.0

    @JvmField
    var bowPower: Double = 0.0

    @JvmField
    var bowPullPos: Double = 0.0

    @JvmField
    var gunSpread: Double = 0.0

    @JvmField
    var fireSpread: Double = 0.0

    @JvmField
    var fireCooldown: Double = 0.0

    @JvmField
    var lookDistance: Double = 0.0

    @JvmField
    var cameraLocation: Double = 0.6

    // 切换载具武器的冷却时间
    @JvmField
    var switchVehicleWeaponCooldown: Int = 0

    @JvmField
    var drawTime: Double = 1.0

    @JvmField
    var shellIndex: Int = 0

    @JvmField
    var shellIndexTime = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    @JvmField
    var randomShell = doubleArrayOf(0.0, 0.0, 0.0)

    @JvmField
    var customZoom: Double = 0.0

    @JvmField
    var artilleryIndicatorZoom: Double = 1.0

    @JvmField
    var artilleryIndicatorCustomZoom: Double = 0.0

    @JvmField
    var clientTimer: MillisTimer = MillisTimer()

    @JvmField
    var clientTimerVehicle: MillisTimer = MillisTimer()

    // 正在按住开火键
    @JvmField
    var holdingFireKey: Boolean = false

    @JvmField
    var bowPull: Boolean = false

    @JvmField
    var zoom: Boolean = false

    @JvmField
    var breath: Boolean = false

    @JvmField
    var stamina: Float = 0f

    @JvmField
    var switchTime: Double = 0.0

    @JvmField
    var moveFadeTime: Double = 0.0

    @JvmField
    var sprintFadeTime: Double = 0.0

    @JvmField
    var exhaustion: Boolean = false

    @JvmField
    var holdFireVehicle: Boolean = false

    @JvmField
    var zoomVehicle: Boolean = false

    @JvmField
    var burstFireAmount: Int = 0

    @JvmField
    var customRpm: Int = 0

    @JvmField
    var gunMelee: Int = 0

    // 按住开火键的持续tick
    @JvmField
    var holdingFireKeyTicks: Int = 0

    @JvmField
    var holdingFireKeyTicks0: Float = 0f

    @JvmField
    var shouldPlayDischargeSound: Boolean = true

    @JvmField
    var revolverPreTime: Double = 0.0

    @JvmField
    var revolverWheelPreTime: Double = 0.0

    @JvmField
    var shakeTime: Double = 0.0

    @JvmField
    var shakeRadius: Double = 0.0

    @JvmField
    var shakeAmplitude: Double = 0.0

    @JvmField
    var shakePos = doubleArrayOf(0.0, 0.0, 0.0)

    @JvmField
    var shakeType: Double = 0.0

    @JvmField
    var lerpShake: Double = 0.0

    @JvmField
    var usingLunge: Boolean = false

    @JvmField
    var lungeAttack: Int = 0

    @JvmField
    var lungeDraw: Int = 0

    @JvmField
    var lungeSprint: Int = 0

    // 智慧芯片锁定的实体
    @JvmField
    var lockedEntity: Entity? = null

    @JvmField
    var dismountCountdown: Int = 0

    @JvmField
    var aimVillagerCountdown: Int = 0

    @JvmField
    var lastCameraType: CameraType? = null

    @JvmField
    var cameraPitch: Float = 0f

    @JvmField
    var cameraYaw: Float = 0f

    @JvmField
    var cameraRoll: Float = 0f

    // 禁止冲刺♿时长tick
    @JvmField
    var noSprintTicks: Float = 0f

    @JvmField
    var canDoubleJump: Boolean = false

    @JvmField
    var holdArtilleryIndicator: Int = 0

    @JvmField
    var holdToEjection: Int = 0

    @JvmField
    var isEditing: Boolean = false

    @JvmField
    var shootCoolDown: Int = 0

    // 锁定类武器用
    @JvmField
    var nearestEntity: Entity? = null

    @JvmField
    var seekingEntity: Entity? = null

    @JvmField
    var lockingEntity: Entity? = null

    @JvmField
    var seekingPos: Vec3? = null

    @JvmField
    var lockingPos: Vec3? = null

    @JvmField
    var seekingTime: Int = 0

    @JvmField
    var guideType: Int = 0

    @JvmField
    var lockOn: Boolean = false

    // 锁定类载具用
    @JvmField
    var nearestEntityVehicle: Entity? = null

    @JvmField
    var seekingEntityVehicle: Entity? = null

    @JvmField
    var lockingEntityVehicle: Entity? = null

    @JvmField
    var seekingPosVehicle: Vec3? = null

    @JvmField
    var lockingPosVehicle: Vec3? = null

    @JvmField
    var seekingTimeVehicle: Int = 0

    @JvmField
    var lockOnVehicle: Boolean = false

    @JvmField
    var lastOperatingGunUUID: UUID? = null

    @JvmField
    var keysCache: Short = 0

    @JvmField
    var tdmSavedData: TDMSavedData = TDMSavedData()

    @JvmField
    var activeThermalImaging: Boolean = false

    // 原VectorUtil的属性
    @JvmField
    var fov: Double = 70.0

    @JvmField
    var modelViewMatrix: Matrix4f? = null

    @JvmField
    var projectionMatrix: Matrix4f? = null

    private var lastX: Float = 0f
    private var lastY: Float = 0f

    @JvmField
    var bombHitPosO: Vec3 = Vec3.ZERO

    @JvmField
    var bombHitPos: Vec3 = Vec3.ZERO

    @SubscribeEvent
    fun handleWeaponTurn(event: RenderHandEvent) {
        val player = localPlayer ?: return
        val xRotOffset = Mth.lerp(event.partialTick, player.xBobO, player.xBob)
        val yRotOffset = Mth.lerp(event.partialTick, player.yBobO, player.yBob)
        val xRot = player.getViewXRot(event.partialTick) - xRotOffset
        val yRot = player.getViewYRot(event.partialTick) - yRotOffset
        turnRot[0] = (0.05 * xRot).coerceIn(-5.0, 5.0) * (1 - 0.75 * zoomTime)
        turnRot[1] = (0.05 * yRot).coerceIn(-10.0, 10.0) * (1 - 0.75 * zoomTime)
        turnRot[2] = (0.1 * yRot).coerceIn(-10.0, 10.0) * (1 - zoomTime)
    }

    @JvmStatic
    fun isFreeCam(player: Player): Boolean {
        val vehicle = player.vehicle
        return vehicle is VehicleEntity && vehicle.allowFreeCam() && ModKeyMappings.FREE_CAMERA.isDown()
    }

    private fun isMoving(): Boolean {
        val player = localPlayer ?: return false
        return mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown()
                || mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || player.isSprinting
    }

    @SubscribeEvent
    fun handleClientTick(event: ClientTickEvent.Post) {
        val player = localPlayer ?: return

        val stack = player.mainHandItem
        if (notInGame && !ClickEventHandler.switchZoom) {
            zoom = false
        }

        if (player.onGround() && canDoubleJump) {
            canDoubleJump = false
        }

        recoilForce *= 0.55

        ClientSyncedEntityHandler.clean()
        isProne(player)
        handleVariableDecrease()
        aimAtVillager(player)
        CrossHairOverlay.handleRenderDamageIndicator()
        staminaSystem()
        handlePlayerSprint()
        handleLungeAttack(player, stack)
        handleGunMelee(player, stack)
        weaponZooming(stack)
        lockWeaponSeeking(player, stack)
        vehicleWeaponSeeking(player)
        handleThermalImaging(player)
        handleShootDelay(player, stack)
        handleControlVehicle(player, stack)
        handleArtilleryIndicator(player, stack)
        calculateBombHitPos(player)
    }

    @JvmStatic
    fun hasThermalImagingGoggles(): Boolean {
        return CuriosApi.getCuriosInventory(localPlayer).map {
            it.findFirstCurio(ModItems.THERMAL_IMAGING_GOGGLES.get()).isPresent
        }.orElse(false)
    }

    fun handleThermalImaging(player: Player) {
        var hasThermalImagingGoggles = hasThermalImagingGoggles()
        val vehicle = player.vehicle

        if (vehicle is VehicleEntity) {
            val index = vehicle.getSeatIndex(player)
            if (index != -1) {
                val seat = vehicle.computed().seats().getOrNull(index)
                if (seat != null && seat.hasThermalImaging) {
                    hasThermalImagingGoggles = true
                }
            }
        }

        if (!activeThermalImaging || !hasThermalImagingGoggles) {
            activeThermalImaging = false
            turnOffThermalImaging()
        } else if (Minecraft.getInstance().gameRenderer.currentEffect() == null) {
            turnOnThermalImaging()
        }

        val active = player.getData(ModAttachments.PLAYER_VARIABLE).activeThermalImaging

        if (activeThermalImaging && !active) {
            sendPacketToServer(ActiveThermalImagingMessage(true))
        }

        if (active && !activeThermalImaging) {
            sendPacketToServer(ActiveThermalImagingMessage(false))
        }
    }

    @JvmStatic
    fun turnOnThermalImaging() {
        ThermalShaderHandler.setActive(true)
        mc.gameRenderer.loadEffect(Mod.loc("shaders/post/night_vision.json"))
    }

    @JvmStatic
    fun turnOffThermalImaging() {
        mc.gameRenderer.shutdownEffect()
        ThermalShaderHandler.setActive(false)
    }

    /**
     *  处理武器射击延迟
     */
    fun handleShootDelay(player: Player, stack: ItemStack) {
        val item = stack.item
        if (item is GunItem) {
            val data = GunData.from(stack)

            var uuid: UUID? = null
            try {
                uuid = data.gunDataTag.getUUID("UUID")
            } catch (_: Exception) {
            }

            if (notInGame) {
                burstFireAmount = 0
            }

            // 切枪时记得重置状态
            if (uuid == null || uuid != lastOperatingGunUUID) {
                resetGunStatus()
                resetLungeMineStatus()
            }
            lastOperatingGunUUID = uuid

            if ((holdingFireKey || (zoom && stack.`is`(ModItems.MINIGUN.get()))) && item.canShoot(data, player)) {
                holdingFireKeyTicks = (holdingFireKeyTicks + 1).coerceAtMost(data.get(GunProp.SHOOT_DELAY) + 1)

                // 加特林特有的旋转音效
                if (stack.`is`(ModItems.MINIGUN.get())) {
                    val rpm = data.get(GunProp.RPM) / 3600F
                    player.playSound(ModSounds.MINIGUN_ROTATE.get(), 1f, 0.7f + rpm)
                }

                // QL特有的樱花特效
                if (stack.`is`(ModItems.QL_1031.get()) && player.tickCount % 5 == 0) {
                    val random = (Math.random() - 0.5) * 2
                    player.level().addParticle(
                        ParticleTypes.CHERRY_LEAVES,
                        player.x + random,
                        player.eyeY + 0.5 * random,
                        player.z + random,
                        0.0,
                        0.0,
                        0.0
                    )
                }
            }
        } else {
            lastOperatingGunUUID = null
        }
    }

    fun handleArtilleryIndicator(player: Player, stack: ItemStack) {
        if ((stack.`is`(ModItems.ARTILLERY_INDICATOR.get()) || (stack.`is`(ModItems.MONITOR.get())
                    && player.offhandItem.`is`(ModItems.ARTILLERY_INDICATOR.get()))) && holdingFireKey
        ) {
            holdArtilleryIndicator = (holdArtilleryIndicator + 1).coerceIn(0, 20)
            if (holdArtilleryIndicator >= 19 && shootCoolDown == 0) {
                sendPacketToServer(ArtilleryIndicatorFireMessage)
                shootCoolDown = 10
            }
        } else {
            holdArtilleryIndicator = 0
        }

        if (shootCoolDown > 0) {
            shootCoolDown--
        }
    }

    fun calculateBombHitPos(player: Player) {
        val vehicle = player.vehicle as? VehicleEntity ?: return
        val gunData = vehicle.getGunData(player)

        bombHitPosO = bombHitPos
        bombHitPos = if (gunData != null && gunData.get(GunProp.CROSSHAIR) == "@AirBomb") {
            vehicle.bombHitPos(player)
        } else {
            Vec3.ZERO
        }
    }

    fun handleControlVehicle(player: Player, stack: ItemStack) {
        val tag = NBTTool.getTag(stack)

        var keys: Short = 0
        val vehicle = player.vehicle

        // 正在游戏内控制载具或无人机
        if (!notInGame && (vehicle is VehicleEntity && vehicle.firstPassenger == player) ||
            (stack.`is`(ModItems.MONITOR.get()) && tag != null
                    && tag.getBoolean(MonitorItem.USING)
                    && tag.getBoolean(MonitorItem.LINKED))
        ) {
            if (ModKeyMappings.MOVE_LEFT.isDown) {
                keys = keys or 0b000000001
            }
            if (ModKeyMappings.MOVE_RIGHT.isDown) {
                keys = keys or 0b000000010
            }
            if (ModKeyMappings.MOVE_FORWARD.isDown) {
                keys = keys or 0b000000100
            }
            if (ModKeyMappings.MOVE_BACKWARD.isDown) {
                keys = keys or 0b000001000
            }
            if (ModKeyMappings.MOVE_SPACE.isDown) {
                keys = keys or 0b000010000
            }
            if (ModKeyMappings.MOVE_SHIFT.isDown) {
                keys = keys or 0b000100000
            }
            if (ModKeyMappings.RELEASE_DECOY.isDown) {
                keys = keys or 0b001000000
            }
            if (holdFireVehicle) {
                keys = keys or 0b010000000
            }
            if (ModKeyMappings.MOVE_CTRL.isDown) {
                keys = keys or 0b100000000
            }
        }

        if (keys != keysCache) {
            sendPacketToServer(VehicleMovementMessage(keys))
            keysCache = keys
        }

        if (vehicle is VehicleEntity && vehicle.allowEjection(vehicle.getSeatIndex(player)) && ModKeyMappings.DISMOUNT.isDown()) {
            holdToEjection = (holdToEjection + 1).coerceIn(0, 10)
            if (holdToEjection >= 10) {
                sendPacketToServer(PlayerStopRidingMessage(true))
                stopVehicleReloadSound(player)
            }
        } else {
            holdToEjection = 0
        }
    }

    fun lockWeaponSeeking(player: Player, stack: ItemStack) {
        val item = stack.item
        if (item is GunItem) {
            val data = GunData.from(stack)
            val lockTime = data.get(GunProp.SEEK_TIME)
            // 搜寻角度
            val fovAdjust = mc.options.fov().get() / 80f
            val seekAngle = data.get(GunProp.SEEK_ANGLE) * fovAdjust
            val range = data.get(GunProp.SEEK_RANGE)
            val maxGuidedRange = data.get(GunProp.MAX_GUIDED_RANGE)
            val canGuidedByRadar = data.get(GunProp.CAN_GUIDED_BY_RADAR)
            val affectedByStealthTarget = data.get(GunProp.AFFECTED_BY_STEALTH_TARGET)
            val cameraPos = mc.gameRenderer.mainCamera.position

            if (zoomTime > 0.7) {
                nearestEntity = SeekTool.Builder(player)
                    .withinRangeSeekWeapon(range, maxGuidedRange, affectedByStealthTarget, canGuidedByRadar)
                    .withinAngle(seekAngle)
                    .baseFilter()
                    .heightRange(data.get(GunProp.MIN_TARGET_HEIGHT), data.get(GunProp.MAX_TARGET_HEIGHT))
                    .smokeFilter()
                    .noVehicle()
                    .noClip()
                    .buildWithClosestSeekWeapon(canGuidedByRadar)

                val decoy = TraceTool.findLookDecoy(player, cameraPos, player.getViewVector(1f), range)
                if (decoy != null && decoy.type.`is`(ModTags.EntityTypes.DECOY)) {
                    nearestEntity = decoy
                    seekFailure(player)
                }

                if (data.get(GunProp.SEEK_TYPE) == SeekType.HOLD_FIRE) {
                    if (nearestEntity == null || player.isShiftKeyDown) {
                        // 锁定方块
                        val result = player.level().clip(
                            ClipContext(
                                player.eyePosition,
                                player.eyePosition.add(player.getViewVector(1f).scale(512.0)),
                                ClipContext.Block.VISUAL,
                                ClipContext.Fluid.ANY,
                                player
                            )
                        )
                        seekingPos = result.location

                        if (seekingTime > lockTime + 2 && !lockOn) {
                            lockOn = true
                        }

                        //锁定失败
                        if (lockingPos != null &&
                            (player.lookAngle.angleTo(
                                player.eyePosition.vectorTo(lockingPos!!)
                            ) > seekAngle || !noClip(player, lockingPos!!))
                        ) {
                            seekingTime = 0
                            seekFailure(player)
                        }

                        if (holdingFireKey) {
                            if (seekingPos != null && seekingPos!!.distanceToSqr(player.eyePosition) < range * range) {
                                seekingTime++
                                if (seekingTime == 1) {
                                    lockingPos = seekingPos
                                }
                            } else {
                                seekingTime = 0
                                lockingPos = null
                            }
                            guideType = 1
                        } else {
                            if (lockOn) {
                                if (lockingPos != null) {
                                    sendPacketToServer(ShootMessage(gunSpread, zoom, null, lockingPos!!.toVector3f()))
                                }
                                lockOn = false
                            }
                            seekFailure(player)
                        }
                    } else {
                        // 锁定实体
                        if (seekingTime > lockTime + 2 && !lockOn) {
                            lockingEntity = seekingEntity
                            lockOn = true
                        }

                        //锁定失败
                        if (seekingEntity != null && (
                                    player.lookAngle.angleTo(
                                        player.eyePosition.vectorTo(
                                            VectorTool.lerpGetEntityBoundingBoxCenter(
                                                seekingEntity!!,
                                                1f
                                            )
                                        )
                                    ) > seekAngle || !SeekTool.NOT_IN_SMOKE.test(seekingEntity) || !noClip(
                                        player,
                                        seekingEntity!!
                                    ))
                        ) {
                            seekFailure(player)
                        }

                        if (holdingFireKey) {
                            if (seekingEntity == null) {
                                seekingEntity = nearestEntity
                            }
                            if (nearestEntity != null && lockingPos == null) {
                                seekingTime++
                                if ((!seekingEntity!!.passengers.isEmpty() || seekingEntity is VehicleEntity)
                                    && player.tickCount % 3 == 0 && !lockOn
                                ) {
                                    sendPacketToServer(
                                        SeekingWeaponWarningMessage(
                                            false,
                                            seekingEntity!!.uuid
                                        )
                                    )
                                }
                                guideType = 0
                            }
                        } else {
                            if (lockOn) {
                                if (lockingEntity != null) {
                                    sendPacketToServer(
                                        ShootMessage(
                                            gunSpread,
                                            zoom,
                                            lockingEntity!!.uuid,
                                            lockingEntity!!.eyePosition.toVector3f()
                                        )
                                    )
                                }
                                lockOn = false
                            }
                            seekFailure(player)
                        }
                    }
                } else if (data.get(GunProp.SEEK_TYPE) == SeekType.HOLD_ZOOM) {
                    // 瞄准锁定只能锁实体
                    if (seekingTime > lockTime + 2 && !lockOn) {
                        lockingEntity = seekingEntity
                        lockOn = true
                    }

                    // 锁定失败
                    if (seekingEntity != null && (player.lookAngle.angleTo(
                            player.eyePosition
                                .vectorTo(VectorTool.lerpGetEntityBoundingBoxCenter(seekingEntity!!, 1f))
                        ) > seekAngle || !SeekTool.NOT_IN_SMOKE.test(seekingEntity) || !noClip(
                            player,
                            seekingEntity!!
                        ))
                    ) {
                        seekFailure(player)
                    }

                    if (zoomTime > 0.7) {
                        if (seekingEntity == null) {
                            seekingEntity = nearestEntity
                        }
                        if (nearestEntity != null && data.hasEnoughAmmoToShoot(player)) {
                            seekingTime++
                            if ((!seekingEntity!!.passengers.isEmpty()
                                        || seekingEntity is VehicleEntity) && player.tickCount % 3 == 0 && !lockOn
                            ) {
                                sendPacketToServer(SeekingWeaponWarningMessage(false, seekingEntity!!.getUUID()))
                            }
                        }
                    } else {
                        seekFailure(player)
                    }

                    if (lockOn && holdingFireKey && lockingEntity != null) {
                        sendPacketToServer(
                            ShootMessage(
                                gunSpread,
                                zoom,
                                lockingEntity!!.getUUID(),
                                lockingEntity!!.eyePosition.toVector3f()
                            )
                        )
                        holdingFireKey = false
                    }
                }
            } else {
                seekFailure(player)
            }

            if (nearestEntity != null && nearestEntity!!.type.`is`(ModTags.EntityTypes.DECOY)) {
                seekFailure(player)
            }

            if (lockingEntity != null && !lockingEntity!!.isAlive) {
                seekFailure(player)
            }

            if (seekingTime == 2) {
                playLockingSound(data, player)
            }

            if (seekingTime > lockTime) {
                playLockedSound(data, player)
                if (guideType == 0 && lockingEntity != null && (!lockingEntity!!.passengers.isEmpty()
                            || lockingEntity is VehicleEntity) && player.tickCount % 2 == 0
                ) {
                    sendPacketToServer(
                        SeekingWeaponWarningMessage(
                            true,
                            lockingEntity!!.uuid
                        )
                    )
                }
            }
        }
    }

    fun vehicleWeaponSeeking(player: Player) {
        val vehicle = player.vehicle as? VehicleEntity ?: return
        val data = vehicle.getGunData(player) ?: return
        val seekWeaponInfo = data.get(GunProp.SEEK_WEAPON_INFO) ?: return

        // 锁定所需时间
        val lockTime = seekWeaponInfo.seekTime
        // 搜寻角度
        val seekAngle = seekWeaponInfo.seekAngle
        // 搜索范围
        val seekRange = seekWeaponInfo.seekRange
        // 视角位置
        val cameraPos = mc.gameRenderer.mainCamera.position
        // 搜寻方向
        val seekVec = vehicle.getSeekVec(player, 1f) ?: return
        // 最小目标高度
        val minTargetHeight = seekWeaponInfo.minTargetHeight
        // 最大目标高度
        val maxTargetHeight = seekWeaponInfo.maxTargetHeight
        // 最小目标碰撞箱大小
        val minTargetSize = seekWeaponInfo.minTargetSize
        // 能被友方雷达引导的最大锁定范围
        val maxGuidedRange = seekWeaponInfo.maxGuidedRange
        // 能否友方雷达引导
        val canGuidedByRadar = seekWeaponInfo.canGuidedByRadar
        // 是否能被隐身目标影响
        val affectedByStealthTarget = seekWeaponInfo.affectedByStealthTarget

        nearestEntityVehicle = SeekTool.Builder(player)
            .withinRangeSeekWeapon(seekRange, maxGuidedRange, affectedByStealthTarget, canGuidedByRadar)
            .withinAngle(cameraPos, seekVec, seekAngle)
            .baseFilter()
            .heightRange(minTargetHeight, maxTargetHeight)
            .sizeBiggerThan(minTargetSize)
            .smokeFilter()
            .noVehicle()
            .noClip()
            .notFriendly()
            .buildWithClosest(cameraPos, seekVec, canGuidedByRadar)

        val decoy = TraceTool.findLookDecoy(player, cameraPos, seekVec, seekRange)
        if (decoy != null && decoy.type.`is`(ModTags.EntityTypes.DECOY)) {
            nearestEntityVehicle = decoy
            seekFailure(player)
        }

        if (seekWeaponInfo.onlyLockBlock) {
            // 锁定方块
            val result = player.level().clip(
                ClipContext(
                    cameraPos, cameraPos.add(seekVec.scale(seekRange)),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, player
                )
            )
            seekingPosVehicle = result.getLocation()

            if (seekingTimeVehicle > lockTime + 2 && !lockOnVehicle) {
                lockOnVehicle = true
            }

            // 锁定失败
            if (lockingPosVehicle != null && (seekVec.angleTo(cameraPos.vectorTo(lockingPosVehicle!!)) > seekAngle
                        || !noClip(player, lockingPosVehicle!!))
            ) {
                seekFailure(player)
            }

            if (ModKeyMappings.VEHICLE_SEEK.isDown()) {
                if (seekingPosVehicle != null && seekingPosVehicle!!.distanceToSqr(cameraPos) < seekRange * seekRange) {
                    seekingTimeVehicle++
                    if (seekingTimeVehicle == 1) {
                        lockingPosVehicle = seekingPosVehicle
                    }
                } else {
                    seekFailure(player)
                }
            } else {
                seekFailure(player)
            }
        } else if (seekWeaponInfo.onlyLockEntity) {
            // 锁定实体
            if (seekingTimeVehicle > lockTime + 2 && !lockOnVehicle) {
                lockingEntityVehicle = seekingEntityVehicle
                lockOnVehicle = true
            }

            if (ModKeyMappings.VEHICLE_SEEK.isDown()) {
                if (seekingEntityVehicle == null) {
                    seekingEntityVehicle = nearestEntityVehicle
                }
                if (seekingEntityVehicle != null && lockingPosVehicle == null) {
                    seekingTimeVehicle++
                    if ((!seekingEntityVehicle!!.getPassengers()
                            .isEmpty() || seekingEntityVehicle is VehicleEntity) && player.tickCount % 3 == 0 && !lockOnVehicle
                    ) {
                        sendPacketToServer(
                            SeekingWeaponWarningMessage(
                                false,
                                seekingEntityVehicle!!.getUUID()
                            )
                        )
                    }
                }
            } else {
                seekFailure(player)
            }
        }

        // 锁定失败
        if (seekingEntityVehicle != null &&
            (seekVec.angleTo(
                cameraPos.vectorTo(
                    VectorTool.lerpGetEntityBoundingBoxCenter(
                        seekingEntityVehicle!!,
                        1f
                    )
                )
            ) > seekAngle
                    || !SeekTool.NOT_IN_SMOKE.test(seekingEntityVehicle)
                    || !noClip(player, seekingEntityVehicle!!))
        ) {
            seekFailure(player)
        }

        if (lockingEntityVehicle != null && !lockingEntityVehicle!!.isAlive) {
            seekFailure(player)
        }

        if (seekingTimeVehicle == 2) {
            playLockingSound(data, player)
        }

        if (seekingTimeVehicle > lockTime) {
            playLockedSound(data, player)
            if (seekWeaponInfo.onlyLockEntity && lockingEntityVehicle != null && (!lockingEntityVehicle!!.passengers.isEmpty()
                        || lockingEntityVehicle is VehicleEntity) && player.tickCount % 2 == 0
            ) {
                sendPacketToServer(
                    SeekingWeaponWarningMessage(
                        true,
                        lockingEntityVehicle!!.getUUID()
                    )
                )
            }
        }
    }

    fun seekFailure(player: Player) {
        seekingTimeVehicle = 0
        lockOnVehicle = false
        lockingEntityVehicle = null
        seekingEntityVehicle = null
        lockingPosVehicle = null
        seekingTime = 0
        lockOn = false
        lockingEntity = null
        seekingEntity = null
        lockingPos = null
        VehicleMainWeaponHudOverlay.lock = false
        stopVehicleSeekSound(player)
    }

    fun playLockingSound(data: GunData, player: Player) {
        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.locking
        player.playSound(sound, 2f, 1f)
    }

    fun playLockedSound(data: GunData, player: Player) {
        val soundInfo = data.get(GunProp.SOUND_INFO)
        val sound = soundInfo.locked
        player.playSound(sound, 2f, 1f)
    }

    fun noClip(entity: Entity, e: Entity): Boolean {
        return entity.level()
            .clip(
                ClipContext(
                    entity.eyePosition,
                    e.eyePosition,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    entity
                )
            )
            .type != HitResult.Type.BLOCK
    }

    fun noClip(entity: Entity, pos: Vec3): Boolean {
        return entity.level()
            .clip(
                ClipContext(
                    entity.eyePosition,
                    pos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    entity
                )
            )
            .type != HitResult.Type.BLOCK
    }

    fun weaponZooming(stack: ItemStack) {
        if (stack.item is GunItem) {
            sendPacketToServer(WeaponZoomingMessage(zoomTime >= 0.7))
        }
    }

    // 耐力
    fun staminaSystem() {
        if (mc.isPaused) return
        if (localPlayer == null) return

        if (breath) {
            stamina += 0.5f
        } else if (stamina > 0) {
            stamina = (stamina - 0.5f).coerceAtLeast(0f)
        }

        if (stamina >= 100) {
            exhaustion = true
            breath = false
        }

        if (exhaustion && stamina <= 0) {
            exhaustion = false
        }

        if ((ModKeyMappings.BREATH.isDown() && zoom)) {
            switchTime = (switchTime + 0.65).coerceAtMost(5.0)
        } else if (switchTime > 0 && stamina == 0f) {
            switchTime = (switchTime - 0.15).coerceAtLeast(0.0)
        }
    }

    /**
     * 禁止玩家奔跑
     */
    fun handlePlayerSprint() {
        val player = localPlayer ?: return

        if (player.isShiftKeyDown
            || player.isPassenger
            || player.isInWater
            || zoom
        ) {
            noSprintTicks = 3f
        }

        if (noSprintTicks > 0) {
            noSprintTicks--
        }

        if (zoom || holdingFireKey) {
            player.isSprinting = false
        }
    }

    private fun handleVariableDecrease() {
        if (holdingFireKeyTicks > 0 && !holdingFireKey) {
            holdingFireKeyTicks--
            if (holdingFireKeyTicks == 0) {
                holdingFireKeyTicks0 = 0f
            }
        }

        if (dismountCountdown > 0) {
            dismountCountdown--
        }

        if (aimVillagerCountdown > 0) {
            aimVillagerCountdown--
        }

        if (switchVehicleWeaponCooldown > 0) {
            switchVehicleWeaponCooldown--
        }
    }

    @JvmStatic
    fun isProne(player: Player): Boolean {
        val level = player.level()
        if (player.pose == Pose.SWIMMING && !player.isSwimming) return true
        val forward = Vec3(player.lookAngle.x, 0.0, player.lookAngle.z).normalize()
        return player.isCrouching && level.getBlockState(
            BlockPos.containing(
                player.x + 0.7 * forward.x,
                player.y + 0.5,
                player.z + 0.7 * forward.z
            )
        ).canOcclude()
                && !level.getBlockState(
            BlockPos.containing(
                player.x + 0.7 * forward.x,
                player.y + 1.5,
                player.z + 0.7 * forward.z
            )
        ).canOcclude()
    }

    fun handleGunMelee(player: Player, stack: ItemStack) {
        val item = stack.item
        if (item is GunItem) {
            val data = GunData.from(stack)
            val vehicle = player.vehicle
            if (item.hasMeleeAttack(data) && gunMelee == 0 && drawTime < 0.01
                && (ModKeyMappings.MELEE.isDown() || (data.meleeOnly() && holdingFireKey))
                && !(vehicle is VehicleEntity && vehicle.banHand(player))
                && !holdFireVehicle
                && !notInGame
                && !isEditing
                && !(GunData.from(stack).reload.normal() || GunData.from(stack).reload.empty())
                && !data.reloading()
                && !data.charging() && !player.cooldowns.isOnCooldown(item)
            ) {
                gunMelee = data.get(GunProp.MELEE_DURATION)
                fireCooldown = gunMelee + 4.0
            }
            if (gunMelee == data.get(GunProp.MELEE_DURATION) - data.get(GunProp.MELEE_DAMAGE_TIME)) {
                doGunMeleeAttack(player, data.get(GunProp.MELEE_ANGLE).toDouble(), data.get(GunProp.MELEE_RANGE))
            }
        }

        if (gunMelee > 0) {
            gunMelee--
        }
    }

    fun doGunMeleeAttack(player: Player, angle: Double, customRange: Double) {
        player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1f)

        val lookingEntity = TraceTool.findMeleeEntity(player, player.entityInteractionRange() + customRange)
        val targetEntities =
            SeekTool.seekLivingEntities(player, player.entityInteractionRange() + customRange, angle / 2)
        val attackList = mutableListOf<Entity>()

        if (lookingEntity != null) {
            attackList += lookingEntity
        }

        if (!targetEntities.isEmpty()) {
            val list = targetEntities.filter { it != null && it.isAlive && it != lookingEntity }
                .sortedBy {
                    player.lookAngle.angleTo(player.eyePosition.vectorTo(it.eyePosition))
                }
            attackList += list
        }

        player.swing(InteractionHand.MAIN_HAND)
        sendPacketToServer(MeleeAttackMessage(attackList.map { it.uuid }))
    }

    fun handleLungeAttack(player: Player, stack: ItemStack) {
        if (stack.`is`(ModItems.LUNGE_MINE.get()) && lungeAttack == 0 && lungeDraw == 0 && usingLunge) {
            lungeAttack = 18
            usingLunge = false
            player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1f)
        }

        if (stack.`is`(ModItems.LUNGE_MINE.get()) && ((lungeAttack >= 9 && lungeAttack <= 10.5) || lungeSprint > 0)) {
            val lookingEntity = TraceTool.findLookingEntity(player, player.getEntityReach() + 1.5)

            val result = player.level().clip(
                ClipContext(
                    player.eyePosition,
                    player.eyePosition.add(player.lookAngle.scale(player.getBlockReach() + 0.5)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
                )
            )

            val looking = Vec3.atLowerCornerOf(
                player.level().clip(
                    ClipContext(
                        player.eyePosition,
                        player.eyePosition.add(player.lookAngle.scale(player.getBlockReach() + 0.5)),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        player
                    )
                ).blockPos
            )
            val blockState = player.level().getBlockState(
                BlockPos.containing(
                    looking.x,
                    looking.y,
                    looking.z
                )
            )

            if (lookingEntity != null) {
                sendPacketToServer(LungeMineAttackMessage(0, lookingEntity.getUUID(), result.location))
                lungeSprint = 0
                lungeAttack = 0
                lungeDraw = 15
            } else if ((blockState.canOcclude() || blockState.block is DoorBlock
                        || blockState.block is CrossCollisionBlock || blockState.block is BellBlock) && lungeSprint == 0
            ) {
                sendPacketToServer(LungeMineAttackMessage(1, player.getUUID(), result.location))
                lungeSprint = 0
                lungeAttack = 0
                lungeDraw = 15
            }
        }

        if (lungeSprint > 0) {
            lungeSprint--
        }

        if (lungeAttack > 0) {
            lungeAttack--
        }

        if (lungeDraw > 0) {
            lungeDraw--
        }
    }

    @SubscribeEvent
    fun handleWeaponFire(@Suppress("unused") event: RenderFrameEvent.Pre) {
        if (clientLevel == null) return
        val player = localPlayer ?: return

        if (notInGame) {
            holdingFireKey = false
        }

        val stack = player.mainHandItem
        val item = stack.item
        if (item !is GunItem) {
            clientTimer.stop()
            fireSpread = 0.0
            gunSpread = 0.0
            return
        }

        val data = GunData.from(stack)
        val resource = GunResource.compute(stack)
        val mode = data.selectedFireModeInfo().mode

        val partialHoldingFireKeyTicks =
            Mth.lerp(getDelta().toDouble(), holdingFireKeyTicks0.toDouble(), holdingFireKeyTicks.toDouble())
        holdingFireKeyTicks0 = holdingFireKeyTicks.toFloat()

        if (partialHoldingFireKeyTicks > holdingFireKeyTicks
            && partialHoldingFireKeyTicks > data.get(GunProp.SHOOT_DELAY) * 0.25 && shouldPlayDischargeSound
        ) {
            val dischargeSound = resource.dischargeSound
            if (dischargeSound != null) {
                player.playSound(
                    dischargeSound,
                    partialHoldingFireKeyTicks.toFloat() * 0.03f,
                    0.6f + partialHoldingFireKeyTicks.toFloat() * 0.02f
                )
            }

            shouldPlayDischargeSound = false
            burstFireAmount = 0
        }

        if (!item.canShoot(data, player)) {
//            if (!data.meleeOnly()) {
//                holdingFireKey = false
//            }
            burstFireAmount = 0
        }

        // 精准度
        val times = getDelta().coerceAtMost(0.8f)

        val basicDev = data.get(GunProp.SPREAD)
        val walk = if (isMoving()) 0.3 * basicDev else 0.0
        val sprint = if (player.isSprinting) 0.25 * basicDev else 0.0
        val crouching = if (player.isCrouching) -0.15 * basicDev else 0.0
        val prone = if (isProne(player)) -0.3 * basicDev else 0.0
        val jump = if (player.onGround()) 0.0 else 0.35 * basicDev
        val ride = if (player.onGround()) -0.25 * basicDev else 0.0

        val zoomSpread = 1 - (1 - data.get(GunProp.ZOOM_SPREAD_RATE)) * zoomTime
        val spread =
            if (data.isShotgun || stack.`is`(ModItems.MINIGUN.get())) 1.2 * zoomSpread * (basicDev + 0.2 * (walk + sprint + crouching + prone + jump + ride) + fireSpread)
            else zoomSpread * (0.7 * basicDev + walk + sprint + crouching + prone + jump + ride + 0.8 * fireSpread)

        gunSpread = Mth.lerp(0.14 * times, gunSpread, spread)

        // 开火部分
        val weight = data.get(GunProp.WEIGHT)
        val speed = 5 / (weight + 4)

        fireCooldown = if (noSprintTicks == 0f && player.isSprinting && !zoom && !holdingFireKey) {
            (fireCooldown + 3 * times).coerceIn(0.0, 24.0)
        } else {
            (fireCooldown - 6 * speed * times).coerceIn(0.0, 40.0)
        }

        val rpm = (data.get(GunProp.RPM) + customRpm).coerceIn(1, 114514)
        val rps = rpm / 60.0

        // cooldown in ms
        val cooldown = (1000 / rps).roundToInt()

        //左轮类
        if (clientTimer.progress == 0L && stack.`is`(ModItems.TRACHELIUM.get()) && holdingFireKey) {
            revolverPreTime = (revolverPreTime + 0.3 * times).coerceIn(0.0, 1.0)
            revolverWheelPreTime =
                (revolverWheelPreTime + 0.32 * times).coerceIn(0.0, if (revolverPreTime > 0.7) 1.0 else 0.55)
        } else {
            revolverPreTime = (revolverPreTime - 1.2 * times).coerceIn(0.0, 1.0)
        }

        val vehicle = player.vehicle
        if (((holdingFireKey || burstFireAmount > 0) && holdingFireKeyTicks >= data.get(GunProp.SHOOT_DELAY))
            && !(vehicle is VehicleEntity && vehicle.banHand(player))
            && !holdFireVehicle
            && item.canShoot(data, player)
            && !item.useSpecialFireProcedure(data)
            && fireCooldown == 0.0
            && sprintBasicRotX * sprintBasicRotY * sprintBasicRotZ < 0.0001
            && drawTime < 0.01
            && !notInGame
            && !isEditing
        ) {
            if (mode == FireMode.SEMI) {
                if (clientTimer.progress == 0L) {
                    clientTimer.start()
                    shootClient(player)
                }
            } else {
                if (!clientTimer.started()) {
                    clientTimer.start()
                    // 首发瞬间发射
                    clientTimer.progress = cooldown.toLong() + 1L
                }

                if (clientTimer.progress >= cooldown) {
                    var newProgress = clientTimer.progress

                    // 低帧率下的开火次数补偿
                    do {
                        shootClient(player)
                        newProgress -= cooldown
                    } while (newProgress - cooldown > 0)

                    clientTimer.progress = newProgress
                }
            }

            if (notInGame) {
                clientTimer.stop()
            }

        } else {
            if (mode != FireMode.SEMI && clientTimer.progress >= cooldown) {
                clientTimer.stop()
            }
            fireSpread = 0.0
        }

        if (mode == FireMode.SEMI && clientTimer.progress >= cooldown) {
            clientTimer.stop()
        }

        if (GunData.from(stack).reload.normal() || GunData.from(stack).reload.empty()) {
            customRpm = 0
        }

        data.save()
    }

    fun shootClient(player: Player) {
        val stack = player.mainHandItem
        val item = stack.item as? GunItem ?: return

        val data = GunData.from(stack)
        if (!item.canShoot(data, player) || item.useSpecialFireProcedure(data)) return

        val mode = data.selectedFireModeInfo().mode
        if (mode != FireMode.AUTO) {
            holdingFireKey = false
        }

        if (data.get(GunProp.CLEAR_HOLD_PROGRESS_AFTER_SHOOT)) {
            holdingFireKeyTicks = 0
        }

        if (mode == FireMode.BURST && burstFireAmount == 1) {
            fireCooldown = data.get(GunProp.BURST_COOLDOWN).toDouble()
        }

        if (burstFireAmount > 0) {
            burstFireAmount--
        }

        for (type in Perk.Type.entries) {
            val instance = data.perk.getInstances(type)
            customRpm = instance.maxOfOrNull { it.perk.getModifiedCustomRPM(customRpm, data, it) } ?: customRpm
        }

        if (stack.`is`(ModItems.DEVOTION.get())) {
            customRpm = (customRpm + 15).coerceAtMost(500)
        }

        // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
        if (data.get(GunProp.BOLT_ACTION_TIME) > 0 && data.hasEnoughAmmoToShoot(player)) {
            data.bolt.needed.set(true)
        }

        revolverPreTime = 0.0
        revolverWheelPreTime = 0.0

        playGunClientSounds(player)
        handleClientShoot()
    }

    fun handleClientShoot() {
        val player = localPlayer ?: return
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return
        val data = GunData.from(stack)

        sendPacketToServer(
            ShootMessage(
                gunSpread,
                zoom,
                if (lockedEntity != null) lockedEntity!!.getUUID() else null, null
            )
        )
        fireRecoilTime = 10.0

        // 真实后坐（
        if (data.get(GunProp.RECOIL) != 0.0) {
            player.deltaMovement = player.deltaMovement.add(player.getViewVector(1f).scale(-data.get(GunProp.RECOIL)))
        }

        val gunRecoilY = data.get(GunProp.RECOIL_Y) * 10

        recoilY = (2 * Math.random() - 1).toFloat() * gunRecoilY

        if (shellIndex < 5) {
            shellIndex++
        }

        noSprintTicks = 7f

        shellIndexTime[shellIndex] = 0.001

        randomShell[0] = (1 + 0.2 * (Math.random() - 0.5))
        randomShell[1] = (0.2 + (Math.random() - 0.5))
        randomShell[2] = (0.7 + (Math.random() - 0.5))
    }

    fun playGunClientSounds(player: Player) {
        val stack = player.mainHandItem
        val item = stack.item
        if (item !is GunItem) return

        if (item == ModItems.SENTINEL.get()) {
            val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM)
            val charged = cap != null && cap.energyStored > 0

            if (charged) {
                player.playSound(
                    ModSounds.SENTINEL_CHARGE_FIRE_1P.get(),
                    2f,
                    ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                )
                return
            }
        }

        if (item == ModItems.SECONDARY_CATACLYSM.get()) {
            val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM)
            val hasEnoughEnergy = cap != null && cap.energyStored > 3000

            val isChargedFire = zoom && hasEnoughEnergy

            if (isChargedFire) {
                player.playSound(
                    ModSounds.SECONDARY_CATACLYSM_FIRE_1P_CHARGE.get(),
                    2f,
                    ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                )
                return
            }
        }

        val data = GunData.from(stack)
        val perk = data.perk.get(Perk.Type.AMMO)
        val soundInfo = data.get(GunProp.SOUND_INFO)

        val pitch = if (data.heat.get() <= 75) 1f else (1 - 0.02 * abs(75 - data.heat.get())).toFloat()

        if (perk == ModPerks.BEAST_BULLET.get()) {
            player.playSound(ModSounds.HENG.get(), 1f, ((2 * Math.random() - 1) * 0.1f + pitch).toFloat())
        }

        val isSilent = data.attachment.get(AttachmentType.BARREL) == 2
        val fire1p = if (isSilent) soundInfo.fire1PSilent else soundInfo.fire1P

        if (fire1p != null) {
            player.playSound(fire1p, 4f, ((2 * Math.random() - 1) * 0.05f + pitch).toFloat())
        }

        val shooterHeight = player.eyePosition.distanceTo(
            (Vec3.atLowerCornerOf(
                player.level().clip(
                    ClipContext(
                        player.eyePosition,
                        player.eyePosition.add(
                            Vec3(0.0, -1.0, 0.0).scale(10.0)
                        ),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
                    )
                ).blockPos
            ))
        )

        queueClientWorkIfDelayed((1 + 1.5 * shooterHeight).toInt()) {
            if (GunResource.compute(stack).ejectShell) {
                if (data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                    val ammoType: Ammo = data.selectedAmmoConsumer().playerAmmoType!!
                    when (ammoType) {
                        Ammo.SHOTGUN ->
                            player.playSound(
                                ModSounds.SHELL_CASING_SHOTGUN.get(),
                                (0.75 - 0.12 * shooterHeight).coerceAtLeast(0.0).toFloat(),
                                ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                            )

                        Ammo.SNIPER, Ammo.HEAVY ->
                            player.playSound(
                                ModSounds.SHELL_CASING_50CAL.get(),
                                (1 - 0.15 * shooterHeight).coerceAtLeast(0.0).toFloat(),
                                ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                            )

                        else ->
                            player.playSound(
                                ModSounds.SHELL_CASING_NORMAL.get(),
                                (1.5 - 0.2 * shooterHeight).coerceAtLeast(0.0).toFloat(),
                                ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                            )
                    }
                } else {
                    player.playSound(
                        ModSounds.SHELL_CASING_NORMAL.get(),
                        (1.5 - 0.2 * shooterHeight).coerceAtLeast(0.0).toFloat(),
                        ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
                    )
                }
            }
        }
    }

    @SubscribeEvent
    fun handleVehicleFire(@Suppress("unused") event: RenderFrameEvent.Pre) {
        if (clientLevel == null) return
        val player = localPlayer ?: return

        if (notInGame) {
            clientTimerVehicle.stop()
            holdFireVehicle = false
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
            val gunData = vehicle.getGunData(vehicle.getSeatIndex(player)) ?: return

            if (!vehicle.canShoot(player)) {
                holdFireVehicle = false
                return
            }

            var rpm = vehicle.vehicleWeaponRpm(player)
            if (rpm == 0) {
                rpm = 240
            }

            val rps = rpm / 60.0
            val cooldown = (1000 / rps).roundToInt()

            if (holdFireVehicle) {
                if (!clientTimerVehicle.started()) {
                    clientTimerVehicle.start()
                    // 首发瞬间发射
                    clientTimerVehicle.progress = (cooldown + 1).toLong()
                }

                if (clientTimerVehicle.progress >= cooldown) {
                    var newProgress = clientTimerVehicle.progress

                    // 低帧率下的开火次数补偿
                    do {
                        sendPacketToServer(
                            VehicleFireMessage(
                                if (lockingEntityVehicle != null) lockingEntityVehicle!!.getUUID() else null,
                                if (lockingPosVehicle != null) lockingPosVehicle!!.toVector3f() else null
                            )
                        )
                        if (mc.options.cameraType == CameraType.FIRST_PERSON || zoomVehicle) {
                            playVehicleClientSounds(player, vehicle)
                        }

                        newProgress -= cooldown
                    } while (newProgress - cooldown > 0)

                    clientTimerVehicle.progress = newProgress
                }
                if (gunData.get(GunProp.DEFAULT_FIRE_MODE) == "Semi") {
                    holdFireVehicle = false
                }
            } else if (clientTimerVehicle.progress >= cooldown) {
                clientTimerVehicle.stop()
            }
        } else {
            clientTimerVehicle.stop()
        }
    }

    fun playVehicleClientSounds(player: Player, vehicle: VehicleEntity) {
        val gunData = vehicle.getGunData(vehicle.getSeatIndex(player)) ?: return
        val soundInfo = gunData.get(GunProp.SOUND_INFO)
        val sound = soundInfo.fire1P ?: return

        val pitch =
            if (vehicle.getWeaponHeat(player) <= 60) 1f else (1 - 0.011 * abs(60 - vehicle.getWeaponHeat(player))).toFloat()
        player.playSound(sound, 1f, pitch)
    }

    @SubscribeEvent
    fun handleWeaponBreathSway(@Suppress("unused") event: RenderFrameEvent.Pre) {
        val player = localPlayer ?: return
        val stack = player.mainHandItem
        val item = stack.item as? GunItem ?: return
        val vehicle = player.vehicle

        if (vehicle is VehicleEntity && player == vehicle.firstPassenger && vehicle.hidePassenger(player)) return

        val data = GunData.from(stack)

        val times = 2 * getDelta().coerceAtMost(0.8f)

        val pose: Float = if (player.isCrouching && player.bbHeight >= 1 && !isProne(player)) {
            0.85f
        } else if (isProne(player)) {
            if (data.attachment.get(AttachmentType.GRIP) == 3 || item.hasBipod(data)) 0f else 0.25f
        } else {
            1f
        }

        val stockType = data.attachment.get(AttachmentType.STOCK)
        val sway: Double = when (stockType) {
            1 -> 1.0
            2 -> 0.55
            else -> 0.8
        }

        val customWeight = data.get(GunProp.WEIGHT).toFloat().coerceIn(1f, 30f)

        if (!breath && zoom) {
            val newPitch = (
                    player.xRot - 0.01f * sin(0.03 * player.tickCount) * pose * Mth.nextDouble(
                        RandomSource.create(),
                        0.1,
                        1.0
                    ) * times * sway * (1 - 0.03 * customWeight)
                    ).toFloat()
            player.xRot = newPitch
            player.xRotO = player.xRot

            val newYaw = (
                    player.yRot - 0.005f * cos(0.025 * (player.tickCount + 2 * Math.PI)) * pose * Mth.nextDouble(
                        RandomSource.create(),
                        0.05,
                        1.25
                    ) * times * sway * (1 - 0.03 * customWeight)
                    ).toFloat()
            player.yRot = newYaw
            player.yRotO = player.yRot
        }
    }

    private fun getDelta(): Float {
        return mc.deltaFrameTime
    }

    @SubscribeEvent
    fun computeCameraAngles(event: ViewportEvent.ComputeCameraAngles) {
        if (clientLevel == null) return
        val entity = event.camera.entity as? LivingEntity ?: return
        val player = localPlayer ?: return
        val stack = entity.mainHandItem

        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            handleDroneCamera(event, entity)
        }

        val yaw = event.yaw
        val pitch = event.pitch
        val roll = event.roll

        shakeTime = Mth.lerp(0.05 * getDelta(), shakeTime, 0.0)

        val vehicle = player.vehicle
        if (shakeTime > 0) {
            val shakeRadiusAmplitude =
                (1 - player.position().distanceTo(Vec3(shakePos[0], shakePos[1], shakePos[2])) / shakeRadius)
                    .toFloat().coerceIn(0f, 1f)

            val onVehicle = vehicle != null
            if (shakeType > 0) {
                event.yaw =
                    (yaw + (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
                event.pitch =
                    (pitch - (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
                cameraRoll =
                    (roll - (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
            } else {
                event.yaw =
                    (yaw - (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
                event.pitch =
                    (pitch + (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude * shakeType *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
                cameraRoll =
                    (roll + (shakeTime * sin(0.5 * Math.PI * shakeTime) * shakeAmplitude * shakeRadiusAmplitude *
                            if (onVehicle) 0.1 else 1.0)).toFloat()
            }
        }

        cameraPitch = event.pitch
        cameraYaw = event.yaw
        cameraRoll *= 0.99f

        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        if (stack.item is GunItem) {
            handleWeaponSway(entity)
            handleWeaponMove(entity)
            handleWeaponZoom(entity)
            handleWeaponFire(event, entity)
            handleWeaponShell()
            handleGunRecoil()
            handleBowPullAnimation(entity, stack)
            handleWeaponDraw(entity)
            handlePlayerCamera(event)
        }

        handleShockCamera(event, entity)
    }

    private fun handleDroneCamera(event: ViewportEvent.ComputeCameraAngles, entity: LivingEntity) {
        val stack = entity.mainHandItem
        val drone = EntityFindUtil.findDrone(entity.level(), stack.getOrCreateTag().getString("LinkedDrone")) ?: return
        cameraRoll =
            drone.getRoll(event.partialTick.toFloat() * (1 - (drone.getPitch(event.partialTick.toFloat()) / 90)))
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderHand(event: RenderHandEvent) {
        val player = localPlayer ?: return

        val leftHand = if (mc.options.mainHand().get() == HumanoidArm.RIGHT)
            InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND

        val rightHand = if (mc.options.mainHand().get() == HumanoidArm.RIGHT)
            InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND

        val rightHandItem = player.getItemInHand(rightHand)

        if (event.hand == leftHand) {
            if (rightHandItem.item is GunItem) {
                event.isCanceled = true
            }
            if (rightHandItem.`is`(ModItems.LUNGE_MINE.get())) {
                event.isCanceled = true
            }
            if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                event.isCanceled = true
            }
        }

        if (event.hand == rightHand) {
            if (rightHandItem.item is GunItem && drawTime > 0.15) {
                event.isCanceled = true
            }
            if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                event.isCanceled = true
            }
        }

        val stack = player.mainHandItem
        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            if (EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone")) != null) {
                event.isCanceled = true
            }
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && (vehicle.banHand(player) ||
                    (!zoom && mc.options.cameraType == CameraType.FIRST_PERSON && ModKeyMappings.FREE_CAMERA.isDown()))
        ) {
            event.isCanceled = true
        }
    }

    private fun handleWeaponSway(entity: LivingEntity) {
        val stack = entity.mainHandItem
        val player = entity as? Player ?: return
        val item = stack.item as? GunItem ?: return
        val data = GunData.from(stack)

        val times = 2 * getDelta().coerceAtMost(0.8f)
        val pose =
            if (player.isShiftKeyDown && player.bbHeight >= 1 && isProne(player)) {
                0.85
            } else if (isProne(player)) {
                if (data.attachment.get(AttachmentType.GRIP) == 3 || item.hasBipod(data)) 0.0 else 0.25
            } else {
                1.0
            }

        swayTime += 0.05 * times
        swayX = pose * -0.008 * sin(swayTime) * (1 - 0.95 * zoomTime)
        swayY = pose * 0.125 * sin(swayTime - 1.585) * (1 - 0.95 * zoomTime) - 3 * moveRotZ
    }

    private fun handleWeaponMove(entity: LivingEntity) {
        val stack = entity.mainHandItem
        val player = entity as? Player ?: return
        val item = stack.item as? GunItem ?: return
        val data = GunData.from(stack)

        val times = 3.7f * getDelta().coerceAtMost(0.8f)
        val moveSpeed = entity.deltaMovement.horizontalDistance()
        val animSpeed =
            if (entity.onGround()) {
                if (entity.isSprinting) {
                    1.8
                } else {
                    2.0
                }
            } else {
                0.005
            }

        val customWeight = data.get(GunProp.WEIGHT).coerceIn(1.0, 50.0)

        if (!isEditing) {
            moveRotZ =
                if (!entity.isSprinting && mc.options.keyUp.isDown() && firePosTimer == 0.0 && item !is SuperStarShooterItem) {
                    Mth.lerp(0.2 * times, moveRotZ, 0.14) * (1 - zoomTime)
                } else {
                    Mth.lerp(0.2 * times, moveRotZ, 0.0) * (1 - zoomTime)
                }

            if (entity.isSprinting && !data.reloading() && firePosTimer == 0.0 && !ModKeyMappings.FIRE.isDown() && noSprintTicks == 0f && zoomTime < 0.5) {
                sprintBasicRotX = Mth.lerp(0.3f * times / (customWeight + 4), sprintBasicRotX, 1.0).coerceIn(0.0, 1.0)
                sprintBasicRotY = Mth.lerp(0.18f * times / (customWeight + 4), sprintBasicRotY, 1.0).coerceIn(0.0, 1.0)
                sprintBasicRotZ = Mth.lerp(0.3f * times / (customWeight + 4), sprintBasicRotZ, 1.0).coerceIn(0.0, 1.0)

                sprintBasicPosX = Mth.lerp(0.8f * times / (customWeight + 4), sprintBasicPosX, 1.0).coerceIn(0.0, 1.0)
                sprintBasicPosY = Mth.lerp(0.25f * times / (customWeight + 4), sprintBasicPosY, 1.0).coerceIn(0.0, 1.0)
                sprintBasicPosZ = Mth.lerp(0.8f * times / (customWeight + 4), sprintBasicPosZ, 1.0).coerceIn(0.0, 1.0)
            } else {
                sprintBasicRotX = Mth.lerp(1.4f * times / customWeight, sprintBasicRotX, 0.0).coerceIn(0.0, 1.0)
                sprintBasicRotY = Mth.lerp(0.96f * times / customWeight, sprintBasicRotY, 0.0).coerceIn(0.0, 1.0)
                sprintBasicRotZ = Mth.lerp(1.4f * times / customWeight, sprintBasicRotZ, 0.0).coerceIn(0.0, 1.0)

                sprintBasicPosX = Mth.lerp(0.8f * times / customWeight, sprintBasicPosX, 0.0).coerceIn(0.0, 1.0)
                sprintBasicPosY = Mth.lerp(0.8f * times / customWeight, sprintBasicPosY, 0.0).coerceIn(0.0, 1.0)
                sprintBasicPosZ = Mth.lerp(0.8f * times / customWeight, sprintBasicPosZ, 0.0).coerceIn(0.0, 1.0)
            }
        }

        if (isMoving()) {
            moveTime += 0.15 * animSpeed * times * moveSpeed * if (firePosTimer != 0.0) 0.4 else 1.0
            sprintTime += 0.15 * animSpeed * times * moveSpeed * (if (player.isSprinting) sprintBasicPosX else 1.0) * (if (firePosTimer != 0.0) 0.4 else 1.0)
            moveFadeTime = Mth.lerp(0.13 * times, moveFadeTime, 1.0)
        } else {
            moveFadeTime = Mth.lerp(0.1 * times, moveFadeTime, 0.0)
        }

        if (entity.isSprinting && !data.reloading() && firePosTimer == 0.0 && !ModKeyMappings.FIRE.isDown() && noSprintTicks == 0f) {
            sprintFadeTime = if (entity.onGround()) {
                Mth.lerp(0.08 * times, sprintFadeTime, 1.0)
            } else {
                Mth.lerp(0.15 * times, sprintFadeTime, 0.0)
            }

            sprintPosX = 2 * sin(1 * PI * sprintTime) * sprintFadeTime
            sprintPosY = 1 * sin(2 * PI * sprintTime) * sprintFadeTime
        } else {
            sprintPosX = Mth.lerp(0.1 * times, sprintPosX, 0.0)
            sprintPosY = Mth.lerp(0.1 * times, sprintPosY, 0.0)
            sprintFadeTime = Mth.lerp(0.1 * times, sprintFadeTime, 0.0)
        }

        movePosX = 0.2 * sin(1 * PI * moveTime) * (1 - 0.4 * zoomTime) * moveFadeTime
        movePosY = -0.135 * sin(2 * PI * (moveTime - 0.25)) * (1 - 0.4 * zoomTime) * moveFadeTime

        val left = mc.options.keyLeft.isDown()
        val right = mc.options.keyRight.isDown()
        var pos = 0.0
        if (left) {
            pos = -0.04
        }
        if (right) {
            pos = 0.04
        }
        if (left && right) {
            pos = 0.0
        }

        movePosHorizon = Mth.lerp(0.1 * times, movePosHorizon, pos * (1 - 1 * zoomTime))

        val velocity = entity.deltaMovement.y + 0.078
        velocityY = (Mth.lerp(0.23 * times, velocityY, velocity) * (1 - 0.5 * zoomTime)).coerceIn(-0.8, 0.8)
    }

    @JvmStatic
    fun gunRootMove(
        animationProcessor: AnimationProcessor<*>,
        customX: Float,
        customY: Float,
        customZ: Float,
        useCustomAnim: Boolean
    ) {
        val root = animationProcessor.getBone("root")
        val walkPosX = movePosX.toFloat()
        val walkPosY = (swayY + movePosY).toFloat()
        val walkPosZ = 0f
        val walkRotX = swayX.toFloat()
        val walkRotY = (0.2f * movePosX).toFloat()
        val walkRotZ = (0.2f * movePosX).toFloat()

        val i = if (useCustomAnim) 0 else 1

        val basicSprintPosX = (sprintBasicPosX * (1.5 + customX)).toFloat() * i
        val basicSprintPosY =
            (sprintBasicPosY * (-2.35 + customY - 8 * AnimationCurves.PARABOLA.apply(sprintBasicPosY))).toFloat() * i
        val basicSprintPosZ = (sprintBasicPosZ * (-0.55 + customZ)).toFloat() * i

        val basicSprintRotX = (sprintBasicRotX * 39 * Mth.DEG_TO_RAD).toFloat() * i
        val basicSprintRotY = (sprintBasicRotY * 35.6 * Mth.DEG_TO_RAD).toFloat() * i
        val basicSprintRotZ = (sprintBasicRotZ * 34.7 * Mth.DEG_TO_RAD).toFloat() * i

        val gunPosX =
            (walkPosX + basicSprintPosX + sprintPosX * i + 20 * drawTime + 9.3f * movePosHorizon).toFloat() * (1 - 0.5 * zoomTime).toFloat()
        val gunPosY =
            (walkPosY + basicSprintPosY + sprintPosY * i - 40 * drawTime - 2f * velocityY).toFloat() * (1 - 0.5 * zoomTime).toFloat()
        val gunPosZ = (walkPosZ + basicSprintPosZ) * (1 - 1 * zoomTime).toFloat()
        val gunRotX =
            ((walkRotX + basicSprintRotX - Mth.DEG_TO_RAD * 60 * drawTime - 0.15f * velocityY) * (1 - 0.5 * zoomTime) + Mth.DEG_TO_RAD * turnRot[0]).toFloat()
        val gunRotY =
            ((walkRotY + basicSprintRotY + (0.2f * sprintBasicPosX * i) + Mth.DEG_TO_RAD * 300 * drawTime) * (1 - 0.75 * zoomTime) + Mth.DEG_TO_RAD * turnRot[1]).toFloat()
        val gunRotZ =
            ((walkRotZ + basicSprintRotZ + moveRotZ + Mth.DEG_TO_RAD * 90 * drawTime + 2.7f * movePosHorizon) * (1 - 0.5 * zoomTime) + Mth.DEG_TO_RAD * turnRot[2]).toFloat()

        root.posX = gunPosX
        root.posY = gunPosY
        root.posZ = gunPosZ
        root.rotX = gunRotX
        root.rotY = gunRotY
        root.rotZ = gunRotZ
    }

    private fun handleWeaponZoom(entity: LivingEntity) {
        val player = entity as? Player ?: return
        val stack = player.mainHandItem
        val data = GunData.from(stack)
        val times = 5 * getDelta()

        val weight = data.get(GunProp.WEIGHT)
        val speed = 7.0 / (weight + 2)
        val vehicle = player.vehicle

        if (zoom
            && !(vehicle is VehicleEntity && vehicle.banHand(player))
            && !notInGame
            && drawTime < 0.01
            && !isEditing
            && !(data.reloading() && !data.get(GunProp.ZOOM_RELOAD))
        ) {
            if (fireCooldown <= 10) {
                zoomTime = (zoomTime + 0.03 * speed * times).coerceIn(0.0, 1.0)
            }
        } else {
            zoomTime = (zoomTime - 0.04 * speed * times).coerceIn(0.0, 1.0)
        }

        if (zoomPos > 0.8) {
            noSprintTicks = 5f
        }

        zoomPos = AnimationCurves.EASE_IN_OUT_QUINT.apply(zoomTime)
        zoomPosZ = AnimationCurves.PARABOLA.apply(zoomTime)
    }

    private fun handleWeaponFire(event: ViewportEvent.ComputeCameraAngles, entity: LivingEntity) {
        val times = (1.65f * customAnimSpeed * mc.deltaFrameTime.coerceAtMost(0.48f)).toFloat()
        val stack = entity.mainHandItem
        val data = GunData.from(stack)
        val amplitude = 25000.0 * data.get(GunProp.RECOIL_Y) * data.get(GunProp.RECOIL_X)

        if (fireRecoilTime > 0.0) {
            firePosTimer = 0.001
            fireRotTimer = if (fireRotTimer > 0) {
                0.12
            } else {
                0.001
            }
            fireRecoilTime -= 7 * times
            fireSpread += 0.1 * times
            firePosZ += (0.8 * firePosZ + 0.4) * (4 * Math.random() + 0.85) * times
            recoilForce += 0.5
        }

        fireSpread = (fireSpread - 0.1 * (fireSpread.pow(2) * times)).coerceIn(0.0, 2.0)
        firePosZ = (firePosZ - 0.7 * (firePosZ.pow(2) * times)).coerceIn(0.0, 2.5)
        firePosZ *= 0.99

        if (0.0 < firePosTimer) {
            firePosTimer += 0.16 * times
        }
        if (0.0 < fireRotTimer) {
            fireRotTimer += 0.24 * times
        }

        if (firePosTimer >= 2.0) {
            firePosTimer = 0.0
        }
        if (fireRotTimer >= 3.0) {
            fireRotTimer = 0.0
        }

        boltMove = if (firePosTimer > 0 && firePosTimer <= 0.5) {
            1.2 * Mth.sin(2 * Mth.PI * firePosTimer.toFloat()).toDouble()
        } else {
            0.0
        }

        if (boltMove > 1) {
            boltMove = 1.0
        }

        if (entity is Player && entity.isSpectator) return

        var shake = (
                MathTool.decayingOscillation(
                    0.6f,
                    2f,
                    2f,
                    firePosTimer.toFloat()
                ) * (1 + amplitude) * (DisplayConfig.WEAPON_SCREEN_SHAKE.get() / 100.0).toFloat()
                )

        if (recoilY > 0) {
            shake = -shake
        }

        lerpShake = Mth.lerp(event.partialTick * 0.5, lerpShake, shake)

        cameraRot[2] = lerpShake
    }

    @JvmStatic
    fun handleShootAnimation(
        bone: GeoBone,
        x: Float,
        y: Float,
        z: Float,
        rotX: Float,
        rotY: Float,
        rotZ: Float,
        zoomMultiply: Float,
        customSpeed: Float
    ) {
        val player = localPlayer ?: return
        val stack = player.mainHandItem
        val item = stack.item as? GunItem ?: return

        customAnimSpeed = customSpeed.toDouble()

        val data = GunData.from(stack)
        val barrelType = data.attachment.get(AttachmentType.BARREL)
        val gripType = data.attachment.get(AttachmentType.GRIP)
        val scopeType = data.attachment.get(AttachmentType.SCOPE)

        val recoil = when (barrelType) {
            1 -> 0.75f
            2 -> 0.95f
            else -> 1f
        }

        val gripRecoilX = when (gripType) {
            1 -> 0.85f
            2 -> 0.95f
            else -> 1f
        }

        val gripRecoilY = when (gripType) {
            1 -> 0.95f
            2 -> 0.85f
            else -> 1f
        }

        val zoomRecoil = when (scopeType) {
            2 -> 1.25f - (zoomTime * 0.8f).toFloat()
            3 -> 1.25f - zoomTime.toFloat()
            else -> 1.25f
        }

        val pose =
            if (player.isShiftKeyDown && player.bbHeight >= 1 && !isProne(player)) {
                0.85f
            } else if (isProne(player)) {
                if (data.attachment.get(AttachmentType.GRIP) == 3 || item.hasBipod(data)) {
                    0.5f
                } else {
                    0.75f
                }
            } else {
                1f
            }

        var zoomMultiply = zoomMultiply
        zoomMultiply = zoomMultiply.coerceIn(0f, 1f)

        val zoom = (1 - zoomMultiply * zoomTime).toFloat() * pose

        bone.posX = zoom * x * (recoilHorizon * (0.5f * firePosZ)).toFloat()
        bone.posY = zoom * y * (getBoneMoveY(firePosTimer.toFloat()) * 0.25 * (1 - 0.25 * zoomTime)).toFloat()
        bone.posZ =
            zoom * z * (getBoneMoveZ(firePosTimer.toFloat()) * 0.05 + 1.1f * firePosZ).toFloat() * (1 - 0.5 * zoomTime).toFloat()
        bone.rotX =
            zoom * rotX * (-getBoneRotX(fireRotTimer.toFloat()) * Mth.DEG_TO_RAD * 0.5f + 0.01f * firePosZ).toFloat() * gripRecoilX * recoil *
                    (1 - 0.85 * zoomTime).toFloat() * zoomRecoil
        bone.rotY =
            (3 * zoom * rotY * getBoneRotY(fireRotTimer.toFloat()) * Mth.DEG_TO_RAD * recoilHorizon * gripRecoilY * recoil *
                    (1 - 0.3 * zoomTime) * zoomRecoil).toFloat()
        bone.rotZ =
            (2 * zoom * rotZ * getBoneRotZ(fireRotTimer.toFloat()) * Mth.DEG_TO_RAD * recoilHorizon * gripRecoilY * recoil *
                    (1 - 0.5 * zoomTime) * zoomRecoil).toFloat()
    }

    @JvmStatic
    fun getBoneRotX(t: Float): Float {
        return when {
            t <= 0.25f -> Mth.lerp(t / (0.25F - 0F), 0F, -5.82024F)
            t <= 0.5f -> Mth.lerp((t - 0.25F) / (0.5F - 0.25F), -5.82024F, -6.38564F)
            t <= 0.75f -> Mth.lerp((t - 0.5F) / (0.75F - 0.5F), -6.38564F, -6.0138F)
            t <= 1f -> Mth.lerp((t - 0.75F) / (1F - 0.75F), -6.0138F, -3.22698F)
            t <= 1.3333f -> Mth.lerp((t - 1F) / (1.3333F - 1F), -3.22698F, -0.42425F)
            t <= 1.75f -> Mth.lerp((t - 1.3333F) / (1.75F - 1.3333F), -0.42425F, 0.23068F)
            t <= 2.0833f -> Mth.lerp((t - 1.75F) / (2.0833F - 1.75F), 0.23068F, -0.09988F)
            t <= 2.4167f -> Mth.lerp((t - 2.0833F) / (2.4167F - 2.0833F), -0.09988F, 0.04509F)
            else -> Mth.lerp((t - 2.4167F) / (3F - 2.4167F), 0.04509F, 0F)
        }
    }

    @JvmStatic
    fun getBoneRotY(t: Float): Float {
        return when {
            t <= 0.25f -> Mth.lerp(t / (0.25F - 0F), 0F, 1.33042F)
            t <= 0.5f -> Mth.lerp((t - 0.25F) / (0.5F - 0.25F), 1.33042F, -0.61289F)
            t <= 0.75f -> Mth.lerp((t - 0.5F) / (0.75F - 0.5F), -0.61289F, -0.64862F)
            t <= 1f -> Mth.lerp((t - 0.75F) / (1F - 0.75F), -0.64862F, -0.95049F)
            t <= 1.3333f -> Mth.lerp((t - 1F) / (1.3333F - 1F), -0.95049F, 0.27786F)
            t <= 1.75f -> Mth.lerp((t - 1.3333F) / (1.75F - 1.3333F), 0.27786F, -0.21405F)
            t <= 2.0833f -> Mth.lerp((t - 1.75F) / (2.0833F - 1.75F), -0.21405F, 0.076F)
            t <= 2.4167f -> Mth.lerp((t - 2.0833F) / (2.4167F - 2.0833F), 0.076F, 0.01634F)
            else -> Mth.lerp((t - 2.4167F) / (3F - 2.4167F), 0.01634F, 0F)
        }
    }

    @JvmStatic
    fun getBoneRotZ(t: Float): Float {
        return when {
            t <= 0.25f -> Mth.lerp(t / (0.25F - 0F), 0F, 5.79388F)
            t <= 0.5f -> Mth.lerp((t - 0.25F) / (0.5F - 0.25F), 5.79388F, -1.91761F)
            t <= 0.75f -> Mth.lerp((t - 0.5F) / (0.75F - 0.5F), -1.91761F, -3.1926F)
            t <= 1f -> Mth.lerp((t - 0.75F) / (1F - 0.75F), -3.1926F, 1.89646F)
            t <= 1.3333f -> Mth.lerp((t - 1F) / (1.3333F - 1F), 1.89646F, 0.43549F)
            t <= 1.75f -> Mth.lerp((t - 1.3333F) / (1.75F - 1.3333F), 0.43549F, -0.46178F)
            t <= 2.0833f -> Mth.lerp((t - 1.75F) / (2.0833F - 1.75F), -0.46178F, 0.12379F)
            t <= 2.4167f -> Mth.lerp((t - 2.0833F) / (2.4167F - 2.0833F), 0.12379F, -0.04605F)
            else -> Mth.lerp((t - 2.4167F) / (3F - 2.4167F), -0.04605F, 0F)
        }
    }

    @JvmStatic
    fun getBoneMoveY(t: Float): Float {
        return when {
            t <= 0.1667f -> Mth.lerp(t / (0.1667F - 0F), 0F, 0.25313F)
            t <= 0.3333f -> Mth.lerp((t - 0.1667F) / (0.3333F - 0.1667F), 0.25313F, 0.69563F)
            t <= 0.5f -> Mth.lerp((t - 0.3333F) / (0.5F - 0.3333F), 0.69563F, 0.54937F)
            t <= 0.6667f -> Mth.lerp((t - 0.5F) / (0.6667F - 0.5F), 0.54937F, 0.05688F)
            t <= 0.8333f -> Mth.lerp((t - 0.6667F) / (0.8333F - 0.6667F), 0.05688F, -0.17F)
            t <= 1f -> Mth.lerp((t - 0.8333F) / (1F - 0.8333F), -0.17F, -0.28F)
            t <= 1.1667f -> Mth.lerp((t - 1F) / (1.1667F - 1F), -0.28F, -0.065F)
            t <= 1.3333f -> Mth.lerp((t - 1.1667F) / (1.3333F - 1.1667F), -0.065F, 0.05F)
            t <= 1.5833f -> Mth.lerp((t - 1.3333F) / (1.5833F - 1.3333F), 0.05F, 0.03F)
            else -> Mth.lerp((t - 1.5833F) / (2F - 1.5833F), 0.03F, 0F)
        }
    }

    @JvmStatic
    fun getBoneMoveZ(t: Float): Float {
        return when {
            t <= 0.1667f -> Mth.lerp(t / (0.1667F - 0F), 0F, 5.205F)
            t <= 0.3333f -> Mth.lerp((t - 0.1667F) / (0.3333F - 0.1667F), 5.205F, 2.775F)
            t <= 0.4167f -> Mth.lerp((t - 0.3333F) / (0.4167F - 0.3333F), 2.775F, 0.66F)
            t <= 0.5833f -> Mth.lerp((t - 0.4167F) / (0.5833F - 0.4167F), 0.66F, -0.005F)
            t <= 0.75f -> Mth.lerp((t - 0.5833F) / (0.75F - 0.5833F), -0.005F, -0.485F)
            t <= 0.9167f -> Mth.lerp((t - 0.75F) / (0.9167F - 0.75F), -0.485F, -0.095F)
            t <= 1.1667f -> Mth.lerp((t - 0.9167F) / (1.1667F - 0.9167F), -0.095F, 0.06F)
            t <= 1.3333f -> Mth.lerp((t - 1.1667F) / (1.3333F - 1.1667F), 0.06F, 0.1F)
            t <= 1.5833f -> Mth.lerp((t - 1.3333F) / (1.5833F - 1.3333F), 0.1F, -0.03F)
            else -> Mth.lerp((t - 1.5833F) / (2F - 1.5833F), -0.03F, 0F)
        }
    }

    private fun handleWeaponShell() {
        if (localPlayer == null) return

        val times = getDelta().coerceAtMost(0.8f)

        if (shellIndex >= 5) {
            shellIndex = 0
            shellIndexTime[0] = 0.001
        }

        for (i in 0..<5) {
            if (shellIndexTime[i] > 0) {
                shellIndexTime[i] = (shellIndexTime[i] + 8 * times).coerceAtMost(50.0)
            }
            if (shellIndexTime[i] == 50.0) {
                shellIndexTime[i] = 0.0
            }
        }
    }

    private fun handleGunRecoil() {
        val player = localPlayer ?: return
        val stack = player.mainHandItem
        val item = stack.item as? GunItem ?: return
        val data = GunData.from(stack)

        val times = getDelta().coerceAtMost(1.6f)
        val barrelType = data.attachment.get(AttachmentType.BARREL)
        val gripType = data.attachment.get(AttachmentType.GRIP)

        val recoil = when (barrelType) {
            1 -> 1.5
            2 -> 2.2
            else -> 2.4
        }

        val gripRecoilX = when (gripType) {
            1 -> 1.25
            2 -> 0.25
            else -> 1.5
        }

        val gripRecoilY = when (gripType) {
            1 -> 0.7
            2 -> 1.75
            else -> 2.0
        }

        val customWeight = data.get(GunProp.WEIGHT)
        val gunRecoilX = data.get(GunProp.RECOIL_X)

        recoilHorizon = Mth.lerp(0.2 * times, recoilHorizon, 0.0) + recoilY
        recoilY = 0.0

        // 计算后坐力
        val pose =
            if (player.isShiftKeyDown && player.bbHeight >= 1 && !isProne(player)) {
                0.7f
            } else if (isProne(player)) {
                if (data.attachment.get(AttachmentType.GRIP) == 3 || item.hasBipod(data)) {
                    0.1f
                } else {
                    0.5f
                }
            } else {
                1f
            }

        // 水平后坐
        val newYaw =
            player.yRot - (0.6 * recoilHorizon * pose * times * (0.5 + fireSpread) * recoil * (4 / (customWeight + 4)) * gripRecoilX).toFloat()
        player.yRot = newYaw
        player.yRotO = player.yRot

        if (firePosTimer > 0.0) {
            var rotateX =
                (70 * pose * gunRecoilX * sin(firePosTimer * PI * 2) * (2.2 - firePosTimer) * recoil * (4 / (customWeight + 4))
                        * gripRecoilY + 2 * recoilForce * recoilForce * gunRecoilX * pose * recoil * (4 / (customWeight + 4))).toFloat() * times

            if (rotateX < 0) {
                rotateX *= 1.8f
            }

            player.xRot -= rotateX
            player.xRotO = player.xRot
        }
    }

    private fun handleShockCamera(event: ViewportEvent.ComputeCameraAngles, entity: LivingEntity) {
        val player = entity as? Player ?: return
        if (player.isSpectator) return

        if (entity.hasEffect(ModMobEffects.SHOCK) && mc.options.cameraType == CameraType.FIRST_PERSON) {
            val shakeStrength = DisplayConfig.SHOCK_SCREEN_SHAKE.get().toFloat() / 100.0f
            if (shakeStrength <= 0.0f) return
            event.yaw = mc.gameRenderer.mainCamera.yRot +
                    Mth.nextDouble(RandomSource.create(), -3.0, 3.0).toFloat() * shakeStrength
            event.pitch = mc.gameRenderer.mainCamera.xRot +
                    Mth.nextDouble(RandomSource.create(), -3.0, 3.0).toFloat() * shakeStrength
        }
    }

    @JvmStatic
    fun handleReloadShake(boneRotX: Double, boneRotY: Double, boneRotZ: Double) {
        val player = localPlayer ?: return
        if (player.isSpectator) return

        val shakeStrength = DisplayConfig.WEAPON_SCREEN_SHAKE.get().toFloat() / 100.0f
        if (shakeStrength <= 0.0f) return

        cameraRot[0] = -boneRotX * shakeStrength
        cameraRot[1] = -boneRotY * shakeStrength
        cameraRot[2] = -boneRotZ * shakeStrength
    }

    private fun handlePlayerCamera(event: ViewportEvent.ComputeCameraAngles) {
        val yaw = event.yaw
        val pitch = event.pitch
        val roll = event.roll
        val times = getDelta().coerceAtMost(0.8f)
        val player = localPlayer

        if (GLFW.glfwGetKey(mc.window.window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            cameraLocation = (cameraLocation - 0.05 * getDelta()).coerceIn(-0.6, 0.6)
        }

        if (GLFW.glfwGetKey(mc.window.window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            cameraLocation = (cameraLocation + 0.05 * getDelta()).coerceIn(-0.6, 0.6)
        }

        if (player == null) return

        val lookingEntity = SeekTool.seekEntity(player, 520.0, 5.0)
        val range: Double =
            if (lookingEntity != null) {
                player.distanceTo(lookingEntity).coerceAtLeast(0.01f).toDouble()
            } else {
                player.position().distanceTo(
                    (Vec3.atLowerCornerOf(
                        player.level().clip(
                            ClipContext(
                                player.eyePosition,
                                player.eyePosition.add(player.lookAngle.scale(520.0)),
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                player
                            )
                        ).blockPos
                    ))
                ).coerceAtLeast(0.01)
            }

        lookDistance = Mth.lerp(0.2 * times, lookDistance, range)

        val angle =
            if (lookDistance != 0.0 && cameraLocation != 0.0) {
                atan(abs(cameraLocation) / (lookDistance + 2.9)) * Mth.RAD_TO_DEG
            } else {
                0.0
            }

        var r = 1

        if (mc.options.cameraType != CameraType.FIRST_PERSON) {
            r = 0
        }

        event.pitch = (pitch + cameraRot[0] + (if (DisplayConfig.CAMERA_ROTATE.get()) 0.2 else 0.0) * turnRot[0]
                + 3 * velocityY).toFloat()
        if (mc.options.cameraType == CameraType.THIRD_PERSON_BACK) {
            event.yaw =
                (yaw + cameraRot[1] + (if (DisplayConfig.CAMERA_ROTATE.get()) 0.8 else 0.0) * turnRot[1] * r - angle * zoomPos).toFloat()
        } else {
            event.yaw =
                (yaw + cameraRot[1] + (if (DisplayConfig.CAMERA_ROTATE.get()) 0.8 else 0.0) * turnRot[1]).toFloat()
        }

        cameraRoll =
            (roll + cameraRot[2] + (if (DisplayConfig.CAMERA_ROTATE.get()) 0.35 else 0.0) * turnRot[2]).toFloat()
    }

    private fun handleBowPullAnimation(entity: LivingEntity, stack: ItemStack) {
        val times = 4 * getDelta().coerceAtMost(0.8f)
        val data = GunData.from(stack)

        if (holdingFireKey && data.hasEnoughAmmoToShoot(entity) && !bowPull && stack.`is`(ModItems.BOCEK.get())) {
            entity.playSound(ModSounds.BOCEK_PULL_1P.get(), 1f, 1f)
            bowPull = true
        }

        if (bowPull) {
            bowPullTimer = (bowPullTimer + 0.024 * times).coerceAtMost(1.4)
            bowPower = (bowPower + 0.018 * times).coerceAtMost(1.0)
        } else {
            bowPullTimer = (bowPullTimer - 0.021 * times).coerceAtLeast(0.0)
            bowPower = (bowPower - 0.04 * times).coerceAtLeast(0.0)
        }
        bowPullPos = 0.5 * cos(PI * (bowPullTimer.coerceIn(0.0, 1.0).pow(2) - 1).pow(2)) + 0.5
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun captureFov(event: ViewportEvent.ComputeFov) {
        if (event.usedConfiguredFov()) {
            fov = event.fov
        }
    }

    @SubscribeEvent
    fun onFovUpdate(event: ViewportEvent.ComputeFov) {
        val player = localPlayer ?: return
        val times = getDelta().coerceAtMost(1.6f)

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player) && zoomVehicle) {
            event.fov /= vehicle.getDefaultZoom(player)
            currentFov = event.fov
            return
        }

        val stack = player.mainHandItem

        val factor: Double =
            if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())
                && mc.options.cameraType == CameraType.FIRST_PERSON
            ) {
                4.0 + artilleryIndicatorCustomZoom
            } else {
                1.0
            }

        artilleryIndicatorZoom = Mth.lerp(0.3 * times, artilleryIndicatorZoom, factor)

        event.fov /= artilleryIndicatorZoom

        if (stack.item is GunItem) {
            if (!event.usedConfiguredFov()) {
                lastX = player.xRot
                lastY = player.yRot
                return
            }

            val p = if (stack.`is`(ModItems.BOCEK.get())) {
                bowPullPos * zoomTime
            } else {
                zoomPos
            }

            val data = GunData.from(stack)

            customZoom = Mth.lerp(0.6 * times, customZoom, data.zoom() + if (breath) 0.75 else 0.0)

            if (mc.options.cameraType.isFirstPerson) {
                event.fov /= (1 + p * (customZoom - 1))
            } else if (mc.options.cameraType == CameraType.THIRD_PERSON_BACK)
                event.fov /= (1 + p * 0.01)
            currentFov = event.fov

            // 智慧芯片
            if (zoom && !notInGame && drawTime < 0.01 && !isEditing) {
                if (player.isShiftKeyDown) {
                    lockedEntity = null
                } else {
                    val intelligentChipLevel = data.perk.getLevel(ModPerks.INTELLIGENT_CHIP)
                    val seekRange = 32.0 + 8.0 * (intelligentChipLevel - 1)

                    if (intelligentChipLevel > 0) {
                        if (lockedEntity == null || !lockedEntity!!.isAlive) {
                            lockedEntity = if (data.perk.has(ModPerks.PHASE_PENETRATING_BULLET.get())
                                || data.perk.has(ModPerks.BEAST_BULLET.get())
                            ) {
                                SeekTool.seekEntityThroughWall(player, seekRange, 16 / customZoom)
                            } else {
                                SeekTool.seekLivingEntity(player, seekRange, 16 / customZoom)
                            }
                        }
                        if (lockedEntity != null && lockedEntity!!.isAlive) {
                            val targetVec = lockedEntity!!.getEyePosition(event.partialTick.toFloat())
                            val playerVec = player.getEyePosition(event.partialTick.toFloat())

                            val hasGravity = data.perk.getLevel(ModPerks.MICRO_MISSILE) <= 0
                            val velocity =
                                if (stack.`is`(ModItems.BOCEK.get())) {
                                    zoomTime * 24
                                } else {
                                    data.get(GunProp.VELOCITY)
                                }

                            val toVec = RangeTool.calculateFiringSolution(
                                playerVec,
                                targetVec,
                                lockedEntity!!.deltaMovement.scale(0.5),
                                velocity,
                                if (hasGravity) data.get(GunProp.GRAVITY) else 0.0
                            )

                            look(player, toVec)

                            if (player.distanceTo(lockedEntity!!) > seekRange) {
                                lockedEntity = null
                            }
                        }
                    }
                }
            } else {
                lockedEntity = null
            }

            lastX = player.xRot
            lastY = player.yRot
        }

        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            droneFovLerp = Mth.lerp(0.1 * getDelta(), droneFovLerp, droneFov)
            event.fov /= droneFovLerp
            currentFov = event.fov
        }
    }

    fun look(player: Player, target: Vec3) {
        val d0 = target.x
        val d1 = target.y
        val d2 = target.z
        val d3 = sqrt(d0 * d0 + d2 * d2)

        val fromX = lastX
        val fromY = Mth.wrapDegrees(lastY)
        val toX = Mth.wrapDegrees(-(Mth.atan2(d1, d3) * 57.2957763671875)).toFloat()
        val toY = Mth.wrapDegrees((Mth.atan2(d2, d0) * 57.2957763671875) - 90F).toFloat()

        val diffY = Mth.wrapDegrees(toY - fromY)
        val finalY = Mth.wrapDegrees(fromY + diffY * 0.2F)

        player.xRot = Mth.wrapDegrees(Mth.lerp(0.2F, fromX, toX))
        player.yRot = Mth.wrapDegrees(finalY)
    }

    @SubscribeEvent
    fun setPlayerInvisible(event: RenderPlayerEvent.Pre) {
        val otherPlayer = event.entity
        val vehicle = otherPlayer.vehicle
        if (vehicle is VehicleEntity && vehicle.hidePassenger(otherPlayer)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun handleRenderCrossHair(event: RenderGuiLayerEvent.Pre) {
        if (event.name != VanillaGuiLayers.CROSSHAIR) return
        val player = localPlayer ?: return
        if (!mc.options.cameraType.isFirstPerson) return

        if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
            event.isCanceled = true
        }

        val stack = player.mainHandItem
        if (stack.item is GunItem) {
            event.isCanceled = true
        }

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
            event.isCanceled = true
        }

        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            event.isCanceled = true
        }
    }

    /**
     * 载具banHand时，禁用快捷栏渲染
     */
    @SubscribeEvent
    fun handleAvoidRenderingHotbar(event: RenderGuiLayerEvent.Pre) {
        if (event.name != VanillaGuiLayers.HOTBAR) return
        val player = localPlayer ?: return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) {
            event.isCanceled = true
        }
    }

    fun resetGunStatus() {
        drawTime = 1.0
        for (i in 0..<5) {
            shellIndexTime[i] = 0.0
        }
        clientTimer.stop()
        zoom = false
//        holdingFireKey = false
        holdingFireKeyTicks = 0
        holdingFireKeyTicks0 = 0f
        ClickEventHandler.switchZoom = false
        burstFireAmount = 0
        bowPullTimer = 0.0
        bowPower = 0.0
        noSprintTicks = 10f
        seekingTime = 0
        lockOn = false
        lockingEntity = null
        seekingEntity = null
        lockingPos = null
        isEditing = false
        zoomTime = 0.0
    }

    fun resetLungeMineStatus() {
        lungeDraw = 30
        lungeSprint = 0
        lungeAttack = 0
        usingLunge = false
    }

    private fun handleWeaponDraw(entity: LivingEntity) {
        val times = getDelta()
        val stack = entity.mainHandItem
        val data = GunData.from(stack)
        val weight = data.get(GunProp.WEIGHT)
        val speed = 20 / (weight + 5)
        drawTime = (drawTime - (0.2 * speed * times * drawTime).coerceAtLeast(0.0008)).coerceAtLeast(0.0)
    }

    @JvmStatic
    fun handleShells(x: Float, y: Float, vararg shells: GeoBone) {
        for (i in 0..<shells.size) {
            if (i >= 5) break
            shells[i].posX = (-x * shellIndexTime[i] * ((150 - shellIndexTime[i]) / 150)).toFloat()
            shells[i].posY = (y * randomShell[0] * shellIndexTime[i] - 0.025 * shellIndexTime[i].pow(2)).toFloat()
            shells[i].rotX = (randomShell[1] * shellIndexTime[i]).toFloat()
            shells[i].rotY = (randomShell[2] * shellIndexTime[i]).toFloat()
        }
    }

    fun aimAtVillager(player: Player) {
        if (aimVillagerCountdown > 0) return

        if (zoom) {
            val entity = TraceTool.findLookingEntity(player, 10.0) as? AbstractVillager ?: return
            val entities = SeekTool.seekLivingEntities(entity, 16.0, 120.0)
            for (e in entities) {
                if (e == player) {
                    sendPacketToServer(AimVillagerMessage(entity.id))
                    aimVillagerCountdown = 80
                }
            }
        }
    }

    /**
     * 能否开启改枪GUI，只有在当前没有待发射的子弹，且物品为武器，主手持有的情况下才能开启
     *
     * @param stack 待改装武器
     * @param hand  持有武器的手
     * @return 能否成功打开GUI
     */
    @JvmStatic
    fun canOpenEditScreen(stack: ItemStack, hand: InteractionHand?): Boolean {
        return burstFireAmount == 0 && stack.item is GunItem && hand == InteractionHand.MAIN_HAND
    }

    @JvmStatic
    fun onOpenEditScreen() {
        val player = localPlayer ?: return
        isEditing = true
        holdingFireKey = false
        player.playSound(ModSounds.EDIT_MODE.get(), 1f, 1f)
    }

    @JvmStatic
    fun onCloseEditScreen() {
        isEditing = false
    }

    @JvmStatic
    fun editModelShake() {
        velocityY = 0.2
    }

    @JvmStatic
    fun stopSoundEvent(location: ResourceLocation, source: SoundSource) {
        mc.soundManager.stop(location, source)
    }

    @JvmStatic
    fun stopVehicleSeekSound(player: Player?) {
        if (player == null) return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity) {
            val gunData = vehicle.getGunData(player) ?: return
            val location = gunData.get(GunProp.SOUND_INFO).locking.location
            stopSoundEvent(location, SoundSource.PLAYERS)
        }
    }

    @JvmStatic
    fun stopWeaponSeekSound(player: Player?) {
        if (player == null) return
        val stack = player.mainHandItem
        if (stack.item is GunItem) {
            val gunData = GunData.from(stack)
            val location = gunData.get(GunProp.SOUND_INFO).locking.location
            stopSoundEvent(location, SoundSource.PLAYERS)
        }
    }

    @JvmStatic
    fun stopVehicleReloadSound(player: Player?) {
        if (player == null) return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity) {
            val gunData = vehicle.getGunData(player) ?: return
            val location = gunData.get(GunProp.SOUND_INFO).vehicleReload.location
            stopSoundEvent(location, SoundSource.PLAYERS)
        }
    }

    @SubscribeEvent
    fun onRenderNameTag(event: RenderNameTagEvent) {
        val entity = event.entity as? Player ?: return
        val self = localPlayer ?: return
        if (self == entity) return
        if (self.vehicle !is VehicleEntity) return
        if (self.isPassengerOfSameVehicle(entity)) {
            event.setCanRender(TriState.FALSE)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        if (!DisplayConfig.ENABLE_VERSION_CHECK_WARNING.get()) return
        val player = event.entity ?: return
        if (ModVersionEventHandler.currentVersion == null || ModVersionEventHandler.previousVersion == null) return

        player.displayClientMessage(
            Component.translatable(
                "tips.superbwarfare.vehicle_reset_kit_1",
                Component.literal("" + ModVersionEventHandler.previousVersion).withStyle(ChatFormatting.YELLOW),
                Component.literal("" + ModVersionEventHandler.currentVersion).withStyle(ChatFormatting.YELLOW)
            )
                .withStyle(ChatFormatting.RED), false
        )
        player.displayClientMessage(
            Component.translatable(
                "tips.superbwarfare.vehicle_reset_kit_2",
                Component.literal("[").append(ModItems.VEHICLE_RESET_KIT.get().defaultInstance.hoverName)
                    .append("]").withStyle(ChatFormatting.GREEN)
            ), false
        )
        player.displayClientMessage(
            Component.translatable("tips.superbwarfare.vehicle_reset_kit_3")
                .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.UNDERLINE), false
        )
    }

    @SubscribeEvent
    fun onFogColor(event: ViewportEvent.ComputeFogColor) {
        if (activeThermalImaging) {
            event.red = 0.1F
            event.green = 0.1F
            event.blue = 0.1F
        }
    }
}