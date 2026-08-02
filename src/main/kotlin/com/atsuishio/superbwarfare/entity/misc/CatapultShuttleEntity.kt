package com.atsuishio.superbwarfare.entity.misc

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.compat.valkyrienskies.ValkyrienSkiesCompat
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleMotionUtils
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.resource.model.EntityModelReloadListener
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.network.NetworkHooks
import kotlin.math.max

open class CatapultShuttleEntity(type: EntityType<out CatapultShuttleEntity>, world: Level) : Entity(type, world) {

    open val modelInstance = EntityModelReloadListener.getModel(MODEL)?.createInstance()

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> {
        return NetworkHooks.getEntitySpawningPacket(this)
    }

    constructor(level: Level) : this(ModEntities.CATAPULT_SHUTTLE.get(), level)

    override fun canCollideWith(entity: Entity): Boolean {
        return entity is CatapultShuttleEntity
    }

    override fun isPickable(): Boolean {
        return !this.isRemoved
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (player.isShiftKeyDown && player.mainHandItem.isEmpty) {
            if (!this.level().isClientSide) {
                clearTowingInfo()
                this.discard()

                val stack = ItemStack(ModItems.CATAPULT_SHUTTLE.get())
                if (!player.inventory.add(stack)) {
                    val itemEntity = ItemEntity(level(), x, y, z, stack)
                    level().addFreshEntity(itemEntity)
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide)
        }
        return super.interact(player, hand)
    }

    override fun defineSynchedData() {
        entityData.define(TOWING_UUID, "")
    }

    var towingUUID by TOWING_UUID

    val towingEntity: Entity?
        get() {
            if (towingUUID.isBlank()) return null
            return EntityFindUtil.findEntity(level(), towingUUID)
        }

    fun clearTowingInfo() {
        val towed = towingEntity
        if (towed is VehicleEntity) {
            towed.towedByUUID = ""
        } else {
            towed?.persistentData?.remove(TOWED_BY_SHUTTLE_TAG_KEY)
        }
        towingUUID = ""
    }

    override fun tick() {
        super.tick()
        val f = 0.8
        this.deltaMovement = this.deltaMovement.multiply(f, 0.0, f)
        this.move(MoverType.SELF, this.deltaMovement)
        towingTick()
    }

    private fun towingTick() {
        val towed = towingEntity
        if (towed == null) {
            clearTowingInfo()
            return
        }
        val shuttleWorldPos = if (ValkyrienSkiesCompat.hasMod())
            ValkyrienSkiesCompat.toWorldSpace(this)
        else this.position()
        val towedPos = towed.position()

        val dist = shuttleWorldPos.distanceTo(towedPos)
        val longestSide = VehicleMotionUtils.calculateLongestSide(towed)

        val maxDist = VehicleConfig.TOW_BAR_EXTRA_LENGTH.get().toDouble() + 1.5 + longestSide

        val worldLookAngle = if (ValkyrienSkiesCompat.hasMod())
            ValkyrienSkiesCompat.toWorldDirection(this, lookAngle)
        else lookAngle
        if (shuttleWorldPos.vectorTo(towedPos).dot(worldLookAngle) > 0) {
            clearTowingInfo()
            return
        }

        if (dist > 16 + maxDist) {
            clearTowingInfo()
            return
        }

        if (dist <= maxDist) return

        val overshoot = dist - maxDist
        val dir = shuttleWorldPos.subtract(towedPos).reverse().normalize()
        val relVelAlong = towed.deltaMovement.subtract(this.deltaMovement).dot(dir)

        val ropeForce = -overshoot - relVelAlong

        val maxDeltaV = max(2.0, this.deltaMovement.length())
        val pullForce = dir.scale((ropeForce / 2.0).coerceIn(-maxDeltaV, maxDeltaV))

        if (towed is Player && towed.level().isClientSide) {
            towed.deltaMovement = towed.deltaMovement.add(pullForce)
        } else {
            towed.deltaMovement = towed.deltaMovement.add(pullForce)
        }
    }

    public override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putString("TowingUUID", towingUUID)
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        towingUUID = compound.getString("TowingUUID")
    }

    override fun remove(reason: RemovalReason) {
        clearTowingInfo()
        super.remove(reason)
    }

    companion object {
        const val TOWED_BY_SHUTTLE_TAG_KEY = "TowedByShuttle"

        val MODEL = loc("models/bedrock/entity/catapult_shuttle.geo.json")

        @JvmField
        val TOWING_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(CatapultShuttleEntity::class.java, EntityDataSerializers.STRING)
    }
}
