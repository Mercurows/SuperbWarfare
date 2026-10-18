package com.atsuishio.superbwarfare.physics.tracked

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.joml.Matrix4d
import org.joml.Vector3d

/**
 * Neutral, server-safe reader for Bedrock geometry.
 *
 * It deliberately does not touch the client model baker: the physics geometry
 * must be derivable from the asset alone, on either logical side, and must be
 * reproducible for auditing. Only pivots, cube extents and the bone hierarchy
 * are used.
 *
 * Model space is Bedrock's: pixels, X to the model's right in file space, Y up,
 * Z forward. [toPhysicsLocal] performs the one documented change of basis into
 * the vehicle-local metre frame.
 */
class BedrockModelGeometry private constructor(
    val bones: Map<String, Bone>,
    val roots: List<String>,
) {
    data class Cube(
        val origin: Vector3d,
        val size: Vector3d,
        val pivot: Vector3d?,
        val rotation: Vector3d?,
        val inflate: Double,
    )

    data class Bone(
        val name: String,
        val parent: String?,
        val pivot: Vector3d,
        val rotation: Vector3d?,
        val cubes: List<Cube>,
    )

    private val children: Map<String, List<String>> = bones.values
        .groupBy { it.parent ?: "" }
        .mapValues { entry -> entry.value.map { it.name } }

    /** Root-to-bone transform in model space, Bedrock's pivot-relative convention. */
    fun localTransform(name: String): Matrix4d {
        val chain = ArrayList<Bone>()
        var current: Bone? = bones[name] ?: throw TrackedGeometryException("missing bone $name")
        val seen = HashSet<String>()
        while (current != null) {
            if (!seen.add(current.name)) {
                throw TrackedGeometryException("cycle in bone ancestry at ${current.name}")
            }
            chain += current
            current = current.parent?.let { bones[it] ?: throw TrackedGeometryException("missing parent $it") }
        }
        val out = Matrix4d()
        for (bone in chain.asReversed()) out.mul(boneLocalTransform(bone))
        return out
    }

    /**
     * The bone's pivot in model space.
     *
     * A Bedrock bone transform is `T(pivot) * R * T(-pivot)`, so its origin is
     * the *parent* origin, not the pivot. The pivot has to be transformed by the
     * parent chain explicitly; transforming the zero vector only returns the
     * rotation-induced offset and collapses every unrotated bone to the origin.
     */
    fun worldPivot(name: String): Vector3d {
        val bone = bones[name] ?: throw TrackedGeometryException("missing bone $name")
        val parentTransform = bone.parent?.let { localTransform(it) } ?: Matrix4d()
        return parentTransform.transformPosition(Vector3d(bone.pivot), Vector3d())
    }

    /** Descendants of [name], including itself, in deterministic order. */
    fun subtree(name: String): List<String> {
        val out = ArrayList<String>()
        fun walk(current: String) {
            if (current !in bones) return
            out += current
            children[current].orEmpty().sorted().forEach(::walk)
        }
        walk(name)
        return out
    }

    /** Eight transformed corners of every cube owned by [bone] and its descendants. */
    fun subtreeCorners(name: String): List<Vector3d> {
        val out = ArrayList<Vector3d>(32)
        for (boneName in subtree(name)) {
            val bone = bones[boneName] ?: continue
            val boneTransform = localTransform(boneName)
            for (cube in bone.cubes) out += cubeCorners(cube, boneTransform)
        }
        return out
    }

    /** Eight transformed corners of the cubes owned by exactly [name]. */
    fun ownCorners(name: String): List<Vector3d> {
        val bone = bones[name] ?: throw TrackedGeometryException("missing bone $name")
        val transform = localTransform(name)
        return bone.cubes.flatMap { cubeCorners(it, transform) }
    }

    /**
     * Converts a model-space point to the vehicle-local metre frame used by the
     * rest of the physics code: `(x, y, -z) / 16 * renderScale`.
     *
     * This is exactly the mapping that the existing authored `Wheels` data was
     * verified against, so a compiled binding can be cross-checked against the
     * legacy numbers without a second convention.
     */
    fun toPhysicsLocal(point: Vector3d, renderScale: Double): Vector3d = Vector3d(
        point.x / 16.0 * renderScale,
        point.y / 16.0 * renderScale,
        -point.z / 16.0 * renderScale,
    )

    private fun cubeCorners(cube: Cube, boneTransform: Matrix4d): List<Vector3d> {
        val local = cubeLocalMatrix(cube)
        val inflate = cube.inflate
        val sx = cube.size.x
        val sy = cube.size.y
        val sz = cube.size.z
        val out = ArrayList<Vector3d>(8)
        for (ix in 0..1) for (iy in 0..1) for (iz in 0..1) {
            val corner = Vector3d(
                if (ix == 0) -inflate else sx + inflate,
                if (iy == 0) -inflate else sy + inflate,
                if (iz == 0) -inflate else sz + inflate,
            )
            out += local.transformPosition(corner)
        }
        return out.map { boneTransform.transformPosition(it, Vector3d()) }
    }

    /** Cube corners in the owning bone's local frame. */
    fun localCubeCorners(cube: Cube): List<Vector3d> {
        val local = cubeLocalMatrix(cube)
        val inflate = cube.inflate
        val out = ArrayList<Vector3d>(8)
        for (ix in 0..1) for (iy in 0..1) for (iz in 0..1) {
            val corner = Vector3d(
                if (ix == 0) -inflate else cube.size.x + inflate,
                if (iy == 0) -inflate else cube.size.y + inflate,
                if (iz == 0) -inflate else cube.size.z + inflate,
            )
            out += local.transformPosition(corner)
        }
        return out
    }

    /** Cube centre in the owning bone's local frame. */
    fun localCubeCentre(cube: Cube): Vector3d =
        cubeLocalMatrix(cube).transformPosition(Vector3d(cube.size.x * 0.5, cube.size.y * 0.5, cube.size.z * 0.5))

    private fun cubeLocalMatrix(cube: Cube): Matrix4d {
        val local = Matrix4d()
        if (cube.pivot != null && cube.rotation != null) {
            local.translate(cube.pivot)
            local.mul(rotationMatrix(cube.rotation))
            local.translate(Vector3d(cube.pivot).negate())
        }
        local.translate(cube.origin)
        return local
    }

    private fun boneLocalTransform(bone: Bone): Matrix4d {
        val out = Matrix4d()
        out.translate(bone.pivot)
        if (bone.rotation != null) out.mul(rotationMatrix(bone.rotation))
        out.translate(Vector3d(bone.pivot).negate())
        return out
    }

    /**
     * Bedrock rotations are degrees about Z, then Y, then X, applied around the
     * owning pivot. The resulting matrix is reused unchanged for cubes, which
     * keeps one convention for bones and cubes.
     */
    private fun rotationMatrix(degrees: Vector3d): Matrix4d = Matrix4d()
        .rotateZ(Math.toRadians(degrees.z))
        .rotateY(Math.toRadians(degrees.y))
        .rotateX(Math.toRadians(degrees.x))

    companion object {
        fun parse(json: String): BedrockModelGeometry {
            val root = JsonParser.parseString(json).asJsonObject
            val geometry = root.getAsJsonArray("minecraft:geometry")?.firstOrNull()?.asJsonObject
                ?: throw TrackedGeometryException("missing minecraft:geometry")
            val array = geometry.getAsJsonArray("bones") ?: throw TrackedGeometryException("missing bones")
            val bones = LinkedHashMap<String, Bone>()
            for (element in array) {
                val obj = element.asJsonObject
                val name = obj.string("name") ?: throw TrackedGeometryException("bone without name")
                if (bones.containsKey(name)) throw TrackedGeometryException("duplicate bone $name")
                val parent = obj.string("parent")
                val pivot = obj.vector("pivot") ?: Vector3d()
                val rotation = obj.vector("rotation")
                val cubes = obj.getAsJsonArray("cubes")?.map { cubeElement ->
                    val cube = cubeElement.asJsonObject
                    Cube(
                        origin = cube.vector("origin") ?: Vector3d(),
                        size = cube.vector("size") ?: Vector3d(),
                        pivot = cube.vector("pivot"),
                        rotation = cube.vector("rotation"),
                        inflate = cube.double("inflate") ?: 0.0,
                    )
                }.orEmpty()
                bones[name] = Bone(name, parent, pivot, rotation, cubes)
            }
            val roots = bones.values.filter { it.parent == null }.map { it.name }
            if (roots.isEmpty()) throw TrackedGeometryException("no root bone")
            return BedrockModelGeometry(bones, roots)
        }

        private fun JsonObject.string(key: String): String? =
            get(key)?.takeIf { !it.isJsonNull }?.asString

        private fun JsonObject.double(key: String): Double? =
            get(key)?.takeIf { !it.isJsonNull }?.asDouble

        private fun JsonObject.vector(key: String): Vector3d? {
            val array = getAsJsonArray(key) ?: return null
            if (array.size() < 3) return null
            return Vector3d(array[0].asDouble, array[1].asDouble, array[2].asDouble)
        }
    }
}

class TrackedGeometryException(message: String) : IllegalArgumentException(message)
