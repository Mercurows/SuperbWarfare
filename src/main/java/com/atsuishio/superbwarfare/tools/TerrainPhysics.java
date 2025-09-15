package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.vehicle.physics.PhysicsVehicle;
import com.bulletphysics.linearmath.Transform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class TerrainPhysics {
    private static final Map<Block, Float> FRICTION_MAP = new HashMap<>();
    private static final Map<Block, Float> RESTITUTION_MAP = new HashMap<>();

    static {
        // 配置不同方块的物理属性
//        FRICTION_MAP.put(Blocks.ICE, 0.1f);
//        FRICTION_MAP.put(Blocks.PACKED_ICE, 0.05f);
//        FRICTION_MAP.put(Blocks.SLIME_BLOCK, 0.8f);
//        FRICTION_MAP.put(Blocks.GRASS_BLOCK, 0.6f);
//        FRICTION_MAP.put(Blocks.DIRT, 0.7f);
//        FRICTION_MAP.put(Blocks.STONE, 0.9f);
//        FRICTION_MAP.put(Blocks.ASPHALT, 1.0f); // 假设有沥青方块
//
//        RESTITUTION_MAP.put(Blocks.SLIME_BLOCK, 0.8f);
//        RESTITUTION_MAP.put(Blocks.HAY_BLOCK, 0.3f);
        // 其他方块的弹性系数...
    }

    public static void updateVehicleForTerrain(PhysicsVehicle vehicle, Level level) {
        // 获取载具位置
        Transform transform = new Transform();
        vehicle.getRigidBody().getMotionState().getWorldTransform(transform);
        Vector3f position = transform.origin;

        // 检测载具下方的方块
        BlockPos blockPos = new BlockPos((int) position.x, (int) (position.y - 0.5), (int) position.z);
        BlockState blockState = level.getBlockState(blockPos);
        Block block = blockState.getBlock();

        // 更新物理属性
        float friction = FRICTION_MAP.getOrDefault(block, 1f);
        float restitution = RESTITUTION_MAP.getOrDefault(block, 1f);

        vehicle.getRigidBody().setFriction(friction);
        vehicle.getRigidBody().setRestitution(restitution);
    }
}
