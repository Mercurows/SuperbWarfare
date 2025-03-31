package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
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
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SenpaiEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SenpaiEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.SENPAI.get(), world);
    }

    public SenpaiEntity(EntityType<SenpaiEntity> type, Level world) {
        super(type, world);
        xpReward = 40;
        setNoAi(false);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    protected float getStandingEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
        return 1.75F / 2;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, false) {
            @Override
            protected double getAttackReachSqr(LivingEntity entity) {
                return this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth();
            }
        });
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this).setAlertOthers());
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new FloatGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
    }

    @Override
    public MobType getMobType() {
        return MobType.ILLAGER;
    }

    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);
        this.spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.IDLE.get();
    }

    @Override
    public void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(ModSounds.STEP.get(), 1f, 1.25f);
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
        return ModSounds.OUCH.get();
    }

    @Override
    public SoundEvent getDeathSound() {
        return ModSounds.GROWL.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose p_33597_) {
        return super.getDimensions(p_33597_).scale((float) 1);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.MAX_HEALTH, 114)
                .add(Attributes.ARMOR, 5)
                .add(Attributes.ATTACK_DAMAGE, 14)
                .add(Attributes.FOLLOW_RANGE, 191)
                .add(Attributes.KNOCKBACK_RESISTANCE, 9.810);
    }

    private PlayState movementPredicate(AnimationState<SenpaiEntity> event) {
        if ((event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F)) && !this.isAggressive()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.senpai.walk"));
        }
        if (this.isDeadOrDying()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.senpai.die"));
        }
        if (this.isAggressive() && event.isMoving()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.senpai.run"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.senpai.idle"));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 540) {
            this.remove(SenpaiEntity.RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    public String getSyncedAnimation() {
        return null;
    }

    public void setAnimation(String animation) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 4, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
