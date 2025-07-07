package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class M18SmokeGrenadeEntity extends FastThrowableProjectile implements GeoEntity {

    private int fuse = 80;
    private int count = 8;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public M18SmokeGrenadeEntity(EntityType<? extends M18SmokeGrenadeEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public M18SmokeGrenadeEntity(EntityType<? extends M18SmokeGrenadeEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
        this.noCulling = true;
    }

    public M18SmokeGrenadeEntity(LivingEntity entity, Level level, int fuse) {
        super(ModEntities.M18_SMOKE_GRENADE.get(), entity, level);
        this.noCulling = true;

        this.fuse = fuse;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Fuse", this.fuse);
        pCompound.putInt("Count", this.count);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Fuse")) {
            this.fuse = pCompound.getInt("Fuse");
        }
        if (pCompound.contains("Count")) {
            this.count = Mth.clamp(pCompound.getInt("Count"), 1, 64);
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.RGO_GRENADE.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        if (level().isClientSide) return;

        switch (result.getType()) {
            case BLOCK:
                BlockHitResult blockResult = (BlockHitResult) result;
                BlockPos resultPos = blockResult.getBlockPos();
                BlockState state = this.level().getBlockState(resultPos);
                if (state.getBlock() instanceof BellBlock bell) {
                    bell.attemptToRing(this.level(), resultPos, blockResult.getDirection());
                }

                releaseSmoke();
                break;
            case ENTITY:
                EntityHitResult entityResult = (EntityHitResult) result;
                Entity entity = entityResult.getEntity();
                if (this.getOwner() != null
                        && this.getOwner().getVehicle() != null
                        && entity == this.getOwner().getVehicle()
                        || entity == this.getOwner()
                ) return;

                releaseSmoke();
                break;
        }
    }

    @Override
    public void tick() {
        super.tick();
        --this.fuse;

        if (this.fuse <= 0) {
            this.discard();
            releaseSmoke();
        }
    }


    // TODO 优化烟雾效果
    public void releaseSmoke() {
        var vec3 = new Vec3(1, 1, 0);

        for (int i = 0; i < this.count; i++) {
            var decoy = new SmokeDecoyEntity(this.level());
            decoy.setPos(this.getX(), this.getY() + getBbHeight(), this.getZ());
            decoy.decoyShoot(this, vec3.yRot(i * (360f / this.count) * Mth.DEG_TO_RAD), 3, 2);
            this.level().addFreshEntity(decoy);
        }

        this.level().playSound(null, this, ModSounds.DECOY_FIRE.get(), this.getSoundSource(), 1, 1);
        this.discard();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.07F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

}
