package com.atsuishio.superbwarfare.entity.vehicle.physics;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.EntityMotionState;
import com.atsuishio.superbwarfare.tools.OBB;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class PhysicsVehicle {
    private VehicleEntity entity;
    private RigidBody rigidBody;
    private CollisionShape collisionShape;
    private MotionState motionState;

    public PhysicsVehicle(VehicleEntity entity, OBB obb) {
        this.entity = entity;

        // OBB碰撞形状
        Vector3f halfExtents = new Vector3f(
                (float)(obb.extents().x),
                (float)(obb.extents().y),
                (float)(obb.extents().z)
        );

        collisionShape = new BoxShape(halfExtents);

        // 创建运动状态
        motionState = new EntityMotionState(entity);

        // 计算质量属性
        float mass = 1000f; // 载具质量
        Vector3f localInertia = new Vector3f(0, 0, 0);
        collisionShape.calculateLocalInertia(mass, localInertia);

        // 创建刚体
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
                mass, motionState, collisionShape, localInertia
        );

        rigidBody = new RigidBody(rbInfo);
        rigidBody.setActivationState(CollisionObject.DISABLE_DEACTIVATION);

        // 设置载具的物理属性
        rigidBody.setFriction(0.7f);
        rigidBody.setRollingFriction(0.3f);
    }

    public void syncWithEntity() {
        // 从物理引擎同步位置和旋转到实体
        Transform transform = new Transform();
        rigidBody.getMotionState().getWorldTransform(transform);

        Vector3f position = transform.origin;
        Quat4f rotation = transform.getRotation(new Quat4f());

        // 转换为Minecraft的坐标和旋转系统
        entity.setPos(position.x, position.y, position.z);

        // 将四元数转换为欧拉角
        double[] euler = quaternionToEuler(rotation.x, rotation.y, rotation.z, rotation.w);
        entity.setXRot((float) Math.toDegrees(euler[0]));
        entity.setYRot((float) Math.toDegrees(euler[1]));
        entity.setZRot((float) Math.toDegrees(euler[2]));
    }

    public void applyEngineForce(float force) {
        // 应用引擎力
        Vector3f direction = getForwardVector();
        direction.scale(force);
        rigidBody.applyCentralForce(direction);
    }

    public void applySteering(float angle) {
        // 应用转向力
        Vector3f torque = new Vector3f(0, angle, 0);
        rigidBody.applyTorque(torque);
    }

    public Vector3f getForwardVector() {
        // 获取载具的前向向量
        Transform transform = new Transform();
        rigidBody.getMotionState().getWorldTransform(transform);

        Quat4f rotation = transform.getRotation(new Quat4f());
        Vector3f forward = new Vector3f(0, 0, 1);
        Matrix4f matrix = new Matrix4f();
        matrix.set(rotation);
        matrix.transform(forward);

        return forward;
    }

    private double[] quaternionToEuler(float x, float y, float z, float w) {
        // 四元数转欧拉角
        double roll = Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y));
        double pitch = Math.asin(2 * (w * y - z * x));
        double yaw = Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));

        return new double[]{pitch, yaw, roll};
    }

    public RigidBody getRigidBody() {
        return rigidBody;
    }
}
