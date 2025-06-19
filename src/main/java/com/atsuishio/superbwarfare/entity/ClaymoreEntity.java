package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

public class ClaymoreEntity extends Entity implements GeoEntity, OwnableEntity {

    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(ClaymoreEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected static final EntityDataAccessor<String> LAST_ATTACKER_UUID = SynchedEntityData.defineId(ClaymoreEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(ClaymoreEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ClaymoreEntity(EntityType<ClaymoreEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public ClaymoreEntity(LivingEntity owner, Level level) {
        super(ModEntities.CLAYMORE.get(), level);
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, Optional.empty())
                .define(LAST_ATTACKER_UUID, "undefined")
                .define(HEALTH, 10f);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof ThrownPotion || source.getDirectEntity() instanceof AreaEffectCloud)
            return false;
        if (source.is(DamageTypes.FALL))
            return false;
        if (source.is(DamageTypes.CACTUS))
            return false;
        if (source.is(DamageTypes.DROWN))
            return false;
        if (source.is(DamageTypes.DRAGON_BREATH))
            return false;
        if (source.is(DamageTypes.WITHER))
            return false;
        if (source.is(DamageTypes.WITHER_SKULL))
            return false;
        if (source.is(ModDamageTypes.CUSTOM_EXPLOSION) || source.is(ModDamageTypes.MINE) || source.is(ModDamageTypes.PROJECTILE_BOOM)) {
            amount *= 0.2f;
        }

        if (source.getEntity() != null) {
            this.entityData.set(LAST_ATTACKER_UUID, source.getEntity().getStringUUID());
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            ParticleTool.sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), this.getX(), this.getY() + 0.2, this.getZ(), 2, 0.02, 0.02, 0.02, 0.1, false);
        }
        this.level().playSound(null, this.getOnPos(), ModSounds.HIT.get(), SoundSource.PLAYERS, 1, 1);
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
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Health", this.entityData.get(HEALTH));
        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID));
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
        if (this.isOwnedBy(player) && player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                this.discard();
            }

            if (!player.getAbilities().instabuild) {
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.CLAYMORE_MINE.get()));
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void tick() {
        super.tick();
        var level = this.level();
        var x = this.getX();
        var y = this.getY();
        var z = this.getZ();

        if (this.tickCount >= 12000) {
            if (!this.level().isClientSide()) this.discard();
        }

        if (this.tickCount >= 40) {
            final Vec3 center = new Vec3(x + 1.5 * this.getLookAngle().x, y + 1.5 * this.getLookAngle().y, z + 1.5 * this.getLookAngle().z);
            for (var target : level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(2.5 / 2d), e -> true)) {
                var condition = this.getOwner() != target
                        && (target instanceof LivingEntity || target instanceof VehicleEntity)
                        && !(target instanceof TargetEntity)
                        && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))
                        && (this.getOwner() != null && !this.getOwner().isAlliedTo(target) || target.getTeam() == null || target.getTeam().getName().equals("TDM"))
                        && !target.isShiftKeyDown();
                if (!condition) continue;

                if (!level.isClientSide()) {
                    if (!this.level().isClientSide()) {
                        ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
                    }
                    this.discard();
                }

                Mod.queueServerWork(1, () -> {
                    if (!level.isClientSide()) {
                        triggerExplode();
                    }
                });
                break;
            }
        }

        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.03, 0.0));

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
            destroy();
        }

        this.refreshDimensions();
    }

    public void destroy() {
        if (level() instanceof ServerLevel) {
            Entity attacker = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_ATTACKER_UUID));
            CustomExplosion explosion = new CustomExplosion(this.level(), attacker == null ? this : attacker,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), attacker == null ? this : attacker, attacker == null ? this : attacker), 25.0f,
                    this.getX(), this.getY(), this.getZ(), 5f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
            explosion.explode();
            EventHooks.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
            this.discard();
        }
    }

    private void triggerExplode() {
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeMineDamage(this.level().registryAccess(), this.getOwner()), 140f,
                this.getX(), this.getEyeY(), this.getZ(), 4f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
        explosion.explode();
        EventHooks.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose p_33597_) {
        return super.getDimensions(p_33597_).scale((float) 0.5);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}