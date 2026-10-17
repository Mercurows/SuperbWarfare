package com.atsuishio.superbwarfare.physics

import cn.solarmoon.spark_core.physics.SparkPhysics
import cn.solarmoon.spark_core.physics.VehicleControlMode
import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData
import com.atsuishio.superbwarfare.data.vehicle.VehicleData
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
    /**
     * Identity-keyed table for the global physics state.
     *
     * In a single-player session the client level and the integrated server
     * level coexist in this JVM, and `Entity.equals`/`hashCode` only compare
     * the network id, so plain Entity keys would alias the two logical sides:
     * the client proxy and the authoritative server body could reuse, corrupt
     * or clear each other's state for the same vehicle. Every table is
     * therefore keyed by entity identity, which also isolates the two levels
     * (an entity instance belongs to exactly one level).
     */
    internal class IdentityTable<V> {
        private val backing = ConcurrentHashMap<EntityKey, V>()
        val keys: List<Entity> get() = backing.keys.map { it.entity }
        fun containsKey(entity: Entity): Boolean = backing.containsKey(EntityKey(entity))
        operator fun get(entity: Entity): V? = backing[EntityKey(entity)]
        operator fun set(entity: Entity, value: V) { backing[EntityKey(entity)] = value }
        fun put(entity: Entity, value: V): V? = backing.put(EntityKey(entity), value)
        fun remove(entity: Entity): V? = backing.remove(EntityKey(entity))
        fun computeIfAbsent(entity: Entity, factory: () -> V): V =
            backing.computeIfAbsent(EntityKey(entity)) { factory() }
    }

    private class EntityKey(val entity: Entity) {
        private val hash = System.identityHashCode(entity)
        override fun hashCode() = hash
        override fun equals(other: Any?): Boolean =
            this === other || (other is EntityKey && other.entity === entity)
    }

    internal val bodies = IdentityTable<VehicleBody>()
    private val saved = IdentityTable<RigidState>()
    private val pending = IdentityTable<RigidState>()
    private val pendingRender = IdentityTable<VehiclePhysicsRenderState>()
    private val inputSequences = IdentityTable<Long>()
    private val controllerOwners = IdentityTable<Int>()
    private val localControl = IdentityTable<Int>()
    private val appliedPositions = IdentityTable<Vec3>()
    // Identity sets: the two logical sides must never see each other's write
    // guards for an entity with the same numeric id.
    private val capturing: MutableSet<Entity> =
        java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(java.util.IdentityHashMap()))
    private val writing: MutableSet<Entity> =
        java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(java.util.IdentityHashMap()))
    private var nextInput = 0L

    fun isWriting(entity: Entity) = entity in capturing || entity in writing
    /** True while an eligible entity is being created or is already physical. */
    fun isManaged(entity: Entity) = bodies.containsKey(entity)
    /** True only after the Spark body has been inserted into the physics space. */
    fun hasBody(entity: Entity) = bodies[entity]?.ready == true
    fun sequence(entity: Entity) = inputSequences[entity] ?: 0L
    fun diagnostic(entity: Entity): VehiclePhysicsDiagnostic? = bodies[entity]?.diagnostic
    fun observeStability(entity: Entity, steps: Int) { bodies[entity]?.startStabilityObservation(steps) }
    fun stabilityObservation(entity: Entity): VehicleStabilityObservation? = bodies[entity]?.stabilityObservation
    fun renderState(entity: Entity): VehiclePhysicsRenderState = bodies[entity]
        ?.takeIf { it.ready }?.renderState ?: VehiclePhysicsRenderState()

    /**
     * Bullet is intentionally limited to the conventional ground roster. A
     * missing or malformed wheel layout is a configuration error, not a reason
     * to silently switch that vehicle back to vanilla movement.
     */
    internal fun groundLayout(entity: VehicleEntity): GroundVehicleLayoutResult {
        val data = entity.computed()
        val id = VehicleData.getRegistryId(entity.type)
        if (!GroundVehicleAllowlist.contains(id)) {
            return GroundVehicleLayoutResult(reason = "vehicle is outside the conventional ground allowlist")
        }
        val result = GroundVehicleLayouts.resolve(data, entity.getCollisionOBBInfo())
        if (!result.valid) warnLayoutOnce(id, result.reason ?: "unknown layout error")
        return result
    }

    private val warnedLayouts = ConcurrentHashMap<String, Boolean>()
    private fun warnLayoutOnce(id: String, reason: String) {
        if (warnedLayouts.putIfAbsent(id, true) == null)
            Mod.LOGGER.error("Conventional ground vehicle {} has no usable wheel layout: {}", id, reason)
    }

    internal fun supportsPhysics(entity: Entity): Boolean = when (entity) {
        is VehicleEntity -> groundLayout(entity).valid
        else -> false
    }

    fun localInput(vehicle: VehicleEntity, keys: Short): Long {
        if (!supportsPhysics(vehicle)) return sequence(vehicle)
        val seq = ++nextInput
        inputSequences[vehicle] = seq
        localControl[vehicle] = vehicle.tickCount
        vehicle.processInput(keys)
        return seq
    }

    fun serverInput(vehicle: VehicleEntity, keys: Short, sequence: Long, player: Player) {
        if (!supportsPhysics(vehicle)) return
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
        val layout = vehicle?.let { groundLayout(it).layout }
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
        val ground = layout != null
        val flight = false
        val (turret, barrel) = if (data != null && settings.articulationEnabled)
            resolveArticulation(data, (info?.position ?: Vec3.ZERO).add(settings.centerOfMassOffset)) else null to null
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
            wheelStations = layout?.stations ?: emptyList(),
            suspensionSides = layout?.suspensionSides ?: emptyList(),
            turret = turret,
            barrel = barrel,
            initial = state,
            diagnosticName = "${entity.type.descriptionId}/${entity.uuid}",
            initialTurretYaw = vehicle?.let { Math.toRadians(it.turretYRot.toDouble()).toFloat() } ?: 0f,
            initialBarrelPitch = vehicle?.let { Math.toRadians(it.turretXRot.toDouble()).toFloat() } ?: 0f,
            trackAnimationLength = vehicle?.getTrackAnimationLength()?.toFloat() ?: 100f,
        )
    }

    /** One articulated sub-body's OBB parts, in the parent frame plus [offset]. */
    private fun articulationParts(
        data: DefaultVehicleData,
        transform: String,
        offset: Vec3,
    ): List<CollisionPart> = data.obb.mapNotNull { info ->
        if (!info.transform.equals(transform, ignoreCase = true)) return@mapNotNull null
        val half = info.size
        if (!half.x.isFinite() || !half.y.isFinite() || !half.z.isFinite() ||
            half.x <= .001 || half.y <= .001 || half.z <= .001) return@mapNotNull null
        CollisionPart(
            half = half,
            center = info.position.add(offset),
            rotation = Quaternionf()
                .rotateY(Math.toRadians(info.customRotate.y.toDouble()).toFloat())
                .rotateX(Math.toRadians(info.customRotate.x.toDouble()).toFloat())
                .rotateZ(Math.toRadians(info.customRotate.z.toDouble()).toFloat()),
        )
    }

    /**
     * Build the articulated turret/barrel specs. Some vehicles author the whole
     * rotating assembly under "Barrel" without a separate "Turret" entry; in
     * that case the barrel parts are carried by the turret body so the assembly
     * still yaws, and the (turret-relative) barrel pivot is folded into the
     * part centres.
     */
    private fun resolveArticulation(data: DefaultVehicleData, chassisCenter: Vec3): Pair<ArticulationSpec?, ArticulationSpec?> {
        val turretPos = data.turretPos ?: return null to null
        val turretParts = articulationParts(data, "Turret", Vec3.ZERO)
        val barrelParts = articulationParts(data, "Barrel", Vec3.ZERO)
        val yaw = data.turretYawRange
        val pitch = data.turretPitchRange
        return when {
            turretParts.isNotEmpty() -> {
                val turret = ArticulationSpec(
                    turretPos.subtract(chassisCenter), turretParts, axis = 1,
                    lowerLimit = Math.toRadians(yaw.x.toDouble()),
                    upperLimit = Math.toRadians(yaw.y.toDouble()),
                )
                val barrel = if (barrelParts.isNotEmpty()) ArticulationSpec(
                    data.barrelPos, barrelParts, axis = 0,
                    lowerLimit = Math.toRadians(pitch.x.toDouble()),
                    upperLimit = Math.toRadians(pitch.y.toDouble()),
                ) else null
                turret to barrel
            }
            barrelParts.isNotEmpty() -> {
                val folded = barrelParts.map { it.copy(center = it.center.add(data.barrelPos)) }
                ArticulationSpec(
                    turretPos.subtract(chassisCenter), folded, axis = 1,
                    lowerLimit = Math.toRadians(yaw.x.toDouble()),
                    upperLimit = Math.toRadians(yaw.y.toDouble()),
                ) to null
            }
            else -> null to null
        }
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
        // Collision is the legacy all-in-one movement envelope (including the
        // empty space under the hull). Prefer the authored solid hull parts
        // whenever the sprung running gear (TRACK plates or WHEEL tires) owns
        // the contact plan: a hull box that reaches the tire contact plane
        // would otherwise carry the whole weight and starve the wheels of the
        // normal load their friction needs.
        val detailedGroundHull = (data.engineType == EngineType.TRACK || data.engineType == EngineType.WHEEL) &&
            data.obb.any {
                it.part != OBB.Part.COLLISION && it.part != OBB.Part.WHEEL_LEFT && it.part != OBB.Part.WHEEL_RIGHT &&
                    (it.transform == null || it.transform.equals("default", true))
            }
        return data.obb.mapIndexedNotNull { index, info ->
            if (detailedGroundHull && info.part == OBB.Part.COLLISION) return@mapIndexedNotNull null
            val transform = info.transform?.lowercase() ?: "default"
            if ((settings.articulationEnabled && (transform == "turret" || transform == "barrel")) || transform.contains("weapon") ||
                info.part == OBB.Part.WHEEL_LEFT ||
                info.part == OBB.Part.WHEEL_RIGHT
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
                val localRotation = Quaternionf(inverseBodyRotation).mul(worldRotation).normalize()
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

    fun begin(entity: Entity) {
        if (entity.isRemoved) return
        if (entity !is VehicleEntity && entity !is TurretWreckEntity) return
        if (entity is VehicleEntity && !supportsPhysics(entity)) {
            bodies.remove(entity)?.close()
            return
        }
        val handle = bodies.computeIfAbsent(entity) {
            // Build the immutable spec on the game thread. Everything the worker
            // needs is copied here so it never touches the Entity or the Level.
            val restored = saved.remove(entity)
            // Containers load NBT before assigning the new deployment position.
            // A redeployment is not a continuation of the old world-space motion.
            val relocated = restored != null && restored.position.distanceToSqr(entity.position()) > .0001
            val state = if (restored != null && !relocated) restored.copy(position = entity.position()) else RigidState(0, 0, entity.position(), rotation(entity),
                if (relocated) Vec3.ZERO else VehiclePhysicsUnits.velocity(entity.deltaMovement), Vec3.ZERO, false)
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
        if (!supportsPhysics(vehicle)) return
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
            val ground = handle.groundVehicle && (engine == EngineType.WHEEL || engine == EngineType.TRACK)
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
            val turretYaw = if (vehicle.hasTurret()) Math.toRadians(vehicle.turretYRot.toDouble()).toFloat() else 0f
            val barrelPitch = if (vehicle.hasTurret()) Math.toRadians(
                vehicle.turretXRot.coerceIn(vehicle.turretMinPitch, vehicle.turretMaxPitch).toDouble()
            ).toFloat() else 0f

            val command = if (ground) {
                val steering = (if (rightInput) 1.0 else 0.0) - (if (leftInput) 1.0 else 0.0)
                val differentialKey = if (engine == EngineType.TRACK) "TrackDifferential" else "WheelDifferential"
                val differential = engineNumber(vehicle, differentialKey, .35).coerceIn(0.0, 1.5)
                // Longitudinal demand and steering split are independent.
                // Clamping each side first and averaging afterwards would let a
                // wide authored differential cancel the throttle completely:
                // the ZTZ-99A authors 1.7, so `power 0.15 + steer` used to
                // collapse to +1.25/-1.25 and a zero forward demand.
                val centerDrive = power.coerceIn(-1.25, 1.25)
                val turnDrive = (steering * differential).coerceIn(-1.5, 1.5)
                // +steering is a right turn: the left contact group drives
                // faster and the right group slower.
                val leftDrive = centerDrive + turnDrive
                val rightDrive = centerDrive - turnDrive
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
                    centerDrive = centerDrive,
                    targetSpeed = maxSpeed.coerceIn(.05, 4.0) * VehiclePhysicsUnits.TICKS_PER_SECOND,
                    steering = steering,
                    turretYaw = turretYaw,
                    barrelPitch = barrelPitch,
                    engineActive = engineActive,
                    awake = engineActive || !sparkGrounded,
                    brake = upInput,
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
        if (event.entity is TurretWreckEntity || event.entity is VehicleEntity && supportsPhysics(event.entity)) {
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
