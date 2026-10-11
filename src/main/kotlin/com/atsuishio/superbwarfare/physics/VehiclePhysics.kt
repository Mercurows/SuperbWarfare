package com.atsuishio.superbwarfare.physics

import cn.solarmoon.spark_core.physics.SparkPhysics
import cn.solarmoon.spark_core.physics.VehicleControlMode
import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.data.vehicle.subdata.OBBInfo
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.network.message.receive.VehiclePhysicsStateMessage
import com.atsuishio.superbwarfare.tools.OBB
import com.atsuishio.superbwarfare.tools.sendPacketToTrackingEntity
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.entity.EntityLeaveLevelEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.eventbus.api.EventPriority
import org.joml.Quaternionf
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
object VehiclePhysics {
    internal val bodies = ConcurrentHashMap<Entity, VehicleBody>()
    private val saved = ConcurrentHashMap<Entity, RigidState>()
    private val pending = ConcurrentHashMap<Entity, RigidState>()
    private val pendingRender = ConcurrentHashMap<Entity, VehiclePhysicsRenderState>()
    private val inputSequences = ConcurrentHashMap<Entity, Long>()
    private val controllerOwners = ConcurrentHashMap<Entity, Int>()
    private val localControl = ConcurrentHashMap<Entity, Int>()
    private val appliedPositions = ConcurrentHashMap<Entity, Vec3>()
    private val capturing = HashSet<Entity>() // game thread only
    private val writing = HashSet<Entity>()
    private var nextInput = 0L

    fun isWriting(entity: Entity) = entity in capturing || entity in writing
    /** True while a managed vehicle is being created or is already physical. */
    fun isManaged(entity: Entity) = bodies.containsKey(entity)
    /** True only after the Spark body has been inserted into the physics space. */
    fun hasBody(entity: Entity) = bodies[entity]?.ready == true
    fun sequence(entity: Entity) = inputSequences[entity] ?: 0L
    fun renderState(entity: Entity): VehiclePhysicsRenderState = bodies[entity]
        ?.takeIf { it.ready }?.renderState ?: VehiclePhysicsRenderState()

    fun localInput(vehicle: VehicleEntity, keys: Short): Long {
        val seq = ++nextInput
        inputSequences[vehicle] = seq
        localControl[vehicle] = vehicle.tickCount
        vehicle.processInput(keys)
        return seq
    }

    fun serverInput(vehicle: VehicleEntity, keys: Short, sequence: Long, player: Player) {
        if (controllerOwners.put(vehicle, player.id) != player.id) inputSequences.remove(vehicle)
        if (sequence <= (inputSequences[vehicle] ?: -1L)) return
        inputSequences[vehicle] = sequence
        vehicle.processInput(keys)
    }

    private fun rotation(entity: Entity): Quaternionf {
        val value = when (entity) {
        is VehicleEntity -> Quaternionf().rotationYXZ(Math.toRadians(-entity.yRot.toDouble()).toFloat(),
            Math.toRadians(entity.xRot.toDouble()).toFloat(), Math.toRadians(entity.roll.toDouble()).toFloat())
        is TurretWreckEntity -> Quaternionf(entity.getQuaternion(1f))
        else -> Quaternionf()
        }
        return if (value.isFinite && value.lengthSquared() > .0001f) value.normalize() else Quaternionf()
    }

    /**
     * Capture every value the Spark worker needs while we are still on the game
     * thread. The result holds no Entity, Level or Bullet reference.
     */
    private fun buildSpec(entity: Entity, state: RigidState): VehiclePhysicsSpec {
        val vehicle = entity as? VehicleEntity
        val data = vehicle?.computed()
        val info = vehicle?.getCollisionOBBInfo()
        val settings = data?.physics ?: VehiclePhysicsSettings()
        val pivot = vehicle?.rotateOffsetHeight ?: 0.0
        // Keep the Bullet body origin in the same pivoted frame as the model.
        // The body shape itself is assembled from the current world OBBs below,
        // then transformed back into this frame so animated model transforms
        // cannot leave collision boxes behind.
        val center = (info?.position ?: Vec3(0.0, entity.bbHeight / 2.0, 0.0))
            .add(settings.centerOfMassOffset).add(0.0, -pivot, 0.0)
        val bodyRotation = Quaternionf(state.rotation)
            .takeIf { it.isFinite && it.lengthSquared() > .0001f }
            ?.normalize() ?: Quaternionf()
        val bodyWorldCenter = state.position.add(0.0, pivot, 0.0)
            .add(bodyRotation.rotate(center))
        if (vehicle != null && data != null) vehicle.updateOBB()
        val collisionParts = resolveCollisionParts(
            vehicle, data, info, settings, bodyRotation, bodyWorldCenter
        )
        val half = info?.size ?: collisionParts.fold(Vec3.ZERO) { current, part ->
            Vec3(max(current.x, abs(part.center.x) + part.half.x),
                max(current.y, abs(part.center.y) + part.half.y),
                max(current.z, abs(part.center.z) + part.half.z))
        }.let { if (it == Vec3.ZERO) Vec3(entity.bbWidth / 2.0, entity.bbHeight / 2.0, entity.bbWidth / 2.0) else it }
        val engine = data?.engineType
        val ground = engine == EngineType.WHEEL || engine == EngineType.TRACK || engine == EngineType.WHEELCHAIR
        val flight = engine == EngineType.AIRCRAFT || engine == EngineType.HELICOPTER ||
            engine == EngineType.AIRSHIP || engine == EngineType.TOM6
        val suspensionSides = resolveSuspensionSides(data, info, half)
        return VehiclePhysicsSpec(
            clientSide = entity.level().isClientSide,
            half = half,
            localCenter = center,
            pivot = pivot,
            massKg = VehiclePhysicsUnits.massKg(vehicle?.mass ?: 1f),
            anchored = engine == EngineType.FIXED,
            groundVehicle = ground,
            trackedVehicle = engine == EngineType.TRACK,
            flightVehicle = flight,
            fixedWing = engine == EngineType.AIRCRAFT || engine == EngineType.TOM6,
            settings = settings,
            collisionParts = collisionParts,
            suspensionSides = suspensionSides,
            initial = state,
        )
    }

    /** Capture all fixed vehicle OBBs in the same local frame as the Bullet body. */
    private fun resolveCollisionParts(
        vehicle: VehicleEntity?,
        data: DefaultVehicleData?,
        collision: OBBInfo?,
        settings: VehiclePhysicsSettings,
        bodyRotation: Quaternionf,
        bodyWorldCenter: Vec3,
    ): List<CollisionPart> {
        if (data == null) return emptyList()
        val reference = collision?.position ?: Vec3.ZERO
        val inverseBodyRotation = Quaternionf(bodyRotation).conjugate()
        val runtimeObbs = vehicle?.getOBBs()
        return data.obb.mapIndexedNotNull { index, info ->
            val transform = info.transform?.lowercase() ?: "default"
            if (transform == "turret" || transform == "barrel" || transform.contains("weapon") ||
                info.part == OBB.Part.TURRET
            ) return@mapIndexedNotNull null
            val half = info.size
            if (!half.x.isFinite() || !half.y.isFinite() || !half.z.isFinite() ||
                half.x <= .001 || half.y <= .001 || half.z <= .001) return@mapIndexedNotNull null

            // VehicleEntity.updateOBB() has already applied the named transform,
            // model rotation and CustomRotate. Convert that exact world OBB into
            // the Bullet body's local frame instead of reconstructing only part
            // of the transform from the data definition.
            val world = runtimeObbs?.getOrNull(index)
            if (world != null && world.center.x.isFinite() && world.center.y.isFinite() &&
                world.center.z.isFinite() && world.rotation.x.isFinite() &&
                world.rotation.y.isFinite() && world.rotation.z.isFinite() &&
                world.rotation.w.isFinite()
            ) {
                val localCenter = inverseBodyRotation.rotate(
                    Vec3(world.center.x, world.center.y, world.center.z).subtract(bodyWorldCenter)
                )
                val worldRotation = Quaternionf(
                    world.rotation.x.toFloat(), world.rotation.y.toFloat(),
                    world.rotation.z.toFloat(), world.rotation.w.toFloat()
                )
                val localRotation = inverseBodyRotation.mul(worldRotation).normalize()
                return@mapIndexedNotNull CollisionPart(half, localCenter, localRotation)
            }

            // Fallback for entities that have not built their runtime OBB cache
            // yet. This keeps the body spawnable while preserving the authored
            // collision reference and centre-of-mass offset.
            CollisionPart(
                half = half,
                center = info.position.subtract(reference).subtract(settings.centerOfMassOffset),
                rotation = Quaternionf()
                    .rotateY(Math.toRadians(info.customRotate.y).toFloat())
                    .rotateX(Math.toRadians(info.customRotate.x).toFloat())
                    .rotateZ(Math.toRadians(info.customRotate.z).toFloat()),
            )
        }
    }

    /** Build exactly one suspension footprint for each side of every ground vehicle. */
    private fun resolveSuspensionSides(data: DefaultVehicleData?, info: OBBInfo?, half: Vec3): List<SideSuspensionSpec> {
        if (data == null) return emptyList()
        val originX = info?.position?.x ?: 0.0
        val originY = info?.position?.y ?: 0.0
        val originZ = info?.position?.z ?: 0.0
        data class Bounds(
            var minX: Double = Double.POSITIVE_INFINITY,
            var maxX: Double = Double.NEGATIVE_INFINITY,
            var minY: Double = Double.POSITIVE_INFINITY,
            var minZ: Double = Double.POSITIVE_INFINITY,
            var maxZ: Double = Double.NEGATIVE_INFINITY,
            var width: Double = 0.0,
        )
        val bounds = mutableMapOf(WheelSide.LEFT to Bounds(), WheelSide.RIGHT to Bounds())

        fun add(side: WheelSide, x: Double, y: Double, z: Double, lateral: Double, vertical: Double, longitudinal: Double) {
            val b = bounds.getValue(side)
            b.minX = min(b.minX, x - lateral)
            b.maxX = max(b.maxX, x + lateral)
            b.minY = min(b.minY, y - vertical)
            b.minZ = min(b.minZ, z - longitudinal)
            b.maxZ = max(b.maxZ, z + longitudinal)
            b.width = max(b.width, lateral)
        }

        data.wheels.forEach { wheel ->
            add(
                if (wheel.side == WheelSide.RIGHT) WheelSide.RIGHT else WheelSide.LEFT,
                wheel.position.x - originX,
                wheel.position.y - originY,
                wheel.position.z - originZ,
                wheel.width.coerceIn(.05, 2.0),
                wheel.radius.coerceIn(.05, 2.0),
                wheel.radius.coerceIn(.05, 2.0),
            )
        }
        data.obb.forEach { obb ->
            val side = when (obb.part) {
                OBB.Part.WHEEL_LEFT -> WheelSide.LEFT
                OBB.Part.WHEEL_RIGHT -> WheelSide.RIGHT
                else -> null
            } ?: return@forEach
            add(
                side,
                obb.position.x - originX,
                obb.position.y - originY,
                obb.position.z - originZ,
                obb.size.x.coerceIn(.05, 2.0),
                obb.size.y.coerceIn(.05, 2.0),
                obb.size.z.coerceIn(.05, 8.0),
            )
        }

        return listOf(WheelSide.LEFT, WheelSide.RIGHT).map { side ->
            val b = bounds.getValue(side)
            if (!b.minX.isFinite()) {
                val x = if (side == WheelSide.LEFT) -max(half.x * .72, .05) else max(half.x * .72, .05)
                SideSuspensionSpec(side, x, -half.y, 0.0, max(half.x * .14, .08), max(half.z * .72, .12))
            } else {
                SideSuspensionSpec(
                    side = side,
                    anchorX = (b.minX + b.maxX) * .5,
                    bottomY = b.minY,
                    centerZ = (b.minZ + b.maxZ) * .5,
                    halfWidth = max(b.width, (b.maxX - b.minX) * .5).coerceIn(.05, 2.0),
                    halfLength = ((b.maxZ - b.minZ) * .5).coerceIn(.12, 8.0),
                )
            }
        }
    }

    fun begin(entity: Entity) {
        if (entity.isRemoved) return
        if (entity !is VehicleEntity && entity !is TurretWreckEntity) return
        val handle = bodies.computeIfAbsent(entity) {
            // Build the immutable spec on the game thread. Everything the worker
            // needs is copied here so it never touches the Entity or the Level.
            val state = saved.remove(entity) ?: RigidState(0, 0, entity.position(), rotation(entity),
                VehiclePhysicsUnits.velocity(entity.deltaMovement), Vec3.ZERO, false)
            val spec = buildSpec(entity, state)
            VehicleBody(SparkPhysics.get(entity.level()), SparkPhysics.host(entity), spec, state).also { body ->
                body.physics.submitImmediateTask { body.create() }
            }
        }
        pending.remove(entity)?.let { handle.authoritative = it }
        pendingRender.remove(entity)?.let { handle.authoritativeRender = it }
        if (!handle.ready) {
            // Body creation runs on Spark's worker. Until it is inserted, the
            // vanilla Entity tick must not accumulate gravity or steering
            // velocity that would be handed to Bullet as a large first-step
            // impulse. Keep the entity at its placement pose and let Bullet
            // start the simulation from a clean zero velocity.
            writing.add(entity)
            try {
                entity.deltaMovement = Vec3.ZERO
            } finally {
                writing.remove(entity)
            }
            return
        }

        if (handle.needsInitialSync) {
            handle.needsInitialSync = false
            val initial = RigidState(0, sequence(entity), entity.position(), rotation(entity),
                VehiclePhysicsUnits.velocity(entity.deltaMovement), Vec3.ZERO, entity.onGround())
            handle.state = initial
            handle.physics.submitImmediateTask { handle.reset(initial) }
        }
        val previous = appliedPositions[entity]
        val entityRotation = rotation(entity)
        val rotationDot = abs(entityRotation.dot(handle.state.rotation))
        val externallyRotated = rotationDot < cos(Math.toRadians(7.5).toFloat())
        if (!entity.level().isClientSide && previous != null &&
            (entity.position().distanceToSqr(previous) > 1.0 || externallyRotated)
        ) {
            val reset = handle.state.copy(position = entity.position(), rotation = entityRotation,
                angularVelocity = Vec3.ZERO)
            handle.physics.submitImmediateTask { handle.reset(reset) }
        }
        // The client never simulates the vehicle; it only interpolates toward
        // the last snapshot the server published.
        apply(entity, handle.state, entity.level().isClientSide)
        appliedPositions[entity] = entity.position()
        if (!entity.level().isClientSide) {
            var maxImpact = 0.0
            val struck = HashMap<LivingEntity, Double>()
            while (true) {
                val event = handle.impacts.poll() ?: break
                val contact = event.contact
                maxImpact = max(maxImpact, contact.normalImpulse / handle.mass.toDouble())
                val other = event.otherOwner as? LivingEntity
                if (other != null && (other !is Player || !other.isSpectator))
                    struck[other] = max(struck[other] ?: 0.0, contact.closingSpeed)
            }
            if (entity is VehicleEntity && entity.collisionCoolDown <= 0) {
                struck.forEach { (other, speed) ->
                    if (speed > 6.0 && other.isAlive && other.vehicle !== entity) {
                        other.hurt(ModDamageTypes.causeVehicleStrikeDamage(entity.level().registryAccess(), entity,
                            entity.firstPassenger ?: entity), ((speed - 6.0).pow(2) * .5).toFloat())
                    }
                }
            }
            if (entity is VehicleEntity && maxImpact > 4.0 && entity.collisionCoolDown <= 0) {
                entity.collisionCoolDown = 4
                entity.hurt(ModDamageTypes.causeVehicleStrikeDamage(entity.level().registryAccess(), entity, entity.lastDriver ?: entity),
                    ((maxImpact - 4.0).pow(2) * .3).toFloat())
            }
            if (entity is TurretWreckEntity && maxImpact > 4.0 && entity.collisionCoolDown <= 0) {
                entity.collisionCoolDown = 4
                entity.hurt(ModDamageTypes.causeVehicleStrikeDamage(entity.level().registryAccess(), entity, entity),
                    ((maxImpact - 4.0).pow(2) * .3).toFloat())
            }
            sendPacketToTrackingEntity(entity, VehiclePhysicsStateMessage.of(entity.id, handle.state, handle.renderState))
        }
    }

    private fun apply(entity: Entity, state: RigidState, smooth: Boolean) {
        if (!state.position.finite() || !state.velocity.finite()) return
        writing.add(entity)
        try {
            val distance = entity.position().distanceToSqr(state.position)
            entity.setPos(if (smooth && distance < 256) entity.position().lerp(state.position, .5) else state.position)
            entity.deltaMovement = VehiclePhysicsUnits.motion(state.velocity)
            entity.setOnGround(state.grounded)
            entity.verticalCollisionBelow = state.grounded
            val q = if (smooth) rotation(entity).slerp(state.rotation, .5f) else Quaternionf(state.rotation)
            if (entity is VehicleEntity) {
                val angles = q.getEulerAnglesYXZ(org.joml.Vector3f())
                entity.yRot = -Math.toDegrees(angles.y.toDouble()).toFloat()
                entity.xRot = Math.toDegrees(angles.x.toDouble()).toFloat()
                entity.setZRot(Math.toDegrees(angles.z.toDouble()).toFloat())
                entity.updateOBB()
            } else if (entity is TurretWreckEntity) entity.setQuaternion(org.joml.Quaterniond(q))
        } finally { writing.remove(entity) }
    }

    private fun engineNumber(vehicle: VehicleEntity, key: String, fallback: Double): Double =
        vehicle.computed().engineInfo[key]?.jsonPrimitive?.doubleOrNull?.takeIf { it.isFinite() } ?: fallback

    /** Run legacy engine bookkeeping transactionally, then emit a Bullet-only command. */
    fun control(vehicle: VehicleEntity, action: () -> Unit) {
        val handle = bodies[vehicle]?.takeIf { it.ready } ?: return
        val motion = vehicle.deltaMovement
        val origin = vehicle.position()
        val q = rotation(vehicle)
        val originalYaw = vehicle.yRot
        val originalPitch = vehicle.xRot
        val originalRoll = vehicle.roll
        val originalOnGround = vehicle.onGround()
        val sparkGrounded = handle.state.grounded
        val hasPassenger = vehicle.getFirstPassenger() != null
        val forwardInput = vehicle.forwardInputDown
        val backwardInput = vehicle.backInputDown
        val leftInput = vehicle.leftInputDown
        val rightInput = vehicle.rightInputDown
        val upInput = vehicle.upInputDown
        val downInput = vehicle.downInputDown
        capturing.add(vehicle)
        try {
            vehicle.setOnGround(sparkGrounded)
            action()
            val engine = vehicle.computed().engineType
            val ground = engine == EngineType.WHEEL || engine == EngineType.TRACK || engine == EngineType.WHEELCHAIR
            val mode = when {
                ground && engine == EngineType.TRACK -> VehicleControlMode.GROUND_TRACK
                ground -> VehicleControlMode.GROUND_WHEEL
                engine == EngineType.AIRCRAFT || engine == EngineType.HELICOPTER ||
                    engine == EngineType.AIRSHIP || engine == EngineType.TOM6 -> VehicleControlMode.FLIGHT
                else -> VehicleControlMode.PASSIVE
            }
            val activeInput = forwardInput || backwardInput || leftInput || rightInput || upInput || downInput
            val power = vehicle.power.toDouble().coerceIn(-1.25, 3.0)
            val engineActive = hasPassenger && !vehicle.isWreck &&
                (activeInput || abs(power) > .001 || vehicle.engineStartOver) && vehicle.energy > 0

            val command = if (ground) {
                val steering = (if (rightInput) 1.0 else 0.0) - (if (leftInput) 1.0 else 0.0)
                val differentialKey = if (engine == EngineType.TRACK) "TrackDifferential" else "WheelDifferential"
                val differential = engineNumber(vehicle, differentialKey, .35).coerceIn(0.0, 1.5)
                // +steering is a right turn: the left contact group drives
                // faster and the right group slower.
                val leftDrive = (power + steering * differential).coerceIn(-1.25, 1.25)
                val rightDrive = (power - steering * differential).coerceIn(-1.25, 1.25)
                val maxSpeed = if (power >= 0.0) {
                    engineNumber(vehicle, "MaxForwardSpeedRate", .2)
                } else {
                    abs(engineNumber(vehicle, "MaxBackwardSpeedRate", .1))
                }
                DriveFrame(
                    sequence = sequence(vehicle),
                    mode = mode,
                    leftDrive = leftDrive,
                    rightDrive = rightDrive,
                    targetSpeed = maxSpeed.coerceIn(.05, 4.0) * VehiclePhysicsUnits.TICKS_PER_SECOND,
                    engineActive = engineActive,
                    awake = engineActive || !sparkGrounded,
                )
            } else {
                val pitchSpeed = engineNumber(vehicle, "PitchSpeed", 1.0)
                val yawSpeed = engineNumber(vehicle, "YawSpeed", 1.0)
                val rollSpeed = engineNumber(vehicle, "RollSpeed", 1.0)
                val liftSpeed = engineNumber(vehicle, "LiftSpeed", 1.0)
                val speedRate = engineNumber(vehicle, "SpeedRate", engineNumber(vehicle, "Speed", 1.0))
                val gravity = handle.settings.gravity
                val thrust = if (engineActive && forwardInput) {
                    power.coerceAtLeast(.05) * 8.0 * speedRate
                } else 0.0
                val lift = when (engine) {
                    EngineType.HELICOPTER, EngineType.AIRSHIP ->
                        if (engineActive && upInput) gravity * liftSpeed.coerceIn(.05, 3.0) *
                            (power.coerceAtLeast(.1) + .35) else 0.0
                    else -> 0.0
                }
                val angular = if (engineActive) {
                    Vec3(
                        -Math.toRadians(vehicle.mouseMoveSpeedY.toDouble()) * pitchSpeed,
                        Math.toRadians(vehicle.mouseMoveSpeedX.toDouble()) * yawSpeed,
                        Math.toRadians(((if (rightInput) 1 else 0) - (if (leftInput) 1 else 0)) * 8.0) * rollSpeed,
                    ).limited(8.0)
                } else Vec3.ZERO
                DriveFrame(
                    sequence = sequence(vehicle),
                    mode = mode,
                    acceleration = Vec3(0.0, lift, thrust).limited(80.0),
                    angularVelocity = angular,
                    aerodynamicLift = engine == EngineType.AIRCRAFT || engine == EngineType.TOM6,
                    engineActive = engineActive,
                    awake = engineActive || !sparkGrounded,
                )
            }
            handle.command = command
        } finally {
            vehicle.setPos(origin)
            vehicle.deltaMovement = motion
            vehicle.setOnGround(originalOnGround)
            vehicle.yRot = originalYaw
            vehicle.xRot = originalPitch
            vehicle.setZRot(originalRoll)
            vehicle.updateOBB()
            capturing.remove(vehicle)
        }
    }

    fun velocityChanged(entity: Entity, old: Vec3, value: Vec3): Boolean {
        val body = bodies[entity]?.takeIf { it.ready } ?: return false
        if (isWriting(entity)) return false
        if (!entity.level().isClientSide) body.impulse(value.subtract(old).scale(20.0 * body.mass))
        return true
    }

    fun impulse(entity: Entity, impulse: Vec3, position: Vec3? = null) {
        if (!entity.level().isClientSide) bodies[entity]?.takeIf { it.ready }?.impulse(impulse, position)
    }

    fun angularImpulse(entity: Entity, deltaAngularVelocity: Vec3) {
        if (!entity.level().isClientSide) bodies[entity]?.takeIf { it.ready }?.angularImpulse(deltaAngularVelocity)
    }

    fun explosion(entity: Entity, center: Vec3, strength: Double) {
        val body = bodies[entity]?.takeIf { it.ready } ?: return
        val direction = center.vectorTo(entity.boundingBox.center).normalize()
        impulse(entity, direction.scale(strength * body.settings.explosionScale), entity.boundingBox.center)
    }

    internal fun accept(entity: Entity, value: RigidState, render: VehiclePhysicsRenderState? = null) {
        val body = bodies[entity]
        if (value.frame < (body?.authoritative?.frame ?: pending[entity]?.frame ?: -1)) return
        if (body != null) {
            body.authoritative = value
            if (render != null) body.authoritativeRender = render
        } else {
            pending[entity] = value
            if (render != null) pendingRender[entity] = render
        }
    }

    fun save(entity: Entity, tag: CompoundTag) {
        val state = bodies[entity]?.state ?: return
        val data = CompoundTag()
        data.putInt("Version", 1)
        listOf(state.rotation.x, state.rotation.y, state.rotation.z, state.rotation.w).forEachIndexed { i, f -> data.putFloat("Q$i", f) }
        listOf(state.velocity.x, state.velocity.y, state.velocity.z, state.angularVelocity.x, state.angularVelocity.y, state.angularVelocity.z)
            .forEachIndexed { i, d -> data.putDouble("V$i", d) }
        tag.put("SparkPhysics", data)
    }

    fun load(entity: Entity, tag: CompoundTag) {
        if (!tag.contains("SparkPhysics")) return
        val data = tag.getCompound("SparkPhysics")
        val q = Quaternionf(data.getFloat("Q0"), data.getFloat("Q1"), data.getFloat("Q2"), data.getFloat("Q3"))
        val v = Vec3(data.getDouble("V0"), data.getDouble("V1"), data.getDouble("V2"))
        val w = Vec3(data.getDouble("V3"), data.getDouble("V4"), data.getDouble("V5"))
        if (q.isFinite && q.lengthSquared() > .001 && v.finite() && w.finite())
            saved[entity] = RigidState(0, 0, entity.position(), q.normalize(), v, w, false)
    }

    @SubscribeEvent fun leave(event: EntityLeaveLevelEvent) = remove(event.entity)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun join(event: EntityJoinLevelEvent) {
        if (event.level.isClientSide || event.entity is VehicleEntity || event.entity is TurretWreckEntity) {
            begin(event.entity)
        }
    }
    private fun remove(entity: Entity) {
        bodies.remove(entity)?.close()
        saved.remove(entity); pending.remove(entity); pendingRender.remove(entity); inputSequences.remove(entity)
        controllerOwners.remove(entity); localControl.remove(entity); appliedPositions.remove(entity); capturing.remove(entity); writing.remove(entity)
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) fun unload(event: LevelEvent.Unload) {
        (bodies.keys + saved.keys + pending.keys).filter { it.level() === event.level }.forEach(::remove)
    }
}
