package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public interface OBBEntity {

    List<OBB> getOBBs();

    default boolean enableAABB() {
        return this.getOBBs().isEmpty();
    }

    default boolean isInObb(BlockPos pos, Vec3 vec3) {
        var obbList = this.getOBBs();
        var vec = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        AABB aabb1 = new AABB(vec, vec).inflate(0.3, 0.6, 0.3);
        for (var obb : obbList) {
            obb = obb.move(vec3);
            if (OBB.isColliding(obb, aabb1)) {
                return true;
            }
        }
        return false;
    }

    default boolean isInObb(Entity entity, Vec3 vec3) {
        var obbList = this.getOBBs();
        for (var obb : obbList) {
            obb = obb.move(vec3);
            if (entity instanceof OBBEntity obbEntity2 && !obbEntity2.enableAABB()) {
                var obbList2 = obbEntity2.getOBBs();
                for (var obb2 : obbList2) {
                    if (OBB.isColliding(obb, obb2)) {
                        return true;
                    }
                }
            } else {
                if (OBB.isColliding(obb, entity.getBoundingBox())) {
                    return true;
                }
            }
        }
        return false;
    }
}
