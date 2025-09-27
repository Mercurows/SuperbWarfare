package com.atsuishio.superbwarfare.entity.vehicle.quatern;

import org.joml.Vector3f;

public class QuaternionHelper {
    public static final QuaternionHelper IDENTITY = new QuaternionHelper(0, 0, 0, 1);

    private final float x;
    private final float y;
    private final float z;
    private final float w;

    public QuaternionHelper(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    // 从欧拉角创建四元数 (弧度制)
    public static QuaternionHelper fromEulerAngles(float pitch, float yaw, float roll) {
        float cy = (float) Math.cos(yaw * 0.5);
        float sy = (float) Math.sin(yaw * 0.5);
        float cp = (float) Math.cos(pitch * 0.5);
        float sp = (float) Math.sin(pitch * 0.5);
        float cr = (float) Math.cos(roll * 0.5);
        float sr = (float) Math.sin(roll * 0.5);

        float w = cr * cp * cy + sr * sp * sy;
        float x = sr * cp * cy - cr * sp * sy;
        float y = cr * sp * cy + sr * cp * sy;
        float z = cr * cp * sy - sr * sp * cy;

        return new QuaternionHelper(x, y, z, w);
    }

    // 四元数乘法
    public QuaternionHelper multiply(QuaternionHelper other) {
        float newW = w * other.w - x * other.x - y * other.y - z * other.z;
        float newX = w * other.x + x * other.w + y * other.z - z * other.y;
        float newY = w * other.y - x * other.z + y * other.w + z * other.x;
        float newZ = w * other.z + x * other.y - y * other.x + z * other.w;

        return new QuaternionHelper(newX, newY, newZ, newW);
    }

    // 标准化四元数
    public QuaternionHelper normalize() {
        float len = (float) Math.sqrt(x * x + y * y + z * z + w * w);
        if (len > 0.0001f) {
            return new QuaternionHelper(x / len, y / len, z / len, w / len);
        }
        return IDENTITY;
    }

    // 转换为欧拉角 (用于兼容Minecraft渲染)
    public Vector3f toEulerAngles() {
        // 俯仰角 (pitch - X轴)
        float sinp = 2 * (w * x - y * z);
        float pitch;
        if (Math.abs(sinp) >= 1) {
            pitch = (float) (Math.copySign(Math.PI / 2, sinp));
        } else {
            pitch = (float) Math.asin(sinp);
        }

        // 偏航角 (yaw - Y轴)
        float siny_cosp = 2 * (w * y + z * x);
        float cosy_cosp = 1 - 2 * (x * x + y * y);
        float yaw = (float) Math.atan2(siny_cosp, cosy_cosp);

        // 翻滚角 (roll - Z轴)
        float sinn_cosp = 2 * (w * z + x * y);
        float cosn_cosp = 1 - 2 * (y * y + z * z);
        float roll = (float) Math.atan2(sinn_cosp, cosn_cosp);

        return new Vector3f(pitch, yaw, roll);
    }

    // 球面线性插值
    public QuaternionHelper slerp(QuaternionHelper target, float alpha) {
        float dot = w * target.w + x * target.x + y * target.y + z * target.z;

        if (dot < 0) {
            target = new QuaternionHelper(-target.x, -target.y, -target.z, -target.w);
            dot = -dot;
        }

        if (dot > 0.9995f) {
            // 线性插值
            float invAlpha = 1.0f - alpha;
            return new QuaternionHelper(
                    invAlpha * x + alpha * target.x,
                    invAlpha * y + alpha * target.y,
                    invAlpha * z + alpha * target.z,
                    invAlpha * w + alpha * target.w
            ).normalize();
        }

        float theta0 = (float) Math.acos(dot);
        float theta = theta0 * alpha;
        float sinTheta = (float) Math.sin(theta);
        float sinTheta0 = (float) Math.sin(theta0);

        float scale0 = (float) Math.cos(theta) - dot * sinTheta / sinTheta0;
        float scale1 = sinTheta / sinTheta0;

        return new QuaternionHelper(
                scale0 * x + scale1 * target.x,
                scale0 * y + scale1 * target.y,
                scale0 * z + scale1 * target.z,
                scale0 * w + scale1 * target.w
        ).normalize();
    }

    /**
     * 从轴角创建四元数
     */
    public static QuaternionHelper fromAxisAngle(Vector3f axis, float angle) {
        float sinHalfAngle = (float) Math.sin(angle * 0.5f);
        float cosHalfAngle = (float) Math.cos(angle * 0.5f);
        return new QuaternionHelper(
                axis.x() * sinHalfAngle,
                axis.y() * sinHalfAngle,
                axis.z() * sinHalfAngle,
                cosHalfAngle
        );
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getW() { return w; }

    /**
     * 修正的欧拉角转四元数方法，确保正确的轴对应
     * 使用ZYX顺序（Roll -> Pitch -> Yaw），这是航空和载具常用的顺序
     */
    public static QuaternionHelper fromEulerAnglesZYX(float roll, float pitch, float yaw) {
        // 半角
        float cy = (float) Math.cos(yaw * 0.5);
        float sy = (float) Math.sin(yaw * 0.5);
        float cp = (float) Math.cos(pitch * 0.5);
        float sp = (float) Math.sin(pitch * 0.5);
        float cr = (float) Math.cos(roll * 0.5);
        float sr = (float) Math.sin(roll * 0.5);

        // ZYX顺序：先Roll(Z)，再Pitch(Y)，最后Yaw(X) - 但注意Minecraft的坐标系
        // 在Minecraft中，我们需要调整轴的对应关系
        float w = cr * cp * cy + sr * sp * sy;
        float x = sr * cp * cy - cr * sp * sy;
        float y = cr * sp * cy + sr * cp * sy;
        float z = cr * cp * sy - sr * sp * cy;

        return new QuaternionHelper(x, y, z, w);
    }

    /**
     * 专门为载具设计的旋转创建方法
     * 参数顺序：Pitch(X), Yaw(Y), Roll(Z)
     */
    public static QuaternionHelper fromVehicleEulerAngles(float pitch, float yaw, float roll) {
        // 由于Minecraft的坐标系，我们需要调整轴的映射
        // 在载具中，我们希望：
        // - Pitch 绕X轴（前后倾斜）
        // - Yaw 绕Y轴（左右转向）
        // - Roll 绕Z轴（左右滚转）

        return fromEulerAngles(pitch, yaw, roll);
    }

    /**
     * 创建绕X轴旋转的四元数（俯仰）
     */
    public static QuaternionHelper fromPitch(float angle) {
        float halfAngle = angle * 0.5f;
        return new QuaternionHelper((float) Math.sin(halfAngle), 0, 0, (float) Math.cos(halfAngle));
    }

    /**
     * 创建绕Y轴旋转的四元数（偏航）
     */
    public static QuaternionHelper fromYaw(float angle) {
        float halfAngle = angle * 0.5f;
        return new QuaternionHelper(0, (float) Math.sin(halfAngle), 0, (float) Math.cos(halfAngle));
    }

    /**
     * 创建绕Z轴旋转的四元数（滚转）
     */
    public static QuaternionHelper fromRoll(float angle) {
        float halfAngle = angle * 0.5f;
        return new QuaternionHelper(0, 0, (float) Math.sin(halfAngle), (float) Math.cos(halfAngle));
    }
}