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
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import cn.solarmoon.spark_core.util.Subscription
import net.minecraftforge.event.TickEvent
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
    /**
     * Pose parked when an entity leaves because its chunk unloaded.
     *
     * Chunk storage can call [save] after [EntityLeaveLevelEvent] has already
     * dropped the live body. Keyed by UUID so the leaving instance can be
     * collected; [begin] drops a leftover so a later incarnation does not
     * reuse it.
     */
    private val chunkUnloadState = ConcurrentHashMap<java.util.UUID, ChunkUnloadSnapshot>()
    /** One shutdown participant per physics level. It runs before native teardown. */
    private val shutdownParticipants = ConcurrentHashMap<PhysicsLevel, Subscription>()
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
    /** Bodies whose native teardown failed or is still running after the entity left. */
    private val closing: MutableSet<VehicleBody> =
        java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(java.util.IdentityHashMap()))
    /**
     * Level of each admitted body, captured on the game thread.
     *
     * Shutdown participants run on the physics thread and must not read
     * `entity.level()` there. Identity keys keep two logical sides apart.
     */
    private val bodyLevels: MutableMap<VehicleBody, net.minecraft.world.level.Level> =
        java.util.Collections.synchronizedMap(java.util.IdentityHashMap())
    /**
     * Last close future per UUID.
     *
     * A removed entity is no longer in [bodies], so callers that must observe
     * "native objects are actually gone" need the future rather than the
     * management flag. The flag is cleared before teardown finishes.
     */
    private val closeFutures = ConcurrentHashMap<java.util.UUID, java.util.concurrent.CompletableFuture<Void>>()
    /** Upper bound for retained close futures; only completed ones are pruned. */
    private const val CLOSE_FUTURE_RETENTION = 256
    private var nextInput = 0L

    fun isWriting(entity: Entity) = entity in capturing || entity in writing
    /** True while an eligible entity is being created or is already physical. */
    fun isManaged(entity: Entity) = bodies.containsKey(entity)
    /** True only after the Spark body has been inserted into the physics space. */
    fun hasBody(entity: Entity) = bodies[entity]?.ready == true
    /** Running gear for a tracked entity, or null before it is assembled. */
    fun trackedRuntime(entity: Entity) = bodies[entity]?.tracked
    /**
     * Last tracked drive command published by the entity control path.
     *
     * Read-only diagnostic: this exposes what `control()` handed to the running
     * gear so a test can assert the real input path reached it. It does not
     * apply, queue or modify a command.
     */
    fun driveCommand(entity: Entity): com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleRuntime.DriveCommand? =
        bodies[entity]?.trackedCommand
    /**
     * Teardown of a removed entity, or null when it was never closed.
     *
     * Use this to assert "owned bodies are gone". [isManaged] becomes false
     * before the queued runtime and hull removal tasks have run.
     */
    fun closeFuture(entity: Entity): java.util.concurrent.CompletableFuture<Void>? = closeFutures[entity.uuid]

    /**
     * Remember the teardown of [uuid], keeping the table bounded.
     *
     * Completed futures stay readable so a caller can assert "the native
     * objects are gone" after the entity is removed. They are pruned once the
     * table grows past [CLOSE_FUTURE_RETENTION] entries.
     */
    private fun rememberClose(uuid: java.util.UUID, future: java.util.concurrent.CompletableFuture<Void>) {
        closeFutures[uuid] = future
        if (closeFutures.size <= CLOSE_FUTURE_RETENTION) return
        val stale = closeFutures.entries.filter { it.value.isDone }.map { it.key }
        stale.take(closeFutures.size - CLOSE_FUTURE_RETENTION).forEach { closeFutures.remove(it) }
    }
    fun sequence(entity: Entity) = inputSequences[entity] ?: 0L
    fun diagnostic(entity: Entity): VehiclePhysicsDiagnostic? = bodies[entity]?.diagnostic
    fun observeStability(entity: Entity, steps: Int) { bodies[entity]?.startStabilityObservation(steps) }
    fun stabilityObservation(entity: Entity): VehicleStabilityObservation? = bodies[entity]?.stabilityObservation
    fun renderState(entity: Entity): VehiclePhysicsRenderState = bodies[entity]
        ?.takeIf { it.ready }?.renderState ?: VehiclePhysicsRenderState()

    /**
     * Signed speed along the entity look direction, kilometres per hour.
     *
     * Uses the synced Bullet velocity. Entity position deltas are not reliable
     * here: [begin] applies the physics pose before `positionO` is sampled, so a
     * moving vehicle can otherwise report zero.
     */
    fun displayKmh(entity: Entity): Double {
        val velocity = bodies[entity]?.takeIf { it.ready }?.state?.velocity ?: return Double.NaN
        if (!velocity.finite()) return Double.NaN
        val forward = if (entity is VehicleEntity) entity.getViewVector(1f) else Vec3(0.0, 0.0, 1.0)
        return velocity.dot(forward) * 3.6
    }

    /** Speed magnitude in blocks per tick, for HUDs that multiply by 72. */
    fun speedBlocksPerTick(entity: Entity): Double {
        val velocity = bodies[entity]?.takeIf { it.ready }?.state?.velocity ?: return Double.NaN
        if (!velocity.finite()) return Double.NaN
        return velocity.length() / VehiclePhysicsUnits.TICKS_PER_SECOND
    }

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
        is VehicleEntity -> {
            if (!groundLayout(entity).valid) false
            else if (entity.computed().engineType != EngineType.TRACK) true
            else {
                // Compilation and mass closure are admission checks, not assembly
                // details. A missing or inconsistent mechanism must not receive a
                // chassis and then fall through to wheeled suspension.
                val id = VehicleData.getRegistryId(entity.type)
                GroundVehicleAllowlist.modelOf(id) == GroundVehicleAllowlist.Model.TRACK_RUN &&
                    trackedRejection(entity) == null
            }
        }
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
     * Compiled running gear for a tracked vehicle, or null when it is not ready.
     *
     * A tracked vehicle must not silently fall back to the wheeled suspension
     * path: if the definition does not compile, the vehicle stays out of physics
     * and the reason is logged once per id.
     */
    /**
     * Why this tracked vehicle cannot enter Spark, or null when it can.
     *
     * The compiled spec is cached, so the check is cheap enough to run from
     * [supportsPhysics] on the game thread.
     */
    private fun trackedRejection(entity: VehicleEntity): String? {
        val compiled = trackedSpecFor(entity) ?: return "missing compiled running gear"
        val data = entity.computed()
        val settings = data.physics ?: VehiclePhysicsSettings()
        val reference = (entity.getCollisionOBBInfo()?.position ?: Vec3.ZERO).add(settings.centerOfMassOffset)
        val (turret, barrel) = if (settings.articulationEnabled) {
            resolveArticulation(data, reference)
        } else {
            null to null
        }
        val plan = VehicleMassPlan.tracked(
            totalMassKg = VehiclePhysicsUnits.massKg(entity.mass),
            spec = compiled,
            settings = settings,
            anchored = false,
            turret = turret,
            barrel = barrel,
        )
        val reason = TrackedPhysicsAdmission.rejectReason(compiled, plan)
        if (reason != null && warnedTracked.putIfAbsent(VehicleData.getRegistryId(entity.type), true) == null) {
            Mod.LOGGER.error("Tracked vehicle {} stayed out of physics: {}", entity.type, reason)
        }
        return reason
    }

    private fun trackedSpecFor(entity: Entity): com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleSpec? {
        val id = VehicleData.getRegistryId(entity.type)
        return when (val result = com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleDefinitions.load(id)) {
            is com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleDefinitions.Result.Loaded -> result.spec
            is com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleDefinitions.Result.Failed -> {
                if (warnedTracked.putIfAbsent(id, true) == null) {
                    Mod.LOGGER.error("Tracked vehicle {} has no compiled running gear: {}", id, result.reason)
                }
                null
            }
        }
    }

    private val warnedTracked = ConcurrentHashMap<String, Boolean>()

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
        val authoredMass = VehiclePhysicsUnits.massKg(vehicle?.mass ?: 1f)
        val anchored = engine == EngineType.FIXED
        val trackedSpec = if (data?.engineType == EngineType.TRACK) trackedSpecFor(entity) else null
        val massPlan = if (trackedSpec != null) {
            VehicleMassPlan.tracked(
                totalMassKg = authoredMass,
                spec = trackedSpec,
                settings = settings,
                anchored = anchored,
                turret = turret,
                barrel = barrel,
            )
        } else {
            VehicleMassPlan.wheeled(
                totalMassKg = authoredMass,
                settings = settings,
                anchored = anchored,
                hasWheelStations = layout?.stations?.isNotEmpty() == true,
                turret = turret,
                barrel = barrel,
            )
        }
        return VehiclePhysicsSpec(
            clientSide = entity.level().isClientSide,
            half = half,
            localCenter = center,
            pivot = pivot,
            massKg = authoredMass,
            anchored = anchored,
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
            massPlan = massPlan,
            diagnosticName = "${entity.type.descriptionId}/${entity.uuid}",
            initialTurretYaw = vehicle?.let { Math.toRadians(it.turretYRot.toDouble()).toFloat() } ?: 0f,
            initialBarrelPitch = vehicle?.let { Math.toRadians(it.turretXRot.toDouble()).toFloat() } ?: 0f,
            trackAnimationLength = vehicle?.getTrackAnimationLength()?.toFloat() ?: 100f,
            trackedSpec = trackedSpec,
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
        // Non-vehicle entities never own a body, so they must not touch the
        // physics level on every baseTick.
        if (entity !is VehicleEntity && entity !is TurretWreckEntity) return
        registerShutdownParticipant(entity.level())
        // Only consume a snapshot that belongs to this instance. A new instance
        // with the same UUID must not erase a save that the leaving instance
        // has not written yet.
        chunkUnloadState.computeIfPresent(entity.uuid) { _, parked ->
            if (parked.source === entity) null else parked
        }
        if (entity is VehicleEntity && !supportsPhysics(entity)) {
            bodies.remove(entity)?.let { observeClose(it) }
            return
        }
        val handle = bodies.computeIfAbsent(entity) {
            // Drop a finished close future so it cannot be mistaken for this
            // body's teardown. A still-running one is kept: the same UUID can
            // re-enter before the previous native objects are gone, and hiding
            // that future would hide a real residual.
            closeFutures[entity.uuid]?.let { previous ->
                if (previous.isDone) closeFutures.remove(entity.uuid, previous)
            }
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
                bodyLevels[body] = entity.level()
                body.physics.submitTask("vehicle body create ${spec.diagnosticName}") {
                    body.create()
                }.whenComplete { _, error ->
                    if (error != null) {
                        Mod.LOGGER.error("Vehicle body create failed for {}", spec.diagnosticName, error)
                        body.markAssemblyFailed()
                    }
                }
            }
        }
        if (handle.assemblyFailed) {
            // Stay managed until teardown finishes so a retry cannot build a
            // second assembly on top of the one being rolled back.
            if (!handle.closeInProgress()) observeClose(handle)
            return
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
            // S4: publish the tracked drive demand for the running gear. This is
            // the same input the legacy DriveFrame above consumes, so the two
            // paths cannot disagree about throttle, steer or brake.
            if (handle.trackedVehicle) {
                // Local terrain sampling is driven by the game-thread auto
                // sampler. It consumes the chassis pose published by the
                // physics thread and never reads Bullet objects here.
                handle.trackedCommand = com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleRuntime.DriveCommand(
                    throttle = power.toFloat().coerceIn(-1f, 1f),
                    steer = ((if (rightInput) 1f else 0f) - (if (leftInput) 1f else 0f)),
                    brake = upInput,
                    powered = engineActive,
                )
            }
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
        val parked = chunkUnloadState[entity.uuid]?.takeIf { it.source === entity }
        val state = bodies[entity]?.state ?: parked?.state ?: return
        if (parked != null && bodies[entity] == null) {
            chunkUnloadState.computeIfPresent(entity.uuid) { _, current ->
                if (current === parked) null else current
            }
        }
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
        val removed = bodies.remove(entity)
        saved.remove(entity); pending.remove(entity); pendingRender.remove(entity); inputSequences.remove(entity)
        controllerOwners.remove(entity); localControl.remove(entity); appliedPositions.remove(entity); capturing.remove(entity); writing.remove(entity)
        bodyLevels.remove(removed)
        if (entity.removalReason == Entity.RemovalReason.UNLOADED_TO_CHUNK && removed != null) {
            chunkUnloadState[entity.uuid] = ChunkUnloadSnapshot(entity, removed.state, entity.tickCount)
        } else {
            chunkUnloadState.computeIfPresent(entity.uuid) { _, parked ->
                if (parked.source === entity) null else parked
            }
        }
        removed?.let { rememberClose(entity.uuid, observeClose(it)) }
    }

    /**
     * Keep a failed teardown reachable. [closeAsync] is idempotent while running
     * and retryable after an exceptional completion, so a later tick or unload
     * can finish the native cleanup instead of logging and forgetting it.
     */
    private fun observeClose(body: VehicleBody): java.util.concurrent.CompletableFuture<Void> {
        closing.add(body)
        val future = body.closeAsync()
        future.whenComplete { _, error ->
            if (error != null) {
                Mod.LOGGER.error("Vehicle physics teardown failed for {}", body.spec.diagnosticName, error)
            } else {
                closing.remove(body)
                bodyLevels.remove(body)
                bodies.keys.filter { bodies[it] === body }.forEach { bodies.remove(it) }
            }
        }
        return future
    }

    @SubscribeEvent
    fun retryFailedCloses(event: TickEvent.LevelTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val retry = synchronized(closing) { closing.filter { it.closeNeedsRetry() } }
        retry.forEach { observeClose(it) }
    }

    /**
     * Close every vehicle in [level] while that level's physics thread is still
     * accepting work. A test can call this without unloading the level.
     *
     * Level unload must not call this and then race [PhysicsLevel.close].
     * [closeVehiclesNow] is the shutdown participant for that path.
     */
    fun closeVehicles(level: net.minecraft.world.level.Level): java.util.concurrent.CompletableFuture<Void> {
        val futures = ArrayList<java.util.concurrent.CompletableFuture<Void>>()
        for (key in bodies.keys.filter { it.level() === level }) {
            val body = bodies.remove(key) ?: continue
            bodyLevels.remove(body)
            saved.remove(key); pending.remove(key); pendingRender.remove(key); inputSequences.remove(key)
            controllerOwners.remove(key); localControl.remove(key); appliedPositions.remove(key)
            capturing.remove(key); writing.remove(key)
            val future = observeClose(body)
            rememberClose(key.uuid, future)
            futures += future
        }
        if (futures.isEmpty()) return java.util.concurrent.CompletableFuture.completedFuture(null)
        return java.util.concurrent.CompletableFuture.allOf(*futures.toTypedArray())
    }

    @SubscribeEvent
    fun onLevelLoad(event: LevelEvent.Load) {
        (event.level as? net.minecraft.world.level.Level)?.let { registerShutdownParticipant(it) }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) fun unload(event: LevelEvent.Unload) {
        // Do not queue closeVehicles() here. Core stops admitting tasks before
        // those tasks run, and a rejected close must not be reported as done.
        // The participant registered below runs inside PhysicsLevel cleanup,
        // on the physics thread, before native teardown.
        (event.level as? net.minecraft.world.level.Level)?.let { registerShutdownParticipant(it) }
    }

    private fun registerShutdownParticipant(level: net.minecraft.world.level.Level) {
        val physics = SparkPhysics.get(level)
        // Shutdown already began: nothing can be registered any more, and the
        // teardown owns whatever native objects are left.
        if (!physics.isAcceptingTasks()) return
        if (shutdownParticipants.containsKey(physics)) return
        val subscription = physics.addShutdownParticipant {
            try {
                closeVehiclesNow(level)
            } finally {
                shutdownParticipants.remove(physics)
            }
        }
        shutdownParticipants.putIfAbsent(physics, subscription)?.dispose()
    }
    /**
     * Ordered vehicle teardown on the physics thread.
     *
     * Called by the level shutdown participant. It removes running gear before
     * the hull and does not submit another task.
     */
    internal fun closeVehiclesNow(level: net.minecraft.world.level.Level) {
        val physics = SparkPhysics.get(level)
        val owned = LinkedHashSet<VehicleBody>()
        // The level is captured on the game thread in `begin`. Reading
        // `entity.level()` here would touch Minecraft state from the physics
        // worker.
        val parked = synchronized(bodyLevels) {
            bodyLevels.entries.filter { it.value === level }.map { it.key }
        }
        for (body in parked) {
            val key = bodies.keys.firstOrNull { bodies[it] === body }
            if (key != null) {
                bodies.remove(key)
                saved.remove(key)
                pending.remove(key)
                pendingRender.remove(key)
                inputSequences.remove(key)
                controllerOwners.remove(key)
                localControl.remove(key)
                appliedPositions.remove(key)
                capturing.remove(key)
                writing.remove(key)
            }
            bodyLevels.remove(body)
            owned += body
        }
        synchronized(closing) {
            closing.filter { it.physics === physics }.forEach { owned += it }
        }
        if (!physics.isPhysicsThread()) {
            // The worker never started, so no native object exists for these
            // bodies. Drop the references instead of failing the close on a
            // thread assertion.
            for (body in owned) {
                closing.remove(body)
                bodyLevels.remove(body)
            }
            return
        }
        physics.assertPhysicsThread()
        val failures = ArrayList<Throwable>()
        for (body in owned) {
            try {
                body.closeNow()
                closing.remove(body)
                bodyLevels.remove(body)
            } catch (error: Throwable) {
                failures += error
            }
        }
        if (failures.isNotEmpty()) {
            val primary = failures.first()
            failures.drop(1).forEach { primary.addSuppressed(it) }
            throw primary
        }
    }

    private data class ChunkUnloadSnapshot(
        val source: Entity,
        val state: RigidState,
        val tick: Int,
    )
}
