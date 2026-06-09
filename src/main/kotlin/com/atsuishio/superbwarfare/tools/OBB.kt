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
 * Codes based on @AnECanSaiTin's [HitboxAPI](https://github.com/AnECanSaiTin/HitboxAPI)
 * 
 * @param center   旋转中心
 * @param extents  三个轴向上的半长
 * @param rotation 旋转
 * @param part     部件
 */
@JvmRecord
data class OBB(
    @JvmField val center: Vector3d,
    val extents: Vector3d,
    val rotation: Quaterniond,
    @JvmField val part: Part
) {
    fun setCenter(center: Vector3d?) {
        this.center.set(center)
    }

    fun setExtents(extents: Vector3d?) {
        this.extents.set(extents)
    }

    fun updateRotation(rotation: Quaterniond?) {
        this.rotation.set(rotation)
    }

    /**
     * 计算与向量相交的某个面
     * 
     * @author YWZJ Ranpoes
     */
    fun getEmbeddingFace(vec3: Vec3): Int {
        val rel: Vector3d = vec3ToVector3d(vec3).sub(center)

        val axes = arrayOfNulls<Vector3d>(3)
        axes[0] = rotation.transform(Vector3d(1.0, 0.0, 0.0))
        axes[1] = rotation.transform(Vector3d(0.0, 1.0, 0.0))
        axes[2] = rotation.transform(Vector3d(0.0, 0.0, 1.0))

        val projX = Math.abs(rel.dot(axes[0]))
        val projY = Math.abs(rel.dot(axes[1]))
        val projZ = Math.abs(rel.dot(axes[2]))

        var min = Double.MAX_VALUE
        var index = 0

        val dx = extents.x - projX
        val dy = extents.y - projY
        val dz = extents.z - projZ

        if (dx < min) {
            min = dx
            index = 1
        }
        if (dy < min) {
            min = dy
            index = 2
        }
        if (dz < min) {
            index = 3
        }

        return index * (if (rel.dot(axes[index - 1]) < 0) -1 else 1)
    }

    fun getEmbeddingDepth(vec3: Vec3): Double {
        val rel: Vector3d = vec3ToVector3d(vec3).sub(center)

        val axes = arrayOfNulls<Vector3d>(3)
        axes[0] = rotation.transform(Vector3d(1.0, 0.0, 0.0))
        axes[1] = rotation.transform(Vector3d(0.0, 1.0, 0.0))
        axes[2] = rotation.transform(Vector3d(0.0, 0.0, 1.0))

        val projX = Math.abs(rel.dot(axes[0]))
        val projY = Math.abs(rel.dot(axes[1]))
        val projZ = Math.abs(rel.dot(axes[2]))

        val dx = extents.x - projX
        val dy = extents.y - projY
        val dz = extents.z - projZ

        var minDepth = Double.MAX_VALUE

        if (Math.abs(dx) < Math.abs(minDepth)) {
            minDepth = dx
        }
        if (Math.abs(dy) < Math.abs(minDepth)) {
            minDepth = dy
        }
        if (Math.abs(dz) < Math.abs(minDepth)) {
            minDepth = dz
        }

        return minDepth
    }

    /**
     * 获取OBB的8个顶点坐标
     *
     * @return 顶点坐标
     */
    fun getVertices(): Array<Vector3d> {
        val vertices = arrayOfNulls<Vector3d>(8)

        val localVertices = arrayOf(
            Vector3d(-extents.x, -extents.y, -extents.z),
            Vector3d(extents.x, -extents.y, -extents.z),
            Vector3d(extents.x, extents.y, -extents.z),
            Vector3d(-extents.x, extents.y, -extents.z),
            Vector3d(-extents.x, -extents.y, extents.z),
            Vector3d(extents.x, -extents.y, extents.z),
            Vector3d(extents.x, extents.y, extents.z),
            Vector3d(-extents.x, extents.y, extents.z)
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
     * 获取OBB的三个正交轴
     *
     * @return 正交轴
     */
    fun getAxes(): Array<Vector3d> {
        val axes: Array<Vector3d> = arrayOf(
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, 0.0, 1.0)
        )
        rotation.transform(axes[0])
        rotation.transform(axes[1])
        rotation.transform(axes[2])
        return axes
    }

    fun clip(pFrom: Vector3d, pTo: Vector3d): Optional<Vector3d> {
        // 计算OBB的局部坐标系基向量（世界坐标系中的方向）
        val axes = arrayOf<Vector3d>(
            rotation.transform(Vector3d(1.0, 0.0, 0.0)),
            rotation.transform(Vector3d(0.0, 1.0, 0.0)),
            rotation.transform(Vector3d(0.0, 0.0, 1.0))
        )

        // 将点转换到OBB局部坐标系
        val localFrom = worldToLocal(pFrom, axes)
        val localTo = worldToLocal(pTo, axes)

        // 射线方向（局部坐标系）
        val dir = Vector3d(localTo).sub(localFrom)

        // Slab算法参数
        var tEnter = 0.0 // 进入时间
        var tExit = 1.0 // 离开时间

        // 在三个轴上执行Slab算法
        for (i in 0..2) {
            val min = -extents.get(i)
            val max = extents.get(i)
            val origin = localFrom.get(i)
            val direction = dir.get(i)

            // 处理射线平行于轴的情况
            if (Math.abs(direction) < 1e-7f) {
                if (origin !in min..max) {
                    return Optional.empty()
                }
                continue
            }

            // 计算与两个平面的交点参数
            val t1 = (min - origin) / direction
            val t2 = (max - origin) / direction

            // 确保tNear是近平面，tFar是远平面
            val tNear = Math.min(t1, t2)
            val tFar = Math.max(t1, t2)

            // 更新进入/离开时间
            if (tNear > tEnter) tEnter = tNear
            if (tFar < tExit) tExit = tFar

            // 检查是否提前退出（无交点）
            if (tEnter > tExit) {
                return Optional.empty()
            }
        }

        // 检查是否有有效交点
        // 计算局部坐标系中的交点
        val localHit = Vector3d(dir).mul(tEnter).add(localFrom)
        // 转换回世界坐标系
        return Optional.of(localToWorld(localHit, axes))
    }

    // 世界坐标转局部坐标
    private fun worldToLocal(worldPoint: Vector3d, axes: Array<Vector3d>): Vector3d {
        val rel = Vector3d(worldPoint).sub(center)
        return Vector3d(
            rel.dot(axes[0]),
            rel.dot(axes[1]),
            rel.dot(axes[2])
        )
    }

    // 局部坐标转世界坐标
    private fun localToWorld(localPoint: Vector3d, axes: Array<Vector3d>): Vector3d {
        val result = Vector3d(center)
        result.add(axes[0].mul(localPoint.x, Vector3d()))
        result.add(axes[1].mul(localPoint.y, Vector3d()))
        result.add(axes[2].mul(localPoint.z, Vector3d()))
        return result
    }

    fun inflate(amount: Double): OBB {
        val newExtents = Vector3d(extents).add(amount, amount, amount)
        return OBB(center, newExtents, rotation, part)
    }

    fun inflate(x: Double, y: Double, z: Double): OBB {
        val newExtents = Vector3d(extents).add(x, y, z)
        return OBB(center, newExtents, rotation, part)
    }

    fun move(vec3: Vec3): OBB {
        val newCenter = Vector3d(center.x + vec3.x, center.y + vec3.y, center.z + vec3.z)
        return OBB(newCenter, extents, rotation, part)
    }

    /**
     * 检查点是否在OBB内部
     * 
     * @return 如果点在OBB内部则返回true，否则返回false
     */
    fun contains(vec3: Vec3): Boolean {
        // 计算点到OBB中心的向量
        val rel: Vector3d = vec3ToVector3d(vec3).sub(center)

        val axes = arrayOfNulls<Vector3d>(3)
        axes[0] = rotation.transform(Vector3d(1.0, 0.0, 0.0))
        axes[1] = rotation.transform(Vector3d(0.0, 1.0, 0.0))
        axes[2] = rotation.transform(Vector3d(0.0, 0.0, 1.0))

        // 将相对向量投影到OBB的三个轴上
        val projX = Math.abs(rel.dot(axes[0]))
        val projY = Math.abs(rel.dot(axes[1]))
        val projZ = Math.abs(rel.dot(axes[2]))

        // 检查投影值是否小于对应轴上的半长
        return projX <= extents.x && projY <= extents.y && projZ <= extents.z
    }

    @Serializable
    enum class Part {
        @SerializedName("Empty")
        @SerialName("Empty")
        EMPTY,

        @SerializedName("WheelLeft")
        @SerialName("WheelLeft")
        WHEEL_LEFT,

        @SerializedName("WheelRight")
        @SerialName("WheelRight")
        WHEEL_RIGHT,

        @SerializedName("Turret")
        @SerialName("Turret")
        TURRET,

        @SerializedName("MainEngine")
        @SerialName("MainEngine")
        MAIN_ENGINE,

        @SerializedName("SubEngine")
        @SerialName("SubEngine")
        SUB_ENGINE,

        @SerializedName("Body")
        @SerialName("Body")
        BODY,

        @SerializedName("Interactive")
        @SerialName("Interactive")
        INTERACTIVE,

        @SerializedName("Collision")
        @SerialName("Collision")
        COLLISION
    }

    companion object {
        /**
         * 判断两个OBB是否相撞
         */
        @JvmStatic
        fun isColliding(obb: OBB, other: OBB): Boolean {
            val axes1 = obb.getAxes()
            val axes2 = other.getAxes()
            return Intersectiond.testObOb(
                obb.center, axes1[0], axes1[1], axes1[2], obb.extents,
                other.center, axes2[0], axes2[1], axes2[2], other.extents
            )
        }

        /**
         * 判断OBB和AABB是否相撞
         */
        @JvmStatic
        fun isColliding(obb: OBB, aabb: AABB): Boolean {
            val obbCenter = obb.center
            val obbAxes = obb.getAxes()
            val obbHalfExtents = obb.extents
            val aabbCenter: Vector3d = vec3ToVector3d(aabb.center)
            val aabbHalfExtents = Vector3d(aabb.xsize / 2, aabb.ysize / 2f, aabb.zsize / 2f)
            return Intersectiond.testObOb(
                obbCenter.x, obbCenter.y, obbCenter.z,
                obbAxes[0].x, obbAxes[0].y, obbAxes[0].z,
                obbAxes[1].x, obbAxes[1].y, obbAxes[1].z,
                obbAxes[2].x, obbAxes[2].y, obbAxes[2].z,
                obbHalfExtents.x, obbHalfExtents.y, obbHalfExtents.z,
                aabbCenter.x, aabbCenter.y, aabbCenter.z,
                1.0, 0.0, 0.0,
                0.0, 1.0, 0.0,
                0.0, 0.0, 1.0,
                aabbHalfExtents.x, aabbHalfExtents.y, aabbHalfExtents.z
            )
        }

        /**
         * 计算OBB和AABB之间的最小平移向量(MTV)
         * 使用分离轴定理(SAT)检测碰撞并计算穿透深度
         *
         * @param obb  OBB盒
         * @param aabb AABB盒
         * @return 若发生碰撞则返回MTV（方向为将OBB推出AABB的方向，大小为最小穿透深度），否则返回null
         */
        @JvmStatic
        fun computeObbAabbMtv(obb: OBB, aabb: AABB): Vector3d? {
            val obbAxes = obb.getAxes()
            val aabbCenter = Vector3d(aabb.center.x, aabb.center.y, aabb.center.z)
            val aabbHalfExtents = Vector3d(aabb.xsize / 2.0, aabb.ysize / 2.0, aabb.zsize / 2.0)
            val obbCenter = obb.center
            val obbExtents = obb.extents

            val worldAxes = arrayOf(
                Vector3d(1.0, 0.0, 0.0),
                Vector3d(0.0, 1.0, 0.0),
                Vector3d(0.0, 0.0, 1.0)
            )

            var minOverlap = Double.MAX_VALUE
            val mtvAxis = Vector3d()
            var pushNegative = false  // true = push along -axis, false = push along +axis

            // 收集所有候选分离轴：3 OBB面法线 + 3 世界轴(AABB面法线) + 9 叉积轴
            val candidateAxes = mutableListOf<Vector3d>()
            candidateAxes.addAll(obbAxes)      // OBB面法线
            candidateAxes.addAll(worldAxes)    // 世界轴（AABB面法线）

            // 叉积轴（边-边碰撞检测）
            for (obbAxis in obbAxes) {
                for (worldAxis in worldAxes) {
                    val cross = Vector3d()
                    obbAxis.cross(worldAxis, cross)
                    if (cross.lengthSquared() > 1e-7) {
                        cross.normalize()
                        candidateAxes.add(cross)
                    }
                }
            }

            for (axis in candidateAxes) {
                // OBB在轴上的投影
                val obbProjCenter = obbCenter.dot(axis)
                val obbRadius = Math.abs(obbAxes[0].dot(axis)) * obbExtents.x +
                        Math.abs(obbAxes[1].dot(axis)) * obbExtents.y +
                        Math.abs(obbAxes[2].dot(axis)) * obbExtents.z
                val obbMin = obbProjCenter - obbRadius
                val obbMax = obbProjCenter + obbRadius

                // AABB在轴上的投影（AABB是轴对齐的，投影半径可以优化计算）
                val aabbProjCenter = aabbCenter.dot(axis)
                val aabbRadius = Math.abs(axis.x) * aabbHalfExtents.x +
                        Math.abs(axis.y) * aabbHalfExtents.y +
                        Math.abs(axis.z) * aabbHalfExtents.z
                val aabbMin = aabbProjCenter - aabbRadius
                val aabbMax = aabbProjCenter + aabbRadius

                // 检查分离
                if (obbMax < aabbMin || aabbMax < obbMin) {
                    return null  // 找到分离轴，无碰撞
                }

                // 计算重叠量：两个重叠方向中取较小者
                val overlap1 = obbMax - aabbMin
                val overlap2 = aabbMax - obbMin
                val overlap = Math.min(overlap1, overlap2)

                if (overlap < minOverlap) {
                    minOverlap = overlap
                    mtvAxis.set(axis)
                    // overlap1 < overlap2 意味着obbMax离aabbMin更近，应沿-axis推出OBB
                    pushNegative = overlap1 < overlap2
                }
            }

            // 构建MTV：方向指向将OBB推出AABB的最小方向
            if (pushNegative) {
                mtvAxis.negate()
            }
            return Vector3d(mtvAxis).mul(minOverlap)
        }

        /**
         * 计算OBB的世界轴对齐包围盒(AABB)
         * 通过8个顶点求最小/最大坐标得出
         *
         * @param obb OBB盒
         * @return OBB的世界AABB
         */
        @JvmStatic
        fun getWorldAABB(obb: OBB): AABB {
            val vertices = obb.getVertices()
            var minX = Double.MAX_VALUE
            var minY = Double.MAX_VALUE
            var minZ = Double.MAX_VALUE
            var maxX = -Double.MAX_VALUE
            var maxY = -Double.MAX_VALUE
            var maxZ = -Double.MAX_VALUE
            for (v in vertices) {
                if (v.x < minX) minX = v.x
                if (v.y < minY) minY = v.y
                if (v.z < minZ) minZ = v.z
                if (v.x > maxX) maxX = v.x
                if (v.y > maxY) maxY = v.y
                if (v.z > maxZ) maxZ = v.z
            }
            return AABB(minX, minY, minZ, maxX, maxY, maxZ)
        }

        /**
         * 计算OBB上离待判定点最近的点
         *
         * @param point 待判定点
         * @param obb   OBB盒
         * @return 在OBB上离待判定点最近的点
         */
        @JvmStatic
        fun getClosestPointOBB(point: Vector3d, obb: OBB): Vector3d {
            val nearP = Vector3d(obb.center)
            val dist = point.sub(nearP, Vector3d())

            val extents = doubleArrayOf(obb.extents.x, obb.extents.y, obb.extents.z)
            val axes = obb.getAxes()

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
         * 获取玩家看向的某个OBB
         */
        @JvmStatic
        fun getLookingObb(player: Player, range: Double): OBB? {
            val lookingEntity = TraceTool.findLookingEntity(player, range)
            if (lookingEntity !is OBBEntity || lookingEntity.enableAABB()) {
                return null
            }

            // 获取玩家视线信息
            val eyePos = player.getEyePosition(1.0f)
            val viewVec = player.getViewVector(1.0f)
            val lookEnd = eyePos.add(viewVec.scale(range))

            var closestOBB: OBB? = null
            var minDistanceSq = Double.MAX_VALUE

            for (obb in lookingEntity.getOBBs()) {
                if (obb.part == Part.COLLISION) continue
                // 使用精确的射线相交检测
                val hitPos: Vec3? = rayIntersect(obb, eyePos, lookEnd)

                if (hitPos != null) {
                    // 计算交点到眼睛的平方距离
                    val distanceSq = eyePos.distanceToSqr(hitPos)

                    if (distanceSq < minDistanceSq) {
                        minDistanceSq = distanceSq
                        closestOBB = obb
                    }
                }
            }

            return closestOBB
        }

        fun rayIntersect(obb: OBB, start: Vec3, end: Vec3): Vec3? {
            // 获取 OBB 信息
            val center: Vec3 = vector3dToVec3(obb.center)
            val extents: Vec3 = vector3dToVec3(obb.extents)
            val rotation = obb.rotation

            // 转换起点和终点到局部坐标系
            val localStart: Vector3d = toLocal(obb, start)
            val localEnd: Vector3d = toLocal(obb, end)

            // 定义 OBB 的 AABB（在局部坐标系中）
            val minX = -extents.x
            val minY = -extents.y
            val minZ = -extents.z
            val maxX = extents.x
            val maxY = extents.y
            val maxZ = extents.z

            // 使用 JOML 的相交检测
            val result = Vector2d()
            val intersects = Intersectiond.intersectRayAab(
                localStart.x, localStart.y, localStart.z,
                localEnd.x - localStart.x, localEnd.y - localStart.y, localEnd.z - localStart.z,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                result
            )

            if (intersects) {
                val t = result.x // 交点参数（可能超出 [0,1] 范围因为 intersectRayAab 检测的是无限射线）
                // 将 t 钳制到线段范围内: [0, 1]。t < 0 意味着射线起点在 OBB 内部，取 t=0
                val clampedT = Math.max(0.0, Math.min(1.0, t))
                val localHit = Vector3d(
                    localStart.x + clampedT * (localEnd.x - localStart.x),
                    localStart.y + clampedT * (localEnd.y - localStart.y),
                    localStart.z + clampedT * (localEnd.z - localStart.z)
                )

                // 转换回世界坐标系
                rotation.transform(localHit)
                return Vec3(localHit.x + center.x, localHit.y + center.y, localHit.z + center.z)
            }
            return null
        }

        // 将世界坐标点转换到 OBB 局部坐标系
        private fun toLocal(obb: OBB, worldPoint: Vec3): Vector3d {
            // 获取 OBB 信息
            val center: Vec3 = vector3dToVec3(obb.center)
            val rotation = obb.rotation
            val inverse = Quaterniond(rotation).conjugate()

            // 计算相对于中心的向量
            val relative = Vector3d(
                worldPoint.x - center.x,
                worldPoint.y - center.y,
                worldPoint.z - center.z
            )

            // 应用逆旋转（世界坐标 -> 局部坐标）
            inverse.transform(relative)
            return relative
        }

        @JvmStatic
        fun vec3ToVector3d(vec3: Vec3): Vector3d {
            return Vector3d(vec3.x, vec3.y, vec3.z)
        }

        @JvmStatic
        fun vector3dToVec3(vector3d: Vector3d): Vec3 {
            return Vec3(vector3d.x, vector3d.y, vector3d.z)
        }
    }
}
