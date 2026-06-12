package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn

abstract class MissileProjectile : DestroyableProjectile, ITrackableProjectile, IEntityWithComplexSpawn {
    private var targetPosValue: Vec3? = null
    override fun getTargetPos(): Vec3? = targetPosValue
    override fun setTargetPos(value: Vec3?) {
        targetPosValue = value
    }

    private var guideTypeValue: Int = 0
    override fun getGuideType(): Int = guideTypeValue
    override fun setGuideType(value: Int) {
        guideTypeValue = value
    }

    private var distractedValue: Boolean = false
    override fun isDistracted(): Boolean = distractedValue
    override fun setDistracted(value: Boolean) {
        distractedValue = value
    }

    private var lostValue: Boolean = false
    override fun isLost(): Boolean = lostValue
    override fun setLost(value: Boolean) {
        lostValue = value
    }

    private var lostTargetValue: Boolean = false
    override fun isLostTarget(): Boolean = lostTargetValue
    override fun setLostTarget(value: Boolean) {
        lostTargetValue = value
    }

    override fun getTargetUUID(): String = entityData.get(TARGET_UUID)
    override fun setTargetUUID(value: String) {
        entityData.set(TARGET_UUID, value)
    }

    constructor(pEntityType: EntityType<out Projectile>, pLevel: Level) : super(pEntityType, pLevel)

    constructor(pEntityType: EntityType<out Projectile>, pShooter: Entity?, pLevel: Level) :
            super(pEntityType, pLevel) {
        this.owner = pShooter
        if (pShooter != null) {
            this.setPos(pShooter.x, pShooter.eyeY - 0.1, pShooter.z)
        }
    }

    fun setTargetUuid(uuid: String) {
        this.setTargetUUID(uuid)
    }

    fun setTargetVec(targetPos: Vec3?) {
        if (targetPos != null) {
            this.targetPosValue = targetPos
        }
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(TARGET_UUID, "none")
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("TargetUuid")) {
            setTargetUUID(compound.getString("TargetUuid"))
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putString("TargetUuid", this.getTargetUUID())
    }

    public override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        if (this.level() is ServerLevel) {
            destroyBlock(result)
        }
    }

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        if (tickCount < 3) return
        val entity = result.entity
        val owner = this.owner
        if (owner != null && owner.vehicle != null && entity == owner.vehicle) return
        if (this.level() is ServerLevel) {
            entity.forceHurt(
                causeProjectileHitDamage(this.level().registryAccess(), this, owner),
                this.damageValue
            )

            if (entity is LivingEntity) {
                entity.invulnerableTime = 0
            }

            causeExplode(result.location)
            this.discard()
        }
    }

    override fun updateRotation() {
    }

    override fun forceLoadChunk(): Boolean {
        return true
    }

    override fun isNoGravity(): Boolean {
        return true
    }

    override fun getCustomGravity(): Float {
        return 0f
    }

    companion object {
        @JvmField
        val TARGET_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(MissileProjectile::class.java, EntityDataSerializers.STRING)
    }

    override fun tick() {
        super.tick()
        // 给队友同步友方导弹位置

        val level = level()
        if (!MiscConfig.SYNC_ENTITY_OVER_RANGE.get()) return
        if (server != null && server!!.tickCount % MiscConfig.SYNC_ENTITY_INTERVAL.get() != 0) return

        if (level is ServerLevel && owner != null) {
            val friendlyMissileList = arrayListOf<EntitySyncMessage.SyncedEntity>()
            val synced = EntitySyncMessage.SyncedEntity(
                id,
                BuiltInRegistries.ENTITY_TYPE.getKey(type),
                position(),
                deltaMovement,
                CompoundTag().also { tag -> this.saveWithoutId(tag) }
            )

            friendlyMissileList.add(synced)

            for (player in server!!.playerList.players) {
                if (SeekTool.IS_FRIENDLY.test(player, this.owner)) {
                    sendPacketTo(player, EntitySyncMessage(level.dimension().location(), friendlyMissileList, true))
                }
            }
        }
    }
}
