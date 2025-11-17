package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.data.vehicle.subdata.OBBInfo;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public interface OBBEntity {

    default List<OBB> getOBBs() {
        return ((VehicleEntity) this).getOBB().stream().filter(Objects::nonNull).map(OBBInfo::getOBB).toList();
    }

    default void updateOBB() {
        var vehicle = (VehicleEntity) this;

        vehicle.getOBB().forEach(obbInfo -> {
            var transformStr = obbInfo.transform.toLowerCase(Locale.ROOT).trim();
            var transform = switch (transformStr) {
                case "vehicle" -> vehicle.getVehicleTransform(1);
                case "turret" -> vehicle.getTurretTransform(1);
                case "barrel" -> vehicle.getBarrelTransform(1);
                case "gun" -> vehicle.getGunTransform(1);
                case "passengerweaponstationbarrel" -> vehicle.getPassengerWeaponStationBarrelTransform(1);
                default -> null;
            };
            if (transform == null) return;

            var obb = obbInfo.getOBB();
            var worldPos = vehicle.transformPosition(transform, (float) obbInfo.position.x, (float) obbInfo.position.y, (float) obbInfo.position.z);
            obb.center().set(new Vector3f(worldPos.x, worldPos.y, worldPos.z));

            switch (transformStr) {
                case "turret" -> obb.setRotation(VectorTool.combineRotationsTurret(1, vehicle));
                case "barrel" -> obb.setRotation(VectorTool.combineRotationsBarrel(1, vehicle));
                case "vehicle" -> obb.setRotation(VectorTool.combineRotations(1, vehicle));
            }
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
