package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PlayMessages;
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
        terrainCompactA10();
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
            Matrix4f transform = this.getVehicleTransform(1);
            // 前轮
            Vector4f vector4f = transformPosition(transform, -0.243f, -0.02f, 4.63f);
            Vec3 p = new Vec3(vector4f.x, vector4f.y, vector4f.z);
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
                heightY = Mth.clamp(p.y - res.getLocation().y, 0, 3);
            } else {
                heightY = 0;
            }

            setXRot((float) (getXRot() + 5f * heightY));
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
