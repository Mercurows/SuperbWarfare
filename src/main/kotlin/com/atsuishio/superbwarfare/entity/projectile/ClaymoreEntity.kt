package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.Companion.createDefaultModifier
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData.Companion.enabledTDM
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.players.OldUsersConverter
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.ItemHandlerHelper
import java.util.*

open class ClaymoreEntity(type: EntityType<ClaymoreEntity>, level: Level) : Entity(type, level), OwnableEntity {
    constructor(owner: LivingEntity?, level: Level) : this(ModEntities.CLAYMORE.get(), level) {
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID())
        }
    }

    init {
        this.noCulling = true
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(OWNER_UUID, Optional.empty())
            .define(LAST_ATTACKER_UUID, "undefined")
            .define(HEALTH, 10f)
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

        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.sendParticle(
                level,
                ModParticleTypes.FIRE_STAR.get(),
                this.x,
                this.y + 0.2,
                this.z,
                2,
                0.02,
                0.02,
                0.02,
                0.1,
                false
            )
        }
        level.playSound(null, this.onPos, ModSounds.HIT.get(), SoundSource.PLAYERS, 1f, 1f)
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
                ItemHandlerHelper.giveItemToPlayer(player, ItemStack(ModItems.CLAYMORE_MINE.get()))
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide())
    }

    override fun tick() {
        super.tick()
        val level = this.level()
        val x = this.x
        val y = this.y
        val z = this.z

        if (this.tickCount >= 12000) {
            if (!this.level().isClientSide()) this.discard()
        }

        if (this.tickCount >= 40) {
            val center =
                Vec3(x + 1.5 * this.lookAngle.x, y + 1.5 * this.lookAngle.y, z + 1.5 * this.lookAngle.z)
            for (target in level.getEntitiesOfClass(
                Entity::class.java,
                AABB(center, center).inflate(2.5 / 2.0),
            ) { true }) {
                val condition = this.owner !== target
                        && (target is LivingEntity || target is VehicleEntity)
                        && target !is TargetEntity
                        && !(target is Player && (target.isCreative || target.isSpectator))
                        && !target.isShiftKeyDown
                        && if (ExplosionConfig.FRIENDLY_MINES.get()) {
                    if (owner == null) true else owner != target && !owner!!.isAlliedTo(target)
                } else {
                    (owner != null && owner != target && !owner!!.isAlliedTo(target)) || target.team == null || enabledTDM(target)
                }
                if (!condition) continue

                ParticleTool.spawnMediumExplosionParticles(this.level(), this.position())
                this.discard()

                queueServerWork(1) {
                    if (!level.isClientSide()) {
                        triggerExplode()
                    }
                }
                break
            }
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
            destroy()
        }

        this.refreshDimensions()
    }

    fun destroy() {
        if (level() is ServerLevel) {
            val attacker = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_ATTACKER_UUID))

            CustomExplosion.Builder(attacker ?: this)
                .damage(ExplosionConfig.CLAYMORE_EXPLOSION_DAMAGE.get().toFloat() / 5)
                .radius(ExplosionConfig.CLAYMORE_EXPLOSION_RADIUS.get().toFloat())
                .position(this.position())
                .withParticleType(ParticleTool.ParticleType.MEDIUM)
                .explode()

            this.discard()
        }
    }

    private fun triggerExplode() {
        CustomExplosion.Builder(this)
            .attacker(this.owner)
            .damage(ExplosionConfig.CLAYMORE_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ExplosionConfig.CLAYMORE_EXPLOSION_RADIUS.get().toFloat())
            .withParticleType(ParticleTool.ParticleType.MEDIUM)
            .explode()
    }

    override fun isPushable(): Boolean {
        return true
    }

    companion object {
        @JvmField
        protected val OWNER_UUID: EntityDataAccessor<Optional<UUID>> = SynchedEntityData.defineId(
            ClaymoreEntity::class.java,
            EntityDataSerializers.OPTIONAL_UUID
        )

        @JvmField
        protected val LAST_ATTACKER_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(ClaymoreEntity::class.java, EntityDataSerializers.STRING)

        @JvmField
        val HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ClaymoreEntity::class.java, EntityDataSerializers.FLOAT)

        private val DAMAGE_MODIFIER = createDefaultModifier()
            .multiply(0.2f, ModDamageTypes.CUSTOM_EXPLOSION)
            .multiply(0.2f, ModDamageTypes.MINE)
            .multiply(0.2f, ModDamageTypes.PROJECTILE_EXPLOSION)
    }
}