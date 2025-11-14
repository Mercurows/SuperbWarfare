package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
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
    protected void defineSynchedData() {
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(LAST_ATTACKER_UUID, "undefined");
        this.entityData.define(HEALTH, 5f);
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
        amount = DAMAGE_MODIFIER.compute(this, source, amount);
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
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
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

        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.03, 0));

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
            this.setDeltaMovement(this.getDeltaMovement().multiply(1, -0.9, 1));
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
        new CustomExplosion.Builder(this)
                .attacker(this.getOwner())
                .damage(10)
                .radius(2f)
                .keepBlock()
                .withParticleType(ParticleTool.ParticleType.SMALL)
                .explode();

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
        Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().add(this.random.triangle(0, 0.0172275 * (double) pInaccuracy), this.random.triangle(0, 0.0172275 * (double) pInaccuracy), this.random.triangle(0, 0.0172275 * (double) pInaccuracy)).scale(pVelocity);
        this.setDeltaMovement(vec3);
    }
}