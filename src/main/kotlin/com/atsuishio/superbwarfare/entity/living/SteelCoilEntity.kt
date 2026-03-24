package com.atsuishio.superbwarfare.entity.living

import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.angleTo
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.entity.vehicle.Minecart
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

open class SteelCoilEntity(type: EntityType<SteelCoilEntity>, level: Level) : Mob(type, level), GeoEntity {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    var wheelRot = 0f
    var wheelRotO = 0f
    open var targetUUID by TARGET_UUID
    open var targetPosition = Vec3(0.0, 0.0, 0.0)
    open var currentPosition = Vec3(0.0, 0.0, 0.0)
    open var startCrush = false
    open var restartCrushTimer = 0

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        with(builder) {
            define(TARGET_UUID, "")
        }
    }

    override fun canCollideWith(pEntity: Entity): Boolean {
        return false
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

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) {}

    override fun causeFallDamage(l: Float, d: Float, source: DamageSource): Boolean {
        return false
    }

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {}

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = this.cache

    //村民测试
    open fun seekNearLivingEntity(
        seekRange: Double,
    ) = level().getEntitiesOfClass(Villager::class.java, AABB(position(), position()).inflate(seekRange)) { true }
        .sortedBy { it.distanceToSqr(position()) }
        .find { target ->
            target.distanceToSqr(this) <= seekRange * seekRange
        }

//    open fun seekNearLivingEntity(
//        seekRange: Double,
//    ) = level().getEntitiesOfClass(Player::class.java, AABB(position(), position()).inflate(seekRange)) { true }
//        .sortedBy { it.distanceToSqr(position()) }
//        .find { target -> target.distanceToSqr(this) <= seekRange * seekRange && !(target.isSpectator || target.isCreative)
//        }

    override fun baseTick() {
        wheelRotO = wheelRot
        super.baseTick()
        val speed = deltaMovement.dot(forward).toFloat()
        val c = 4f * Mth.PI
        val t = c / speed
        val rpt = 360f / t
        wheelRot += Mth.PI * rpt

        val target = target
        if (tickCount % 10 == 0 && target == null) {
            val player = seekNearLivingEntity(attributes.getValue(Attributes.FOLLOW_RANGE))
            if (player != null) {
                setTarget(player)
            }
        }

        if (target != null) {
            val targetPos = target.position().add(
                position().vectorTo(target.position()).normalize()
                    .scale(position().distanceTo(target.position()).coerceAtLeast(12.0))
            )

            if (!startCrush) {
                targetPosition = targetPos
                currentPosition = position()
                startCrush = true
                restartCrushTimer = 0
            }

            if (startCrush) {
                restartCrushTimer++

                val d0: Double = target.position().x - this.x
                val d1: Double = target.position().z - this.z

                if (attackableEntity(target)) {
                    val f9 = (Mth.atan2(d1, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat() - 90.0f
                    this.yRot = rotlerp(this.yRot, f9, 3.0f)
                }

                val s = (position().distanceToSqr(targetPosition) / (currentPosition.distanceToSqr(targetPosition)))

                this.moveControl.setWantedPosition(
                    targetPosition.x,
                    targetPosition.y,
                    targetPosition.z,
                    5 * Mth.clamp(Mth.sin(Mth.PI * s.toFloat()).toDouble(), 0.4, 1.0)
                )
                if (position().distanceToSqr(targetPosition) < 2 || restartCrushTimer > 100) {
                    if (!attackableEntity(target)) {
                        setTarget(null as LivingEntity?)
                    }
                    startCrush = false
                    restartCrushTimer = 0
                }
            }
        }

//        val targetPlayer = EntityFindUtil.findEntity(level(), targetUUID)
//
//        if (tickCount % 10 == 0 && targetPlayer == null) {
//            val player = seekNearLivingEntity(attributes.getValue(Attributes.FOLLOW_RANGE))
//            if (player != null) {
//                targetUUID = player.stringUUID
//            }
//        }
//
//        if (player != null) {
//            this.moveControl.setWantedPosition(player.position().x, player.position().y, player.position().z, 2.0)
//        }
        crushEntities()
    }

    fun attackableEntity(entity: Entity): Boolean {
        return !(!entity.isAlive || (entity is Player && (entity.isCreative || entity.isSpectator)))
    }

    protected fun rotlerp(pSourceAngle: Float, pTargetAngle: Float, pMaximumChange: Float): Float {
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

    fun getRotaion(ticks: Float): Float {
        return Mth.lerp(ticks, wheelRotO, wheelRot)
    }

    companion object {
        @JvmField
        val TARGET_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(SteelCoilEntity::class.java, EntityDataSerializers.STRING)

        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
        }
    }

    @Suppress("DEPRECATION")
    fun crushEntities() {
        if (this.isRemoved) return
        val vec3 = this.deltaMovement
        val entities: List<Entity>?

        val frontBox = this.boundingBox.move(vec3)
        entities = this.level().getEntities(
            EntityTypeTest.forClass(Entity::class.java),
            frontBox
        ) { entity -> entity !== this && entity!!.vehicle == null }
            .stream().filter { entity ->
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

            if (entity is LivingEntity && entity.hasEffect(ModMobEffects.STRIKE_PROTECTION)) {
                continue
            }

            if (entity is VehicleEntity) {
                f = Mth.clamp((entity.mass / 30).toDouble(), 0.25, 4.0)
                f1 = Mth.clamp((30 / entity.mass).toDouble(), 0.25, 4.0)
            } else {
                f = Mth.clamp(2 * entitySize / thisSize, 0.25, 4.0)
                f1 = Mth.clamp(thisSize / 2 * entitySize, 0.25, 4.0)
            }

            val length = v0.length().toFloat()
            val velAdd = v0.normalize().scale(0.8 * length)

            if (length <= 0.2) {
                continue
            }

            this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.soundSource, 1f, 1f)

            entity.forceHurt(
                ModDamageTypes.causeVehicleStrikeDamage(
                    this.level().registryAccess(),
                    this, this
                ),
                (f1 * 120 * (Mth.abs(length) - 0.2) * (Mth.abs(length) - 0.2)).toFloat()
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
}