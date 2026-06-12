package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter
import com.atsuishio.superbwarfare.tools.OBB
import com.atsuishio.superbwarfare.tools.OBB.Companion.vec3ToVector3d
import com.atsuishio.superbwarfare.tools.OBB.Companion.vector3dToVec3
import com.atsuishio.superbwarfare.world.phys.EntityResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

/**
 * 高级命中检测接口 — 标记一个投射物具有不同于原版的命中判定逻辑，
 * 包括 OBB 碰撞检测、爆头/打腿判定、穿甲伤害计算、多目标穿透等。
 *
 * 实现此接口的实体需要提供这些高级命中判定方法
 */
interface IAdvancedHitDetection {
    /**
     * 在路径上查找所有可命中实体
     */
    fun findEntitiesOnPath(startVec: Vec3, endVec: Vec3): MutableList<EntityResult>

    /**
     * 对单个实体执行 OBB + AABB 混合碰撞检测，返回命中结果（含爆头/打腿信息）
     */
    fun getHitResult(entity: Entity, startVec: Vec3, endVec: Vec3): EntityResult?

    /**
     * 执行带穿甲/爆头倍率的伤害
     */
    fun performDamage(entity: Entity, damage: Float, isHeadshot: Boolean)

    /**
     * 执行命中后的伤害施加与击退
     */
    fun performOnHit(entity: Entity, damage: Float, headshot: Boolean, knockback: Double)

    /**
     * 记录靶环分数（用于训练场计分）
     */
    fun recordHitScore(direction: net.minecraft.core.Direction, hitVec: Vec3)

    companion object {
        /**
         * 静态 OBB raycast —— 返回世界空间的命中位置，若射线不与任何非COLLISION OBB相交则返回null。
         * 供 FastThrowableProjectile 子类绕过原版命中检测时使用。
         */
        @JvmStatic
        fun clipObb(projectile: Entity, entity: Entity, startVec: Vec3, endVec: Vec3): Vec3? {
            if (entity is OBBEntity && !entity.enableAABB()) {
                var collisionHit: Vec3? = null
                for (obb in entity.getOBBs()) {
                    if (obb.part == OBB.Part.COLLISION) {
                        // Save COLLISION OBB for fallback, but check BODY OBBs first
                        val obbVec = obb.clip(vec3ToVector3d(startVec), vec3ToVector3d(endVec)).orElse(null)
                        if (obbVec != null) {
                            collisionHit = vector3dToVec3(obbVec)
                        }
                        continue
                    }
                    val obbVec = obb.clip(vec3ToVector3d(startVec), vec3ToVector3d(endVec)).orElse(null)
                    if (obbVec != null) {
                        val hitPos = vector3dToVec3(obbVec)
                        OBBHitter.getInstance(projectile).`sbw$setCurrentHitPart`(obb.part)
                        return hitPos
                    }
                }
                // Fallback: use COLLISION OBB if no BODY OBB was hit (covers gaps on large vehicles)
                return collisionHit
            }
            return null
        }
    }
}
