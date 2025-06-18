package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.UUID;

import static com.atsuishio.superbwarfare.item.C4Bomb.TAG_CONTROL;

public class C4Entity extends Entity implements GeoEntity, OwnableEntity {

    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(C4Entity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected static final EntityDataAccessor<String> LAST_ATTACKER_UUID = SynchedEntityData.defineId(C4Entity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(C4Entity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_CONTROLLABLE = SynchedEntityData.defineId(C4Entity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> BOMB_TICK = SynchedEntityData.defineId(C4Entity.class, EntityDataSerializers.INT);

    public static final int DEFAULT_DEFUSE_PROGRESS = 100;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected boolean inGround;
    protected boolean onEntity;
    @Nullable
    private BlockState lastState;

    public C4Entity(EntityType<C4Entity> type, Level level) {
        super(type, level);
    }

    public C4Entity(LivingEntity owner, Level level) {
        this(owner, level, false);
    }

    public C4Entity(LivingEntity owner, Level level, boolean isControllable) {
        super(ModEntities.C_4.get(), level);
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID());
        }
        this.entityData.set(IS_CONTROLLABLE, isControllable);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, Optional.empty())
                .define(LAST_ATTACKER_UUID, "undefined")
                .define(TARGET_UUID, "undefined")
                .define(IS_CONTROLLABLE, false)
                .define(BOMB_TICK, 0);
    }

    public void setOwnerUUID(@Nullable UUID pUuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.putString("Target", this.entityData.get(TARGET_UUID));
        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID));
        compound.putBoolean("IsControllable", this.entityData.get(IS_CONTROLLABLE));
        compound.putInt("BombTick", this.entityData.get(BOMB_TICK));

        if (this.lastState != null) {
            compound.put("InBlockState", NbtUtils.writeBlockState(this.lastState));
        }

        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        if (compound.contains("LastAttacker")) {
            this.entityData.set(LAST_ATTACKER_UUID, compound.getString("LastAttacker"));
        }

        if (compound.contains("Target")) {
            this.entityData.set(TARGET_UUID, compound.getString("Target"));
        }

        if (compound.contains("InBlockState", 10)) {
            this.lastState = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), compound.getCompound("InBlockState"));
        }

        if (compound.contains("IsControllable")) {
            this.entityData.set(IS_CONTROLLABLE, compound.getBoolean("IsControllable"));
        }

        if (compound.contains("BombTick")) {
            this.entityData.set(BOMB_TICK, compound.getInt("BombTick"));
        }

        UUID uuid;
        if (compound.hasUUID("Owner")) {
            uuid = compound.getUUID("Owner");
        } else {
            String s = compound.getString("Owner");

            assert this.getServer() != null;
            uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
        }

        if (uuid != null) {
            try {
                this.setOwnerUUID(uuid);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResult interact(Player player, InteractionHand hand) {
        if (this.getOwner() == player && player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                this.discard();
            }

            if (!player.getAbilities().instabuild) {
                ItemHandlerHelper.giveItemToPlayer(player, this.getItemStack());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.entityData.get(IS_CONTROLLABLE)) {
            int bombTick = this.entityData.get(BOMB_TICK);

            if (bombTick >= ExplosionConfig.C4_EXPLOSION_COUNTDOWN.get()) {
                this.explode();
            }

            int countdown = ExplosionConfig.C4_EXPLOSION_COUNTDOWN.get();
            if (countdown - bombTick > 39 && bombTick % ((20 * (countdown - bombTick)) / countdown + 1) == 0) {
                this.level().playSound(null, this.getOnPos(), ModSounds.C4_BEEP.get(), SoundSource.PLAYERS, 1, 1);
            }

            if (bombTick == countdown - 39) {
                this.level().playSound(null, this.getOnPos(), ModSounds.C4_FINAL.get(), SoundSource.PLAYERS, 2, 1);
            }
            this.entityData.set(BOMB_TICK, bombTick + 1);
        }

        Vec3 motion = this.getDeltaMovement();
        if (this.xRotO == 0.0F && this.yRotO == 0.0F && !this.inGround) {
            double d0 = motion.horizontalDistance();
            this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (double) (180F / (float) Math.PI)));
            this.setXRot((float) (Mth.atan2(motion.y, d0) * (double) (180F / (float) Math.PI)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }

        BlockPos blockpos = this.blockPosition();
        BlockState blockstate = this.level().getBlockState(blockpos);
        if (!blockstate.isAir()) {
            VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
            if (!voxelshape.isEmpty()) {
                Vec3 vec31 = this.position();

                for (AABB aabb : voxelshape.toAabbs()) {
                    if (aabb.move(blockpos).contains(vec31)) {
                        this.inGround = true;
                        break;
                    }
                }
            }
        }

        if (this.inGround) {
            if (this.lastState != blockstate && this.shouldFall()) {
                this.startFalling();
            }
        } else if (!this.onEntity) {
            Vec3 position = this.position();
            Vec3 nextPosition = position.add(motion);
            HitResult hitresult = this.level().clip(new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hitresult.getType() != HitResult.Type.MISS) {
                nextPosition = hitresult.getLocation();
            }

            while (!this.isRemoved()) {
                EntityHitResult entityhitresult = this.findHitEntity(position, nextPosition);
                if (entityhitresult != null) {
                    hitresult = entityhitresult;
                }

                if (hitresult != null && hitresult.getType() != HitResult.Type.MISS) {
                    this.onHit(hitresult);
                    this.hasImpulse = true;
                    break;
                }

                if (entityhitresult == null) {
                    break;
                }

                hitresult = null;
            }

            if (this.isRemoved()) {
                return;
            }

            motion = this.getDeltaMovement();
            double pX = motion.x;
            double pY = motion.y;
            double pZ = motion.z;

            double nX = this.getX() + pX;
            double nY = this.getY() + pY;
            double nZ = this.getZ() + pZ;

            this.updateRotation();

            float f = 0.99F;
            if (this.isInWater()) {
                for (int j = 0; j < 4; ++j) {
                    this.level().addParticle(ParticleTypes.BUBBLE, nX - pX * 0.25D, nY - pY * 0.25D, nZ - pZ * 0.25D, pX, pY, pZ);
                }

                f = this.getWaterInertia();
            }

            this.setDeltaMovement(motion.scale(f));
            if (!this.isNoGravity()) {
                Vec3 vec34 = this.getDeltaMovement();
                this.setDeltaMovement(vec34.x, vec34.y - (double) 0.05F, vec34.z);
            }

            this.setPos(nX, nY, nZ);
            this.checkInsideBlocks();
        } else {
            Entity target = EntityFindUtil.findEntity(level(), entityData.get(TARGET_UUID));
            if (target != null) {
                this.setPos(target.getX(), target.getY() + target.getBbHeight(), target.getZ());
            } else {
                this.onEntity = false;
            }
        }

        this.refreshDimensions();
    }

    private boolean shouldFall() {
        return this.inGround && this.level().noCollision((new AABB(this.position(), this.position())).inflate(0.06D));
    }

    private void startFalling() {
        this.inGround = false;
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.multiply(this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void move(MoverType pType, Vec3 pPos) {
        super.move(pType, pPos);
        if (pType != MoverType.SELF && this.shouldFall()) {
            this.startFalling();
        }
    }

    public void look(Vec3 pTarget) {
        double d0 = pTarget.x;
        double d1 = pTarget.y;
        double d2 = pTarget.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        setXRot(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 57.2957763671875))));
        setYHeadRot(getYRot());
        this.xRotO = getXRot();
        this.yRotO = getYRot();
    }

    protected void updateRotation() {
        if (getDeltaMovement().length() > 0.05 && !inGround && !onEntity) {
            Vec3 vec3 = this.getDeltaMovement();
            double d0 = vec3.horizontalDistance();
            this.setXRot(lerpRotation(this.xRotO, (float) (Mth.atan2(vec3.y, d0) * (double) (180F / (float) Math.PI))));
            this.setYRot(lerpRotation(this.yRotO, (float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI))));
        }
    }

    protected static float lerpRotation(float pCurrentRotation, float pTargetRotation) {
        while (pTargetRotation - pCurrentRotation < -180.0F) {
            pCurrentRotation -= 360.0F;
        }

        while (pTargetRotation - pCurrentRotation >= 180.0F) {
            pCurrentRotation += 360.0F;
        }

        return Mth.lerp(0.2F, pCurrentRotation, pTargetRotation);
    }

    @Nullable
    protected EntityHitResult findHitEntity(Vec3 pStartVec, Vec3 pEndVec) {
        return ProjectileUtil.getEntityHitResult(this.level(), this, pStartVec, pEndVec, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D),
                this::canHitEntity);
    }

    protected boolean canHitEntity(Entity pTarget) {
        if (!pTarget.canBeHitByProjectile()) {
            return false;
        } else {
            Entity entity = this.getOwner();
            return entity == null || (entity == pTarget && this.tickCount > 2) || !entity.isPassengerOfSameVehicle(pTarget);
        }
    }

    protected void onHit(HitResult pResult) {
        switch (pResult.getType()) {
            case BLOCK:
                this.onHitBlock((BlockHitResult) pResult);
                break;
            case ENTITY:
                this.onHitEntity((EntityHitResult) pResult);
                break;
            default:
                break;
        }
    }

    protected void onHitEntity(@NotNull EntityHitResult pResult) {
        Entity entity = pResult.getEntity();
        if (tickCount < 2 || entity == this.getVehicle() || entity instanceof C4Entity) return;
        this.entityData.set(TARGET_UUID, entity.getStringUUID());
        this.onEntity = true;
        this.setDeltaMovement(this.getDeltaMovement().multiply(0, 0, 0));
        this.setXRot(-90);
        this.xRotO = this.getXRot();
    }

    protected void onHitBlock(BlockHitResult pResult) {
        this.lastState = this.level().getBlockState(pResult.getBlockPos());
        Vec3 vec3 = pResult.getLocation().subtract(this.getX(), this.getY(), this.getZ());
        this.setDeltaMovement(vec3);
        Vec3 vec31 = vec3.normalize().scale(0.05F);
        this.setPosRaw(this.getX() - vec31.x, this.getY() - vec31.y, this.getZ() - vec31.z);

        this.look(Vec3.atLowerCornerOf(pResult.getDirection().getNormal()));
        this.setYRot((float) (pResult.getDirection().get2DDataValue() * 90));

        BlockPos resultPos = pResult.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);
        SoundEvent event = state.getBlock().getSoundType(state, this.level(), resultPos, this).getBreakSound();
        double speed = this.getDeltaMovement().length();
        if (speed > 0.1) {
            this.level().playSound(null, pResult.getLocation().x, pResult.getLocation().y, pResult.getLocation().z, event, SoundSource.AMBIENT, 1.0F, 1.0F);
        }
        this.inGround = true;
    }

    public void explode() {
        Vec3 pos = position();

        if (onEntity) {
            Entity target = EntityFindUtil.findEntity(level(), entityData.get(TARGET_UUID));
            if (target != null) {
                pos = target.position();
            }
        }

        if (this.level() instanceof ServerLevel) {
            AABB aabb = new AABB(pos, pos).inflate(2);
            BlockPos.betweenClosedStream(aabb).forEach((blockPos) -> {
                float hard = this.level().getBlockState(blockPos).getBlock().defaultDestroyTime();
                if (ExplosionConfig.EXPLOSION_DESTROY.get() && hard != -1) {
                    this.level().destroyBlock(blockPos, true);
                }
            });
        }

        CustomExplosion explosion = new CustomExplosion(level(), this,
                ModDamageTypes.causeProjectileBoomDamage(level().registryAccess(), this, this.getOwner()), ExplosionConfig.C4_EXPLOSION_DAMAGE.get(),
                pos.x, pos.y, pos.z, ExplosionConfig.C4_EXPLOSION_RADIUS.get(), ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
        explosion.explode();
        EventHooks.onExplosionStart(level(), explosion);
        ParticleTool.spawnHugeExplosionParticles(level(), position());
        explosion.finalizeExplosion(false);

        this.discard();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pPose) {
        return super.getDimensions(pPose).scale((float) 0.5);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    protected float getWaterInertia() {
        return 0.6F;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(ModItems.C4_BOMB.get());
        if (this.getEntityData().get(IS_CONTROLLABLE)) {
            final var tag = NBTTool.getTag(stack);
            tag.putBoolean(TAG_CONTROL, true);
            NBTTool.saveTag(stack, tag);
        }
        return stack;
    }

    public void defuse() {
        this.discard();
        ItemEntity entity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.getItemStack());
        if (!this.level().isClientSide) {
            this.level().addFreshEntity(entity);
        }
    }

    public int getBombTick() {
        return this.entityData.get(BOMB_TICK);
    }
}