package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Mi28Entity extends GeoVehicleEntity implements WeaponVehicleEntity {

    public Mi28Entity(EntityType<Mi28Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        mi28TerrainCompact();
    }

    public void mi28TerrainCompact() {
        if (onGround()) {
            Matrix4f transform = getVehicleTransform(1);

            // 后轮
            Vector4f position = transformPosition(transform, 0, 0.58f, -11.1f);
            Vec3 p = new Vec3(position.x, position.y, position.z);

            var level = level();
            var res = level.clip(new ClipContext(p, p.add(0, -100, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            double heightY;

            BlockPos blockPos = BlockPos.containing(p);
            BlockPos blockPosUp = BlockPos.containing(p.add(0, 1, 0));
            if (level.getBlockState(blockPosUp).canOcclude()) {
                blockPos = blockPosUp;
            }
            BlockState state = level.getBlockState(blockPos);
            VoxelShape shape = state.getCollisionShape(level, blockPos);

            if (!shape.isEmpty()) {
                heightY = p.y - (shape.max(Direction.Axis.Y) + blockPos.getY());
                if (heightY < -0.4) {
                    addDeltaMovement(blockPos.getCenter().vectorTo(p).scale(0.02));
                }
            } else if (res.getType() == HitResult.Type.BLOCK && level.noCollision(new AABB(p, p))) {
                heightY = Mth.clamp(p.y - res.getLocation().y, 0, 2);
            } else {
                heightY = 0;
            }

            setXRot((float) (getXRot() - 5f * heightY));
        }
    }

    @Override
    public double getMouseSensitivity() {
        return 0.25;
    }
}
