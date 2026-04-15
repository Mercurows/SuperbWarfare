package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.misc.FiringParametersItemKt;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
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
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix4d;
import org.joml.Vector4d;
import software.bernie.geckolib.animation.*;

import java.util.Locale;
import java.util.UUID;

public class AnnihilatorEntity extends ArtilleryEntity {
    public static final EntityDataAccessor<Float> LASER_LEFT_LENGTH = SynchedEntityData.defineId(AnnihilatorEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_MIDDLE_LENGTH = SynchedEntityData.defineId(AnnihilatorEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_RIGHT_LENGTH = SynchedEntityData.defineId(AnnihilatorEntity.class, EntityDataSerializers.FLOAT);

    public AnnihilatorEntity(EntityType<AnnihilatorEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LASER_LEFT_LENGTH, 0f)
                .define(LASER_MIDDLE_LENGTH, 0f)
                .define(LASER_RIGHT_LENGTH, 0f);
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
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
            if (getChargeProgress() >= 1) {
                vehicleShoot(player, "Main");
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    public void setTarget(ItemStack stack) {
        if (this.isWreck()) return;
        var parameters = FiringParametersItemKt.getFiringParameters(stack);
        var pos = parameters.pos();
        setTargetPos(pos);
    }

    @Override
    public @NotNull DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 3) * damage);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.isWreck()) return;
        String weaponName = "Main";
        var data = getGunData(weaponName);
        if (data != null) {
            var projectileInfo = data.get(GunProp.PROJECTILE);
            var projectileType = projectileInfo.getId();
            var projectileTypeStr = projectileType.trim().toLowerCase(Locale.ROOT);
            int rpm = (int) Math.ceil(20f / ((float) vehicleWeaponRpm(weaponName) / 60));

            if (projectileTypeStr.equals("ray") && getChargeProgress() < 1 && getEnergy() > data.get(GunProp.AMMO_COST_PER_SHOOT)) {
                float chargeSpeed = 1f / rpm;
                setChargeProgress(Mth.clamp(getChargeProgress() + chargeSpeed, 0, 1));
            }
        }
    }

    private float laserLength(Vec3 pos, LivingEntity living, GunData data) {
        BlockHitResult result = level().clip(new ClipContext(pos, pos.add(getBarrelVector(1).scale(512)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        Vec3 hitPos = result.getLocation();
        BlockPos blockPos = result.getBlockPos();

        float hardness = this.level().getBlockState(blockPos).getBlock().defaultDestroyTime();

        if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get() && hardness != -1) {
            Block.dropResources(this.level().getBlockState(blockPos), this.level(), blockPos, null);
            this.level().destroyBlock(blockPos, true);
        }

        causeLaserExplode(hitPos, data, living);
        this.level().explode(living, hitPos.x, hitPos.y, hitPos.z, (float) (data.get(GunProp.EXPLOSION_RADIUS) * 0.5f), ExplosionConfig.EXPLOSION_DESTROY.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);

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

                    DamageHandler.doDamage(target, ModDamageTypes.causeLaserDamage(this.level().registryAccess(), this, passenger), data.get(GunProp.DAMAGE).floatValue());
                    target.invulnerableTime = 0;
                    causeLaserExplode(targetPos, data, living);
                    return (float) pos.distanceTo(hitResult.getLocation());
                }
            }
        }
        return 512;
    }

    private void causeLaserExplode(Vec3 vec3, GunData gunData, Entity living) {
        float radius = gunData.get(GunProp.EXPLOSION_RADIUS).floatValue();
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
                .damage(gunData.get(GunProp.EXPLOSION_DAMAGE).floatValue())
                .radius(radius)
                .attacker(living)
                .position(vec3)
                .withParticleType(particleType)
                .explode();
    }

    @Override
    public void vehicleShoot(LivingEntity living, @NotNull String weaponName) {
        if (this.isWreck()) return;
        var data = getGunData(weaponName);
        shoot(living, data);
    }

    @Override
    public void vehicleShoot(LivingEntity living, UUID uuid, Vec3 targetPos) {
        if (this.isWreck()) return;
        var data = getGunData(living);
        shoot(living, data);
    }

    public void shoot(LivingEntity living, GunData gunData) {
        if (gunData == null) return;
        if (level() instanceof ServerLevel) {
            setChargeProgress(0f);
            this.consumeEnergy(gunData.get(GunProp.AMMO_COST_PER_SHOOT));

            Matrix4d transform = getBarrelTransform(1);
            Vector4d worldPosition1 = transformPosition(transform, 2.703, -0.045, 15.75);
            Vector4d worldPosition2 = transformPosition(transform, 0, -0.045, 15.75);
            Vector4d worldPosition3 = transformPosition(transform, -2.703, -0.045, 15.75);
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
                    this.level().playSound(null, this.getOnPos(), gunData.get(GunProp.SOUND_INFO).vehicleReload, SoundSource.PLAYERS, 1, 1);
                }
            });

            gunData.shakePlayers(this);
            playShootSound3p(living, gunData, barrelMiddlePos);
        }
    }

    @Override
    public boolean canShoot(LivingEntity living) {
        var gunData = getGunData(getSeatIndex(living));
        return gunData != null && gunData.canShoot(getAmmoSupplier()) && this.canConsume(gunData.get(GunProp.AMMO_COST_PER_SHOOT)) && !isWreck();
    }

    private PlayState movementPredicate(AnimationState<AnnihilatorEntity> event) {
        if (getChargeProgress() < 1) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.annihilator.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.annihilator.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }
}
