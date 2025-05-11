package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.client.AnimationTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Code based on @BobMowzie's MowziesMobs, @EEEAB's EEEABsMobs and @Mercurows's DreamaticVoyage
 */
public abstract class AbstractLaserEntity extends Entity implements TraceableEntity {

    public LivingEntity caster;
    public float yaw, pitch;
    public float preYaw, prePitch;
    public double endPosX, endPosY, endPosZ;
    public double collidePosX, collidePosY, collidePosZ;
    public double prevCollidePosX, prevCollidePosY, prevCollidePosZ;
    public Direction blockSide = null;
    public boolean on = true;
    public AnimationTicker ticker = new AnimationTicker(3);

    private static final EntityDataAccessor<Integer> DATA_CASTER_ID = SynchedEntityData.defineId(AbstractLaserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(AbstractLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(AbstractLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(AbstractLaserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COUNT_DOWN = SynchedEntityData.defineId(AbstractLaserEntity.class, EntityDataSerializers.INT);

    public AbstractLaserEntity(EntityType<?> type, Level level, int countDown) {
        super(type, level);
        this.setCountDown(countDown);
        this.noCulling = true;
    }

    @Override
    public void tick() {
        super.tick();
        this.prevCollidePosX = this.collidePosX;
        this.prevCollidePosY = this.collidePosY;
        this.prevCollidePosZ = this.collidePosZ;
        this.preYaw = this.yaw;
        this.prePitch = this.pitch;
        this.yaw = this.getYaw();
        this.pitch = this.getPitch();
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        if (this.tickCount == 1 && this.level().isClientSide) {
            this.caster = (LivingEntity) this.level().getEntity(getCasterId());
        }

        this.beamTick();

        if ((!this.on && this.ticker.isStopped()) || (this.caster != null && !this.caster.isAlive())) {
            this.discard();
        }
        this.ticker.changeTimer(this.on && this.isAccumulating());

        if (this.tickCount - this.getCountDown() > this.getDuration()) {
            this.on = false;
        }
    }
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
    }

    protected void beamTick() {
    }

    @Nullable
    public LivingEntity getOwner() {
        return caster;
    }

    @Override
    public boolean isPickable() {
        return super.isPickable();
    }

    @Override
    public void push(@NotNull Entity entityIn) {
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder
                .define(DATA_CASTER_ID, -1)
                .define(DATA_YAW, 0F)
                .define(DATA_PITCH, 0F)
                .define(DATA_DURATION, 0)
                .define(DATA_COUNT_DOWN, 0);
    }

    public void setCasterId(int id) {
        this.entityData.set(DATA_CASTER_ID, id);
    }

    public int getCasterId() {
        return this.entityData.get(DATA_CASTER_ID);
    }

    public boolean isAccumulating() {
        return this.tickCount > this.getCountDown();
    }

    public float getYaw() {
        return getEntityData().get(DATA_YAW);
    }

    public void setYaw(float rotAngle) {
        getEntityData().set(DATA_YAW, rotAngle);
    }

    public float getPitch() {
        return getEntityData().get(DATA_PITCH);
    }

    public void setPitch(float rotAngle) {
        getEntityData().set(DATA_PITCH, rotAngle);
    }

    public int getDuration() {
        return getEntityData().get(DATA_DURATION);
    }

    public void setDuration(int duration) {
        getEntityData().set(DATA_DURATION, duration);
    }

    public int getCountDown() {
        return getEntityData().get(DATA_COUNT_DOWN);
    }

    public void setCountDown(int countDown) {
        getEntityData().set(DATA_COUNT_DOWN, countDown);
    }

    protected void calculateEndPos(double radius) {
        if (level().isClientSide()) {
            endPosX = getX() + radius * Math.cos(yaw) * Math.cos(pitch);
            endPosZ = getZ() + radius * Math.sin(yaw) * Math.cos(pitch);
            endPosY = getY() + radius * Math.sin(pitch);
        } else {
            endPosX = getX() + radius * Math.cos(getYaw()) * Math.cos(getPitch());
            endPosZ = getZ() + radius * Math.sin(getYaw()) * Math.cos(getPitch());
            endPosY = getY() + radius * Math.sin(getPitch());
        }
    }

    public CustomHitResult raytraceEntities(Level world, Vec3 from, Vec3 to) {
        CustomHitResult result = new CustomHitResult();
        result.setBlockHit(world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)));
        if (result.getBlockHit() != null) {
            Vec3 hitVec = result.getBlockHit().getLocation();
            collidePosX = hitVec.x;
            collidePosY = hitVec.y;
            collidePosZ = hitVec.z;
            blockSide = result.getBlockHit().getDirection();
        } else {
            collidePosX = endPosX;
            collidePosY = endPosY;
            collidePosZ = endPosZ;
            blockSide = null;
        }
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, new AABB(Math.min(getX(), collidePosX), Math.min(getY(), collidePosY), Math.min(getZ(), collidePosZ), Math.max(getX(), collidePosX), Math.max(getY(), collidePosY), Math.max(getZ(), collidePosZ)).inflate(1, 1, 1));
        for (LivingEntity entity : entities) {
            if (entity == this.caster) {
                continue;
            }
            float pad = entity.getPickRadius() + getBaseScale();
            AABB aabb = entity.getBoundingBox().inflate(pad, pad, pad);
            Optional<Vec3> hit = aabb.clip(from, to);
            if (aabb.contains(from)) {
                result.addEntityHit(entity);
            } else if (hit.isPresent()) {
                result.addEntityHit(entity);
            }
        }
        return result;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    protected void onHit(HitResult hitResult) {
        HitResult.Type hitresult$type = hitResult.getType();
        if (hitresult$type == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) hitResult);
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, hitResult.getLocation(), GameEvent.Context.of(this, null));
        } else if (hitresult$type == HitResult.Type.BLOCK) {
            BlockHitResult blockhitresult = (BlockHitResult) hitResult;
            this.onHitBlock(blockhitresult);
            BlockPos blockpos = blockhitresult.getBlockPos();
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, blockpos, GameEvent.Context.of(this, this.level().getBlockState(blockpos)));
        }
    }

    protected void onHitEntity(EntityHitResult result) {
    }

    protected void onHitBlock(BlockHitResult result) {
    }

    protected float getBaseScale() {
        return 0.5F;
    }

    public static class CustomHitResult {

        private BlockHitResult blockHit;
        private final List<LivingEntity> entities = new ArrayList<>();

        public BlockHitResult getBlockHit() {
            return blockHit;
        }

        public List<LivingEntity> getEntities() {
            return entities;
        }

        public void setBlockHit(HitResult rayTraceResult) {
            if (rayTraceResult.getType() == HitResult.Type.BLOCK)
                this.blockHit = (BlockHitResult) rayTraceResult;
        }

        public void addEntityHit(LivingEntity entity) {
            entities.add(entity);
        }
    }

}
