package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import org.joml.Vector3f;

public class EntityMotionState extends MotionState {
    private VehicleEntity entity;
    private Transform transform;

    public EntityMotionState(VehicleEntity entity) {
        this.entity = entity;
        this.transform = new Transform();

        // 初始化变换
        updateTransform();
    }

    private void updateTransform() {
        // 从实体更新变换
        transform.origin.set(
                (float) entity.getX(),
                (float) entity.getY(),
                (float) entity.getZ()
        );

        // 将欧拉角转换为四元数
        Quat4f rotation = eulerToQuaternion(
                (float) Math.toRadians(entity.getXRot()),
                (float) Math.toRadians(entity.getYRot()),
                (float) Math.toRadians(entity.getRoll())
        );

        transform.setRotation(rotation);
    }

    @Override
    public Transform getWorldTransform(Transform transform) {
        transform.set(this.transform);
        return transform;
    }

    @Override
    public void setWorldTransform(Transform transform) {
        this.transform.set(transform);

        // 更新实体位置和旋转
        Vector3f position = transform.origin;
        entity.setPos(position.x, position.y, position.z);

        Quat4f rotation = transform.getRotation(new Quat4f());
        double[] euler = quaternionToEuler(rotation.x, rotation.y, rotation.z, rotation.w);

        entity.setXRot((float) Math.toDegrees(euler[0]));
        entity.setYRot((float) Math.toDegrees(euler[1]));
        entity.setZRot((float) Math.toDegrees(euler[2]));
    }

    private Quat4f eulerToQuaternion(float pitch, float yaw, float roll) {
        // 欧拉角转四元数
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);

        Quat4f q = new Quat4f();
        q.w = (float) (cr * cp * cy + sr * sp * sy);
        q.x = (float) (sr * cp * cy - cr * sp * sy);
        q.y = (float) (cr * sp * cy + sr * cp * sy);
        q.z = (float) (cr * cp * sy - sr * sp * cy);

        return q;
    }

    private double[] quaternionToEuler(float x, float y, float z, float w) {
        // 四元数转欧拉角
        double roll = Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y));
        double pitch = Math.asin(2 * (w * y - z * x));
        double yaw = Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));

        return new double[]{pitch, yaw, roll};
    }
}
