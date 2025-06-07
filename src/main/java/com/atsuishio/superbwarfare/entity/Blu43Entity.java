package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
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

public class Blu43Entity extends Entity implements GeoEntity, OwnableEntity {

    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(Blu43Entity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected static final EntityDataAccessor<String> LAST_ATTACKER_UUID = SynchedEntityData.defineId(Blu43Entity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(Blu43Entity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Blu43Entity(EntityType<Blu43Entity> type, Level world) {
        super(type, world);
    }

    public Blu43Entity(LivingEntity owner, Level level) {
        super(ModEntities.BLU_43.get(), level);
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(OWNER_UUID, Optional.empty())
                .define(LAST_ATTACKER_UUID, "undefined")
                .define(HEALTH, 5f);
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
        if (source.is(ModDamageTypes.CUSTOM_EXPLOSION) || source.is(ModDamageTypes.MINE) || source.is(ModDamageTypes.PROJECTILE_BOOM) || source.is(DamageTypes.EXPLOSION)) {
            amount *= 0.02f;
        }
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
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.BLU_43_MINE.get()));
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount >= 20 && onGround()) {
            touchEntity();
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
            triggerExplode();
        }

        this.refreshDimensions();
    }

    public void touchEntity() {
        if (level() instanceof ServerLevel) {
            var frontBox = getBoundingBox().inflate(0.2);
            boolean trigger = false;

            var entities = level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox, entity -> entity != this && !(entity instanceof HangingEntity) && entity.getBoundingBox().getSize() > 0.4).stream().toList();

            for (var entity : entities) {
                if (entity != null) {
                    trigger = true;
                    if (!entity.level().isClientSide() && entity instanceof LivingEntity living) {
                        int baseAmplifier = 3;
                        int baseDuration = 600;

                        var boot = living.getItemBySlot(EquipmentSlot.FEET);
                        var leggings = living.getItemBySlot(EquipmentSlot.LEGS);
                        if (!boot.isEmpty()) {
                            baseAmplifier--;
                            baseDuration -= 100;
                            if (boot.getItem() instanceof ArmorItem armorItem) {
                                baseDuration -= armorItem.getDefense() * 10;
                            }
                        }
                        if (!leggings.isEmpty()) {
                            baseAmplifier--;
                            baseDuration -= 100;
                            if (leggings.getItem() instanceof ArmorItem armorItem) {
                                baseDuration -= armorItem.getDefense() * 10;
                            }
                        }

                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(baseDuration, 20), baseAmplifier, false, false), this.getOwner());
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.max(baseDuration, 20), baseAmplifier, false, false), this.getOwner());
                        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false), this.getOwner());
                    }
                    break;
                }
            }

            if (trigger) {
                triggerExplode();
            }
        }
    }

    private void triggerExplode() {
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, this.getOwner()), 10f,
                this.getX(), this.getEyeY(), this.getZ(), 2f, Explosion.BlockInteraction.KEEP, false);
        explosion.explode();
        EventHooks.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnSmallExplosionParticles(this.level(), this.position());
        this.discard();
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

    public void shoot(double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
        Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().add(this.random.triangle(0.0, 0.0172275 * (double) pInaccuracy), this.random.triangle(0.0, 0.0172275 * (double) pInaccuracy), this.random.triangle(0.0, 0.0172275 * (double) pInaccuracy)).scale(pVelocity);
        this.setDeltaMovement(vec3);
    }
}