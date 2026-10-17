package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelRole
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide
import com.atsuishio.superbwarfare.tools.OBB
import net.minecraft.world.phys.Vec3

/**
 * Immutable, validated ground-vehicle geometry captured from one vehicle data
 * definition.  The coordinates are relative to the collision OBB centre and
 * include the configured centre-of-mass offset, matching [CollisionPart].
 */
internal data class GroundVehicleLayout(
    val engine: EngineType,
    val stations: List<WheelStationSpec>,
    val suspensionSides: List<SideSuspensionSpec>,
) {
    val tracked: Boolean get() = engine == EngineType.TRACK
}

internal data class GroundVehicleLayoutResult(
    val layout: GroundVehicleLayout? = null,
    val reason: String? = null,
) {
    val valid: Boolean get() = layout != null
}

/**
 * Converts the legacy wheel OBB representation and the newer explicit Wheels
 * representation into one physics layout.  This is deliberately a pure data
 * operation so it can run during the game-thread capture phase and be handed to
 * Spark without retaining any Minecraft objects.
 */
internal object GroundVehicleLayouts {
    fun resolve(data: DefaultVehicleData, collision: com.atsuishio.superbwarfare.data.vehicle.subdata.OBBInfo?): GroundVehicleLayoutResult {
        val engine = data.engineType
        if (engine != EngineType.WHEEL && engine != EngineType.TRACK) {
            return GroundVehicleLayoutResult(reason = "engine type $engine is outside conventional ground physics")
        }
        if (collision == null) return GroundVehicleLayoutResult(reason = "missing Collision OBB")

        val origin = collision.position
        val com = data.physics.centerOfMassOffset
        val explicit = data.wheels.isNotEmpty()
        val stations = if (explicit) {
            data.wheels.mapIndexed { index, wheel ->
                WheelStationSpec(
                    index = wheel.index ?: data.wheels.take(index).count { it.side == wheel.side },
                    side = wheel.side,
                    role = wheel.role,
                    center = wheel.position.subtract(origin).subtract(com),
                    radius = wheel.radius,
                    halfWidth = wheel.width,
                    halfLength = wheel.radius,
                    contactRadius = wheel.contactRadius ?: wheel.radius,
                )
            }
        } else {
            legacyStations(data, origin, com)
        }

        // A whole-track OBB cannot tell us the number or positions of road wheels.
        // Never silently manufacture three supports for an authored track.
        if (engine == EngineType.TRACK && !explicit)
            return GroundVehicleLayoutResult(reason = "TRACK requires explicit Wheels with every road-wheel station")
        val normalized = stations
        val withSteering = if (engine == EngineType.WHEEL) {
            val frontBySide = normalized.groupBy { it.side }.mapValues { (_, group) -> group.maxOf { it.center.z } }
            normalized.map { it.copy(steerable = it.center.z >= (frontBySide[it.side] ?: Double.POSITIVE_INFINITY) - .05) }
        } else normalized
        val indexed = if (engine == EngineType.WHEEL) normalizeWheelIndices(withSteering) else withSteering
        val validation = validate(engine, indexed, explicit)
        if (validation != null) return GroundVehicleLayoutResult(reason = validation)

        val sides = buildSuspensionSides(indexed)
        if (sides.size != 2) return GroundVehicleLayoutResult(reason = "both LEFT and RIGHT support groups are required")
        return GroundVehicleLayoutResult(GroundVehicleLayout(engine, indexed, sides))
    }

    /** Model wheel bones use side-local ordinals, so do not expose OBB list offsets. */
    private fun normalizeWheelIndices(stations: List<WheelStationSpec>): List<WheelStationSpec> =
        stations.groupBy { it.side }.values.flatten().let { grouped ->
            grouped.groupBy { it.side }.flatMap { (side, values) ->
                values.sortedWith(compareByDescending<WheelStationSpec> { it.center.z }.thenBy { it.index })
                    .mapIndexed { ordinal, station -> station.copy(index = ordinal) }
            }
        }

    private fun legacyStations(
        data: DefaultVehicleData,
        origin: Vec3,
        com: Vec3,
    ): List<WheelStationSpec> = data.obb.mapIndexedNotNull { index, info ->
        val side = when (info.part) {
            OBB.Part.WHEEL_LEFT -> WheelSide.LEFT
            OBB.Part.WHEEL_RIGHT -> WheelSide.RIGHT
            else -> return@mapIndexedNotNull null
        }
        if (!validExtents(info.size)) return@mapIndexedNotNull null
        WheelStationSpec(
            index = index,
            side = side,
            role = WheelRole.SUPPORT,
            center = info.position.subtract(origin).subtract(com),
            radius = info.size.y,
            halfWidth = info.size.x,
            halfLength = info.size.z,
        )
    }

    private fun validate(engine: EngineType, stations: List<WheelStationSpec>, explicit: Boolean): String? {
        if (stations.isEmpty()) return "no wheel or track stations found"
        if (stations.any { !validStation(it) }) return "station contains a non-finite or non-positive dimension"
        if (stations.distinctBy { it.side to it.index }.size != stations.size)
            return "duplicate side-local station index"
        if (stations.any { station ->
                (station.side == WheelSide.LEFT && station.center.x <= .02) ||
                    (station.side == WheelSide.RIGHT && station.center.x >= -.02)
            }) return "station lateral coordinate does not match its LEFT/RIGHT side"

        for (side in listOf(WheelSide.LEFT, WheelSide.RIGHT)) {
            val sideStations = stations.filter { it.side == side }
            if (sideStations.isEmpty()) return "missing $side stations"
            val supports = sideStations.count { it.role == WheelRole.SUPPORT }
            if (supports == 0) return "$side has no SUPPORT station"
            if (engine == EngineType.WHEEL && supports < 2) return "$side needs at least two independent SUPPORT wheels"
            if (engine == EngineType.WHEEL && sideStations.any { it.role != WheelRole.SUPPORT }) {
                return "WHEEL vehicles cannot declare guide stations"
            }
            if (engine == EngineType.TRACK && explicit) {
                if (sideStations.count { it.role == WheelRole.DRIVE_GUIDE } != 1) {
                    return "$side needs exactly one DRIVE_GUIDE station"
                }
                if (sideStations.count { it.role == WheelRole.IDLER_GUIDE } != 1) {
                    return "$side needs exactly one IDLER_GUIDE station"
                }
            }
            if (engine == EngineType.TRACK && sideStations.none { it.role == WheelRole.DRIVE_GUIDE }) {
                return "$side has no DRIVE_GUIDE station"
            }
            if (engine == EngineType.TRACK && sideStations.none { it.role == WheelRole.IDLER_GUIDE }) {
                return "$side has no IDLER_GUIDE station"
            }
        }
        return null
    }

    private fun buildSuspensionSides(stations: List<WheelStationSpec>): List<SideSuspensionSpec> =
        listOf(WheelSide.LEFT, WheelSide.RIGHT).mapNotNull { side ->
            val all = stations.filter { it.side == side }
            val support = all.filter { it.role == WheelRole.SUPPORT }
            if (all.isEmpty() || support.isEmpty()) return@mapNotNull null
            // One preprocessed rectangular bottom face per side. Guides only
            // animate: they cannot extend the flat footprint or set its height.
            val minX = support.minOf { it.center.x - it.halfWidth }
            val maxX = support.maxOf { it.center.x + it.halfWidth }
            val minZ = support.minOf { it.center.z - it.halfLength }
            val maxZ = support.maxOf { it.center.z + it.halfLength }
            val bottomY = support.minOf { it.center.y - it.contactRadius }
            SideSuspensionSpec(
                side = side,
                anchorX = (minX + maxX) * .5,
                bottomY = bottomY,
                centerZ = (minZ + maxZ) * .5,
                halfWidth = ((maxX - minX) * .5).coerceIn(.05, 2.0),
                halfLength = ((maxZ - minZ) * .5).coerceIn(.12, 8.0),
            )
        }

    private fun validStation(station: WheelStationSpec): Boolean =
        station.center.finite() && station.radius.isFinite() && station.halfWidth.isFinite() &&
            station.radius in .05..2.0 && station.contactRadius.isFinite() &&
            station.contactRadius in station.radius..2.0 && station.halfWidth in .05..2.0 &&
            station.halfLength in .05..8.0

    private fun validExtents(size: Vec3): Boolean =
        size.finite() && size.x > .05 && size.y > .05 && size.z > .05
}
