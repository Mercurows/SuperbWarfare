package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.Ptkm1rAnimationInstance
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.living.SenpaiEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.Companion.createDefaultModifier
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.SeekTool
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.players.OldUsersConverter
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.ItemHandlerHelper
import java.util.*

open class Ptkm1rEntity : Entity, OwnableEntity {
    var aimingTime: Int = 0
    var target: String? = "none"
    open val animationInstance: Ptkm1rAnimationInstance? =
        if (this.level().isClientSide) Ptkm1rAnimationInstance(this) else null

    constructor(type: EntityType<Ptkm1rEntity>, world: Level) : super(type, world)

    constructor(owner: LivingEntity?, level: Level) : super(ModEntities.PTKM_1R.get(), level) {
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID())
        }
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        with(builder) {
            define(OWNER_UUID, Optional.empty())
            define(LAST_ATTACKER_UUID, "undefined")
            define(TARGET_UUID, "undefined")
            define(HEALTH, 40f)
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
        compound.putString("Target", this.entityData.get(TARGET_UUID))
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
        if (compound.contains("Target")) {
            this.entityData.set(TARGET_UUID, compound.getString("Target"))
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
                ItemHandlerHelper.giveItemToPlayer(player, ItemStack(ModItems.PTKM_1R.get()))
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide())
    }

    override fun tick() {
        super.tick()

        this.deltaMovement = this.deltaMovement.add(0.0, -0.04, 0.0)

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

        if (tickCount == 1) {
            level().playSound(
                null,
                BlockPos.containing(position()),
                ModSounds.PTKM_1R_DEPLOY.get(),
                SoundSource.PLAYERS,
                1f,
                1f
            )
        }

        if (tickCount > 20 && onGround()) {
            findTarget()
        }

        this.refreshDimensions()
    }

    open fun findTarget() {
        val range = 40
        if (target.equals("none") && tickCount % 10 == 0) {
            val list = SeekTool.Builder(this)
                .withinRange(range.toDouble())
                .build()
            for (entity in list) {
                val condition =
                    entity.onGround()
                            && this.owner !== entity
                            && !(entity is Player && (entity.isCreative || entity.isSpectator))
                            && !entity.isShiftKeyDown
                            && ((entity.boundingBox.size > 1.5 || entity is VehicleEntity || entity is SenpaiEntity) && entity.deltaMovement.lengthSqr() > 0.009)
                            && this.owner?.vehicle !== entity
                            && (!ExplosionConfig.FRIENDLY_MINES.get() || !SeekTool.IS_FRIENDLY.test(this.owner, entity))
                if (!condition) continue

                target = entity.stringUUID
                break
            }
        }

        val targetEntity = EntityFindUtil.findEntity(level(), target)

        if (targetEntity == null) {
            target = "none"
            if (aimingTime > 0) {
                aimingTime--
            }
        } else {
            val targetXRot: Float
            val distance = targetEntity.distanceTo(this).toDouble()

            if (distance < range) {
                targetXRot = -40f
                this.look(targetEntity.position())
                if (distance < range - 5) {
                    aimingTime++
                } else if (aimingTime > 0) {
                    aimingTime--
                }
            } else {
                this.xRot = 0f
                targetXRot = 0f
            }

            val diffX = Mth.wrapDegrees(targetXRot - this.xRot).coerceIn(-60f, 60f)
            this.xRot += 0.25f * diffX

            if (aimingTime > 10) {
                shoot(targetEntity, distance)
            }
        }
    }

    private fun shoot(entity: Entity?, distance: Double) {
        val level = this.level()
        if (level is ServerLevel) {
            val ptkmProjectile = PtkmProjectileEntity(this.owner, level)
            ptkmProjectile.setDamage(ExplosionConfig.PTKM_1R_PROJECTILE_HIT_DAMAGE.get().toFloat())
            ptkmProjectile.setExplosionDamage(ExplosionConfig.PTKM_1R_PROJECTILE_EXPLOSION_DAMAGE.get().toFloat())
            ptkmProjectile.setExplosionRadius(ExplosionConfig.PTKM_1R_PROJECTILE_EXPLOSION_RADIUS.get().toFloat())
            ptkmProjectile.setTarget(entity)
            ptkmProjectile.setShootTime((0.5f * distance).toInt())
            ptkmProjectile.setPos(position().x, eyePosition.y, position().z)
            ptkmProjectile.shoot(lookAngle.x, lookAngle.y, lookAngle.z, 4f, 0.4f)
            level.addFreshEntity(ptkmProjectile)

            var count = 6

            var i = 1f
            while (i < 8) {
                level.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    position().x + i * lookAngle.x,
                    eyePosition.y + i * lookAngle.y,
                    position().z + i * lookAngle.z,
                    Mth.clamp(count--, 1, 3), 0.15, 0.15, 0.15, 0.0025
                )
                i += .5f
            }

            ParticleTool.spawnSmallExplosionParticles(level, position())
            this.discard()
        }
    }

    fun look(pTarget: Vec3) {
        val vec3 = EntityAnchorArgument.Anchor.EYES.apply(this)
        val d0 = (pTarget.x - vec3.x) * 0.2
        val d2 = (pTarget.z - vec3.z) * 0.2
        val diffY =
            Mth.wrapDegrees(Mth.wrapDegrees((Mth.atan2(d2, d0) * 57.2957763671875).toFloat() - 90f) - this.yRot)
        this.yRot += 0.5f * diffY
    }

    private fun triggerExplode() {
        CustomExplosion.Builder(this)
            .damage(ExplosionConfig.PTKM_1R_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ExplosionConfig.PTKM_1R_EXPLOSION_RADIUS.get().toFloat())
            .attacker(this.owner)
            .withParticleType(ParticleTool.ParticleType.HUGE)
            .explode()

        this.discard()
    }

    companion object {
        @JvmField
        protected val OWNER_UUID: EntityDataAccessor<Optional<UUID>> =
            SynchedEntityData.defineId(Ptkm1rEntity::class.java, EntityDataSerializers.OPTIONAL_UUID)

        @JvmField
        protected val LAST_ATTACKER_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(Ptkm1rEntity::class.java, EntityDataSerializers.STRING)

        @JvmField
        val HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(Ptkm1rEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        protected val TARGET_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(Ptkm1rEntity::class.java, EntityDataSerializers.STRING)

        private val DAMAGE_MODIFIER = createDefaultModifier()
            .multiply(0.02f, ModDamageTypes.CUSTOM_EXPLOSION)
            .multiply(0.02f, ModDamageTypes.MINE)
            .multiply(0.02f, ModDamageTypes.PROJECTILE_EXPLOSION)
            .multiply(0.02f, DamageTypes.EXPLOSION)
    }
}