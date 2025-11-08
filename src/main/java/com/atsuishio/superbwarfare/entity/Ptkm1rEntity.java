package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.PtkmProjectileEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.UUID;

public class Ptkm1rEntity extends Entity implements GeoEntity, OwnableEntity {

    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(Ptkm1rEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected static final EntityDataAccessor<String> LAST_ATTACKER_UUID = SynchedEntityData.defineId(Ptkm1rEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(Ptkm1rEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(Ptkm1rEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Ptkm1rEntity(EntityType<Ptkm1rEntity> type, Level world) {
        super(type, world);
    }

    public int aimingTime;

    public Ptkm1rEntity(LivingEntity owner, Level level) {
        super(ModEntities.PTKM_1R.get(), level);
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID());
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(LAST_ATTACKER_UUID, "undefined");
        this.entityData.define(TARGET_UUID, "undefined");
        this.entityData.define(HEALTH, 40f);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    private static final DamageModifier DAMAGE_MODIFIER = DamageModifier.createDefaultModifier()
            .multiply(0.02f, ModDamageTypes.CUSTOM_EXPLOSION)
            .multiply(0.02f, ModDamageTypes.MINE)
            .multiply(0.02f, ModDamageTypes.PROJECTILE_EXPLOSION)
            .multiply(0.02f, DamageTypes.EXPLOSION);

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        amount = DAMAGE_MODIFIER.compute(source, amount);
        if (source.getEntity() != null) {
            this.entityData.set(LAST_ATTACKER_UUID, source.getEntity().getStringUUID());
        }
        this.entityData.set(HEALTH, this.entityData.get(HEALTH) - amount);
        return super.hurt(source, amount);
    }

    public void setOwnerUUID(@Nullable UUID pUuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public boolean isOwnedBy(LivingEntity pEntity) {
        return pEntity == this.getOwner();
    }

    @Override
    protected float getEyeHeight(@NotNull Pose pPose, @NotNull EntityDimensions pSize) {
        return 0.2F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Health", this.entityData.get(HEALTH));
        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID));
        compound.putString("Target", this.entityData.get(TARGET_UUID));
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("Health")) {
            this.entityData.set(HEALTH, compound.getFloat("Health"));
        }

        if (compound.contains("LastAttacker")) {
            this.entityData.set(LAST_ATTACKER_UUID, compound.getString("LastAttacker"));
        }
        if (compound.contains("Target")) {
            this.entityData.set(TARGET_UUID, compound.getString("Target"));
        }

        UUID uuid;
        if (compound.hasUUID("Owner")) {
            uuid = compound.getUUID("Owner");
        } else {
            String s = compound.getString("Owner");

            try {
                if (this.getServer() == null) {
                    uuid = UUID.fromString(s);
                } else {
                    uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
                }
            } catch (Exception exception) {
                Mod.LOGGER.error("Couldn't load owner UUID of {}: {}", this, exception);
                uuid = null;
            }
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
        if (this.isOwnedBy(player) && player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                this.discard();
            }

            if (!player.getAbilities().instabuild) {
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.PTKM_1R.get()));
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void tick() {
        super.tick();

        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));

        if (!this.level().noCollision(this.getBoundingBox())) {
            this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        float f = 0.98F;
        if (this.onGround()) {
            BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
            f = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98F;
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply(f, 0.98, f));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, -0.9, 1.0));
        }

        if (this.entityData.get(HEALTH) <= 0) {
            triggerExplode();
        }

        if (tickCount == 1) {
            level().playSound(null, BlockPos.containing(position()), ModSounds.PTKM_1R_DEPLOY.get(), SoundSource.PLAYERS, 1, 1);
        }

        if (tickCount > 20 && onGround()) {
            findTarget();
        }

        this.refreshDimensions();
    }

    public void findTarget() {
        int range = 60;
        Entity target = null;

        var list = new SeekTool.Builder(this)
                .withinRange(range)
                .build();

        for (var entity : list) {
            var condition =
                    entity.onGround()
                    && this.getOwner() != entity
                    && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()))
                    && (this.getOwner() != null && !SeekTool.IS_FRIENDLY.test(this.getOwner(), entity) && entity != this.getOwner().getVehicle())
                    && !entity.isShiftKeyDown()
                    && ((entity.getBoundingBox().getSize() > 1.5 || entity instanceof VehicleEntity || entity instanceof SenpaiEntity) && entity.getDeltaMovement().lengthSqr() > 0.009);
            if (!condition) continue;

            target = entity;
            break;
        }

        if (target != null) {
            float targetXRot;
            double distance = target.distanceTo(this);


            if (distance < range) {
                targetXRot = -60;
                this.look(target.position());
                if (distance < range - 10) {
                    aimingTime++;

                } else if (aimingTime > 0) {
                    aimingTime--;
                }
            } else {
                this.setXRot(0);
                targetXRot = 0;
            }

            float diffX = Math.clamp(-60f, 60f, Mth.wrapDegrees(targetXRot - this.getXRot()));
            this.setXRot(getXRot() + 0.25f * diffX);

            if (aimingTime > 10) {
                shoot(target, distance);
            }

        } else if (aimingTime > 0) {
            aimingTime--;
        }
    }

    private void shoot(Entity entity, double distance) {
        if (this.level() instanceof ServerLevel serverLevel) {
            PtkmProjectileEntity ptkmProjectile = new PtkmProjectileEntity(this.getOwner(), serverLevel);
            ptkmProjectile.setDamage(500);
            ptkmProjectile.setExplosionDamage(80);
            ptkmProjectile.setExplosionRadius(7);
            ptkmProjectile.setTarget(entity);
            ptkmProjectile.setShootTime((int) (0.5f * distance));
            ptkmProjectile.setPos(position().x, getEyePosition().y, position().z);
            ptkmProjectile.shoot(getLookAngle().x, getLookAngle().y, getLookAngle().z, 4f, 0.4f);
            serverLevel.addFreshEntity(ptkmProjectile);

            int count = 6;

            for (float i = 1f; i < 8; i += .5f) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        position().x + i * getLookAngle().x,
                        getEyePosition().y + i * getLookAngle().y,
                        position().z + i * getLookAngle().z,
                        Mth.clamp(count--, 1, 3), 0.15, 0.15, 0.15, 0.0025);
            }

            ParticleTool.spawnSmallExplosionParticles(serverLevel, position());
            this.discard();
        }
    }

    public void look(Vec3 pTarget) {
        Vec3 vec3 = EntityAnchorArgument.Anchor.EYES.apply(this);
        double d0 = (pTarget.x - vec3.x) * 0.2;
        double d2 = (pTarget.z - vec3.z) * 0.2;
        float diffY = Mth.wrapDegrees(Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90F) - this.getYRot());
        this.setYRot(getYRot() + 0.5f * diffY);
    }

    private void triggerExplode() {
        new CustomExplosion.Builder(this)
                .damage(100)
                .radius(6)
                .attacker(this.getOwner())
                .withParticleType(ParticleTool.ParticleType.HUGE)
                .explode();

        this.discard();
    }

    private PlayState movementPredicate(AnimationState<Ptkm1rEntity> event) {
        if (onGround()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ptkm.deploy"));
        } else {
            return PlayState.STOP;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}