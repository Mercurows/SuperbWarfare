package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.Companion.createDefaultModifier
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData.Companion.enabledTDM
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.players.OldUsersConverter
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.decoration.HangingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.ItemHandlerHelper
import java.util.*
import kotlin.math.max

open class Blu43Entity : Entity, OwnableEntity {
    constructor(type: EntityType<Blu43Entity>, world: Level) : super(type, world)

    constructor(owner: LivingEntity?, level: Level) : super(ModEntities.BLU_43.get(), level) {
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID())
        }
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        with(builder) {
            define(OWNER_UUID, Optional.empty())
            define(LAST_ATTACKER_UUID, "undefined")
            define(HEALTH, 5f)
        }
    }

    override fun isPickable(): Boolean {
        return !this.isRemoved
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        var amount = amount
        amount = DAMAGE_MODIFIER.compute(source, amount)
        if (source.entity != null) {
            this.entityData.set(LAST_ATTACKER_UUID, source.entity!!.getStringUUID())
        }
        this.entityData.set(HEALTH, this.entityData.get(HEALTH) - amount)
        return super.hurt(source, amount)
    }

    fun setOwnerUUID(pUuid: UUID?) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid))
    }

    override fun getOwnerUUID(): UUID? {
        return this.entityData.get(OWNER_UUID).orElse(null)
    }

    fun isOwnedBy(pEntity: LivingEntity?): Boolean {
        return pEntity === this.owner
    }

    public override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putFloat("Health", this.entityData.get(HEALTH))
        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID))
        if (this.ownerUUID != null) {
            compound.putUUID("Owner", this.ownerUUID!!)
        }
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        if (compound.contains("Health")) {
            this.entityData.set(HEALTH, compound.getFloat("Health"))
        }

        if (compound.contains("LastAttacker")) {
            this.entityData.set(LAST_ATTACKER_UUID, compound.getString("LastAttacker"))
        }

        var uuid: UUID?
        if (compound.hasUUID("Owner")) {
            uuid = compound.getUUID("Owner")
        } else {
            val s = compound.getString("Owner")

            try {
                uuid = if (this.server == null) {
                    UUID.fromString(s)
                } else {
                    OldUsersConverter.convertMobOwnerIfNecessary(this.server!!, s)
                }
            } catch (exception: Exception) {
                Mod.LOGGER.error("Couldn't load owner UUID of {}: {}", this, exception)
                uuid = null
            }
        }

        if (uuid != null) {
            try {
                this.setOwnerUUID(uuid)
            } catch (_: Throwable) {
            }
        }
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (this.isOwnedBy(player) && player.isShiftKeyDown) {
            if (!this.level().isClientSide()) {
                this.discard()
            }

            if (!player.abilities.instabuild) {
                ItemHandlerHelper.giveItemToPlayer(player, ItemStack(ModItems.BLU_43_MINE.get()))
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide())
    }

    override fun tick() {
        super.tick()

        if (this.tickCount >= 20 && onGround()) {
            touchEntity()
        }

        this.deltaMovement = this.deltaMovement.add(0.0, -0.03, 0.0)

        if (!this.level().noCollision(this.boundingBox)) {
            this.moveTowardsClosestSpace(
                this.x,
                (this.boundingBox.minY + this.boundingBox.maxY) / 2.0,
                this.z
            )
        }

        this.move(MoverType.SELF, this.deltaMovement)
        var f = 0.98f
        if (this.onGround()) {
            val pos = this.blockPosBelowThatAffectsMyMovement
            f = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98f
        }

        this.deltaMovement = this.deltaMovement.multiply(f.toDouble(), 0.98, f.toDouble())
        if (this.onGround()) {
            this.deltaMovement = this.deltaMovement.multiply(1.0, -0.9, 1.0)
        }

        if (this.entityData.get(HEALTH) <= 0) {
            triggerExplode()
        }

        this.refreshDimensions()
    }

    open fun touchEntity() {
        if (level() is ServerLevel) {
            val frontBox = boundingBox.inflate(0.2)
            var trigger = false

            val entities = level().getEntities(
                EntityTypeTest.forClass(Entity::class.java),
                frontBox
            ) { true }.asSequence().filter {
                it != this
                        && !(it is Player && it.isSpectator)
                        && it !is HangingEntity
                        && it !is Display
                        && !it.type.`is`(ModTags.EntityTypes.DECOY)
                        && it.boundingBox.size > 0.4
                        && if (ExplosionConfig.FRIENDLY_MINES.get()) {
                    if (owner == null) true else owner != it && !owner!!.isAlliedTo(it)
                } else {
                    (owner != null && owner != it && !owner!!.isAlliedTo(it)) || it.team == null || enabledTDM(it)
                }
            }.toList()

            for (entity in entities) {
                if (entity != null) {
                    trigger = true
                    if (!entity.level().isClientSide() && entity is LivingEntity) {
                        var baseAmplifier = 3
                        var baseDuration = 600

                        val boot = entity.getItemBySlot(EquipmentSlot.FEET)
                        val leggings = entity.getItemBySlot(EquipmentSlot.LEGS)

                        if (!boot.isEmpty) {
                            baseAmplifier--
                            baseDuration -= 100
                            val item = boot.item
                            if (item is ArmorItem) {
                                baseDuration -= item.defense * 10
                            }
                        }
                        if (!leggings.isEmpty) {
                            baseAmplifier--
                            baseDuration -= 100
                            val item = leggings.item
                            if (item is ArmorItem) {
                                baseDuration -= item.defense * 10
                            }
                        }

                        entity.addEffect(
                            MobEffectInstance(
                                MobEffects.MOVEMENT_SLOWDOWN,
                                max(baseDuration, 20),
                                baseAmplifier,
                                false,
                                false
                            ), this.owner
                        )
                        entity.addEffect(
                            MobEffectInstance(
                                MobEffects.WEAKNESS,
                                max(baseDuration, 20),
                                baseAmplifier,
                                false,
                                false
                            ), this.owner
                        )
                        entity.addEffect(MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false), this.owner)
                    }
                    break
                }
            }

            if (trigger) {
                triggerExplode()
            }
        }
    }

    private fun triggerExplode() {
        CustomExplosion.Builder(this)
            .attacker(this.owner)
            .damage(ExplosionConfig.BLU_43_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ExplosionConfig.BLU_43_EXPLOSION_RADIUS.get().toFloat())
            .keepBlock()
            .withParticleType(ParticleTool.ParticleType.SMALL)
            .explode()

        this.discard()
    }

    override fun isPushable(): Boolean {
        return true
    }

    open fun shoot(pX: Double, pY: Double, pZ: Double, pVelocity: Float, pInaccuracy: Float) {
        val vec3 = (Vec3(pX, pY, pZ)).normalize().add(
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble())
        ).scale(pVelocity.toDouble())
        this.deltaMovement = vec3
    }

    companion object {
        @JvmField
        protected val OWNER_UUID: EntityDataAccessor<Optional<UUID>> =
            SynchedEntityData.defineId(Blu43Entity::class.java, EntityDataSerializers.OPTIONAL_UUID)

        @JvmField
        protected val LAST_ATTACKER_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(Blu43Entity::class.java, EntityDataSerializers.STRING)

        @JvmField
        val HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(Blu43Entity::class.java, EntityDataSerializers.FLOAT)

        private val DAMAGE_MODIFIER = createDefaultModifier()
            .multiply(0.02f, ModDamageTypes.CUSTOM_EXPLOSION)
            .multiply(0.02f, ModDamageTypes.MINE)
            .multiply(0.02f, ModDamageTypes.PROJECTILE_EXPLOSION)
            .multiply(0.02f, DamageTypes.EXPLOSION)
    }
}