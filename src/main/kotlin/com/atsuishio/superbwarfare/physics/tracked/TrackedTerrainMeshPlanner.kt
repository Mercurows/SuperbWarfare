package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.terrain.TerrainRevisionToken
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Pure geometry planner for a tracked vehicle's local terrain mesh.
 *
 * The output is the only terrain collision a tracked vehicle may use. It is
 * deliberately a triangle mesh:
 *
 * - horizontal tops and bottoms from the detached block collision boxes;
 * - vertical faces for every exposed side, including every rise that is not a
 *   generated ramp;
 * - discrete planar ramp triangles for the rises the vehicle is allowed to
 *   climb.
 *
 * This class knows nothing about Bullet or Minecraft. It consumes detached
 * [TerrainBox] values, so it can be unit-tested on the JVM.
 */
object TrackedTerrainMeshPlanner {

    private const val HEIGHT_EPS = 1.0e-4
    private const val MIN_RISE = 1.0e-4
    private const val QUANT = 10_000.0
    /** Cell size used where two same-layer ramps overlap, metres. */
    private const val COUPLE_CELL = 0.25

    private data class Interval(var minY: Double, var maxY: Double)

    private data class Candidate(
        val direction: RampDirection,
        val face: Double,
        val lowY: Double,
        val highY: Double,
        val step: Double,
        val lateralCell: Int,
    )

    private data class CandidateKey(
        val direction: RampDirection,
        val face: Long,
        val lowY: Long,
        val highY: Long,
        val step: Long,
    )

    private data class BoxFace(
        val p0: Vec3,
        val p1: Vec3,
        val p2: Vec3,
        val p3: Vec3,
        val desiredNormal: Vec3,
        val key: FaceKey,
        val sign: Int,
    )

    private data class FaceKey(
        val axis: Int,
        val plane: Long,
        val minA: Long,
        val maxA: Long,
        val minB: Long,
        val maxB: Long,
    )

    fun build(
        boxes: List<TerrainBox>,
        terrain: CompiledTerrain,
        originX: Double,
        originY: Double,
        originZ: Double,
        contentHash: Long,
        region: TerrainBox = TerrainBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        terrainRevisionHash: Long = Long.MIN_VALUE,
        requestId: Long = -1L,
        runtimeGeneration: Long = -1L,
        complete: Boolean = true,
        terrainRevision: TerrainRevisionToken? = null,
        /**
         * When set, only a rise aligned with the vehicle axis and inside the
         * track corridor may become a ramp. Nose, tail and the ground under
         * the hull are all allowed. A side rise stays a wall. Null keeps the
         * ungated geometry used by shape tests.
         */
        rampGate: RampGate? = null,
        /**
         * Running-gear centres captured for this build. A ramp whose surface
         * passes through the inner band of a probe is dropped and the riser
         * stays a wall. Empty means "no occupancy known", which is the shape
         * tests' path and must not invent a rejection.
         */
        gear: List<GearProbe> = emptyList(),
    ): TerrainMeshPlan {
        val ramps = if (terrain.rampsEnabled) {
            planRamps(boxes, terrain, rampGate).filter { !rampCutsGear(it, gear) }
        } else {
            emptyList()
        }
        val vertices = ArrayList<Float>(boxes.size * 72)
        val indices = ArrayList<Int>(boxes.size * 36)
        var wallQuads = 0

        fun addQuad(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, desiredNormal: Vec3) {
            var a = p1
            var d = p3
            val normal = p1.subtract(p0).cross(p2.subtract(p0))
            if (normal.dot(desiredNormal) < 0.0) {
                a = p3
                d = p1
            }
            val base = vertices.size / 3
            for (p in listOf(p0, a, p2, d)) {
                vertices += (p.x - originX).toFloat()
                vertices += (p.y - originY).toFloat()
                vertices += (p.z - originZ).toFloat()
            }
            indices += base
            indices += base + 1
            indices += base + 2
            indices += base
            indices += base + 2
            indices += base + 3
            if (abs(desiredNormal.y) < 0.5) wallQuads++
        }

        // Tops stay at block scale on purpose. Bullet derives mesh/box contacts
        // from triangle vertices and edges; a large coplanar triangle whose
        // vertices fall outside the footprint generates *no* contact at all, so
        // merging tiles would make a hovering, unsupported vehicle. Keeping one
        // tile per supported block gives the solver vertices under every part
        // of the belt while the surface itself stays exactly planar.
        // Every face plane is indexed by the *opposite* face it would cancel
        // against: a top face disappears under a box that starts on that plane,
        // a bottom face disappears over a box that ends on it, and a wall face
        // disappears inside a neighbour that already covers that span. Exact key
        // equality is not enough: a slab on a block, or a one-block step beside a
        // two-block wall, leaves a face buried in solid geometry that Bullet would
        // otherwise collide with.
        val tops = HashMap<Long, MutableList<BoxRect>>()
        val bottoms = HashMap<Long, MutableList<BoxRect>>()
        val verticalX = HashMap<Long, MutableList<BoxSide>>()
        val verticalZ = HashMap<Long, MutableList<BoxSide>>()

        boxes.forEachIndexed { owner, box ->
            if (box.maxX - box.minX <= HEIGHT_EPS || box.maxY - box.minY <= HEIGHT_EPS ||
                box.maxZ - box.minZ <= HEIGHT_EPS
            ) return@forEachIndexed
            val rect = BoxRect(owner, box.minX, box.maxX, box.minZ, box.maxZ)
            tops.getOrPut(q(box.maxY)) { ArrayList() }.add(rect)
            bottoms.getOrPut(q(box.minY)) { ArrayList() }.add(rect)
            verticalX.getOrPut(q(box.minX)) { ArrayList() }
                .add(BoxSide(owner, box.minZ, box.maxZ, box.minY, box.maxY, -1))
            verticalX.getOrPut(q(box.maxX)) { ArrayList() }
                .add(BoxSide(owner, box.minZ, box.maxZ, box.minY, box.maxY, 1))
            verticalZ.getOrPut(q(box.minZ)) { ArrayList() }
                .add(BoxSide(owner, box.minX, box.maxX, box.minY, box.maxY, -1))
            verticalZ.getOrPut(q(box.maxZ)) { ArrayList() }
                .add(BoxSide(owner, box.minX, box.maxX, box.minY, box.maxY, 1))
        }

        fun visibleRects(face: Rect2, covers: List<Rect2>?, owner: Int): List<Rect2> =
            uncoveredRectangles(face, covers.orEmpty().filter { it.owner != owner })

        fun horizontalRects(index: Map<Long, List<BoxRect>>, plane: Long): List<Rect2> =
            index[plane].orEmpty().map { Rect2(it.minA, it.maxA, it.minB, it.maxB, it.owner) }

        fun sideRects(index: Map<Long, List<BoxSide>>, plane: Long, face: BoxSide): List<Rect2> =
            index[plane].orEmpty().asSequence()
                .filter { it.sign != face.sign || it.owner < face.owner }
                .map { Rect2(it.minA, it.maxA, it.minY, it.maxY, it.owner) }
                .toList()

        boxes.forEachIndexed { owner, box ->
            val x0 = box.minX; val x1 = box.maxX
            val y0 = box.minY; val y1 = box.maxY
            val z0 = box.minZ; val z1 = box.maxZ
            if (x1 - x0 <= 0.0 || y1 - y0 <= 0.0 || z1 - z0 <= 0.0) return@forEachIndexed

            // Top: hidden when another box starts on this exact plane over this
            // footprint. That is the face-cancellation rule, not a shrink test.
            val topOccluders = horizontalRects(bottoms, q(y1)) +
                horizontalRects(tops, q(y1)).filter { it.owner < owner }
            for (face in visibleRects(Rect2(x0, x1, z0, z1, owner), topOccluders, owner)) {
                addQuad(
                    Vec3(face.minA, y1, face.minB), Vec3(face.minA, y1, face.maxB),
                    Vec3(face.maxA, y1, face.maxB), Vec3(face.maxA, y1, face.minB),
                    Vec3(0.0, 1.0, 0.0),
                )
            }
            // Bottom: a deck underside is real collision and must exist. The one
            // bottom that is skipped is the region floor: nothing inside the
            // sampled volume can reach under it, and a skin there would double the
            // floor triangle count for no contact.
            val bottomOccluders = horizontalRects(tops, q(y0)) +
                horizontalRects(bottoms, q(y0)).filter { it.owner < owner }
            if (y0 > region.minY + HEIGHT_EPS) for (face in visibleRects(
                Rect2(x0, x1, z0, z1, owner), bottomOccluders, owner,
            )) {
                addQuad(
                    Vec3(face.minA, y0, face.maxB), Vec3(face.minA, y0, face.minB),
                    Vec3(face.maxA, y0, face.minB), Vec3(face.maxA, y0, face.maxB),
                    Vec3(0.0, -1.0, 0.0),
                )
            }
            val sideX = BoxSide(owner, z0, z1, y0, y1, -1)
            riserTop(ramps, 0, x0, y0, y1, z0, z1, terrain.maxClimbM)?.let { top ->
                for (face in visibleRects(
                    Rect2(z0, z1, y0, top, owner),
                    sideRects(verticalX, q(x0), sideX),
                    owner,
                )) {
                    addQuad(
                        Vec3(x0, face.minB, face.minA), Vec3(x0, face.minB, face.maxA),
                        Vec3(x0, face.maxB, face.maxA), Vec3(x0, face.maxB, face.minA),
                        Vec3(-1.0, 0.0, 0.0),
                    )
                }
            }
            riserTop(ramps, 0, x1, y0, y1, z0, z1, terrain.maxClimbM)?.let { top ->
                val faceSide = BoxSide(owner, z0, z1, y0, y1, 1)
                for (face in visibleRects(
                    Rect2(z0, z1, y0, top, owner),
                    sideRects(verticalX, q(x1), faceSide),
                    owner,
                )) {
                    addQuad(
                        Vec3(x1, face.minB, face.maxA), Vec3(x1, face.minB, face.minA),
                        Vec3(x1, face.maxB, face.minA), Vec3(x1, face.maxB, face.maxA),
                        Vec3(1.0, 0.0, 0.0),
                    )
                }
            }
            val sideZ = BoxSide(owner, x0, x1, y0, y1, -1)
            riserTop(ramps, 1, z0, y0, y1, x0, x1, terrain.maxClimbM)?.let { top ->
                for (face in visibleRects(
                    Rect2(x0, x1, y0, top, owner),
                    sideRects(verticalZ, q(z0), sideZ),
                    owner,
                )) {
                    addQuad(
                        Vec3(face.maxA, face.minB, z0), Vec3(face.minA, face.minB, z0),
                        Vec3(face.minA, face.maxB, z0), Vec3(face.maxA, face.maxB, z0),
                        Vec3(0.0, 0.0, -1.0),
                    )
                }
            }
            riserTop(ramps, 1, z1, y0, y1, x0, x1, terrain.maxClimbM)?.let { top ->
                val faceSide = BoxSide(owner, x0, x1, y0, y1, 1)
                for (face in visibleRects(
                    Rect2(x0, x1, y0, top, owner),
                    sideRects(verticalZ, q(z1), faceSide),
                    owner,
                )) {
                    addQuad(
                        Vec3(face.minA, face.minB, z1), Vec3(face.maxA, face.minB, z1),
                        Vec3(face.maxA, face.maxB, z1), Vec3(face.minA, face.maxB, z1),
                        Vec3(0.0, 0.0, 1.0),
                    )
                }
            }
        }

        // Ramps are appended after every block face. The flag array uses that
        // order, so the debug view can colour the exact triangles installed
        // into the Bullet mesh without guessing from the slope.
        val rampTriangleStart = indices.size / 3
        for ((index, ramp) in ramps.withIndex()) {
            addRampTriangles(ramp, index, ramps, terrain.maxClimbM, ::addQuad)
        }

        val vertexArray = FloatArray(vertices.size) { vertices[it] }
        val indexArray = IntArray(indices.size) { indices[it] }
        val rampTriangles = BooleanArray(indexArray.size / 3)
        for (triangle in rampTriangleStart until rampTriangles.size) rampTriangles[triangle] = true
        return TerrainMeshPlan(
            originX = originX,
            originY = originY,
            originZ = originZ,
            vertices = vertexArray,
            indices = indexArray,
            ramps = ramps,
            wallQuadCount = wallQuads,
            triangleCount = indexArray.size / 3,
            contentHash = contentHash,
            complete = complete,
            region = region,
            terrainRevisionHash = terrainRevisionHash,
            requestId = requestId,
            runtimeGeneration = runtimeGeneration,
            terrainRevision = terrainRevision,
            rampHeadingBucket = rampGate?.headingBucket ?: -1,
            rampTriangles = rampTriangles,
        )
    }

    /** One box footprint, used to index horizontal faces. */
    private class BoxRect(
        val owner: Int,
        val minA: Double,
        val maxA: Double,
        val minB: Double,
        val maxB: Double,
    )

    /** A rectangle in the two coordinates spanning one planar face. */
    private data class Rect2(
        val minA: Double,
        val maxA: Double,
        val minB: Double,
        val maxB: Double,
        val owner: Int,
    )

    /** One vertical face span, used to test whether a wall face is buried. */
    private class BoxSide(
        val owner: Int,
        val minA: Double,
        val maxA: Double,
        val minY: Double,
        val maxY: Double,
        val sign: Int,
    )

    /** Return all non-occluded rectangular pieces of [face]. */
    private fun uncoveredRectangles(face: Rect2, covers: List<Rect2>): List<Rect2> {
        if (face.maxA - face.minA <= HEIGHT_EPS || face.maxB - face.minB <= HEIGHT_EPS) return emptyList()
        val relevant = covers.filter {
            it.maxA > face.minA + HEIGHT_EPS && it.minA < face.maxA - HEIGHT_EPS &&
                it.maxB > face.minB + HEIGHT_EPS && it.minB < face.maxB - HEIGHT_EPS
        }
        if (relevant.isEmpty()) return listOf(face)

        val aCuts = (listOf(face.minA, face.maxA) + relevant.flatMap {
            listOf(it.minA.coerceIn(face.minA, face.maxA), it.maxA.coerceIn(face.minA, face.maxA))
        }).distinct().sorted()
        val bCuts = (listOf(face.minB, face.maxB) + relevant.flatMap {
            listOf(it.minB.coerceIn(face.minB, face.maxB), it.maxB.coerceIn(face.minB, face.maxB))
        }).distinct().sorted()
        val visible = ArrayList<Rect2>()
        for (ai in 0 until aCuts.lastIndex) for (bi in 0 until bCuts.lastIndex) {
            val minA = aCuts[ai]; val maxA = aCuts[ai + 1]
            val minB = bCuts[bi]; val maxB = bCuts[bi + 1]
            if (maxA - minA <= HEIGHT_EPS || maxB - minB <= HEIGHT_EPS) continue
            val midA = (minA + maxA) * 0.5
            val midB = (minB + maxB) * 0.5
            if (relevant.any { midA > it.minA - HEIGHT_EPS && midA < it.maxA + HEIGHT_EPS &&
                    midB > it.minB - HEIGHT_EPS && midB < it.maxB + HEIGHT_EPS }) continue
            visible += Rect2(minA, maxA, minB, maxB, face.owner)
        }
        return visible
    }

    private fun addRampTriangles(
        ramp: TerrainRampPlan,
        index: Int,
        ramps: List<TerrainRampPlan>,
        maxClimb: Double,
        addQuad: (Vec3, Vec3, Vec3, Vec3, Vec3) -> Unit,
    ) {
        val up = Vec3(0.0, 1.0, 0.0)
        // Split both ways into ~1 block quads. Bullet builds a mesh contact
        // from triangle vertices and edges; a 3 m quad whose vertices sit at
        // the foot and the riser reports no contact under the wheels in
        // between, so the road wheel drops back to the floor in front of the
        // step. A ramp that crosses another same-layer ramp is split finer so
        // the lower envelope can follow the intersection instead of keeping
        // both penetrating quads.
        if (ramp.runM <= 1.0e-6) return
        val step = if (needsCoupling(ramp, index, ramps, maxClimb)) COUPLE_CELL else 1.0
        var lateral = ramp.lateralMin
        while (lateral < ramp.lateralMax - 1.0e-9) {
            val lateralEnd = min(ramp.lateralMax, lateral + step)
            var along = 0.0
            while (along < ramp.runM - 1.0e-9) {
                val alongEnd = min(ramp.runM, along + step)
                val sample = rampPoint(ramp, (along + alongEnd) * 0.5, (lateral + lateralEnd) * 0.5)
                if (!surfaceSuppressed(ramp, index, ramps, sample.x, sample.z, maxClimb)) {
                    addQuad(
                        rampPoint(ramp, along, lateral),
                        rampPoint(ramp, along, lateralEnd),
                        rampPoint(ramp, alongEnd, lateralEnd),
                        rampPoint(ramp, alongEnd, lateral),
                        up,
                    )
                }
                along = alongEnd
            }
            lateral = lateralEnd
        }
    }

    /** Point on the ramp, [along] metres up the climb and [lateral] on the face. */
    private fun rampPoint(ramp: TerrainRampPlan, along: Double, lateral: Double): Vec3 {
        val t = if (ramp.runM <= 1.0e-9) 0.0 else along / ramp.runM
        val y = ramp.lowY + (ramp.highY - ramp.lowY) * t
        return when (ramp.direction) {
            RampDirection.EAST -> Vec3(ramp.lowX + along, y, lateral)
            RampDirection.WEST -> Vec3(ramp.lowX - along, y, lateral)
            RampDirection.SOUTH -> Vec3(lateral, y, ramp.lowZ + along)
            RampDirection.NORTH -> Vec3(lateral, y, ramp.lowZ - along)
        }
    }

    private fun planRamps(
        boxes: List<TerrainBox>,
        terrain: CompiledTerrain,
        gate: RampGate?,
    ): List<TerrainRampPlan> {
        if (terrain.stepM <= MIN_RISE || terrain.maxClimbM <= MIN_RISE) return emptyList()
        val cells = cellTops(boxes)
        if (cells.isEmpty()) return emptyList()
        val candidates = ArrayList<Candidate>()
        val directions = arrayOf(
            RampDirection.EAST to (1 to 0),
            RampDirection.WEST to (-1 to 0),
            RampDirection.SOUTH to (0 to 1),
            RampDirection.NORTH to (0 to -1),
        )
        for ((cellKey, tops) in cells) {
            val x = cellX(cellKey)
            val z = cellZ(cellKey)
            val sorted = tops.sorted()
            for (lowY in sorted) {
                for ((direction, delta) in directions) {
                    val highX = x + delta.first
                    val highZ = z + delta.second
                    val highTops = cells[cellKey(highX, highZ)] ?: continue
                    val highY = highTops.asSequence()
                        .filter { it > lowY + MIN_RISE && it <= lowY + terrain.maxClimbM + HEIGHT_EPS }
                        .minOrNull() ?: continue
                    val face = when (direction) {
                        RampDirection.EAST -> x + 1.0
                        RampDirection.WEST -> x.toDouble()
                        RampDirection.SOUTH -> z + 1.0
                        RampDirection.NORTH -> z.toDouble()
                    }
                    val lateralCell = if (direction == RampDirection.EAST || direction == RampDirection.WEST) z else x
                    if (gate != null && !gate.allows(direction, face, lateralCell)) continue
                    // Step is the longest ramp, not a minimum. A one-block stair
                    // tread in front of a 3 m authored step still gets the 1 m of
                    // surface that really exists; extending the triangle into air
                    // is what used to leave the riser as a red wall.
                    val available = coveredApproachLength(boxes, direction, face, terrain.stepM, lateralCell, lowY)
                    if (available <= MIN_RISE) continue
                    if (!hasRampClearance(boxes, direction, x, z, face, available, lowY, highY)) continue
                    val foot = rampFootprint(direction, face, available, lateralCell)
                    if (!surfaceCovered(boxes, foot[0], foot[1], foot[2], foot[3], lowY)) continue
                    val high = highFootprint(direction, x, z, lateralCell)
                    if (!surfaceCovered(boxes, high[0], high[1], high[2], high[3], highY)) continue
                    candidates += Candidate(
                        direction = direction,
                        face = face,
                        lowY = lowY,
                        highY = highY,
                        step = available,
                        lateralCell = lateralCell,
                    )
                }
            }
        }
        if (candidates.isEmpty()) return emptyList()

        val grouped = LinkedHashMap<CandidateKey, MutableList<Candidate>>()
        for (candidate in candidates) {
            val key = CandidateKey(
                candidate.direction,
                q(candidate.face),
                q(candidate.lowY),
                q(candidate.highY),
                q(candidate.step),
            )
            grouped.getOrPut(key) { ArrayList() }.add(candidate)
        }

        val ramps = ArrayList<TerrainRampPlan>()
        for ((key, list) in grouped) {
            list.sortBy { it.lateralCell }
            var start = 0
            while (start < list.size) {
                var end = start
                while (end + 1 < list.size && list[end + 1].lateralCell == list[end].lateralCell + 1) end++
                val first = list[start]
                val last = list[end]
                // World coordinates are kept absolute here; the mesh builder
                // subtracts the proxy origin exactly once.
                val worldLatMin = first.lateralCell.toDouble()
                val worldLatMax = last.lateralCell + 1.0
                when (key.direction) {
                    RampDirection.EAST, RampDirection.WEST -> {
                        val lowX = if (key.direction == RampDirection.EAST) first.face - first.step else first.face + first.step
                        ramps += TerrainRampPlan(
                            direction = key.direction,
                            lowX = lowX,
                            lowY = first.lowY,
                            lowZ = (worldLatMin + worldLatMax) * .5,
                            highX = first.face,
                            highY = first.highY,
                            highZ = (worldLatMin + worldLatMax) * .5,
                            lateralMin = worldLatMin,
                            lateralMax = worldLatMax,
                            riseM = first.highY - first.lowY,
                            runM = first.step,
                        )
                    }
                    RampDirection.SOUTH, RampDirection.NORTH -> {
                        val lowZ = if (key.direction == RampDirection.SOUTH) first.face - first.step else first.face + first.step
                        ramps += TerrainRampPlan(
                            direction = key.direction,
                            lowX = (worldLatMin + worldLatMax) * .5,
                            lowY = first.lowY,
                            lowZ = lowZ,
                            highX = (worldLatMin + worldLatMax) * .5,
                            highY = first.highY,
                            highZ = first.face,
                            lateralMin = worldLatMin,
                            lateralMax = worldLatMax,
                            riseM = first.highY - first.lowY,
                            runM = first.step,
                        )
                    }
                }
                start = end + 1
            }
        }
        return ramps
    }

    /**
     * Metres of real [lowY] surface available behind [face], capped by [step].
     *
     * The walk stops at the first slice that is missing or only partly covered.
     * The returned length is the ramp that can be installed without crossing air.
     */
    private fun coveredApproachLength(
        boxes: List<TerrainBox>,
        direction: RampDirection,
        face: Double,
        step: Double,
        lateralCell: Int,
        lowY: Double,
    ): Double {
        if (step <= MIN_RISE) return 0.0
        var covered = 0.0
        while (covered < step - 1.0e-9) {
            val next = min(step, covered + 1.0)
            val slice = approachSlice(direction, face, covered, next, lateralCell)
            if (!surfaceCovered(boxes, slice[0], slice[1], slice[2], slice[3], lowY)) break
            covered = next
        }
        return covered
    }

    /** One slice of the approach, as minX, maxX, minZ, maxZ. [from] is nearer the face. */
    private fun approachSlice(
        direction: RampDirection,
        face: Double,
        from: Double,
        to: Double,
        lateralCell: Int,
    ): DoubleArray {
        val lat0 = lateralCell.toDouble()
        val lat1 = lat0 + 1.0
        return when (direction) {
            RampDirection.EAST -> doubleArrayOf(face - to, face - from, lat0, lat1)
            RampDirection.WEST -> doubleArrayOf(face + from, face + to, lat0, lat1)
            RampDirection.SOUTH -> doubleArrayOf(lat0, lat1, face - to, face - from)
            RampDirection.NORTH -> doubleArrayOf(lat0, lat1, face + from, face + to)
        }
    }

    /**
     * Horizontal footprint of one candidate ramp, as minX, maxX, minZ, maxZ.
     *
     * The whole rectangle must be a real walkable top. A stair or slab that
     * only occupies part of a cell is not that top: ramping across the rest
     * would put a triangle over air, and the road wheel falls through it.
     */
    private fun rampFootprint(
        direction: RampDirection,
        face: Double,
        step: Double,
        lateralCell: Int,
    ): DoubleArray {
        val lat0 = lateralCell.toDouble()
        val lat1 = lat0 + 1.0
        return when (direction) {
            RampDirection.EAST -> doubleArrayOf(face - step, face, lat0, lat1)
            RampDirection.WEST -> doubleArrayOf(face, face + step, lat0, lat1)
            RampDirection.SOUTH -> doubleArrayOf(lat0, lat1, face - step, face)
            RampDirection.NORTH -> doubleArrayOf(lat0, lat1, face, face + step)
        }
    }

    /** The high cell this candidate climbs onto, same axis order as [rampFootprint]. */
    private fun highFootprint(
        direction: RampDirection,
        cellX: Int,
        cellZ: Int,
        lateralCell: Int,
    ): DoubleArray {
        val lat0 = lateralCell.toDouble()
        val lat1 = lat0 + 1.0
        return when (direction) {
            RampDirection.EAST -> doubleArrayOf((cellX + 1).toDouble(), (cellX + 2).toDouble(), lat0, lat1)
            RampDirection.WEST -> doubleArrayOf((cellX - 1).toDouble(), cellX.toDouble(), lat0, lat1)
            RampDirection.SOUTH -> doubleArrayOf(lat0, lat1, (cellZ + 1).toDouble(), (cellZ + 2).toDouble())
            RampDirection.NORTH -> doubleArrayOf(lat0, lat1, (cellZ - 1).toDouble(), cellZ.toDouble())
        }
    }

    /**
     * True when boxes whose top is [topY] cover the whole rectangle.
     *
     * Cell membership is not enough: `cellTops` records a cell as soon as any
     * AABB touches it, including a stair that covers a quarter of the cell.
     */
    private fun surfaceCovered(
        boxes: List<TerrainBox>,
        minX: Double,
        maxX: Double,
        minZ: Double,
        maxZ: Double,
        topY: Double,
    ): Boolean {
        if (maxX - minX <= HEIGHT_EPS || maxZ - minZ <= HEIGHT_EPS) return false
        val covers = ArrayList<Rect2>()
        for (box in boxes) {
            if (abs(box.maxY - topY) > HEIGHT_EPS) continue
            if (box.maxY - box.minY <= HEIGHT_EPS) continue
            if (box.maxX <= minX + HEIGHT_EPS || box.minX >= maxX - HEIGHT_EPS) continue
            if (box.maxZ <= minZ + HEIGHT_EPS || box.minZ >= maxZ - HEIGHT_EPS) continue
            covers += Rect2(
                box.minX.coerceAtLeast(minX),
                box.maxX.coerceAtMost(maxX),
                box.minZ.coerceAtLeast(minZ),
                box.maxZ.coerceAtMost(maxZ),
                -1,
            )
        }
        if (covers.isEmpty()) return false
        return uncoveredRectangles(Rect2(minX, maxX, minZ, maxZ, -2), covers).isEmpty()
    }

    private fun hasRampClearance(
        boxes: List<TerrainBox>,
        direction: RampDirection,
        cellX: Int,
        cellZ: Int,
        face: Double,
        step: Double,
        lowY: Double,
        highY: Double,
    ): Boolean {
        val range = runCellRange(direction, cellX, cellZ, face, step) ?: return false
        for ((x, z) in range) {
            for (box in boxes) {
                if (box.maxX <= x + HEIGHT_EPS || box.minX >= x + 1.0 - HEIGHT_EPS) continue
                if (box.maxZ <= z + HEIGHT_EPS || box.minZ >= z + 1.0 - HEIGHT_EPS) continue
                if (box.maxY > lowY + HEIGHT_EPS && box.minY < highY - HEIGHT_EPS) return false
            }
        }
        return true
    }

    private fun runCellRange(
        direction: RampDirection,
        cellX: Int,
        cellZ: Int,
        face: Double,
        step: Double,
    ): List<Pair<Int, Int>>? {
        val output = ArrayList<Pair<Int, Int>>()
        when (direction) {
            RampDirection.EAST -> {
                val start = ceil(face - step - .5 - 1.0e-9).toInt()
                for (x in start..cellX) output += x to cellZ
            }
            RampDirection.WEST -> {
                val end = floor(face + step - .5 + 1.0e-9).toInt()
                for (x in cellX..end) output += x to cellZ
            }
            RampDirection.SOUTH -> {
                val start = ceil(face - step - .5 - 1.0e-9).toInt()
                for (z in start..cellZ) output += cellX to z
            }
            RampDirection.NORTH -> {
                val end = floor(face + step - .5 + 1.0e-9).toInt()
                for (z in cellZ..end) output += cellX to z
            }
        }
        return output.ifEmpty { null }
    }


    private fun cellTops(boxes: List<TerrainBox>): Map<Long, List<Double>> {
        val intervals = LinkedHashMap<Long, MutableList<Interval>>()
        for (box in boxes) {
            if (box.maxX - box.minX <= HEIGHT_EPS || box.maxY - box.minY <= HEIGHT_EPS ||
                box.maxZ - box.minZ <= HEIGHT_EPS
            ) continue
            val minX = floor(box.minX + 1.0e-7).toInt()
            val maxX = ceil(box.maxX - 1.0e-7).toInt() - 1
            val minZ = floor(box.minZ + 1.0e-7).toInt()
            val maxZ = ceil(box.maxZ - 1.0e-7).toInt() - 1
            for (x in minX..maxX) for (z in minZ..maxZ) {
                intervals.getOrPut(cellKey(x, z)) { ArrayList() }
                    .add(Interval(box.minY, box.maxY))
            }
        }
        val result = LinkedHashMap<Long, List<Double>>()
        for ((key, value) in intervals) {
            value.sortBy { it.minY }
            val merged = ArrayList<Interval>()
            for (interval in value) {
                val last = merged.lastOrNull()
                if (last != null && interval.minY <= last.maxY + HEIGHT_EPS) {
                    if (interval.maxY > last.maxY) last.maxY = interval.maxY
                } else {
                    merged += Interval(interval.minY, interval.maxY)
                }
            }
            val tops = merged.map { it.maxY }.distinctBy { q(it) }
            if (tops.isNotEmpty()) result[key] = tops
        }
        return result
    }


    /**
     * Top of the vertical wall that remains once a ramp replaces the upper
     * part of a riser. Returns null when the whole face is covered, and the
     * original face top when no ramp applies. Only the covered upper part is
     * removed; the rock below the ramp still has a real vertical side.
     */
    private fun riserTop(
        ramps: List<TerrainRampPlan>,
        axis: Int,
        plane: Double,
        minY: Double,
        maxY: Double,
        minOther: Double,
        maxOther: Double,
        maxClimb: Double,
    ): Double? {
        var top = maxY
        for ((index, ramp) in ramps.withIndex()) {
            val matches = when {
                axis == 0 && ramp.direction == RampDirection.EAST -> abs(plane - ramp.highX) <= HEIGHT_EPS
                axis == 0 && ramp.direction == RampDirection.WEST -> abs(plane - ramp.highX) <= HEIGHT_EPS
                axis == 1 && ramp.direction == RampDirection.SOUTH -> abs(plane - ramp.highZ) <= HEIGHT_EPS
                axis == 1 && ramp.direction == RampDirection.NORTH -> abs(plane - ramp.highZ) <= HEIGHT_EPS
                else -> false
            }
            if (!matches) continue
            if (minOther < ramp.lateralMin - HEIGHT_EPS || maxOther > ramp.lateralMax + HEIGHT_EPS) continue
            if (maxY <= ramp.lowY + HEIGHT_EPS || minY >= ramp.highY - HEIGHT_EPS) continue
            // A ramp that lost this face to a lower same-layer surface must not
            // delete the wall. Removing it would leave a hole where the winning
            // surface does not reach the riser.
            val mid = (minOther + maxOther) * 0.5
            val sampleX = if (axis == 0) plane else mid
            val sampleZ = if (axis == 0) mid else plane
            if (surfaceSuppressed(ramp, index, ramps, sampleX, sampleZ, maxClimb)) continue
            top = min(top, ramp.lowY)
        }
        if (top <= minY + HEIGHT_EPS) return null
        return top
    }

    /**
     * True when [ramp] crosses or stacks on another ramp in the same climb band.
     *
     * A deck several metres above the ground overlaps in XZ but is a different
     * layer: both surfaces stay. Taking a global maximum would fill the gap
     * under the deck.
     */
    private fun needsCoupling(
        ramp: TerrainRampPlan,
        index: Int,
        ramps: List<TerrainRampPlan>,
        maxClimb: Double,
    ): Boolean {
        for (other in ramps.indices) {
            if (other == index) continue
            val candidate = ramps[other]
            if (overlapsXZ(ramp, candidate) && heightBandsOverlap(ramp, candidate, maxClimb)) return true
        }
        return false
    }

    /**
     * True when a lower same-layer ramp already owns this point.
     *
     * Equal heights keep the earlier ramp so the seam is not emitted twice.
     * A surface more than [maxClimb] below is another layer and does not win.
     */
    private fun surfaceSuppressed(
        ramp: TerrainRampPlan,
        index: Int,
        ramps: List<TerrainRampPlan>,
        x: Double,
        z: Double,
        maxClimb: Double,
    ): Boolean {
        val height = rampHeightAt(ramp, x, z) ?: return true
        for (otherIndex in ramps.indices) {
            if (otherIndex == index) continue
            val otherHeight = rampHeightAt(ramps[otherIndex], x, z) ?: continue
            val gap = height - otherHeight
            if (gap > maxClimb + HEIGHT_EPS) continue
            if (gap < -HEIGHT_EPS) continue
            if (gap <= HEIGHT_EPS && otherIndex > index) continue
            return true
        }
        return false
    }

    /**
     * True when the ramp surface passes through the inner band of a gear probe.
     *
     * Contact at the bottom of a wheel is not a cut. The inner band is the
     * middle 70 percent of the probe, so a slope the wheel is sitting on stays.
     */
    private fun rampCutsGear(ramp: TerrainRampPlan, gear: List<GearProbe>): Boolean {
        if (gear.isEmpty() || ramp.runM <= MIN_RISE) return false
        val bounds = rampBounds(ramp)
        for (probe in gear) {
            if (probe.halfY <= MIN_RISE) continue
            val x = probe.x.coerceIn(bounds[0], bounds[1])
            val z = probe.z.coerceIn(bounds[2], bounds[3])
            if (abs(x - probe.x) > probe.halfX + HEIGHT_EPS) continue
            if (abs(z - probe.z) > probe.halfZ + HEIGHT_EPS) continue
            val height = rampHeightAt(ramp, x, z) ?: continue
            val inner = probe.halfY * 0.35
            if (height > probe.y - inner && height < probe.y + inner) return true
        }
        return false
    }

    /** minX, maxX, minZ, maxZ of one ramp footprint. */
    private fun rampBounds(ramp: TerrainRampPlan): DoubleArray = when (ramp.direction) {
        RampDirection.EAST, RampDirection.WEST -> doubleArrayOf(
            min(ramp.lowX, ramp.highX), max(ramp.lowX, ramp.highX), ramp.lateralMin, ramp.lateralMax,
        )
        RampDirection.SOUTH, RampDirection.NORTH -> doubleArrayOf(
            ramp.lateralMin, ramp.lateralMax, min(ramp.lowZ, ramp.highZ), max(ramp.lowZ, ramp.highZ),
        )
    }

    /** Height of [ramp] at a world point, or null outside its footprint. */
    private fun rampHeightAt(ramp: TerrainRampPlan, x: Double, z: Double): Double? {
        val bounds = rampBounds(ramp)
        if (x < bounds[0] - HEIGHT_EPS || x > bounds[1] + HEIGHT_EPS) return null
        if (z < bounds[2] - HEIGHT_EPS || z > bounds[3] + HEIGHT_EPS) return null
        val t = when (ramp.direction) {
            RampDirection.EAST -> if (ramp.runM <= 1.0e-9) 0.0 else (x - ramp.lowX) / ramp.runM
            RampDirection.WEST -> if (ramp.runM <= 1.0e-9) 0.0 else (ramp.lowX - x) / ramp.runM
            RampDirection.SOUTH -> if (ramp.runM <= 1.0e-9) 0.0 else (z - ramp.lowZ) / ramp.runM
            RampDirection.NORTH -> if (ramp.runM <= 1.0e-9) 0.0 else (ramp.lowZ - z) / ramp.runM
        }.coerceIn(0.0, 1.0)
        return ramp.lowY + (ramp.highY - ramp.lowY) * t
    }

    private fun overlapsXZ(a: TerrainRampPlan, b: TerrainRampPlan): Boolean {
        val aa = rampBounds(a)
        val bb = rampBounds(b)
        return aa[0] < bb[1] - HEIGHT_EPS && aa[1] > bb[0] + HEIGHT_EPS &&
            aa[2] < bb[3] - HEIGHT_EPS && aa[3] > bb[2] + HEIGHT_EPS
    }

    private fun heightBandsOverlap(a: TerrainRampPlan, b: TerrainRampPlan, maxClimb: Double): Boolean {
        val a0 = min(a.lowY, a.highY)
        val a1 = max(a.lowY, a.highY)
        val b0 = min(b.lowY, b.highY)
        val b1 = max(b.lowY, b.highY)
        return a1 + maxClimb >= b0 - HEIGHT_EPS && b1 + maxClimb >= a0 - HEIGHT_EPS
    }

    private fun cellKey(x: Int, z: Int): Long =
        (x.toLong() shl 32) xor (z.toLong() and 0xffffffffL)

    private fun cellX(key: Long): Int = (key shr 32).toInt()
    private fun cellZ(key: Long): Int = key.toInt()

    private fun q(value: Double): Long = (value * QUANT).roundToLong()
}
