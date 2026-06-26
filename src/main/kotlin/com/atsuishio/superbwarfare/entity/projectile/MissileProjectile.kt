package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModSerializers
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
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

abstract class MissileProjectile : DestroyableProjectile, ITrackableProjectile, IEntityAdditionalSpawnData {
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
        @JvmField
        val TARGET_POS: EntityDataAccessor<Vec3> =
            SynchedEntityData.defineId(MissileProjectile::class.java, ModSerializers.VEC3_SERIALIZER.get())
    }

    override fun tick() {
        super.tick()
        this.distractedByDecoy()
        // 向服务端同步处理器注册自身
        if (level() is ServerLevel && owner != null) {
            val targetEntity = EntityFindUtil.findEntity(level(), getTargetUUID())
            if (targetEntity != null) {
                setTargetPos(targetEntity.position())
            }
            ServerSyncedEntityHandler.register(this, getTargetPos())
        }
        this.syncPosition()
    }

    override fun remove(reason: net.minecraft.world.entity.Entity.RemovalReason) {
        if (!level().isClientSide) {
            ServerSyncedEntityHandler.unregister(this)
        }
        super.remove(reason)
    }

    open fun distractedByDecoy() {
        if (this.isDistracted()) return

        val decoy = SeekTool.seekLivingEntities(this, 32.0, 90.0)
            .asSequence()
            .filter { it.type.`is`(ModTags.EntityTypes.DECOY) }
            .toList()

        if (decoy.isNotEmpty()) {
            this.setTargetUUID(decoy.first().stringUUID)
            this.setDistracted(true)
        }
    }

    /**
     * 给队友同步友方导弹位置（从 ServerSyncedEntityHandler 查询，避免遍历所有实体）
     */
    open fun syncPosition() {
        val level = this.level()
        if (!MiscConfig.SYNC_ENTITY_OVER_RANGE.get()) return
        if (server != null && server!!.tickCount % MiscConfig.SYNC_ENTITY_INTERVAL.get() != 0) return

        if (level is ServerLevel && owner != null) {
            val dim = level.dimension().location()
            val friendlyMissileList = ServerSyncedEntityHandler.getEntries(dim)
                .asSequence()
                .mapNotNull { entry ->
                    val entity = level.getEntity(entry.entityId) ?: return@mapNotNull null
                    if (entity !is MissileProjectile) return@mapNotNull null
                    if (!SeekTool.IS_FRIENDLY.test(owner, entity)) return@mapNotNull null
                    EntitySyncMessage.SyncedEntity(
                        entry.entityId, entry.entityType, entry.pos, entry.targetPos, entry.nbt, entry.yRot,
                        heightAboveGround = entry.heightAboveGround,
                    )
                }.toList()

            if (friendlyMissileList.isEmpty()) return

            for (player in server!!.playerList.players) {
                if (SeekTool.IS_FRIENDLY.test(player, this.owner)) {
                    sendPacketTo(player, EntitySyncMessage(dim, friendlyMissileList, true))
                }
            }
        }
    }

    override fun getNoHitTicks(): Int {
        return 3
    }
}
