package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SenpaiEntity extends Monster implements GeoEntity {
    public static final EntityDataAccessor<Boolean> RUNNER = SynchedEntityData.defineId(SenpaiEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SenpaiEntity(EntityType<SenpaiEntity> type, Level world) {
        super(type, world);
        xpReward = 40;
        setNoAi(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RUNNER, Math.random() < 0.5);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Runner", this.entityData.get(RUNNER));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(RUNNER, compound.getBoolean("Runner"));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, false));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this).setAlertOthers());
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new FloatGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        double random = Math.random();
        if (random < 0.01) {
            this.spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        } else if (random < 0.2) {
            this.spawnAtLocation(new ItemStack(Items.GOLDEN_APPLE));
        } else {
            this.spawnAtLocation(new ItemStack(Items.APPLE));
        }
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.IDLE.get();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(ModSounds.STEP.get(), 0.25f, 1);
    }

    @Override
    public SoundEvent getHurtSound(@NotNull DamageSource ds) {
        return ModSounds.OUCH.get();
    }

    @Override
    public SoundEvent getDeathSound() {
        return ModSounds.GROWL.get();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.refreshDimensions();
    }

    @Override
    protected @NotNull EntityDimensions getDefaultDimensions(@NotNull Pose pose) {
        return super.getDefaultDimensions(pose);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    private PlayState movementPredicate(AnimationState<SenpaiEntity> event) {
        if ((event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F)) && !this.isAggressive()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.senpai.walk"));
        }
        if (this.isDeadOrDying()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.senpai.die"));
        }
        if (this.isAggressive() && event.isMoving()) {
            if (entityData.get(RUNNER)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.senpai.run2"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.senpai.run"));
            }
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.senpai.idle"));
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 540) {
            this.remove(RemovalReason.KILLED);
            this.dropExperience(null);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 4, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof SenpaiEntity senpai)) return;

        if (senpai.entityData.get(RUNNER)) {
            var attribute = senpai.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attribute != null) {
                attribute.addPermanentModifier(new AttributeModifier(com.atsuishio.superbwarfare.Mod.ATTRIBUTE_MODIFIER, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        } else {
            var attribute = senpai.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attribute != null) {
                attribute.addPermanentModifier(new AttributeModifier(com.atsuishio.superbwarfare.Mod.ATTRIBUTE_MODIFIER, 3, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }
}
