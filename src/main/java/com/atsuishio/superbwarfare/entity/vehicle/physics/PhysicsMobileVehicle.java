package com.atsuishio.superbwarfare.entity.vehicle.physics;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.PhysicsManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class PhysicsMobileVehicle extends VehicleEntity implements OBBEntity {
    private PhysicsVehicle physicsVehicle;
    private OBB obb; // OBB碰撞箱

    public PhysicsMobileVehicle(EntityType<?> type, Level level) {
        super(type, level);

        // 设置OBB大小（根据实际载具模型调整）
        this.obb = this.getOBBs().get(1);

        if (!level.isClientSide()) {
            // 在服务器端创建物理车辆
            PhysicsManager.getInstance().addVehicle(this, obb);
            physicsVehicle = PhysicsManager.getInstance().getVehicle(this.getUUID());
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // 服务器端处理物理更新
            if (physicsVehicle != null) {
                // 物理更新由PhysicsManager统一处理
                // 这里可以添加特定的载具逻辑
            }
        } else {
            // 客户端处理
            // 可以添加视觉特效等
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            PhysicsManager.getInstance().removeVehicle(this.getUUID());
        }
        super.remove(reason);
    }

    public void applyEngineForce(float force) {
        if (physicsVehicle != null) {
            physicsVehicle.applyEngineForce(force);
        }
    }

    public void applySteering(float angle) {
        if (physicsVehicle != null) {
            physicsVehicle.applySteering(angle);
        }
    }

    @Override
    protected void defineSynchedData() {
        // 定义同步数据
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 读取保存数据
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 添加保存数据
    }
}
