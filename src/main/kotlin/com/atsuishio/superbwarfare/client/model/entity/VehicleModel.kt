package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.vehicle.DefaultVehicleResource
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import oshi.util.tuples.Pair
import software.bernie.geckolib.core.animatable.GeoAnimatable
import software.bernie.geckolib.core.animatable.model.CoreGeoBone
import software.bernie.geckolib.core.animation.AnimationState
import software.bernie.geckolib.model.GeoModel
import java.util.regex.Matcher
import java.util.regex.Pattern

@Deprecated("Geckolib will be removed since 0.8.10", ReplaceWith("BedrockVehicleModel"))
open class VehicleModel<T> : GeoModel<T>() where T : VehicleEntity, T : GeoAnimatable {
    protected var pitch = 0f
    protected var yaw = 0f
    protected var roll = 0f
    protected var leftWheelRot = 0f
    protected var rightWheelRot = 0f
    protected var leftTrack = 0f
    protected var rightTrack = 0f

    protected var turretYRot = 0f

    protected var turretXRot = 0f
    protected var turretYaw = 0f
    protected var recoilShake = 0f

    protected var hideForTurretControllerWhileZooming = false
    protected var hideForPassengerWeaponStationControllerWhileZooming = false

    private val LOGGER = ResourceOnceLogger()

    override fun getAnimationResource(vehicle: T): ResourceLocation? {
        return getDefault(vehicle).getModel().animation
    }

    protected var modelCache: ResourceLocation? = null

    override fun getModelResource(vehicle: T): ResourceLocation? {
        if (RenderDistanceHelper.isInGui()) {
            return getDefault(vehicle).getModel().model
        }

        val lodLevel = getLODLevel(vehicle)
        val lodModel: ResourceLocation? = getDefault(vehicle).getModel().getLODModel(lodLevel)

        if (lodModel == null) {
            if (modelCache != null) {
                return modelCache
            }

            LOGGER.log(vehicle) { logger -> logger.error("failed to load model for {}!", vehicle) }
            val loc = loc("geo/" + EntityType.getKey(vehicle.type).path + ".geo.json")
            modelCache = loc
            return loc
        }

        modelCache = lodModel
        return lodModel
    }

    fun getPreciseModelResource(vehicle: T): ResourceLocation? = getDefault(vehicle).getModel().model

    protected var textureCache: ResourceLocation? = null

    override fun getTextureResource(vehicle: T): ResourceLocation? {
        if (RenderDistanceHelper.isInGui()) {
            return getDefault(vehicle).getModel().texture
        }

        val lodLevel = getLODLevel(vehicle)
        val lodTexture: ResourceLocation? = getDefault(vehicle).getModel().getLODTexture(lodLevel)

        if (lodTexture == null) {
            if (textureCache != null) {
                return textureCache
            }

            LOGGER.log(vehicle) { logger -> logger.error("failed to load texture for {}!", vehicle) }
            val loc = loc("textures/entity/" + EntityType.getKey(vehicle.type).path + ".png")
            textureCache = loc
            return loc
        }

        textureCache = lodTexture
        return lodTexture
    }

    fun getPreciseTextureResource(vehicle: T): ResourceLocation? = getDefault(vehicle).getModel().texture

    fun getLODLevel(vehicle: T): Int {
        val defaultData: DefaultVehicleResource = getDefault(vehicle)
        val model = defaultData.getModel()
        if (defaultData.lodDistance == null || defaultData.lodDistance.list.isEmpty() || !model.hasLOD()) return 0

        val player: Player? = Minecraft.getInstance().player
        if (player == null || player.isScoping) return 0

        val distance = player.position().distanceTo(vehicle.position())
        for (i in defaultData.lodDistance.list.indices) {
            if (distance <= defaultData.lodDistance.list[i]) {
                return i
            }
        }

        return Int.MAX_VALUE
    }

    fun interface TransformContext<T> where T : VehicleEntity, T : GeoAnimatable {
        fun transform(bone: CoreGeoBone, vehicle: T, animationState: AnimationState<T>)
    }

    protected var init = false

    protected val TRANSFORMS = mutableListOf<Pair<String, TransformContext<T>>>()

    open fun collectTransform(boneName: String): TransformContext<T>? {
        // 瞄准时隐藏车体
        if (boneName == "root" && hideForTurretControllerWhileZooming()) {
            return TransformContext { bone, _, _ ->
                bone.isHidden = hideForTurretControllerWhileZooming
            }
        }

        // 瞄准时隐藏乘客武器站
        if (boneName == "passengerWeaponStation" && hideForTurretControllerWhileZooming()) {
            return TransformContext { bone, _, _ ->
                bone.isHidden = hideForPassengerWeaponStationControllerWhileZooming
            }
        }

        if (boneName == "laser") {
            return TransformContext { bone, vehicle, state ->
                bone.scaleZ = 10 * vehicle.laserLength
                val scale = Mth.lerp(
                    state.partialTick,
                    vehicle.laserScaleO,
                    vehicle.laserScale
                ).coerceAtMost(1.2f)

                bone.scaleX = scale
                bone.scaleY = scale
            }
        }

        //射击时带来的车体摇晃视觉效果
        when (boneName) {
            "base" -> {
                return TransformContext { bone, vehicle, _ ->
                    val a = vehicle.yawWhileShoot
                    val r = (Mth.abs(a) - 90f) / 90f

                    val r2 = if (Mth.abs(a) <= 90f) {
                        a / 90f
                    } else {
                        if (a < 0) {
                            -(180f + a) / 90f
                        } else {
                            (180f - a) / 90f
                        }
                    }

                    bone.posX = r2 * recoilShake * 0.5f
                    bone.posZ = r * recoilShake * 1f
                    bone.rotX = r * recoilShake * Mth.DEG_TO_RAD
                    bone.rotZ = r2 * recoilShake * Mth.DEG_TO_RAD
                }
            }

            "turret" -> {
                return TransformContext { bone, vehicle, _ ->
                    bone.rotY = turretYRot * Mth.DEG_TO_RAD
                    val turretLaser = animationProcessor.getBone("turretLaser")
                    turretLaser?.rotY = bone.rotY

                    bone.isHidden = vehicle.isWreck && vehicle.hasTurret() && vehicle.sympatheticDetonated
                }
            }

            "barrel" -> {
                return TransformContext { bone, vehicle, _ ->
                    bone.rotX = Mth.clamp(
                        -turretXRot,
                        vehicle.turretMinPitch,
                        vehicle.turretMaxPitch
                    ) * Mth.DEG_TO_RAD

                    val barrelLaser = animationProcessor.getBone("barrelLaser")
                    barrelLaser?.rotX = bone.rotX
                }
            }

            "passengerWeaponStationYaw" -> {
                return TransformContext { bone, vehicle, state ->
                    bone.rotY = Mth.lerp(
                        state.partialTick,
                        vehicle.gunYRotO,
                        vehicle.gunYRot
                    ) * Mth.DEG_TO_RAD - turretYRot * Mth.DEG_TO_RAD
                }
            }

            "passengerWeaponStationPitch" -> {
                return TransformContext { bone, vehicle, state ->

                    bone.rotX = Mth.clamp(
                        -Mth.lerp(
                            state.partialTick,
                            vehicle.gunXRotO,
                            vehicle.gunXRot
                        ) * Mth.DEG_TO_RAD,
                        vehicle.passengerWeaponMinPitch * Mth.DEG_TO_RAD,
                        vehicle.passengerWeaponMaxPitch * Mth.DEG_TO_RAD
                    )
                }
            }
        }

        // track(Mov|Rot)[RL]\d+
        val trackMatcher: Matcher = TRACK_PATTERN.matcher(boneName)
        if (trackMatcher.matches()) {
            val isRot = trackMatcher.group("type") == "Rot"
            val isL = trackMatcher.group("direction") == "L"
            val index = trackMatcher.group("id").toInt()

            if (isRot) {
                return if (isL) {
                    TransformContext { bone, vehicle, _ ->
                        val t = wrap(leftTrack + getTrackDistance() * index, vehicle)
                        bone.rotX = -getBoneRotX(t) * Mth.DEG_TO_RAD
                    }
                } else {
                    TransformContext { bone, vehicle, _ ->
                        val t2 = wrap(rightTrack + getTrackDistance() * index, vehicle)
                        bone.rotX = -getBoneRotX(t2) * Mth.DEG_TO_RAD
                    }
                }
            } else {
                return if (isL) {
                    TransformContext { bone, vehicle, _ ->
                        val t = wrap(leftTrack + getTrackDistance() * index, vehicle)
                        bone.posY = getBoneMoveY(t)
                        bone.posZ = getBoneMoveZ(t)
                    }
                } else {
                    TransformContext { bone, vehicle, _ ->
                        val t2 = wrap(rightTrack + getTrackDistance() * index, vehicle)
                        bone.posY = getBoneMoveY(t2)
                        bone.posZ = getBoneMoveZ(t2)
                    }
                }
            }
        }

        val wheelMatcher: Matcher = WHEEL_PATTERN.matcher(boneName)
        if (wheelMatcher.matches()) {
            val isL = wheelMatcher.group("direction") == "L"

            return if (boneName.endsWith("Turn")) {
                TransformContext { bone, vehicle, state ->
                    bone.rotX = 1.5f * (if (isL) leftWheelRot else rightWheelRot)
                    bone.rotY = Mth.lerp(state.partialTick, vehicle.rudderRotO, vehicle.rudderRot)
                }
            } else {
                TransformContext { bone, _, _ -> bone.rotX = 1.5f * (if (isL) leftWheelRot else rightWheelRot) }
            }
        }

        return null
    }

    override fun setCustomAnimations(vehicle: T, instanceId: Long, animationState: AnimationState<T>) {
        if (!init) {
            animationProcessor.registeredBones.forEach { bone ->
                val name = bone.name
                try {
                    val transform = collectTransform(name)
                    if (transform != null) {
                        TRANSFORMS.add(Pair(name, transform))
                    }
                } catch (exception: Exception) {
                    Mod.LOGGER.error("failed to collect transform for vehicle {} bone {}:", vehicle, name, exception)
                }
            }
            init = true
        }

        val partialTick = animationState.partialTick

        pitch = vehicle.getPitch(partialTick)
        yaw = vehicle.getYaw(partialTick)
        roll = vehicle.getRoll(partialTick)

        leftWheelRot = Mth.lerp(partialTick, vehicle.leftWheelRotO, vehicle.leftWheelRot)
        rightWheelRot = Mth.lerp(partialTick, vehicle.rightWheelRotO, vehicle.rightWheelRot)

        leftTrack = Mth.lerp(partialTick, vehicle.leftTrackO, vehicle.leftTrack)
        rightTrack = Mth.lerp(partialTick, vehicle.rightTrackO, vehicle.rightTrack)

        turretYRot = Mth.lerp(partialTick, vehicle.turretYRotO, vehicle.turretYRot)
        turretXRot = Mth.lerp(partialTick, vehicle.turretXRotO, vehicle.turretXRot)

        turretYaw = vehicle.getTurretYaw(partialTick)

        recoilShake = Mth.lerp(partialTick, vehicle.recoilShakeO.toFloat(), vehicle.recoilShake.toFloat())

        hideForTurretControllerWhileZooming =
            ClientEventHandler.zoomVehicle && vehicle.getNthEntity(vehicle.turretControllerIndex) === Minecraft.getInstance().player
        hideForPassengerWeaponStationControllerWhileZooming =
            ClientEventHandler.zoomVehicle && vehicle.getNthEntity(vehicle.passengerWeaponStationControllerIndex) === Minecraft.getInstance().player

        TRANSFORMS.forEach { pair ->
            val name = pair.getA()
            val bone = animationProcessor.getBone(name)

            if (bone != null) {
                pair.getB().transform(bone, vehicle, animationState)
            }
        }
    }

    open fun hideForTurretControllerWhileZooming() = false

    open fun getBoneRotX(t: Float) = t
    open fun getBoneMoveY(t: Float) = t
    open fun getBoneMoveZ(t: Float) = t
    open fun getTrackDistance() = 2f

    protected fun wrap(value: Float, range: Int) = ((value % range) + range) % range

    protected fun wrap(value: Float, vehicle: VehicleEntity) = wrap(value, getDefaultWrapRange(vehicle))

    fun getDefaultWrapRange(vehicle: VehicleEntity) = vehicle.getTrackAnimationLength()

    companion object {
        private fun <T> getDefault(vehicle: T): DefaultVehicleResource where T : VehicleEntity, T : GeoAnimatable {
            return VehicleResource.getDefault(vehicle)
        }

        val TRACK_PATTERN: Pattern = Pattern.compile("^track(?<type>Mov|Rot)(?<direction>[LR])(?<id>\\d+)$")
        val WHEEL_PATTERN: Pattern = Pattern.compile("^wheel(?<direction>[LR]).*$")
    }
}
