package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class IglaMissileEntity extends MissileProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public IglaMissileEntity(EntityType<? extends IglaMissileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public IglaMissileEntity(Entity entity, Level level, float damage, float explosionDamage, float explosionRadius) {
        super(ModEntities.IGLA_MISSILE.get(), entity, level);
        this.noCulling = true;
        this.damage = damage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
        this.durability = 0;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.MEDIUM_ANTI_AIR_MISSILE.get();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
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
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            destroyBlock(blockHitResult);
        }
    }

    @Override
    public void tick() {
        super.tick();

        mediumTrail();

        Entity entity = EntityFindUtil.findEntity(this.level(), entityData.get(TARGET_UUID));
        List<Entity> decoy = SeekTool.seekLivingEntities(this, 32, 90);

        for (var e : decoy) {
            if (e.getType().is(ModTags.EntityTypes.DECOY) && !this.distracted) {
                this.entityData.set(TARGET_UUID, e.getStringUUID());
                this.distracted = true;
                break;
            }
        }

        if (entity != null && !entityData.get(TARGET_UUID).equals("none")) {
            if ((!entity.getPassengers().isEmpty() || entity instanceof VehicleEntity) && entity.tickCount % ((int) Math.max(0.04 * this.distanceTo(entity), 2)) == 0) {
                entity.level().playSound(null, entity.getOnPos(), entity instanceof Pig ? SoundEvents.PIG_HURT : ModSounds.MISSILE_WARNING.get(), SoundSource.PLAYERS, 2, 1f);
            }


            Vec3 targetPos = new Vec3(entity.getX(), entity.getY() + 0.5f * entity.getBbHeight() + (entity instanceof EnderDragon ? -3 : 0), entity.getZ());
            Vec3 toVec = RangeTool.calculateFiringSolution(position(), targetPos, entity.getDeltaMovement(), getDeltaMovement().length(), 0);

            if (this.tickCount > 1) {

                lostTarget = VectorTool.calculateAngle(getDeltaMovement(), toVec) > 120 && !lostTarget;

                if (getOwner() instanceof Player player && player.getMainHandItem().is(ModItems.IGLA_9K38.get()) && !lost) {
                    var handItem = player.getMainHandItem();
                    var data = GunData.from(handItem);
                    lost = !data.zooming.get() || !VectorTool.checkNoClip(player.getEyePosition(), targetPos, this.level());
                }

                if (!lostTarget && !lost) {
                    turn(toVec, Mth.clamp((tickCount - 1) * 0.5f, 0, 15));
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.05).add(getLookAngle().scale(8)));
                }

                if (lostTarget) {
                    this.entityData.set(TARGET_UUID, "none");
                }
            }
        }

        if (lost) {
            setDeltaMovement(getDeltaMovement().add(0, 0.03, 0));
            this.entityData.set(TARGET_UUID, "none");
        }
    }

    private PlayState movementPredicate(AnimationState<IglaMissileEntity> event) {
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
}
