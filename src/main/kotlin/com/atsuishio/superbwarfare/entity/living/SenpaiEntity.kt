package com.atsuishio.superbwarfare.entity.living

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.animation.entity.SenpaiAnimationInstance
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.resource.model.EntityModelReloadListener
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
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

open class SenpaiEntity(type: EntityType<SenpaiEntity>, level: Level) : Monster(type, level) {
    open val animationInstance: SenpaiAnimationInstance? =
        if (this.level().isClientSide) SenpaiAnimationInstance(this) else null
    open val modelInstance = EntityModelReloadListener.getModel(MODEL)?.createInstance()
    open var runner by RUNNER

    init {
        xpReward = 40
        isNoAi = false
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(RUNNER, false)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnType: MobSpawnType,
        spawnGroupData: SpawnGroupData?
    ): SpawnGroupData? {
        this.runner = Math.random() < 0.3

        if (this.runner) {
            this.getAttribute(Attributes.MOVEMENT_SPEED)?.addPermanentModifier(
                AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    0.4,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
            )
        } else {
            this.getAttribute(Attributes.ATTACK_DAMAGE)?.addPermanentModifier(
                AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    3.0,
                    AttributeModifier.Operation.ADD_VALUE
                )
            )
        }

        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("Runner", this.runner)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        this.runner = compound.getBoolean("Runner")
    }

    override fun getEyeY(): Double =
        if (isDeadOrDying) DEAD_DIMENSIONS.height * 0.85 else 1.75

    /**
     * 死亡后把碰撞箱压到贴地。
     *
     * 野兽先辈死后尸体要在地上躺满 27 秒（`tickDeath()` 到 `deathTime == 540` 才 `remove()`），
     * 存活时 0.65×2.0 的箱子（注册值见 `ModEntities`）立在那儿整段时间都在挡路 ——
     * 死亡动画里模型其实早就躺平了（`upper`/`lower` 绕 X 转 -90°、下移 10 个 Bedrock 单位），
     * 箱子却还是原来那么高。这里让死亡后箱子几乎贴地。
     *
     * 箱子只变矮不变宽，且实体的 Y 是箱子底面（脚底），所以新箱子恒为旧箱子的子集，
     * 不会因为变矮卡进上方方块里。`baseTick()` 本来就每 tick 调 `refreshDimensions()`，
     * 尺寸变化会立刻生效；双端都算得出来（`isDeadOrDying()` 读的是同步过的血量）。
     */
    override fun getDefaultDimensions(pose: Pose): EntityDimensions =
        if (isDeadOrDying) DEAD_DIMENSIONS.scale(scale) else super.getDimensions(pose)

    /**
     * 死亡后不再推开其他实体。
     *
     * `LivingEntity.aiStep()` → `pushEntities()` 每 tick 把周围 `isPushable()` 的实体挑出来交给
     * `doPush()`，默认实现是 `entityIn.push(this)` —— 也就是**尸体把玩家/生物推走**。
     * 反方向（玩家推尸体）本来就不会发生：尸体 `isAlive()` 为 false，`isPushable()` 跟着为 false，
     * `EntitySelector.pushableBy()` 压根不会把尸体选进来。所以只要堵住这一边。
     *
     * 存活时保持原版行为（会推挤别人），只在死亡后才吞掉。
     */
    override fun doPush(entityIn: Entity) {
        if (!isDeadOrDying) super.doPush(entityIn)
    }

    override fun registerGoals() {
        super.registerGoals()
        this.goalSelector.addGoal(1, MeleeAttackGoal(this, 1.4, false))
        this.targetSelector.addGoal(2, HurtByTargetGoal(this).setAlertOthers())
        this.goalSelector.addGoal(3, RandomLookAroundGoal(this))
        this.goalSelector.addGoal(4, FloatGoal(this))
        this.goalSelector.addGoal(5, RandomStrollGoal(this, 0.8))
        this.targetSelector.addGoal(6, NearestAttackableTargetGoal(this, Player::class.java, false, false))
    }

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
            this.dropExperience(null)
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

        /** 死亡后贴地的碰撞箱（注册时的存活箱是 0.65×2.0）。 */
        val DEAD_DIMENSIONS: EntityDimensions = EntityDimensions.scalable(0.65f, 0.5f)

        val MODEL = loc("models/bedrock/entity/senpai.geo.json")
    }
}