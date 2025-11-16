package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Locale;

public class AnnihilatorEntity extends ArtilleryEntity implements GeoEntity, OBBEntity {
    public static final EntityDataAccessor<Float> LASER_LEFT_LENGTH = SynchedEntityData.defineId(AnnihilatorEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_MIDDLE_LENGTH = SynchedEntityData.defineId(AnnihilatorEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_RIGHT_LENGTH = SynchedEntityData.defineId(AnnihilatorEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;

    public AnnihilatorEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.ANNIHILATOR.get(), world);
    }

    public AnnihilatorEntity(EntityType<AnnihilatorEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(6.4375f, 1.84375f, 4.125f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(5.0625f, 1.40625f, 1.5f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(5.1875f, 1.84375f, 1.96875f), new Quaternionf(), OBB.Part.BODY);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(4.125f, 1.84375f, 0.75f), new Quaternionf(), OBB.Part.BODY);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(7.75f, 0.71875f, 1.46875f), new Quaternionf(), OBB.Part.BODY);
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LASER_LEFT_LENGTH, 0f);
        this.entityData.define(LASER_MIDDLE_LENGTH, 0f);
        this.entityData.define(LASER_RIGHT_LENGTH, 0f);
    }


    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();

        if (player.getMainHandItem().getItem() == ModItems.FIRING_PARAMETERS.get() && player.isCrouching()) {
            setTarget(player.getOffhandItem());
            return InteractionResult.SUCCESS;
        }
        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get() && player.isCrouching()) {
            setTarget(player.getOffhandItem());
            return InteractionResult.SUCCESS;
        }

        if (stack.is(ModTags.Items.TOOLS_CROWBAR) && !player.isCrouching()) {
            if (this.entityData.get(CHARGE_PROGRESS) >= 1) {
                vehicleShoot(player, "Main");
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    public void setTarget(ItemStack stack) {
        int targetX = stack.getOrCreateTag().getInt("TargetX");
        int targetY = stack.getOrCreateTag().getInt("TargetY");
        int targetZ = stack.getOrCreateTag().getInt("TargetZ");
        entityData.set(TARGET_POS, new Vector3f((float) targetX, (float) targetY, (float) targetZ));
    }

    @Override
    public void baseTick() {
        super.baseTick();

        updateOBB();

        String weaponName = "Main";
        var data = getGunData(weaponName);
        if (data != null) {
            var projectileInfo = data.compute().projectile();
            var projectileType = projectileInfo.type;
            var projectileTypeStr = projectileType.trim().toLowerCase(Locale.ROOT);
            int rpm = (int) Mth.clamp(20 / ((float) Math.max(vehicleWeaponRpm(weaponName), 1) / 60), 1, 2147483647);

            if (projectileTypeStr.equals("ray") && this.entityData.get(CHARGE_PROGRESS) < 1 && getEnergy() > data.compute().ammoCostPerShoot) {
                float chargeSpeed = 1f / rpm;
                this.entityData.set(CHARGE_PROGRESS, Mth.clamp(this.entityData.get(CHARGE_PROGRESS) + chargeSpeed, 0, 1));
            }
        }



//        if (this.entityData.get(COOL_DOWN) == 20) {
//            this.level().playSound(null, this.getOnPos(), ModSounds.ANNIHILATOR_RELOAD.get(), SoundSource.PLAYERS, 1, 1);
//        }
    }

    private float laserLength(Vec3 pos, LivingEntity living, GunData data) {
        BlockHitResult result = level().clip(new ClipContext(pos, pos.add(getBarrelVector(1).scale(512)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        Vec3 hitPos = result.getLocation();
        BlockPos _pos = result.getBlockPos();

        float hardness = this.level().getBlockState(_pos).getBlock().defaultDestroyTime();

        if (ExplosionConfig.EXPLOSION_DESTROY.get() && hardness != -1) {
            Block.dropResources(this.level().getBlockState(_pos), this.level(), _pos, null);
            this.level().destroyBlock(_pos, true);
        }

        causeLaserExplode(hitPos, data, living);
        this.level().explode(living, hitPos.x, hitPos.y, hitPos.z, (float) (data.compute().explosionRadius * 0.5f), ExplosionConfig.EXPLOSION_DESTROY.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);

        return (float) pos.distanceTo(hitPos);
    }

    private float laserLengthEntity(Vec3 pos, LivingEntity living, GunData data) {
        if (this.level() instanceof ServerLevel) {
            double distance = 512 * 512;
            HitResult hitResult = TraceTool.pickNew(pos, 512, getBarrelVector(1), this);
            if (hitResult.getType() != HitResult.Type.MISS) {
                distance = hitResult.getLocation().distanceToSqr(pos);
                double blockReach = 5;
                if (distance > blockReach * blockReach) {
                    Vec3 posB = hitResult.getLocation();
                    hitResult = BlockHitResult.miss(posB, Direction.getNearest(pos.x, pos.y, pos.z), BlockPos.containing(posB));
                }
            }
            Vec3 viewVec = getBarrelVector(1);
            Vec3 toVec = pos.add(viewVec.x * 512, viewVec.y * 512, viewVec.z * 512);
            AABB aabb = this.getBoundingBox().expandTowards(viewVec.scale(512)).inflate(1);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(this, pos, toVec, aabb, p -> !p.isSpectator(), distance);
            if (entityhitresult != null) {
                Vec3 targetPos = entityhitresult.getLocation();
                double distanceToTarget = pos.distanceToSqr(targetPos);
                if (distanceToTarget > distance || distanceToTarget > 512 * 512) {
                    hitResult = BlockHitResult.miss(targetPos, Direction.getNearest(viewVec.x, viewVec.y, viewVec.z), BlockPos.containing(targetPos));
                } else if (distanceToTarget < distance) {
                    hitResult = entityhitresult;
                }
                if (hitResult.getType() == HitResult.Type.ENTITY) {
                    Entity passenger = this.getFirstPassenger();
                    Entity target = ((EntityHitResult) hitResult).getEntity();

                    DamageHandler.doDamage(target, ModDamageTypes.causeLaserDamage(this.level().registryAccess(), this, passenger), (float) data.compute().damage);
                    target.invulnerableTime = 0;
                    causeLaserExplode(targetPos, data, living);
                }
            }
        }
        return 512;
    }

    private void causeLaserExplode(Vec3 vec3, GunData gunData, Entity living) {
        float radius = (float) gunData.compute().explosionRadius;
        ParticleTool.ParticleType particleType;

        if (radius <= 4) {
            particleType = ParticleTool.ParticleType.SMALL;
        } else if (radius > 4 && radius < 10) {
            particleType = ParticleTool.ParticleType.MEDIUM;
        } else if (radius >= 10 && radius < 20) {
            particleType = ParticleTool.ParticleType.HUGE;
        } else {
            particleType = ParticleTool.ParticleType.GIANT;
        }

        createCustomExplosion()
                .damage((float) gunData.compute().explosionDamage)
                .radius(radius)
                .attacker(living)
                .position(vec3)
                .withParticleType(particleType)
                .explode();
    }

    @Override
    public void vehicleShoot(LivingEntity living, String weaponName) {
        var data = getGunData(weaponName);
        shoot(living, data);
    }

    @Override
    public void vehicleShoot(LivingEntity living) {
        var data = getGunData(living);
        shoot(living, data);
    }

    public void shoot(LivingEntity living, GunData gunData) {
        if (gunData == null) return;
        if (level() instanceof ServerLevel) {
            this.entityData.set(CHARGE_PROGRESS, 0f);
            this.consumeEnergy(VehicleConfig.ANNIHILATOR_SHOOT_COST.get());

            Matrix4f transform = getBarrelTransform(1);
            Vector4f worldPosition1 = transformPosition(transform, 2.703f, -0.045f, 15.75f);
            Vector4f worldPosition2 = transformPosition(transform, 0, -0.045f, 15.75f);
            Vector4f worldPosition3 = transformPosition(transform, -2.703f, -0.045f, 15.75f);
            Vec3 barrelLeftPos = new Vec3(worldPosition1.x, worldPosition1.y, worldPosition1.z);
            Vec3 barrelMiddlePos = new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z);
            Vec3 barrelRightPos = new Vec3(worldPosition3.x, worldPosition3.y, worldPosition3.z);


            for (int i = 0; i < 10; i++) {
                Mod.queueServerWork(i, () -> {
                    this.entityData.set(LASER_LEFT_LENGTH, Math.min(laserLength(barrelLeftPos, living, gunData), laserLengthEntity(barrelLeftPos, living, gunData)));
                    this.entityData.set(LASER_MIDDLE_LENGTH, Math.min(laserLength(barrelMiddlePos, living, gunData), laserLengthEntity(barrelMiddlePos, living, gunData)));
                    this.entityData.set(LASER_RIGHT_LENGTH, Math.min(laserLength(barrelRightPos, living, gunData), laserLengthEntity(barrelRightPos, living, gunData)));
                });
            }

            int reloadTime = (int) Mth.clamp(20 / ((float) Math.max(vehicleWeaponRpm("Main"), 1) / 60), 1, 2147483647);

            Mod.queueServerWork(reloadTime - 20, () -> {
                if (this.isAlive()) {
                    this.level().playSound(null, this.getOnPos(), gunData.compute().soundInfo.reloadEmpty, SoundSource.PLAYERS, 1, 1);
                }
            });

            gunData.shakePlayers(this);
            playShootSound3p(living, gunData, barrelMiddlePos);
        }
    }

    @Override
    public boolean canShoot(LivingEntity living) {
        var gunData = getGunData(getSeatIndex(living));
        return gunData != null && gunData.canShoot(getAmmoSupplier()) && this.canConsume(gunData.compute().ammoCostPerShoot);
    }

    private PlayState movementPredicate(AnimationState<AnnihilatorEntity> event) {
        if (this.entityData.get(CHARGE_PROGRESS) < 1) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.annihilator.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.annihilator.idle"));
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
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleFlatTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 2.28125f, 0.875f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotationsYaw(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 1.84375f, 6.5f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotationsYaw(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 0, 2.28125f, -5.21875f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotationsYaw(1, this));

        Vector4f worldPosition4 = transformPosition(transform, 0, 2.28125f, -7.9375f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotationsYaw(1, this));

        Vector4f worldPosition5 = transformPosition(transform, 0, 2.46875f, -5.28125f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotationsYaw(1, this));
    }
}
