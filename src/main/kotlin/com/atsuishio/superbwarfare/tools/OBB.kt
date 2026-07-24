package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.entity.OBBEntity
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.*
import java.util.*

/**
 * Oriented Bounding Box (OBB) used for precise vehicle collision detection and hit registration.
 *
 * An OBB is a box that can be arbitrarily oriented in 3D space, defined by:
 * - A **centre** position in world space
 * - Three **half-extents** (distances from the centre to each face along the local axes)
 * - A **rotation** quaternion describing the box's orientation relative to world axes
 * - A **part** identifier used for damage routing (e.g. turret, wheel, body)
 *
 * This implementation is based on @AnECanSaiTin's [HitboxAPI](https://github.com/AnECanSaiTin/HitboxAPI),
 * with extensive performance optimizations for high-frequency vehicle physics ticks:
 * - ThreadLocal axis buffers eliminate per-call allocations in SAT collision tests
 * - Scalar cross-product expansion avoids allocating intermediate [Vector3d] instances
 * - Cached world-space axes reuse for iterative collision resolution loops
 *
 * @param center   rotation centre in world space; mutable via [setCenter]
 * @param extents  half-lengths along the three **local** axes (X, Y, Z); mutable via [setExtents]
 * @param rotation orientation as a quaternion; mutable via [updateRotation]
 * @param part     logical part identifier (see [Part] enum)
 *
 * @author atsuishio
 * @author SuperbWarfare contributors
 * @since 0.8.9
 */
@JvmRecord
data class OBB(
    @JvmField val center: Vector3d,
    val extents: Vector3d,
    val rotation: Quaterniond,
    @JvmField val part: Part
) {

    //
    // Mutators
    //

    /**
     * Updates this OBB's centre to [center] in-place.
     *
     * @param center new world-space centre position
     */
    fun setCenter(center: Vector3d?) { this.center.set(center) }

    /**
     * Updates this OBB's half-extents to [extents] in-place.
     *
     * @param extents new half-lengths along local X/Y/Z axes
     */
    fun setExtents(extents: Vector3d?) { this.extents.set(extents) }

    /**
     * Updates this OBB's orientation to [rotation] in-place.
     *
     * @param rotation new rotation quaternion
     */
    fun updateRotation(rotation: Quaterniond?) { this.rotation.set(rotation) }

    //
    // Axis helpers
    //

    /**
     * Fills the provided array [out] with this OBB's three orthonormal world-space axes.
     *
     * This is the **zero-allocation** variant for performance-critical paths. The caller
     * must supply a pre-allocated array of exactly 3 [Vector3d] instances. The method
     * overwrites each element in-place by:
     * 1. Setting it to the corresponding local basis vector (X = `(1,0,0)`, Y = `(0,1,0)`, Z = `(0,0,1)`)
     * 2. Rotating it by [rotation] to yield the world-space axis direction
     *
     * **Usage contract:**
     * - The array [out] must have length 3
     * - The caller must consume the results **before** making any further [getAxesInto] or
     *   [getAxes] calls on this or any other OBB instance on the same thread
     * - For pre-allocated buffers to use, see [Companion.AXES_A] and [Companion.AXES_B]
     *
     * @param out destination array; its three elements are overwritten in-place
     * @throws ArrayIndexOutOfBoundsException if `out.size < 3`
     */
    fun getAxesInto(out: Array<Vector3d>) {
        out[0].set(1.0, 0.0, 0.0); rotation.transform(out[0])
        out[1].set(0.0, 1.0, 0.0); rotation.transform(out[1])
        out[2].set(0.0, 0.0, 1.0); rotation.transform(out[2])
    }

    /**
     * Returns this OBB's three orthonormal world-space axes as a newly allocated array.
     *
     * **Allocates 4 objects on every call** (one array + three [Vector3d] instances).
     * Prefer [getAxesInto] in performance-critical paths (tick loops, iterative collision
     * resolution) to avoid GC pressure. This method is provided as a convenience for
     * one-off queries where allocation cost is negligible.
     *
     * @return newly allocated array `[axisX, axisY, axisZ]` in world space
     */
    fun getAxes(): Array<Vector3d> {
        val out = arrayOf(Vector3d(), Vector3d(), Vector3d())
        getAxesInto(out)
        return out
    }

    //
    // Geometry queries
    //

    /**
     * Computes which face of this OBB the given world-space point [vec3] is closest to
     * **from the inside**, assuming [vec3] lies within or on the surface of the OBB.
     *
     * The return value encodes both the **axis** (1 = X, 2 = Y, 3 = Z) and the **direction**
     * (sign: positive = +axis, negative = -axis). For example:
     * - `+1` → point is closest to the +X face
     * - `-2` → point is closest to the -Y face (bottom, if Y is up)
     * - `+3` → point is closest to the +Z face
     *
     * This method finds the axis along which the point has the **smallest clearance** to
     * the OBB's extents (i.e. the face it would penetrate first if pushed further inward).
     *
     * @param vec3 world-space query point (should be inside or on the OBB surface for
     *             meaningful results)
     * @return signed face index: ±1 for ±X, ±2 for ±Y, ±3 for ±Z
     *
     * @author YWZJ Ranpoes
     */
    fun getEmbeddingFace(vec3: Vec3): Int {
        val axes = AXES_A.get()
        getAxesInto(axes)
        val rel = vec3ToVector3d(vec3).sub(center)

        val projX = Math.abs(rel.dot(axes[0]))
        val projY = Math.abs(rel.dot(axes[1]))
        val projZ = Math.abs(rel.dot(axes[2]))

        var min = Double.MAX_VALUE
        var index = 0

        val dx = extents.x - projX
        val dy = extents.y - projY
        val dz = extents.z - projZ

        if (dx < min) { min = dx; index = 1 }
        if (dy < min) { min = dy; index = 2 }
        if (dz < min) { index = 3 }

        return index * (if (rel.dot(axes[index - 1]) < 0) -1 else 1)
    }

    /**
     * Returns the **minimum penetration depth** of world-space point [vec3] into this OBB.
     *
     * The penetration depth is defined as the smallest distance [vec3] would need to move
     * to reach the surface of the OBB. If [vec3] is already outside, the result is negative
     * (strictly speaking, undefined for points far outside; this method assumes near-surface queries).
     *
     * @param vec3 world-space query point
     * @return penetration depth (positive if inside, near-zero if on surface)
     */
    fun getEmbeddingDepth(vec3: Vec3): Double {
        val axes = AXES_A.get()
        getAxesInto(axes)
        val rel = vec3ToVector3d(vec3).sub(center)

        val projX = Math.abs(rel.dot(axes[0]))
        val projY = Math.abs(rel.dot(axes[1]))
        val projZ = Math.abs(rel.dot(axes[2]))

        val dx = extents.x - projX
        val dy = extents.y - projY
        val dz = extents.z - projZ

        var minDepth = Double.MAX_VALUE
        if (Math.abs(dx) < Math.abs(minDepth)) minDepth = dx
        if (Math.abs(dy) < Math.abs(minDepth)) minDepth = dy
        if (Math.abs(dz) < Math.abs(minDepth)) minDepth = dz

        return minDepth
    }

    /**
     * Returns the 8 corner vertices of this OBB in world space.
     *
     * Vertices are ordered as: `-X-Y-Z, +X-Y-Z, +X+Y-Z, -X+Y-Z, -X-Y+Z, +X-Y+Z, +X+Y+Z, -X+Y+Z`
     * (same ordering as a standard cube mesh).
     *
     * **Allocates 8 [Vector3d] instances on every call.** Avoid calling in tight loops.
     *
     * @return array of 8 world-space corner positions
     */
    fun getVertices(): Array<Vector3d> {
        val vertices = arrayOfNulls<Vector3d>(8)
        val localVertices = arrayOf(
            Vector3d(-extents.x, -extents.y, -extents.z),
            Vector3d( extents.x, -extents.y, -extents.z),
            Vector3d( extents.x,  extents.y, -extents.z),
            Vector3d(-extents.x,  extents.y, -extents.z),
            Vector3d(-extents.x, -extents.y,  extents.z),
            Vector3d( extents.x, -extents.y,  extents.z),
            Vector3d( extents.x,  extents.y,  extents.z),
            Vector3d(-extents.x,  extents.y,  extents.z)
        )
        for (i in 0..7) {
            val vertex = localVertices[i]
            vertex.rotate(rotation)
            vertex.add(center)
            vertices[i] = vertex
        }
        return vertices.filterNotNull().toTypedArray()
    }

    /**
     * Performs a ray–OBB intersection test using the slab method in local OBB coordinates.
     *
     * The segment from [pFrom] to [pTo] is transformed into this OBB's local coordinate
     * frame, then tested against the axis-aligned slab `[-extents, +extents]`. If the
     * segment intersects, the method returns the first intersection point in world space.
     *
     * @param pFrom ray origin in world space
     * @param pTo   ray endpoint in world space
     * @return [Optional] containing the intersection point if the segment hits this OBB,
     *         or [Optional.empty] if there is no intersection
     */
    fun clip(pFrom: Vector3d, pTo: Vector3d): Optional<Vector3d> {
        val axes = AXES_A.get()
        getAxesInto(axes)

        val localFrom = worldToLocal(pFrom, axes)
        val localTo   = worldToLocal(pTo,   axes)
        val dir = Vector3d(localTo).sub(localFrom)

        var tEnter = 0.0
        var tExit  = 1.0

        for (i in 0..2) {
            val min = -extents.get(i)
            val max =  extents.get(i)
            val origin    = localFrom.get(i)
            val direction = dir.get(i)

            if (Math.abs(direction) < 1e-7f) {
                if (origin !in min..max) return Optional.empty()
                continue
            }

            val t1 = (min - origin) / direction
            val t2 = (max - origin) / direction
            val tNear = Math.min(t1, t2)
            val tFar  = Math.max(t1, t2)

            if (tNear > tEnter) tEnter = tNear
            if (tFar  < tExit)  tExit  = tFar
            if (tEnter > tExit) return Optional.empty()
        }

        val localHit = Vector3d(dir).mul(tEnter).add(localFrom)
        return Optional.of(localToWorld(localHit, axes))
    }

    //
    // Coordinate transformation helpers (private)
    //

    private fun worldToLocal(worldPoint: Vector3d, axes: Array<Vector3d>): Vector3d {
        val rel = Vector3d(worldPoint).sub(center)
        return Vector3d(rel.dot(axes[0]), rel.dot(axes[1]), rel.dot(axes[2]))
    }

    private fun localToWorld(localPoint: Vector3d, axes: Array<Vector3d>): Vector3d {
        val result = Vector3d(center)
        result.add(axes[0].mul(localPoint.x, Vector3d()))
        result.add(axes[1].mul(localPoint.y, Vector3d()))
        result.add(axes[2].mul(localPoint.z, Vector3d()))
        return result
    }

    //
    // Geometric operations (immutable transforms)
    //

    /**
     * Returns a **copy** of this OBB with each half-extent increased uniformly by [amount].
     *
     * The original OBB is not modified. Negative [amount] values shrink the box.
     *
     * @param amount uniform inflation along all three axes
     * @return new inflated OBB
     */
    fun inflate(amount: Double): OBB =
        OBB(center, Vector3d(extents).add(amount, amount, amount), rotation, part)

    /**
     * Returns a **copy** of this OBB with half-extents increased per-axis.
     *
     * @param x inflation on the local X axis
     * @param y inflation on the local Y axis
     * @param z inflation on the local Z axis
     * @return new inflated OBB
     */
    fun inflate(x: Double, y: Double, z: Double): OBB =
        OBB(center, Vector3d(extents).add(x, y, z), rotation, part)

    /**
     * Returns a **copy** of this OBB translated by [vec3].
     *
     * The rotation and extents remain unchanged; only the centre is offset.
     *
     * @param vec3 translation vector in world space
     * @return new translated OBB
     */
    fun move(vec3: Vec3): OBB =
        OBB(Vector3d(center.x + vec3.x, center.y + vec3.y, center.z + vec3.z), extents, rotation, part)

    /**
     * Tests whether world-space point [vec3] lies inside (or on the surface of) this OBB.
     *
     * The test projects the vector from the OBB's centre to [vec3] onto each of the OBB's
     * three world-space axes and compares the projection magnitudes against the extents.
     *
     * @param vec3 point to test in world space
     * @return `true` if the point is inside or on the boundary; `false` otherwise
     */
    fun contains(vec3: Vec3): Boolean {
        val axes = AXES_A.get()
        getAxesInto(axes)
        val rel = vec3ToVector3d(vec3).sub(center)
        val projX = Math.abs(rel.dot(axes[0]))
        val projY = Math.abs(rel.dot(axes[1]))
        val projZ = Math.abs(rel.dot(axes[2]))
        return projX <= extents.x && projY <= extents.y && projZ <= extents.z
    }

    //
    // Part enum
    //

    /**
     * Logical part identifiers used to route collision/damage events to specific
     * sub-components of a vehicle.
     */
    @Serializable
    enum class Part {
        @SerializedName("Empty")       @SerialName("Empty")       EMPTY,
        @SerializedName("WheelLeft")   @SerialName("WheelLeft")   WHEEL_LEFT,
        @SerializedName("WheelRight")  @SerialName("WheelRight")  WHEEL_RIGHT,
        @SerializedName("Turret")      @SerialName("Turret")      TURRET,
        @SerializedName("MainEngine")  @SerialName("MainEngine")  MAIN_ENGINE,
        @SerializedName("SubEngine")   @SerialName("SubEngine")   SUB_ENGINE,
        @SerializedName("Body")        @SerialName("Body")        BODY,
        @SerializedName("Interactive") @SerialName("Interactive") INTERACTIVE,
        @SerializedName("Collision")   @SerialName("Collision")   COLLISION
    }

    //
    // Companion — static helpers
    //

    companion object {

        //
        // Thread-local axis buffers
        //
        // Two separate pools are required because some methods (e.g. isColliding OBB×OBB,
        // computeObbAabbMtv's candidate-axis loop when testing cross-product axes) need
        // the world-space axes of TWO distinct OBBs alive simultaneously on the same thread.
        // A single pool would let the second getAxesInto() call overwrite the first result.
        //
        // Usage contract:
        //   • AXES_A — primary OBB ("this" / "obb" / left operand)
        //   • AXES_B — secondary OBB ("other" / right operand in dual-OBB operations)
        //   • Never escape these arrays beyond the call frame that borrowed them
        //   • Re-borrow is safe only after all previous uses of the same pool on this
        //     thread have completed (i.e. the arrays are conceptually stack-allocated)
        //

        /**
         * Thread-local axis buffer for the **primary** OBB in each operation.
         *
         * Pre-allocated to eliminate per-call `arrayOf(Vector3d(), Vector3d(), Vector3d())`
         * allocations in [getAxesInto]. Each thread receives its own isolated instance,
         * so no synchronization is required.
         *
         * **Must be consumed before the next [getAxesInto] call on the same thread.**
         */
        @JvmField
        val AXES_A: ThreadLocal<Array<Vector3d>> =
            ThreadLocal.withInitial { arrayOf(Vector3d(), Vector3d(), Vector3d()) }

        /**
         * Thread-local axis buffer for the **secondary** OBB in dual-OBB operations
         * (e.g. [isColliding] OBB×OBB, where both operands' axes must be available concurrently).
         */
        @JvmField
        val AXES_B: ThreadLocal<Array<Vector3d>> =
            ThreadLocal.withInitial { arrayOf(Vector3d(), Vector3d(), Vector3d()) }

        //
        // Collision tests
        //

        /**
         * Tests whether two OBBs overlap using the Separating Axis Theorem (SAT).
         *
         * Two oriented boxes collide if and only if they overlap when projected onto
         * all 15 candidate separating axes: 3 face normals from [obb], 3 face normals
         * from [other], and 9 edge-cross-product axes. This method delegates the
         * actual SAT logic to JOML's optimized `Intersectiond.testObOb`.
         *
         * @param obb   first oriented bounding box
         * @param other second oriented bounding box
         * @return `true` if the two OBBs intersect (share at least one point)
         */
        @JvmStatic
        fun isColliding(obb: OBB, other: OBB): Boolean {
            val axes1 = AXES_A.get(); obb.getAxesInto(axes1)
            val axes2 = AXES_B.get(); other.getAxesInto(axes2)
            return Intersectiond.testObOb(
                obb.center,   axes1[0], axes1[1], axes1[2], obb.extents,
                other.center, axes2[0], axes2[1], axes2[2], other.extents
            )
        }

        /**
         * Tests whether an OBB overlaps an axis-aligned bounding box (AABB).
         *
         * The test is equivalent to OBB×OBB collision where the AABB's "rotation"
         * is the identity (its axes are always aligned with world X/Y/Z).
         *
         * @param obb  oriented bounding box
         * @param aabb axis-aligned bounding box in world space
         * @return `true` if the shapes intersect
         */
        @JvmStatic
        fun isColliding(obb: OBB, aabb: AABB): Boolean {
            val axes = AXES_A.get(); obb.getAxesInto(axes)
            val aabbCenter      = Vector3d(aabb.center.x, aabb.center.y, aabb.center.z)
            val aabbHalfExtents = Vector3d(aabb.xsize / 2.0, aabb.ysize / 2.0, aabb.zsize / 2.0)
            return Intersectiond.testObOb(
                obb.center.x, obb.center.y, obb.center.z,
                axes[0].x, axes[0].y, axes[0].z,
                axes[1].x, axes[1].y, axes[1].z,
                axes[2].x, axes[2].y, axes[2].z,
                obb.extents.x, obb.extents.y, obb.extents.z,
                aabbCenter.x, aabbCenter.y, aabbCenter.z,
                1.0, 0.0, 0.0,
                0.0, 1.0, 0.0,
                0.0, 0.0, 1.0,
                aabbHalfExtents.x, aabbHalfExtents.y, aabbHalfExtents.z
            )
        }

        //
        // Minimum Translation Vector (MTV) computation
        //

        /**
         * Computes the minimum translation vector (MTV) needed to separate an oriented bounding
         * box (OBB) from an axis-aligned bounding box (AABB), using the Separating Axis Theorem (SAT).
         *
         * ### Why this overload exists
         *
         * The convenience overload [computeObbAabbMtv] re-derives the OBB's three
         * world-space axes via [OBB.getAxes] on **every single call**. Iterative collision
         * resolution (see `VehicleMotionUtils.resolveObbWorldCollision`) tests the same OBB
         * against dozens of candidate world AABBs, across three separate movement axes,
         * every single game tick. Because translating an OBB never changes its orientation,
         * repeatedly re-deriving its axes for each of those calls wastes CPU cycles on redundant
         * quaternion-vector transforms and short-lived [Vector3d] allocations — this was
         * measured to be one of the largest self-time contributors in production server profiling
         * of vehicle physics (~1.5ms/tick under load).
         *
         * Callers that already hold the OBB's axes (because they call [OBB.getAxes] once per
         * tick and then only translate the OBB's center) should use this overload instead,
         * passing the cached axes in directly.
         *
         * ### Implementation notes
         *
         * This method performs the full 15-axis SAT test (3 OBB face normals, 3 AABB face normals,
         * and 9 edge-cross-product axes) required to correctly resolve OBB-vs-AABB collisions, but
         * avoids all incidental heap allocation on the hot path:
         * - World axes are handled via direct component access (e.g. `axis.x`) instead of
         *   constructing unit [Vector3d] instances, since projecting onto a world axis is
         *   equivalent to reading the corresponding component.
         * - Edge-cross-product axes are computed from their closed-form scalar expression
         *   (e.g. `axis × (1,0,0) = (0, axis.z, -axis.y)`) instead of calling
         *   [Vector3d.cross] into a freshly allocated output vector.
         * - The separating-axis loop returns `null` immediately upon finding the first
         *   axis with no overlap, per standard SAT early-exit practice.
         *
         * The sign convention for the returned MTV: the vector points in the direction the
         * OBB must be pushed to resolve the overlap. The sign is determined by the relative
         * positions of the two boxes' projected centres along the collision axis.
         *
         * @param obbCenter  world-space center of the OBB (may be a translated copy of the original
         *                   center; only the orientation encoded in [obbAxes] must remain unchanged)
         * @param obbAxes    the OBB's three orthonormal world-space axes, as returned by [OBB.getAxes]
         * @param obbExtents the OBB's half-lengths along its own three local axes
         * @param aabb       the world-space axis-aligned bounding box to test against
         * @return the MTV (direction the OBB must be pushed to resolve the overlap, with
         *         magnitude equal to the minimal penetration depth) if the shapes overlap, or
         *         `null` if a separating axis was found (i.e. no collision)
         */
        @JvmStatic
        fun computeObbAabbMtv(
            obbCenter: Vector3d,
            obbAxes: Array<Vector3d>,
            obbExtents: Vector3d,
            aabb: AABB
        ): Vector3d? {
            val obbAxis0 = obbAxes[0]
            val obbAxis1 = obbAxes[1]
            val obbAxis2 = obbAxes[2]
            val extX = obbExtents.x
            val extY = obbExtents.y
            val extZ = obbExtents.z

            val aabbCenterX = (aabb.minX + aabb.maxX) * 0.5
            val aabbCenterY = (aabb.minY + aabb.maxY) * 0.5
            val aabbCenterZ = (aabb.minZ + aabb.maxZ) * 0.5
            val aabbHalfX = aabb.xsize * 0.5
            val aabbHalfY = aabb.ysize * 0.5
            val aabbHalfZ = aabb.zsize * 0.5

            // Vector between box centers — computed once, reused by every axis projection
            val dX = obbCenter.x - aabbCenterX
            val dY = obbCenter.y - aabbCenterY
            val dZ = obbCenter.z - aabbCenterZ

            var minOverlap = Double.MAX_VALUE
            var mtvAxisX = 0.0
            var mtvAxisY = 0.0
            var mtvAxisZ = 0.0
            var pushNegative = false

            //  Axes 1-3: OBB face normals 
            // Projected onto its own face normal, the OBB's radius equals exactly its extent
            // along that axis (the axes are orthonormal by construction), so no summed
            // dot-product is required on the OBB side — only the AABB side needs the
            // general projection formula.
            val obbAxesArr = arrayOf(obbAxis0, obbAxis1, obbAxis2)
            val obbHalfArr = doubleArrayOf(extX, extY, extZ)
            for (i in 0..2) {
                val axis = obbAxesArr[i]
                val axisX = axis.x; val axisY = axis.y; val axisZ = axis.z
                val centerDist = dX * axisX + dY * axisY + dZ * axisZ
                val obbRadius = obbHalfArr[i]
                val aabbRadius = Math.abs(axisX) * aabbHalfX + Math.abs(axisY) * aabbHalfY + Math.abs(axisZ) * aabbHalfZ
                val overlap = obbRadius + aabbRadius - Math.abs(centerDist)
                if (overlap < 0.0) return null
                if (overlap < minOverlap) {
                    minOverlap = overlap
                    mtvAxisX = axisX; mtvAxisY = axisY; mtvAxisZ = axisZ
                    pushNegative = centerDist < 0.0
                }
            }

            //  Axes 4-6: AABB face normals (world X/Y/Z) 
            // Projected onto a world axis, the AABB's radius equals exactly its half-extent
            // along that axis; only the OBB side needs the general projected-extent formula.
            run {
                val obbRadius = Math.abs(obbAxis0.x) * extX + Math.abs(obbAxis1.x) * extY + Math.abs(obbAxis2.x) * extZ
                val overlap = obbRadius + aabbHalfX - Math.abs(dX)
                if (overlap < 0.0) return null
                if (overlap < minOverlap) {
                    minOverlap = overlap
                    mtvAxisX = 1.0; mtvAxisY = 0.0; mtvAxisZ = 0.0
                    pushNegative = dX < 0.0
                }
            }
            run {
                val obbRadius = Math.abs(obbAxis0.y) * extX + Math.abs(obbAxis1.y) * extY + Math.abs(obbAxis2.y) * extZ
                val overlap = obbRadius + aabbHalfY - Math.abs(dY)
                if (overlap < 0.0) return null
                if (overlap < minOverlap) {
                    minOverlap = overlap
                    mtvAxisX = 0.0; mtvAxisY = 1.0; mtvAxisZ = 0.0
                    pushNegative = dY < 0.0
                }
            }
            run {
                val obbRadius = Math.abs(obbAxis0.z) * extX + Math.abs(obbAxis1.z) * extY + Math.abs(obbAxis2.z) * extZ
                val overlap = obbRadius + aabbHalfZ - Math.abs(dZ)
                if (overlap < 0.0) return null
                if (overlap < minOverlap) {
                    minOverlap = overlap
                    mtvAxisX = 0.0; mtvAxisY = 0.0; mtvAxisZ = 1.0
                    pushNegative = dZ < 0.0
                }
            }

            //  Axes 7-15: edge cross-products (3 OBB axes × 3 world axes) 
            // For a world basis vector e, (a × e) has a closed-form, allocation-free expression:
            //   a × (1,0,0) = ( 0,   a.z, -a.y)
            //   a × (0,1,0) = (-a.z,  0,   a.x)
            //   a × (0,0,1) = ( a.y, -a.x,  0)
            // This avoids calling Vector3d.cross into a freshly allocated output vector
            // 9 times per candidate AABB.
            for (i in 0..2) {
                val a = obbAxesArr[i]
                for (j in 0..2) {
                    val axisX: Double; val axisY: Double; val axisZ: Double
                    when (j) {
                        0 -> { axisX = 0.0;  axisY =  a.z; axisZ = -a.y }
                        1 -> { axisX = -a.z; axisY = 0.0;  axisZ =  a.x }
                        else -> { axisX =  a.y; axisY = -a.x; axisZ = 0.0 }
                    }

                    val lenSq = axisX * axisX + axisY * axisY + axisZ * axisZ
                    // Degenerate axis (this OBB axis is parallel to the corresponding world axis):
                    // carries no separating information and would divide by ~0 when normalized.
                    if (lenSq < 1.0e-9) continue

                    val invLen = 1.0 / Math.sqrt(lenSq)
                    val nx = axisX * invLen; val ny = axisY * invLen; val nz = axisZ * invLen

                    val centerDist = dX * nx + dY * ny + dZ * nz
                    var obbRadius = 0.0
                    for (k in 0..2) {
                        val ak = obbAxesArr[k]
                        obbRadius += Math.abs(ak.x * nx + ak.y * ny + ak.z * nz) * obbHalfArr[k]
                    }
                    val aabbRadius = Math.abs(nx) * aabbHalfX + Math.abs(ny) * aabbHalfY + Math.abs(nz) * aabbHalfZ
                    val overlap = obbRadius + aabbRadius - Math.abs(centerDist)
                    if (overlap < 0.0) return null
                    if (overlap < minOverlap) {
                        minOverlap = overlap
                        mtvAxisX = nx; mtvAxisY = ny; mtvAxisZ = nz
                        pushNegative = centerDist < 0.0
                    }
                }
            }

            // The MTV must push the OBB out of the AABB: if the OBB's projected center lies
            // on the negative side of the axis relative to the AABB's projected center,
            // the push direction along that axis must be negated.
            val sign = if (pushNegative) -1.0 else 1.0
            return Vector3d(mtvAxisX * sign * minOverlap, mtvAxisY * sign * minOverlap, mtvAxisZ * sign * minOverlap)
        }

        /**
         * Convenience overload of [computeObbAabbMtv] that derives the OBB's world-space axes
         * internally via [OBB.getAxes].
         *
         * **Prefer the explicit-axes overload** when testing the same OBB against multiple
         * candidate AABBs within the same tick (e.g. inside a broad-phase collision loop),
         * to avoid deriving the same orientation-invariant axes repeatedly; see the
         * explicit-axes overload's Javadoc for details and measured impact.
         *
         * @param obb  the OBB to test
         * @param aabb the world-space axis-aligned bounding box to test against
         * @return the MTV if the shapes overlap, or `null` otherwise
         */
        @JvmStatic
        fun computeObbAabbMtv(obb: OBB, aabb: AABB): Vector3d? =
            computeObbAabbMtv(obb.center, obb.getAxes(), obb.extents, aabb)

        //
        // AABB helpers
        //

        /**
         * Computes the world-space AABB of an OBB **after** applying a translation.
         *
         * Since translation does not affect orientation, the half-extents of the resulting
         * AABB are identical to the untranslated OBB's projected half-extents (same projection
         * formula used by `VehicleMotionUtils.calculateCombinedAABBOptimized`); only the
         * box's center needs to be offset by [translation].
         *
         * This avoids allocating a translated [OBB] copy and re-deriving its 8 corner vertices
         * through quaternion rotation (as [getWorldAABB] combined with [OBB.move] would).
         *
         * @param obb         the source OBB (only its [OBB.center] and [OBB.extents] are read)
         * @param axes        the OBB's three orthonormal world-space axes, as returned by [OBB.getAxes]
         * @param translation the world-space offset to apply to the OBB's center before computing
         *                    the resulting AABB
         * @return the world-space AABB of the OBB after being translated by [translation]
         */
        @JvmStatic
        fun getTranslatedWorldAABB(obb: OBB, axes: Array<Vector3d>, translation: Vec3): AABB {
            val ext = obb.extents
            val halfX = Math.abs(axes[0].x) * ext.x + Math.abs(axes[1].x) * ext.y + Math.abs(axes[2].x) * ext.z
            val halfY = Math.abs(axes[0].y) * ext.x + Math.abs(axes[1].y) * ext.y + Math.abs(axes[2].y) * ext.z
            val halfZ = Math.abs(axes[0].z) * ext.x + Math.abs(axes[1].z) * ext.y + Math.abs(axes[2].z) * ext.z
            val cx = obb.center.x + translation.x
            val cy = obb.center.y + translation.y
            val cz = obb.center.z + translation.z
            return AABB(cx - halfX, cy - halfY, cz - halfZ, cx + halfX, cy + halfY, cz + halfZ)
        }

        /**
         * Computes the axis-aligned bounding box (AABB) enclosing [obb] in world space.
         *
         * Uses the projection formula instead of vertex enumeration:
         * `halfX = |axis0.x|·ex + |axis1.x|·ey + |axis2.x|·ez`, etc.
         *
         * @param obb source OBB
         * @return world-space AABB enclosing [obb]
         */
        @JvmStatic
        fun getWorldAABB(obb: OBB): AABB {
            val axes = AXES_A.get(); obb.getAxesInto(axes)
            val c = obb.center; val e = obb.extents

            val halfX = Math.abs(axes[0].x) * e.x + Math.abs(axes[1].x) * e.y + Math.abs(axes[2].x) * e.z
            val halfY = Math.abs(axes[0].y) * e.x + Math.abs(axes[1].y) * e.y + Math.abs(axes[2].y) * e.z
            val halfZ = Math.abs(axes[0].z) * e.x + Math.abs(axes[1].z) * e.y + Math.abs(axes[2].z) * e.z

            return AABB(c.x - halfX, c.y - halfY, c.z - halfZ,
                        c.x + halfX, c.y + halfY, c.z + halfZ)
        }

        //
        // Spatial queries
        //

        /**
         * Returns the closest point on [obb]'s surface (or interior) to [point].
         *
         * The algorithm projects the vector from [obb]'s centre to [point] onto each of
         * [obb]'s three world-space axes, clamps each projection to `[-extent, +extent]`,
         * and reconstructs the world-space point from the clamped projections.
         *
         * @param point world-space query point
         * @param obb   target OBB
         * @return closest world-space point on or inside [obb]
         */
        @JvmStatic
        fun getClosestPointOBB(point: Vector3d, obb: OBB): Vector3d {
            val axes    = AXES_A.get(); obb.getAxesInto(axes)
            val nearP   = Vector3d(obb.center)
            val dist    = point.sub(nearP, Vector3d())
            val extents = doubleArrayOf(obb.extents.x, obb.extents.y, obb.extents.z)

            for (i in 0..2) {
                var distance = dist.dot(axes[i])
                distance = Math.clamp(distance, -extents[i], extents[i])
                nearP.x += distance * axes[i].x
                nearP.y += distance * axes[i].y
                nearP.z += distance * axes[i].z
            }
            return nearP
        }

        /**
         * Returns the OBB that [player]'s line-of-sight intersects within [range], if any.
         *
         * Finds the entity the player is looking at via [TraceTool.findLookingEntity], then
         * performs precise ray–OBB intersection tests against all of that entity's OBBs
         * (excluding [Part.COLLISION] parts). The closest intersected OBB is returned.
         *
         * @param player player performing the look-up
         * @param range  maximum ray distance in blocks
         * @return the closest intersected OBB, or `null` if none (or if the entity uses AABB mode)
         */
        @JvmStatic
        fun getLookingObb(player: Player, range: Double): OBB? {
            val lookingEntity = TraceTool.findLookingEntity(player, range)
            if (lookingEntity !is OBBEntity || lookingEntity.enableAABB()) return null

            val eyePos  = player.getEyePosition(1.0f)
            val viewVec = player.getViewVector(1.0f)
            val lookEnd = eyePos.add(viewVec.scale(range))

            var closestOBB: OBB? = null
            var minDistanceSq = Double.MAX_VALUE

            for (obb in lookingEntity.getOBBs()) {
                if (obb.part == Part.COLLISION) continue
                val hitPos: Vec3 = rayIntersect(obb, eyePos, lookEnd) ?: continue
                val distanceSq = eyePos.distanceToSqr(hitPos)
                if (distanceSq < minDistanceSq) {
                    minDistanceSq = distanceSq
                    closestOBB = obb
                }
            }
            return closestOBB
        }

        /**
         * Tests whether the line segment from [start] to [end] intersects [obb].
         *
         * Transforms the segment endpoints into [obb]'s local coordinate frame and performs
         * a ray–AABB intersection test against the local-space slab `[-extents, +extents]`.
         * The first intersection point (if any) is transformed back to world space.
         *
         * @param obb   target OBB
         * @param start segment start in world space
         * @param end   segment end in world space
         * @return world-space hit position, or `null` if no intersection
         */
        fun rayIntersect(obb: OBB, start: Vec3, end: Vec3): Vec3? {
            val center  = vector3dToVec3(obb.center)
            val extents = vector3dToVec3(obb.extents)

            val localStart = toLocal(obb, start)
            val localEnd   = toLocal(obb, end)

            val result = Vector2d()
            val intersects = Intersectiond.intersectRayAab(
                localStart.x, localStart.y, localStart.z,
                localEnd.x - localStart.x, localEnd.y - localStart.y, localEnd.z - localStart.z,
                -extents.x, -extents.y, -extents.z,
                 extents.x,  extents.y,  extents.z,
                result
            )

            if (!intersects) return null

            val clampedT = Math.max(0.0, Math.min(1.0, result.x))
            val localHit = Vector3d(
                localStart.x + clampedT * (localEnd.x - localStart.x),
                localStart.y + clampedT * (localEnd.y - localStart.y),
                localStart.z + clampedT * (localEnd.z - localStart.z)
            )
            obb.rotation.transform(localHit)
            return Vec3(localHit.x + center.x, localHit.y + center.y, localHit.z + center.z)
        }

        /** Transforms [worldPoint] into [obb]'s local coordinate frame. */
        private fun toLocal(obb: OBB, worldPoint: Vec3): Vector3d {
            val center  = vector3dToVec3(obb.center)
            val inverse = Quaterniond(obb.rotation).conjugate()
            val relative = Vector3d(
                worldPoint.x - center.x,
                worldPoint.y - center.y,
                worldPoint.z - center.z
            )
            inverse.transform(relative)
            return relative
        }

        //
        // Coordinate conversion helpers
        //

        /** Converts a Minecraft [Vec3] to a JOML [Vector3d]. */
        @JvmStatic
        fun vec3ToVector3d(vec3: Vec3): Vector3d = Vector3d(vec3.x, vec3.y, vec3.z)

        /** Converts a JOML [Vector3d] to a Minecraft [Vec3]. */
        @JvmStatic
        fun vector3dToVec3(vector3d: Vector3d): Vec3 = Vec3(vector3d.x, vector3d.y, vector3d.z)
    }
}