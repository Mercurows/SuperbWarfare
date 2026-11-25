package com.atsuishio.superbwarfare.entity.vehicle.base

import com.atsuishio.superbwarfare.entity.vehicle.remote.RemoteControl
import com.atsuishio.superbwarfare.item.misc.MonitorItem
import com.atsuishio.superbwarfare.world.saveddata.RemoteBindingData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.util.FakePlayer

open class RCEntity(type: EntityType<*>, level: Level) : VehicleEntity(type, level) {
    open fun canBindRemoteControl(player: ServerPlayer): Boolean {
        if (!isAvailableForRemoteControl() || player is FakePlayer || level() !== player.level()) return false
        val operator = RemoteBindingData.get(player.server).getOperator(uuid)
        return operator == player.uuid || operator == null && !locked
    }

    open fun isAvailableForRemoteControl(): Boolean = !isRemoved && !isWreck && health > 0

    open fun canStartRemoteControl(player: ServerPlayer): Boolean =
        isAvailableForRemoteControl() && maxPassengers > 0 && passengers.isEmpty() &&
                RemoteBindingData.get(player.server).getVehicle(player.uuid) == uuid

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        val monitor = stack.item as? MonitorItem
        if (monitor != null) return monitor.bindTo(this, player)

        // Retain the normal service and retrieval interactions without creating a physical rider.
        return if (player.isShiftKeyDown || locked) super.interact(player, hand) else InteractionResult.PASS
    }

    override fun canAddPassenger(pPassenger: Entity): Boolean =
        super.canAddPassenger(pPassenger) && (level().isClientSide || RemoteControl.isMounting(pPassenger, this))

    override fun changeSeat(entity: Entity, index: Int): Boolean = false

    override fun hidePassenger(index: Int): Boolean = index == 0 || super.hidePassenger(index)

    override fun banHand(entity: LivingEntity?): Boolean = getSeatIndex(entity) == 0 || super.banHand(entity)

    override fun allowEjection(seatIndex: Int): Boolean = false

    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3 = passenger.position()

    override fun destroy() {
        if (!level().isClientSide) {
            RemoteControl.endSession(this)
            clearRemoteBinding()
        }
        super.destroy()
    }

    override fun remove(reason: RemovalReason) {
        if (!level().isClientSide) {
            RemoteControl.endSession(this)
            if (reason.shouldDestroy()) clearRemoteBinding()
        }
        super.remove(reason)
    }

    override fun onRemovedFromWorld() {
        if (removalReason?.shouldDestroy() == true) clearRemoteBinding()
        super.onRemovedFromWorld()
    }

    protected open fun clearRemoteBinding() {
        val level = level() as? ServerLevel ?: return
        RemoteBindingData.get(level.server).removeVehicle(level.server, uuid)
    }
}
