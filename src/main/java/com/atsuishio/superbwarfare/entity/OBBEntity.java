package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.data.vehicle.subdata.OBBInfo;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

public interface OBBEntity {

    default List<OBB> getOBBs() {
        return ((VehicleEntity) this).getOBB().stream().filter(Objects::nonNull).map(OBBInfo::getOBB).toList();
    }

    default void updateOBB() {
        var vehicle = (VehicleEntity) this;

        vehicle.getOBB().forEach(obbInfo -> {
            var transform = vehicle.getTransformFromString(obbInfo.transform);

            var obb = obbInfo.getOBB();
            var worldPos = vehicle.transformPosition(transform, (float) obbInfo.position.x, (float) obbInfo.position.y, (float) obbInfo.position.z);

            obb.center().set(new Vector3f(worldPos.x, worldPos.y, worldPos.z));
            obb.setRotation(vehicle.getRotationFromString(obbInfo.rotation));
        });
    }

    default boolean isInObb(BlockPos pos, Vec3 vec3) {
        var obbList = this.getOBBs();
        AABB aabb1 = new AABB(pos, pos).inflate(0.3, 0.6, 0.3);
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
            if (entity instanceof OBBEntity obbEntity2) {
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
