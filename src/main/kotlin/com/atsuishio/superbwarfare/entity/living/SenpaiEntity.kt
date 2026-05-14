package com.atsuishio.superbwarfare.entity.living

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.SenpaiAnimationInstance
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.network.NetworkHooks

open class SenpaiEntity(type: EntityType<SenpaiEntity>, level: Level) : Monster(type, level) {
    open val animationInstance: SenpaiAnimationInstance? =
        if (this.level().isClientSide) SenpaiAnimationInstance(this) else null
    open var runner by RUNNER

    init {
        xpReward = 40
        setNoAi(false)
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        this.entityData.define(RUNNER, false)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun finalizeSpawn(
        pLevel: ServerLevelAccessor,
        pDifficulty: DifficultyInstance,
        pReason: MobSpawnType,
        pSpawnData: SpawnGroupData?,
        pDataTag: CompoundTag?
    ): SpawnGroupData? {
        this.runner = Math.random() < 0.3

        if (this.runner) {
            this.getAttribute(Attributes.MOVEMENT_SPEED)?.addPermanentModifier(
                AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    0.4,
                    AttributeModifier.Operation.MULTIPLY_BASE
                )
            )
        } else {
            this.getAttribute(Attributes.ATTACK_DAMAGE)?.addPermanentModifier(
                AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    3.0,
                    AttributeModifier.Operation.ADDITION
                )
            )
        }

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("Runner", this.runner)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        this.runner = compound.getBoolean("Runner")
    }

    override fun getStandingEyeHeight(poseIn: Pose, sizeIn: EntityDimensions): Float = 1.75f

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener?> {
        return NetworkHooks.getEntitySpawningPacket(this)
    }

    override fun registerGoals() {
        super.registerGoals()
        this.goalSelector.addGoal(1, object : MeleeAttackGoal(this, 1.4, false) {
            override fun getAttackReachSqr(entity: LivingEntity): Double {
                return (this.mob.bbWidth * this.mob.bbWidth + entity.bbWidth).toDouble()
            }
        })
        this.targetSelector.addGoal(2, HurtByTargetGoal(this).setAlertOthers())
        this.goalSelector.addGoal(3, RandomLookAroundGoal(this))
        this.goalSelector.addGoal(4, FloatGoal(this))
        this.goalSelector.addGoal(5, RandomStrollGoal(this, 0.8))
        this.targetSelector.addGoal(6, NearestAttackableTargetGoal(this, Player::class.java, false, false))
    }

    override fun getMobType(): MobType = MobType.ILLAGER

    public override fun getAmbientSound(): SoundEvent? {
        return ModSounds.IDLE.get()
    }

    public override fun playStepSound(pos: BlockPos, blockIn: BlockState) {
        this.playSound(ModSounds.STEP.get(), 0.25f, 1f)
    }

    public override fun getHurtSound(ds: DamageSource): SoundEvent {
        return ModSounds.OUCH.get()
    }

    public override fun getDeathSound(): SoundEvent {
        return ModSounds.GROWL.get()
    }

    override fun baseTick() {
        super.baseTick()
        this.refreshDimensions()
    }

    override fun aiStep() {
        super.aiStep()
        this.updateSwingTime()
    }

    override fun tickDeath() {
        ++this.deathTime
        if (this.deathTime == 540) {
            this.remove(RemovalReason.KILLED)
            this.dropExperience()
        }
    }

    companion object {
        val RUNNER: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(SenpaiEntity::class.java, EntityDataSerializers.BOOLEAN)

        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
        }
    }
}