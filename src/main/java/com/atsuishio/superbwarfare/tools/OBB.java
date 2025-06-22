package com.atsuishio.superbwarfare.tools;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Intersectionf;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Codes based on @AnECanSaiTin's <a href="https://github.com/AnECanSaiTin/HitboxAPI">HitboxAPI</a>
 *
 * @param center   旋转中心
 * @param extents  三个轴向上的半长
 * @param rotation 旋转
 * @param part     部件
 */
public record OBB(Vector3f center, Vector3f extents, Quaternionf rotation, Part part) {

    public void setCenter(Vector3f center) {
        this.center.set(center);
    }

    public void setExtents(Vector3f extents) {
        this.extents.set(extents);
    }

    public void setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
    }

    /**
     * 获取OBB的8个顶点坐标
     *
     * @return 顶点坐标
     */
    public Vector3f[] getVertices() {
        Vector3f[] vertices = new Vector3f[8];

        Vector3f[] localVertices = new Vector3f[]{
                new Vector3f(-extents.x, -extents.y, -extents.z),
                new Vector3f(extents.x, -extents.y, -extents.z),
                new Vector3f(extents.x, extents.y, -extents.z),
                new Vector3f(-extents.x, extents.y, -extents.z),
                new Vector3f(-extents.x, -extents.y, extents.z),
                new Vector3f(extents.x, -extents.y, extents.z),
                new Vector3f(extents.x, extents.y, extents.z),
                new Vector3f(-extents.x, extents.y, extents.z)
        };

        for (int i = 0; i < 8; i++) {
            Vector3f vertex = localVertices[i];
            vertex.rotate(rotation);
            vertex.add(center);
            vertices[i] = vertex;
        }

        return vertices;
    }

    /**
     * 获取OBB的三个正交轴
     *
     * @return 正交轴
     */
    public Vector3f[] getAxes() {
        Vector3f[] axes = new Vector3f[]{
                new Vector3f(1, 0, 0),
                new Vector3f(0, 1, 0),
                new Vector3f(0, 0, 1)};
        rotation.transform(axes[0]);
        rotation.transform(axes[1]);
        rotation.transform(axes[2]);
        return axes;
    }

    /**
     * 判断两个OBB是否相撞
     */
    public static boolean isColliding(OBB obb, OBB other) {
        Vector3f[] axes1 = obb.getAxes();
        Vector3f[] axes2 = other.getAxes();
        return Intersectionf.testObOb(obb.center(), axes1[0], axes1[1], axes1[2], obb.extents(),
                other.center(), axes2[0], axes2[1], axes2[2], other.extents());
    }

    /**
     * 判断OBB和AABB是否相撞
     */
    public static boolean isColliding(OBB obb, AABB aabb) {
        Vector3f obbCenter = obb.center();
        Vector3f[] obbAxes = obb.getAxes();
        Vector3f obbHalfExtents = obb.extents();
        Vector3f aabbCenter = aabb.getCenter().toVector3f();
        Vector3f aabbHalfExtents = new Vector3f((float) (aabb.getXsize() / 2f), (float) (aabb.getYsize() / 2f), (float) (aabb.getZsize() / 2f));
        return Intersectionf.testObOb(
                obbCenter.x, obbCenter.y, obbCenter.z,
                obbAxes[0].x, obbAxes[0].y, obbAxes[0].z,
                obbAxes[1].x, obbAxes[1].y, obbAxes[1].z,
                obbAxes[2].x, obbAxes[2].y, obbAxes[2].z,
                obbHalfExtents.x, obbHalfExtents.y, obbHalfExtents.z,
                aabbCenter.x, aabbCenter.y, aabbCenter.z,
                1, 0, 0,
                0, 1, 0,
                0, 0, 1,
                aabbHalfExtents.x, aabbHalfExtents.y, aabbHalfExtents.z
        );
    }

    /**
     * 计算OBB上离待判定点最近的点
     *
     * @param point 待判定点
     * @param obb   OBB盒
     * @return 在OBB上离待判定点最近的点
     */
    public static Vector3f getClosestPointOBB(Vector3f point, OBB obb) {
        Vector3f nearP = new Vector3f(obb.center());
        Vector3f dist = point.sub(nearP, new Vector3f());

        float[] extents = new float[]{obb.extents().x, obb.extents().y, obb.extents().z};
        Vector3f[] axes = obb.getAxes();

        for (int i = 0; i < 3; i++) {
            float distance = dist.dot(axes[i]);
            distance = Math.clamp(distance, -extents[i], extents[i]);

            nearP.x += distance * axes[i].x;
            nearP.y += distance * axes[i].y;
            nearP.z += distance * axes[i].z;
        }

        return nearP;
    }

    public Optional<Vector3f> clip(Vector3f pFrom, Vector3f pTo) {
        // 计算OBB的局部坐标系基向量（世界坐标系中的方向）
        Vector3f[] axes = new Vector3f[3];
        axes[0] = rotation.transform(new Vector3f(1, 0, 0));
        axes[1] = rotation.transform(new Vector3f(0, 1, 0));
        axes[2] = rotation.transform(new Vector3f(0, 0, 1));

        // 将点转换到OBB局部坐标系
        Vector3f localFrom = worldToLocal(pFrom, axes);
        Vector3f localTo = worldToLocal(pTo, axes);

        // 射线方向（局部坐标系）
        Vector3f dir = new Vector3f(localTo).sub(localFrom);

        // Slab算法参数
        double tEnter = 0.0;      // 进入时间
        double tExit = 1.0;       // 离开时间

        // 在三个轴上执行Slab算法
        for (int i = 0; i < 3; i++) {
            double min = -extents.get(i);
            double max = extents.get(i);
            double origin = localFrom.get(i);
            double direction = dir.get(i);

            // 处理射线平行于轴的情况
            if (Math.abs(direction) < 1e-7f) {
                if (origin < min || origin > max) {
                    return Optional.empty();
                }
                continue;
            }

            // 计算与两个平面的交点参数
            double t1 = (min - origin) / direction;
            double t2 = (max - origin) / direction;

            // 确保tNear是近平面，tFar是远平面
            double tNear = Math.min(t1, t2);
            double tFar = Math.max(t1, t2);

            // 更新进入/离开时间
            if (tNear > tEnter) tEnter = tNear;
            if (tFar < tExit) tExit = tFar;

            // 检查是否提前退出（无交点）
            if (tEnter > tExit) {
                return Optional.empty();
            }
        }

        // 检查是否有有效交点
        // 计算局部坐标系中的交点
        Vector3f localHit = new Vector3f(dir).mul((float) tEnter).add(localFrom);
        // 转换回世界坐标系
        return Optional.of(localToWorld(localHit, axes));

    }

    // 世界坐标转局部坐标
    private Vector3f worldToLocal(Vector3f worldPoint, Vector3f[] axes) {
        Vector3f rel = new Vector3f(worldPoint).sub(center);
        return new Vector3f(
                rel.dot(axes[0]),
                rel.dot(axes[1]),
                rel.dot(axes[2])
        );
    }

    // 局部坐标转世界坐标
    private Vector3f localToWorld(Vector3f localPoint, Vector3f[] axes) {
        Vector3f result = new Vector3f(center);
        result.add(axes[0].mul(localPoint.x, new Vector3f()));
        result.add(axes[1].mul(localPoint.y, new Vector3f()));
        result.add(axes[2].mul(localPoint.z, new Vector3f()));
        return result;
    }

    public OBB inflate(float amount) {
        Vector3f newExtents = new Vector3f(extents).add(amount, amount, amount);
        return new OBB(center, newExtents, rotation, part);
    }

    public OBB inflate(float x, float y, float z) {
        Vector3f newExtents = new Vector3f(extents).add(x, y, z);
        return new OBB(center, newExtents, rotation, part);
    }

    public OBB move(Vec3 vec3) {
        Vector3f newCenter = new Vector3f((float) (center.x + vec3.x), (float) (center.y + vec3.y), (float) (center.z + vec3.z));
        return new OBB(newCenter, extents, rotation, part);
    }

    /**
     * 检查点是否在OBB内部
     *
     * @return 如果点在OBB内部则返回true，否则返回false
     */
    public boolean contains(Vec3 vec3) {
        // 计算点到OBB中心的向量
        Vector3f rel = new Vector3f(vec3.toVector3f()).sub(center);

        Vector3f[] axes = new Vector3f[3];
        axes[0] = rotation.transform(new Vector3f(1, 0, 0));
        axes[1] = rotation.transform(new Vector3f(0, 1, 0));
        axes[2] = rotation.transform(new Vector3f(0, 0, 1));

        // 将相对向量投影到OBB的三个轴上
        float projX = Math.abs(rel.dot(axes[0]));
        float projY = Math.abs(rel.dot(axes[1]));
        float projZ = Math.abs(rel.dot(axes[2]));

        // 检查投影值是否小于对应轴上的半长
        return projX <= extents.x &&
                projY <= extents.y &&
                projZ <= extents.z;
    }

    // 获取最近面的全局法向量
    public Vector3f getClosestFaceNormal(Vec3 vec3) {
        // 转换玩家位置到Vector3f
        Vector3f pos = new Vector3f((float) vec3.x, (float) vec3.y, (float) vec3.z);

        // 1. 转换到局部坐标系
        Vector3f localPos = new Vector3f(pos).sub(center); // 减去中心
        Quaternionf invRotation = new Quaternionf(rotation).invert(); // 旋转的逆
        invRotation.transform(localPos); // 应用逆旋转

        // 2. 计算到六个面的距离
        float[] distances = new float[6];
        distances[0] = Math.abs(localPos.x - extents.x); // +X 面
        distances[1] = Math.abs(localPos.x + extents.x); // -X 面
        distances[2] = Math.abs(localPos.y - extents.y); // +Y 面
        distances[3] = Math.abs(localPos.y + extents.y); // -Y 面
        distances[4] = Math.abs(localPos.z - extents.z); // +Z 面
        distances[5] = Math.abs(localPos.z + extents.z); // -Z 面

        // 3. 找到最近面的索引
        int minIndex = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] < distances[minIndex]) {
                minIndex = i;
            }
        }

        // 4. 获取局部法向量并转换到全局坐标系
        Vector3f localNormal = getLocalNormalByIndex(minIndex);
        Vector3f globalNormal = new Vector3f(localNormal);
        rotation.transform(globalNormal);
        globalNormal.normalize(); // 确保单位长度

        return globalNormal;
    }

    // 根据索引返回局部法向量
    private Vector3f getLocalNormalByIndex(int index) {
        return switch (index) {
            case 0 -> new Vector3f(1, 0, 0);  // +X
            case 1 -> new Vector3f(-1, 0, 0); // -X
            case 2 -> new Vector3f(0, 1, 0);  // +Y
            case 3 -> new Vector3f(0, -1, 0); // -Y
            case 4 -> new Vector3f(0, 0, 1);  // +Z
            case 5 -> new Vector3f(0, 0, -1); // -Z
            default -> throw new IllegalArgumentException("Invalid face index");
        };
    }

    // 计算OBB的包围球（中心点相同，半径为对角线长度）
    public float getBoundingSphereRadius() {
        return extents.length();
    }

    // 获取面的信息（全局中心点和法向量）
    public FaceInfo getFaceInfo(int faceIndex) {
        // 局部坐标系的面法向量
        Vector3f localNormal = getLocalNormalByIndex(faceIndex);

        // 局部中心点：从OBB中心指向该面中心
        Vector3f localCenter = new Vector3f(localNormal).mul(extents);

        // 转换到全局坐标系
        Vector3f globalCenter = new Vector3f(localCenter);
        rotation.transform(globalCenter);
        globalCenter.add(center);

        Vector3f globalNormal = new Vector3f(localNormal);
        rotation.transform(globalNormal);
        globalNormal.normalize(); // 确保单位向量

        return new FaceInfo(globalCenter, globalNormal);
    }

    // 计算玩家位置到指定面的距离
    public float distanceToFace(int faceIndex, Vector3f playerPos) {
        FaceInfo faceInfo = getFaceInfo(faceIndex);
        Vector3f diff = new Vector3f(playerPos).sub(faceInfo.center());
        return Math.abs(diff.dot(faceInfo.normal()));
    }

    // 存储面信息
    public record FaceInfo(Vector3f center, Vector3f normal) {
    }

    // 计算点到OBB的距离（平方）
    public float distanceSquaredToPoint(Vector3f point) {
        Vector3f localPoint = new Vector3f(point).sub(center);
        Quaternionf invRotation = new Quaternionf(rotation).invert();
        invRotation.transform(localPoint);

        Vector3f closestPoint = new Vector3f();

        closestPoint.x = Math.max(-extents.x, Math.min(localPoint.x, extents.x));
        closestPoint.y = Math.max(-extents.y, Math.min(localPoint.y, extents.y));
        closestPoint.z = Math.max(-extents.z, Math.min(localPoint.z, extents.z));

        return localPoint.distanceSquared(closestPoint);
    }

    /**
     * 寻找距离某一个位置最近的OBB
     */
    @Nullable
    public static OBB findClosestOBB(List<OBB> obbList, Vec3 vec3) {
        if (obbList == null || obbList.isEmpty()) {
            return null;
        }

        Vector3f pos = vec3.toVector3f();
        OBB closestOBB = null;
        float minDistanceSq = Float.MAX_VALUE;

        for (OBB obb : obbList) {
            float distToCenterSq = pos.distanceSquared(obb.center());
            float boundingRadiusSq = obb.getBoundingSphereRadius() * obb.getBoundingSphereRadius();

            if (distToCenterSq - boundingRadiusSq > minDistanceSq) {
                continue;
            }

            float distSq = obb.distanceSquaredToPoint(pos);

            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                closestOBB = obb;
            }
        }
        return closestOBB;
    }

    // 查找最近的面
    @Nullable
    public static ClosestFaceResult findClosestFace(List<OBB> obbList, Vec3 playerPos) {
        if (obbList == null || obbList.isEmpty()) {
            return null;
        }

        Vector3f pos = playerPos.toVector3f();
        OBB closestOBB = null;
        int closestFaceIndex = -1;
        float minDistance = Float.MAX_VALUE;
        Vector3f closestFaceNormal = null;
        Vector3f closestFaceCenter = null;

        // 第一阶段：使用包围球快速筛选候选OBB
        for (OBB obb : obbList) {
            // 计算玩家到OBB中心的距离
            float distToCenter = pos.distance(obb.center());

            // 如果距离大于包围球半径，不可能比当前最小值更近
            if (distToCenter > obb.getBoundingSphereRadius()) {
                continue;
            }

            // 第二阶段：检查该OBB的所有面
            for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
                float dist = obb.distanceToFace(faceIndex, pos);

                // 更新最小距离
                if (dist < minDistance) {
                    minDistance = dist;
                    closestOBB = obb;
                    closestFaceIndex = faceIndex;
                    OBB.FaceInfo faceInfo = obb.getFaceInfo(faceIndex);
                    closestFaceNormal = faceInfo.normal();
                    closestFaceCenter = faceInfo.center();
                }
            }
        }

        if (closestOBB == null) {
            return null;
        }

        return new ClosestFaceResult(closestOBB, closestFaceIndex, minDistance,
                closestFaceCenter, closestFaceNormal);
    }

    // 存储最近面结果
    public record ClosestFaceResult(OBB obb, int faceIndex, float distance, Vector3f faceCenter, Vector3f faceNormal) {
    }

    public enum Part {
        EMPTY,
        WHEEL_LEFT,
        WHEEL_RIGHT,
        TURRET,
        ENGINE1,
        ENGINE2,
        BODY
    }
}
