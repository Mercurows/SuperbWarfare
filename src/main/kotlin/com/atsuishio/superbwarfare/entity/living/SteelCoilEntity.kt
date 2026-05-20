package com.atsuishio.superbwarfare.entity.living

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.Companion.createDefaultModifier
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.angleTo
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.entity.vehicle.Minecart
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.ForgeMod
import net.minecraftforge.network.NetworkHooks
import java.util.*
import java.util.function.Consumer

open class SteelCoilEntity(type: EntityType<SteelCoilEntity>, level: Level) : PathfinderMob(type, level), NeutralMob {
    var wheelRot = 0f
    var wheelRotO = 0f
    open var targetPosition = Vec3(0.0, 0.0, 0.0)
    open var startCrush = false
    open var restartCrushTimer = 0
    private var remainingPersistentAngerTime = 0
    private var persistentAngerTarget: UUID? = null

    private var wasMoving = false

    override fun addAdditionalSaveData(pCompound: CompoundTag) {
        super.addAdditionalSaveData(pCompound)
        this.addPersistentAngerSaveData(pCompound)
    }

    override fun readAdditionalSaveData(pCompound: CompoundTag) {
        super.readAdditionalSaveData(pCompound)
        this.readPersistentAngerSaveData(this.level(), pCompound)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        return super.hurt(source, DAMAGE_MODIFIER.compute(this, source, amount))
    }

    override fun registerGoals() {
        this.goalSelector.addGoal(0, SteelCoilCrushGoal(this))
        this.targetSelector.addGoal(0, HurtByTargetGoal(this, SteelCoilEntity::class.java).setAlertOthers())
        this.targetSelector.addGoal(2, ResetUniversalAngerTargetGoal(this, true))
    }

    override fun canBeAffected(pEffectInstance: MobEffectInstance): Boolean {
        return false
    }

    override fun getHurtSound(pDamageSource: DamageSource): SoundEvent {
        return ModSounds.INDICATION_VEHICLE.get()
    }

    override fun getDeathSound(): SoundEvent {
        return ModSounds.STEEL_PIPE_DROP.get()
    }

    override fun aiStep() {
        super.aiStep()

        val level = this.level()
        if (level is ServerLevel) {
            this.updatePersistentAnger(level, true)
        }
    }

    override fun canCollideWith(entity: Entity): Boolean {
        return entity is SteelCoilEntity
    }

    override fun canBeCollidedWith(): Boolean {
        return true
    }

    override fun isPushable(): Boolean {
        return false
    }

    override fun getArmorSlots(): Iterable<ItemStack> {
        return NonNullList.withSize(1, ItemStack.EMPTY)
    }

    override fun getItemBySlot(pSlot: EquipmentSlot): ItemStack {
        return ItemStack.EMPTY
    }

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) {
    }

    override fun causeFallDamage(l: Float, d: Float, source: DamageSource): Boolean {
        return false
    }

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> {
        return NetworkHooks.getEntitySpawningPacket(this)
    }

    override fun baseTick() {
        if (this.level().isClientSide) {
            if (!this.wasMoving && this.moving()) {
                playMoveSound.accept(this)
            }
        }

        this.wasMoving = this.moving()
        wheelRotO = wheelRot
        super.baseTick()
        val speed = deltaMovement.dot(forward).toFloat()
        if (speed > 0) {
            val c = 4f * Mth.PI
            val t = c / speed
            val rpt = 360f / t
            wheelRot += Mth.PI * rpt
        }

        if (this.target != null && this.tickCount % 20 == 0) {
            val targetPos = target!!.position().add(
                this.position().vectorTo(target!!.position()).normalize()
                    .scale(this.position().distanceTo(target!!.position()).coerceAtLeast(1.0))
            )

            this.targetPosition = targetPos
        }

        crushEntities()
    }

    open fun moving() = deltaMovement.lengthSqr() > 0.0001 && onGround()

    fun isAttackableEntity(entity: Entity): Boolean {
        return entity.isAlive || (entity is Player && (!entity.isCreative && !entity.isSpectator))
    }

    protected fun lerpRot(pSourceAngle: Float, pTargetAngle: Float, pMaximumChange: Float): Float {
        var f = Mth.wrapDegrees(pTargetAngle - pSourceAngle)
        if (f > pMaximumChange) {
            f = pMaximumChange
        }

        if (f < -pMaximumChange) {
            f = -pMaximumChange
        }

        var f1 = pSourceAngle + f
        if (f1 < 0.0f) {
            f1 += 360.0f
        } else if (f1 > 360.0f) {
            f1 -= 360.0f
        }

        return f1
    }

    fun getRotation(ticks: Float): Float {
        return Mth.lerp(ticks, wheelRotO, wheelRot)
    }

    companion object {
        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(ForgeMod.STEP_HEIGHT_ADDITION.get(), 2.0)
        }

        private val DAMAGE_MODIFIER = createDefaultModifier()
            .immuneTo(DamageTypes.IN_WALL)
            .immuneTo(DamageTypes.DROWN)
            .immuneTo(DamageTypes.FALL)
            .immuneTo(DamageTypes.IN_FIRE)
            .immuneTo(DamageTypes.ON_FIRE)
            .immuneTo(DamageTypes.CACTUS)
            .immuneTo(DamageTypes.MAGIC)
            .multiply(0.5f, DamageTypes.EXPLOSION)
            .multiply(0.5f, DamageTypes.PLAYER_EXPLOSION)
            .multiply(0.25f, ModTags.DamageTypes.PROJECTILE)
            .multiply(0.5f, DamageTypes.PLAYER_ATTACK)
            .multiply(2f, ModDamageTypes.REPAIR_TOOL)

        var playMoveSound: Consumer<SteelCoilEntity> = Consumer { }
    }

    @Suppress("DEPRECATION")
    fun crushEntities() {
        if (this.isRemoved) return
        val vec3 = this.deltaMovement
        if (vec3.lengthSqr() <= 0.0001) return
        val frontBox = this.boundingBox.move(vec3)
        val entities = this.level().getEntities(
            EntityTypeTest.forClass(Entity::class.java),
            frontBox
        ) { entity -> entity !== this && entity!!.vehicle == null && entity !is SteelCoilEntity }
            .asSequence().filter { entity ->
                if (entity.isAlive) {
                    val type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
                    return@filter (entity is VehicleEntity || entity is Boat || entity is Minecart || (entity is TurretWreckEntity && entity.tickCount > 5)
                            || (entity is LivingEntity && !(entity is Player && entity.isSpectator)))
                            || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString())
                }
                false
            }
            .toList()

        for (entity in entities) {
            val entitySize = entity.boundingBox.getSize()
            val thisSize = this.boundingBox.getSize()
            val f: Double
            val f1: Double

            val v0 = vec3.subtract(entity.deltaMovement)
            if (v0.angleTo(this.position().vectorTo(entity.position())) > 90) return

            if (this.deltaMovement.lengthSqr() < 0.04) return

            if (entity is LivingEntity && entity.hasEffect(ModMobEffects.STRIKE_PROTECTION.get())) {
                continue
            }

            if (entity is VehicleEntity) {
                f = (entity.mass / 30).toDouble().coerceIn(0.25, 4.0)
                f1 = (30 / entity.mass).toDouble().coerceIn(0.25, 4.0)
            } else {
                f = (2 * entitySize / thisSize).coerceIn(0.25, 4.0)
                f1 = (thisSize / 2 * entitySize).coerceIn(0.25, 4.0)
            }

            val length = v0.length().toFloat()
            val velAdd = v0.normalize().scale(0.8 * length)

            if (length <= 0.01) {
                continue
            }

            this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.soundSource, 1f, 1f)

            entity.forceHurt(
                ModDamageTypes.causeVehicleStrikeDamage(
                    this.level().registryAccess(),
                    this, this
                ),
                (this.attributes.getValue(Attributes.ATTACK_DAMAGE) + f1 * 240 * (Mth.abs(length) - 0.01) * (Mth.abs(
                    length
                ) - 0.01)).toFloat()
            )

            this.pushNew(-0.3f * f * velAdd.x, -0.3f * f * velAdd.y, -0.3f * f * velAdd.z)

            if (entity is VehicleEntity) {
                val vec31 = this.deltaMovement.normalize().scale(velAdd.length())
                entity.pushNew(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z)
            } else {
                val vec31 = this.deltaMovement.normalize().scale(velAdd.length())
                entity.push(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z)
            }
        }
    }

    open fun pushNew(pX: Double, pY: Double, pZ: Double) {
        this.deltaMovement = this.deltaMovement.add(pX, pY, pZ)
    }

    override fun getRemainingPersistentAngerTime(): Int {
        return this.remainingPersistentAngerTime
    }

    override fun setRemainingPersistentAngerTime(pRemainingPersistentAngerTime: Int) {
        this.remainingPersistentAngerTime = pRemainingPersistentAngerTime
    }

    override fun getPersistentAngerTarget(): UUID? {
        return this.persistentAngerTarget
    }

    override fun setPersistentAngerTarget(pPersistentAngerTarget: UUID?) {
        this.persistentAngerTarget = pPersistentAngerTarget
    }

    override fun startPersistentAngerTimer() {
        this.remainingPersistentAngerTime = this.random.nextIntBetweenInclusive(20, 30)
    }

    class SteelCoilCrushGoal(val entity: SteelCoilEntity) : Goal() {
        init {
            this.flags = EnumSet.of(Flag.MOVE)
        }

        override fun canUse(): Boolean {
            val rate = entity.health / entity.maxHealth
            if (rate > MiscConfig.STEEL_COIL_AWAKE_PERCENTAGE.get()) return false
            return entity.target != null
        }

        fun refresh() {
            if (!entity.startCrush) {
                entity.startCrush = true
                entity.restartCrushTimer = 0
            }
            if (entity.target != null) {
                if (!entity.isAttackableEntity(entity.target!!)) {
                    entity.target = null
                }
            }
        }

        override fun start() {
            super.start()
            this.refresh()
        }

        override fun stop() {
            super.stop()

            entity.moveControl.setWantedPosition(entity.x, entity.y, entity.z, 0.0)

            entity.startCrush = false
            entity.restartCrushTimer = 0
            entity.setTarget(null)
        }

        override fun canContinueToUse(): Boolean {
            return entity.target?.isAlive ?: false
        }

        override fun tick() {
            super.tick()

            val target = entity.target ?: return
            if (entity.startCrush) {
                entity.restartCrushTimer++

                val d0 = target.position().x - entity.x
                val d1 = target.position().z - entity.z

                if (entity.isAttackableEntity(target)) {
                    val f9 = (Mth.atan2(d1, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat() - 90.0f
                    entity.yRot = entity.lerpRot(entity.yRot, f9, 3.0f)
                }

                val rate = entity.health / entity.maxHealth
                val speed = ((1 - rate.coerceAtLeast(0f)) * 5.0).coerceIn(2.0, 5.0)

                entity.moveControl.setWantedPosition(
                    entity.targetPosition.x,
                    entity.targetPosition.y,
                    entity.targetPosition.z,
                    speed
                )

                if (entity.position().distanceToSqr(entity.targetPosition) < 2 || entity.restartCrushTimer > 100) {
                    this.refresh()
                    if (entity.target == null) {
                        this.stop()
                    }
                }
            }
        }
    }
}