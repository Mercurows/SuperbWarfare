package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class WireGuideMissileEntity extends MissileProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public UUID launcherVehicle;

    public WireGuideMissileEntity(EntityType<? extends WireGuideMissileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.MEDIUM_ANTI_GROUND_MISSILE.get();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            destroyBlock(blockHitResult);
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle() || entity instanceof WireGuideMissileEntity)
            return;
        if (this.level() instanceof ServerLevel) {

            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            causeExplode(result.getLocation());
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        mediumTrail();

        if (tickCount > 0 && this.getOwner() != null && getOwner().getVehicle() instanceof VehicleEntity vehicle) {
            Entity shooter = this.getOwner();
            Vec3 toVec = getDeltaMovement();
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(getLookAngle().scale(2)));

            if (launcherVehicle == vehicle.getUUID()) {
                Vec3 lookVec;
                if ((vehicle.getVehicleType() == VehicleType.AIRPLANE || vehicle.getVehicleType() == VehicleType.HELICOPTER) && shooter == vehicle.getFirstPassenger()) {
                    lookVec = vehicle.getViewVector(1).scale(1.6);
                } else {
                    lookVec = vehicle.getBarrelVector(1).scale(1.6);
                }
                Vec3 missileVec = vehicle.getShootPosForHud(shooter, 1).vectorTo(position()).normalize();
                toVec = missileVec.vectorTo(lookVec);
            }

            turn(toVec, Mth.clamp((tickCount - 1) * 0.4f, 0, 6));
        }
    }

    private PlayState movementPredicate(AnimationState<WireGuideMissileEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.jvm.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public @NotNull SoundEvent getSound() {
        return ModSounds.ROCKET_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.4f;
    }

    public void setLauncherVehicle(UUID uuid) {
        this.launcherVehicle = uuid;
    }

    @Override
    public float getMaxHealth() {
        return 20;
    }
}
