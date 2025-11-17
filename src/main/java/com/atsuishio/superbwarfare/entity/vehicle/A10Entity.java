package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.zoomVehicle;

public class A10Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public A10Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.A_10A.get(), world);
    }

    public A10Entity(EntityType<A10Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public void baseTick() {
        super.baseTick();

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

}
