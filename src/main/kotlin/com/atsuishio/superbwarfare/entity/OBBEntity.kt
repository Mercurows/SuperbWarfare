package com.atsuishio.superbwarfare.entity

import com.atsuishio.superbwarfare.tools.OBB
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

interface OBBEntity {
    fun getOBBs(): MutableList<OBB>

    fun enableAABB(): Boolean {
        return this.getOBBs().isEmpty()
    }

    fun isInObb(pos: BlockPos, vec3: Vec3): Boolean {
        val obbList: MutableList<OBB> = this.getOBBs()
        val aabb1 = AABB(pos, pos).inflate(0.3, 0.6, 0.3)
        for (obb in obbList) {
            var obb = obb
            obb = obb.move(vec3)
            if (OBB.isColliding(obb, aabb1)) {
                return true
            }
        }
        return false
    }

    fun isInObb(entity: Entity, vec3: Vec3): Boolean {
        val obbList: MutableList<OBB> = this.getOBBs()
        for (obb in obbList) {
            var obb = obb
            obb = obb.move(vec3)
            if (entity is OBBEntity && !entity.enableAABB()) {
                val obbList2: MutableList<OBB> = entity.getOBBs()
                for (obb2 in obbList2) {
                    if (OBB.isColliding(obb, obb2)) {
                        return true
                    }
                }
            } else {
                if (OBB.isColliding(obb, entity.boundingBox)) {
                    return true
                }
            }
        }
        return false
    }
}