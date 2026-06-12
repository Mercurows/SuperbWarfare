package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.entity.IEntityAdditionalSpawnData
import net.minecraftforge.registries.ForgeRegistries

abstract class MissileProjectile : DestroyableProjectile, ITrackableProjectile, IEntityAdditionalSpawnData {
    override var targetPos: Vec3? = null

    override var guideType: Int = 0

    override var distracted: Boolean = false

    override var lost: Boolean = false

    override var lostTarget: Boolean = false

    override var targetUUID by TARGET_UUID

    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, pLevel: Level) : super(pEntityType, pLevel)

    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, pShooter: Entity?, pLevel: Level) :
            super(pEntityType, pLevel) {
        this.owner = pShooter
        if (pShooter != null) {
            this.setPos(pShooter.x, pShooter.eyeY - 0.1, pShooter.z)
        }
    }

    fun setTargetUuid(uuid: String) {
        this.targetUUID = uuid
    }

    fun setTargetVec(targetPos: Vec3?) {
        if (targetPos != null) {
            this.targetPos = targetPos
        }
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        this.entityData.define(TARGET_UUID, "none")
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("TargetUuid")) {
            targetUUID = compound.getString("TargetUuid")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putString("TargetUuid", this.targetUUID)
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

    override fun getGravity(): Float {
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
                ForgeRegistries.ENTITY_TYPES.getKey(type)!!,
                position(),
                deltaMovement,
                serializeNBT()
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
