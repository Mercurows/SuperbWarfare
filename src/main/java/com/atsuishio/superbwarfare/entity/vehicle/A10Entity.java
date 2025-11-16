package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.zoomVehicle;

public class A10Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;
    public OBB obb8;
    public OBB obb9;
    public OBB obb10;
    public OBB obb11;

    public A10Entity(EntityType<A10Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(0.6875f, 1.09375f, 3.65625f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(8.8125f, 0.3125f, 1.40625f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(3.1875f, 0.125f, 0.96875f), new Quaternionf(), OBB.Part.BODY);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.0625f, 1.09375f, 0.84375f), new Quaternionf(), OBB.Part.BODY);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(0.0625f, 1.09375f, 0.84375f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(0.625f, 0.78125f, 1.09375f), new Quaternionf(), OBB.Part.BODY);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(0.6875f, 0.75f, 2.9375f), new Quaternionf(), OBB.Part.BODY);
        this.obb8 = new OBB(this.position().toVector3f(), new Vector3f(0.75f, 0.75f, 1.5625f), new Quaternionf(), OBB.Part.MAIN_ENGINE);
        this.obb9 = new OBB(this.position().toVector3f(), new Vector3f(0.75f, 0.75f, 1.5625f), new Quaternionf(), OBB.Part.SUB_ENGINE);
        this.obb10 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.359375f, 1.78125f), new Quaternionf(), OBB.Part.BODY);
        this.obb11 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.359375f, 1.78125f), new Quaternionf(), OBB.Part.BODY);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.updateOBB();

        if (onGround()) {
            terrainCompactA10();
        }
    }

    @Override
    public void onEngine1Damaged(Vec3 pos) {
        if (level().isClientSide) {
            float random = 2 * (this.random.nextFloat() - 0.5f);
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0, level(), 0.25f, 5);
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1);
            addRandomParticle(new CustomCloudOption(1f, 0.25f, 0, (int) (240 + 40 * random), 2.5f + 0.5f * random, -0.07f, true, true), pos, 0.5f, level(), 1.5f, 1);
        }
    }

    @Override
    public void onEngine2Damaged(Vec3 pos) {
        if (level().isClientSide) {
            float random = 2 * (this.random.nextFloat() - 0.5f);
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0, level(), 0.25f, 5);
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1);
            addRandomParticle(new CustomCloudOption(1f, 0.25f, 0, (int) (240 + 40 * random), 2.5f + 0.5f * random, -0.07f, true, true), pos, 0.5f, level(), 1.5f, 1);
        }
    }

    public void terrainCompactA10() {
        if (onGround()) {
            Matrix4f transform = this.getWheelsTransform(1);

            // 前
            Vector4f positionF = transformPosition(transform, 0.141675f, 0, 4.6315125f);
            // 左后
            Vector4f positionLB = transformPosition(transform, 2.5752f, 0, -0.7516125f);
            // 右后
            Vector4f positionRB = transformPosition(transform, -2.5752f, 0, -0.7516125f);

            Vec3 p1 = new Vec3(positionF.x, positionF.y, positionF.z);
            Vec3 p2 = new Vec3(positionLB.x, positionLB.y, positionLB.z);
            Vec3 p3 = new Vec3(positionRB.x, positionRB.y, positionRB.z);

            // 确定点位是否在墙里来调整点位高度
            float p1y = (float) this.traceBlockY(p1, 3);
            float p2y = (float) this.traceBlockY(p2, 3);
            float p3y = (float) this.traceBlockY(p3, 3);

            p1 = new Vec3(positionF.x, p1y, positionF.z);
            p2 = new Vec3(positionLB.x, p2y, positionLB.z);
            p3 = new Vec3(positionRB.x, p3y, positionRB.z);
            Vec3 p4 = p2.add(p3).scale(0.5);

            // 通过点位位置获取角度

            // 左后-右后
            Vec3 v1 = p2.vectorTo(p3);
            // 后-前
            Vec3 v2 = p4.vectorTo(p1);

            double x = VehicleVecUtils.getXRotFromVector(v2);
            double z = VehicleVecUtils.getXRotFromVector(v1);

            float diffX = Math.clamp(-5f, 5f, Mth.wrapDegrees((float) (-2 * x) - getXRot()));
            setXRot(Mth.clamp(getXRot() + 0.05f * diffX, -45f, 45f));

            float diffZ = Math.clamp(-5f, 5f, Mth.wrapDegrees((float) (-2 * z) - getRoll()));
            setZRot(Mth.clamp(getRoll() + 0.05f * diffZ, -45f, 45f));
        } else if (isInWater()) {
            setXRot(getXRot() * 0.9f);
            setZRot(getRoll() * 0.9f);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getMouseSensitivity() {
        return zoomVehicle ? 0.03 : 0.07;
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7, this.obb8, this.obb9, this.obb10, this.obb11);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 2.65625f, 1.71875f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 2.35f, -0.46875f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 0, 2.4375f, -6.71875f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -3.125f, 3.65625f, -6.71875f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, 3.125f, 3.65625f, -6.71875f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition6 = transformPosition(transform, 0f, 2.34375f, 6.46875f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition7 = transformPosition(transform, 0f, 2.5625f, -4.875f);
        this.obb7.center().set(new Vector3f(worldPosition7.x, worldPosition7.y, worldPosition7.z));
        this.obb7.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition8 = transformPosition(transform, -1.625f, 3.375f, -3.5f);
        this.obb8.center().set(new Vector3f(worldPosition8.x, worldPosition8.y, worldPosition8.z));
        this.obb8.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition9 = transformPosition(transform, 1.625f, 3.375f, -3.5f);
        this.obb9.center().set(new Vector3f(worldPosition9.x, worldPosition9.y, worldPosition9.z));
        this.obb9.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition10 = transformPosition(transform, -2.703125f, 1.921875f, 0.03125f);
        this.obb10.center().set(new Vector3f(worldPosition10.x, worldPosition10.y, worldPosition10.z));
        this.obb10.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition11 = transformPosition(transform, 2.703125f, 1.921875f, 0.03125f);
        this.obb11.center().set(new Vector3f(worldPosition11.x, worldPosition11.y, worldPosition11.z));
        this.obb11.setRotation(VectorTool.combineRotations(1, this));
    }
}
