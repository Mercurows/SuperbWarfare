package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;

import java.util.Optional;

/**
 * Codes based on @AnECanSaiTin's <a href="https://github.com/AnECanSaiTin/HitboxAPI">HitboxAPI</a>
 *
 * @param center   旋转中心
 * @param extents  三个轴向上的半长
 * @param rotation 旋转
 * @param part     部件
 */
public record OBB(Vector3d center, Vector3d extents, Quaterniond rotation, Part part) {

    public void setCenter(Vector3d center) {
        this.center.set(center);
    }

    public void setExtents(Vector3d extents) {
        this.extents.set(extents);
    }

    public void setRotation(Quaterniond rotation) {
        this.rotation.set(rotation);
    }

    /**
     * 计算与向量相交的某个面
     *
     * @author YWZJ Ranpoes
     */
    public int getEmbeddingFace(Vec3 vec3) {
        Vector3d rel = vec3ToVector3d(vec3).sub(center);

        Vector3d[] axes = new Vector3d[3];
        axes[0] = rotation.transform(new Vector3d(1, 0, 0));
        axes[1] = rotation.transform(new Vector3d(0, 1, 0));
        axes[2] = rotation.transform(new Vector3d(0, 0, 1));

        double projX = Math.abs(rel.dot(axes[0]));
        double projY = Math.abs(rel.dot(axes[1]));
        double projZ = Math.abs(rel.dot(axes[2]));

        double min = Double.MAX_VALUE;
        int index = 0;

        double dx = extents.x - projX;
        double dy = extents.y - projY;
        double dz = extents.z - projZ;

        if (dx < min) {
            min = dx;
            index = 1;
        }
        if (dy < min) {
            min = dy;
            index = 2;
        }
        if (dz < min) {
            index = 3;
        }

        return index * (rel.dot(axes[index - 1]) < 0 ? -1 : 1);
    }

    public double getEmbeddingDepth(Vec3 vec3) {
        Vector3d rel = vec3ToVector3d(vec3).sub(center);

        Vector3d[] axes = new Vector3d[3];
        axes[0] = rotation.transform(new Vector3d(1, 0, 0));
        axes[1] = rotation.transform(new Vector3d(0, 1, 0));
        axes[2] = rotation.transform(new Vector3d(0, 0, 1));

        double projX = Math.abs(rel.dot(axes[0]));
        double projY = Math.abs(rel.dot(axes[1]));
        double projZ = Math.abs(rel.dot(axes[2]));

        double dx = extents.x - projX;
        double dy = extents.y - projY;
        double dz = extents.z - projZ;

        double minDepth = Double.MAX_VALUE;

        if (Math.abs(dx) < Math.abs(minDepth)) {
            minDepth = dx;
        }
        if (Math.abs(dy) < Math.abs(minDepth)) {
            minDepth = dy;
        }
        if (Math.abs(dz) < Math.abs(minDepth)) {
            minDepth = dz;
        }

        return minDepth;
    }

    /**
     * 获取OBB的8个顶点坐标
     *
     * @return 顶点坐标
     */
    public Vector3d[] getVertices() {
        Vector3d[] vertices = new Vector3d[8];

        Vector3d[] localVertices = new Vector3d[]{
                new Vector3d(-extents.x, -extents.y, -extents.z),
                new Vector3d(extents.x, -extents.y, -extents.z),
                new Vector3d(extents.x, extents.y, -extents.z),
                new Vector3d(-extents.x, extents.y, -extents.z),
                new Vector3d(-extents.x, -extents.y, extents.z),
                new Vector3d(extents.x, -extents.y, extents.z),
                new Vector3d(extents.x, extents.y, extents.z),
                new Vector3d(-extents.x, extents.y, extents.z)
        };

        for (int i = 0; i < 8; i++) {
            Vector3d vertex = localVertices[i];
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
    public Vector3d[] getAxes() {
        Vector3d[] axes = new Vector3d[]{
                new Vector3d(1, 0, 0),
                new Vector3d(0, 1, 0),
                new Vector3d(0, 0, 1)};
        rotation.transform(axes[0]);
        rotation.transform(axes[1]);
        rotation.transform(axes[2]);
        return axes;
    }

    /**
     * 判断两个OBB是否相撞
     */
    public static boolean isColliding(OBB obb, OBB other) {
        Vector3d[] axes1 = obb.getAxes();
        Vector3d[] axes2 = other.getAxes();
        return Intersectiond.testObOb(obb.center(), axes1[0], axes1[1], axes1[2], obb.extents(),
                other.center(), axes2[0], axes2[1], axes2[2], other.extents());
    }

    /**
     * 判断OBB和AABB是否相撞
     */
    public static boolean isColliding(OBB obb, AABB aabb) {
        Vector3d obbCenter = obb.center();
        Vector3d[] obbAxes = obb.getAxes();
        Vector3d obbHalfExtents = obb.extents();
        Vector3d aabbCenter = vec3ToVector3d(aabb.getCenter());
        Vector3d aabbHalfExtents = new Vector3d(aabb.getXsize() / 2, aabb.getYsize() / 2f, aabb.getZsize() / 2f);
        return Intersectiond.testObOb(
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
    public static Vector3d getClosestPointOBB(Vector3d point, OBB obb) {
        Vector3d nearP = new Vector3d(obb.center());
        Vector3d dist = point.sub(nearP, new Vector3d());

        double[] extents = new double[]{obb.extents().x, obb.extents().y, obb.extents().z};
        Vector3d[] axes = obb.getAxes();

        for (int i = 0; i < 3; i++) {
            double distance = dist.dot(axes[i]);
            distance = Math.clamp(distance, -extents[i], extents[i]);

            nearP.x += distance * axes[i].x;
            nearP.y += distance * axes[i].y;
            nearP.z += distance * axes[i].z;
        }

        return nearP;
    }

    public Optional<Vector3d> clip(Vector3d pFrom, Vector3d pTo) {
        // 计算OBB的局部坐标系基向量（世界坐标系中的方向）
        Vector3d[] axes = new Vector3d[3];
        axes[0] = rotation.transform(new Vector3d(1, 0, 0));
        axes[1] = rotation.transform(new Vector3d(0, 1, 0));
        axes[2] = rotation.transform(new Vector3d(0, 0, 1));

        // 将点转换到OBB局部坐标系
        Vector3d localFrom = worldToLocal(pFrom, axes);
        Vector3d localTo = worldToLocal(pTo, axes);

        // 射线方向（局部坐标系）
        Vector3d dir = new Vector3d(localTo).sub(localFrom);

        // Slab算法参数
        double tEnter = 0;      // 进入时间
        double tExit = 1;       // 离开时间

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
        Vector3d localHit = new Vector3d(dir).mul(tEnter).add(localFrom);
        // 转换回世界坐标系
        return Optional.of(localToWorld(localHit, axes));
    }

    // 世界坐标转局部坐标
    private Vector3d worldToLocal(Vector3d worldPoint, Vector3d[] axes) {
        Vector3d rel = new Vector3d(worldPoint).sub(center);
        return new Vector3d(
                rel.dot(axes[0]),
                rel.dot(axes[1]),
                rel.dot(axes[2])
        );
    }

    // 局部坐标转世界坐标
    private Vector3d localToWorld(Vector3d localPoint, Vector3d[] axes) {
        Vector3d result = new Vector3d(center);
        result.add(axes[0].mul(localPoint.x, new Vector3d()));
        result.add(axes[1].mul(localPoint.y, new Vector3d()));
        result.add(axes[2].mul(localPoint.z, new Vector3d()));
        return result;
    }

    public OBB inflate(double amount) {
        Vector3d newExtents = new Vector3d(extents).add(amount, amount, amount);
        return new OBB(center, newExtents, rotation, part);
    }

    public OBB inflate(double x, double y, double z) {
        Vector3d newExtents = new Vector3d(extents).add(x, y, z);
        return new OBB(center, newExtents, rotation, part);
    }

    public OBB move(Vec3 vec3) {
        Vector3d newCenter = new Vector3d(center.x + vec3.x, center.y + vec3.y, center.z + vec3.z);
        return new OBB(newCenter, extents, rotation, part);
    }

    /**
     * 检查点是否在OBB内部
     *
     * @return 如果点在OBB内部则返回true，否则返回false
     */
    public boolean contains(Vec3 vec3) {
        // 计算点到OBB中心的向量
        Vector3d rel = vec3ToVector3d(vec3).sub(center);

        Vector3d[] axes = new Vector3d[3];
        axes[0] = rotation.transform(new Vector3d(1, 0, 0));
        axes[1] = rotation.transform(new Vector3d(0, 1, 0));
        axes[2] = rotation.transform(new Vector3d(0, 0, 1));

        // 将相对向量投影到OBB的三个轴上
        double projX = Math.abs(rel.dot(axes[0]));
        double projY = Math.abs(rel.dot(axes[1]));
        double projZ = Math.abs(rel.dot(axes[2]));

        // 检查投影值是否小于对应轴上的半长
        return projX <= extents.x &&
                projY <= extents.y &&
                projZ <= extents.z;
    }

    /**
     * 获取玩家看向的某个OBB
     */
    @Nullable
    public static OBB getLookingObb(Player player, double range) {
        Entity lookingEntity = TraceTool.findLookingEntity(player, range);
        if (!(lookingEntity instanceof OBBEntity obbEntity) || obbEntity.enableAABB()) {
            return null;
        }

        // 获取玩家视线信息
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 viewVec = player.getViewVector(1.0f);
        Vec3 lookEnd = eyePos.add(viewVec.scale(range));

        OBB closestOBB = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (OBB obb : obbEntity.getOBBs()) {
            // 使用精确的射线相交检测
            Vec3 hitPos = rayIntersect(obb, eyePos, lookEnd);

            if (hitPos != null) {
                // 计算交点到眼睛的平方距离
                double distanceSq = eyePos.distanceToSqr(hitPos);

                if (distanceSq < minDistanceSq) {
                    minDistanceSq = distanceSq;
                    closestOBB = obb;
                }
            }
        }

        return closestOBB;
    }

    @Nullable
    public static Vec3 rayIntersect(OBB obb, Vec3 start, Vec3 end) {
        // 获取 OBB 信息
        Vec3 center = vector3dToVec3(obb.center());
        Vec3 extents = vector3dToVec3(obb.extents());
        Quaterniond rotation = obb.rotation();

        // 计算逆旋转
        Quaterniond inverse = new Quaterniond(rotation).conjugate();

        // 转换起点和终点到局部坐标系
        Vector3d localStart = toLocal(obb, start);
        Vector3d localEnd = toLocal(obb, end);

        // 定义 OBB 的 AABB（在局部坐标系中）
        double minX = -extents.x, minY = -extents.y, minZ = -extents.z;
        double maxX = extents.x, maxY = extents.y, maxZ = extents.z;

        // 使用 JOML 的相交检测
        Vector2d result = new Vector2d();
        boolean intersects = Intersectiond.intersectRayAab(
                localStart.x, localStart.y, localStart.z,
                localEnd.x - localStart.x, localEnd.y - localStart.y, localEnd.z - localStart.z,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                result
        );

        if (intersects) {
            double t = result.x; // 交点参数
            Vector3d localHit = new Vector3d(
                    localStart.x + t * (localEnd.x - localStart.x),
                    localStart.y + t * (localEnd.y - localStart.y),
                    localStart.z + t * (localEnd.z - localStart.z)
            );

            // 转换回世界坐标系
            rotation.transform(localHit);
            return new Vec3(localHit.x + center.x, localHit.y + center.y, localHit.z + center.z);
        }
        return null;
    }

    // 将世界坐标点转换到 OBB 局部坐标系
    private static Vector3d toLocal(OBB obb, Vec3 worldPoint) {
        // 获取 OBB 信息
        Vec3 center = vector3dToVec3(obb.center());
        Quaterniond rotation = obb.rotation();
        Quaterniond inverse = new Quaterniond(rotation).conjugate();

        // 计算相对于中心的向量
        Vector3d relative = new Vector3d(
                worldPoint.x - center.x,
                worldPoint.y - center.y,
                worldPoint.z - center.z
        );

        // 应用逆旋转（世界坐标 -> 局部坐标）
        inverse.transform(relative);
        return relative;
    }

    public enum Part {
        @SerializedName("Empty")
        EMPTY,
        @SerializedName("WheelLeft")
        WHEEL_LEFT,
        @SerializedName("WheelRight")
        WHEEL_RIGHT,
        @SerializedName("Turret")
        TURRET,
        @SerializedName("MainEngine")
        MAIN_ENGINE,
        @SerializedName("SubEngine")
        SUB_ENGINE,
        @SerializedName("Body")
        BODY,
        @SerializedName("Interactive")
        INTERACTIVE
    }
    
    public static Vector3d vec3ToVector3d (Vec3 vec3) {
        return new Vector3d(vec3.x, vec3.y, vec3.z);
    }

    public static Vec3 vector3dToVec3 (Vector3d vector3d) {
        return new Vec3(vector3d.x, vector3d.y, vector3d.z);
    }
}
