package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.SyncConfig
import com.atsuishio.superbwarfare.entity.IBvrSyncableEntity
import com.atsuishio.superbwarfare.init.ModSerializers
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.network.message.receive.EntityRelationSyncMessage
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.ServerSyncedEntityHandler
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.entity.IEntityAdditionalSpawnData

abstract class MissileProjectile : DestroyableProjectile, ITrackableProjectile, IEntityAdditionalSpawnData, IBvrSyncableEntity {
    override fun getTargetPos(): Vec3? {
        val v = entityData.get(TARGET_POS)
        return if (v == Vec3.ZERO) null else v
    }
    override fun setTargetPos(value: Vec3?) {
        entityData.set(TARGET_POS, value ?: Vec3.ZERO)
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

    var lostTargetTick = 0

    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, pLevel: Level) : super(pEntityType, pLevel)

    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, pShooter: Entity?, pLevel: Level) :
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
            setTargetPos(targetPos)
        }
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        this.entityData.define(TARGET_UUID, "none")
        this.entityData.define(TARGET_POS, Vec3.ZERO)
    }

    /**
     * Direct lightweight BVR serialization for guided missiles.
     *
     * Writes only spatial coordinates, target vectors, and orientation without triggering
     * full throwable projectile NBT saves.
     *
     * @param tag destination compound tag.
     */
    override fun buildBvrSyncNbt(tag: CompoundTag) {
        val encodeId = this.encodeId ?: return
        tag.putString("id", encodeId)
        tag.putInt("EntityId", this.id)
        tag.putDouble("PosX", x)
        tag.putDouble("PosY", y)
        tag.putDouble("PosZ", z)
        tag.putDouble("MotionX", deltaMovement.x)
        tag.putDouble("MotionY", deltaMovement.y)
        tag.putDouble("MotionZ", deltaMovement.z)
        tag.putFloat("Yaw", yRot)
        tag.putFloat("Pitch", xRot)
        tag.putUUID("UUID", this.uuid)

        tag.putString("TargetUuid", getTargetUUID())
        val tp = getTargetPos()
        if (tp != null) {
            tag.putDouble("TargetPosX", tp.x)
            tag.putDouble("TargetPosY", tp.y)
            tag.putDouble("TargetPosZ", tp.z)
        }
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("TargetUuid")) {
            setTargetUUID(compound.getString("TargetUuid"))
        }
        if (compound.contains("TargetPosX")) {
            setTargetPos(Vec3(
                compound.getDouble("TargetPosX"),
                compound.getDouble("TargetPosY"),
                compound.getDouble("TargetPosZ")
            ))
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putString("TargetUuid", this.getTargetUUID())
        val tp = getTargetPos()
        if (tp != null) {
            compound.putDouble("TargetPosX", tp.x)
            compound.putDouble("TargetPosY", tp.y)
            compound.putDouble("TargetPosZ", tp.z)
        }
    }

    override fun afterHitBlock(result: BlockHitResult) {
        if (this.level() is ServerLevel) {
            destroyBlock(result)
        }
    }

    override fun updateRotation() {
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
        @JvmField
        val TARGET_POS: EntityDataAccessor<Vec3> =
            SynchedEntityData.defineId(MissileProjectile::class.java, ModSerializers.VEC3_SERIALIZER.get())
    }

    override fun tick() {
        super.tick()
        this.distractedByDecoy()
        if (level() is ServerLevel && owner != null && isAlive) {
            val targetEntity = EntityFindUtil.findEntity(level(), getTargetUUID())
            if (targetEntity != null) {
                setTargetPos(targetEntity.position())
            }

            if (this.tickCount % SyncConfig.SYNC_ENTITY_INTERVAL.get() == 0) {
                ServerSyncedEntityHandler.register(this, getTargetPos())

                // 向友方玩家同步自身 ID（轻量级，实体状态数据由 BeyondVisualEntitySyncMessage 统一发送）
                val srv = this.server
                if (srv != null) {
                    val dim = level().dimension().location()
                    val msg = EntityRelationSyncMessage(dim, friendlyIds = listOf(id))
                    for (player in srv.playerList.players) {
                        if (SeekTool.IS_FRIENDLY.test(player, this.owner)) {
                            sendPacketTo(player, msg)
                        }
                    }
                }
            }

            if (lostTargetTick > 100) {
                discard()
                causeExplode(position())
            }
        }
    }

    override fun remove(reason: RemovalReason) {
        if (!level().isClientSide) {
            ServerSyncedEntityHandler.unregister(this)
        }
        super.remove(reason)
    }

    open fun distractedByDecoy() {
        if (this.isDistracted()) return

        val decoy = SeekTool.seekLivingEntities(this, 32.0, 60.0)
            .asSequence()
            .filter { it.type.`is`(ModTags.EntityTypes.DECOY) }
            .toList()

        for (d in decoy) {
            if (Math.random() < 0.25) {
                this.setTargetUUID(d.stringUUID)
                this.setDistracted(true)
                return
            }
        }
    }


    override fun getNoHitTicks(): Int {
        return 3
    }
}
