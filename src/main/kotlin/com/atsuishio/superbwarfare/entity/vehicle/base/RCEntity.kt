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
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.items.ItemHandlerHelper

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

    override fun onAddedToWorld() {
        super.onAddedToWorld()
        val level = level() as? ServerLevel ?: return
        RemoteBindingData.get(level.server).recordVehicleLocation(this)
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        val monitor = stack.item as? MonitorItem
        if (monitor != null) return monitor.bindTo(this, player)

        val result = interactMount(player, hand)
        if (result != InteractionResult.PASS) return result

        if (!locked && hand == InteractionHand.MAIN_HAND && stack.isEmpty && hasMenu() &&
            isAvailableForRemoteControl() && !player.isSpectator && player.vehicle !== this) {
            if (!level().isClientSide) openMenu(player)
            return InteractionResult.sidedSuccess(level().isClientSide)
        }

        // Retain the normal service and retrieval interactions without creating a physical rider.
        return if (player.isShiftKeyDown || locked) super.interact(player, hand) else InteractionResult.PASS
    }

    open fun interactMount(player: Player, hand: InteractionHand): InteractionResult {
        if (locked || !isAvailableForRemoteControl() || player.isSpectator || player.vehicle === this) return InteractionResult.PASS

        val stack = player.getItemInHand(hand)
        val occupied = mounts.occupiedSlotIds()
        if (stack.isEmpty && hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown && occupied.isNotEmpty()) {
            if (!level().isClientSide) {
                val removed = mounts.uninstall(occupied.first())
                if (removed.isEmpty) return InteractionResult.PASS
                ItemHandlerHelper.giveItemToPlayer(player, removed)
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }

        if (stack.isEmpty || occupied.isNotEmpty()) return InteractionResult.PASS
        val slot = mounts.slots().firstOrNull { mounts.previewInstallation(it.id, stack) != null } ?: return InteractionResult.PASS
        if (!level().isClientSide && !mounts.install(slot.id, stack, !player.isCreative)) return InteractionResult.PASS
        return InteractionResult.sidedSuccess(level().isClientSide)
    }

    override fun canAddPassenger(pPassenger: Entity): Boolean =
        super.canAddPassenger(pPassenger) && (level().isClientSide || RemoteControl.isMounting(pPassenger, this))

    override fun removePassenger(pPassenger: Entity) {
        super.removePassenger(pPassenger)
        if (!entityData.get(MOUNT_STATE).isEmpty) mounts.stopGuns()
    }

    override fun changeSeat(entity: Entity, index: Int): Boolean = false

    override fun hidePassenger(index: Int): Boolean = index == 0 || super.hidePassenger(index)

    override fun canAbsorbPassengerDamage(passenger: Entity): Boolean = false

    override fun banHand(entity: LivingEntity?): Boolean = getSeatIndex(entity) == 0 || super.banHand(entity)

    override fun allowEjection(seatIndex: Int): Boolean = false

    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3 = passenger.position()

    override fun beforeVehicleDestruction() {
        if (!level().isClientSide) {
            RemoteControl.endSession(this)
            clearRemoteBinding()
            dropDestroyedInventory()
        }
        super.beforeVehicleDestruction()
    }

    override fun remove(reason: RemovalReason) {
        if (!level().isClientSide) {
            RemoteControl.endSession(this)
            if (reason.shouldDestroy()) clearRemoteBinding()
            if (reason == RemovalReason.KILLED) dropDestroyedInventory()
        }
        super.remove(reason)
    }

    override fun shouldDropInventoryOnRemoval(reason: RemovalReason): Boolean = false

    open fun dropDestroyedInventory() {
        if (level() !is ServerLevel) return
        for (index in 0 until inventory.slots) {
            val stack = inventory.getStackInSlot(index)
            if (stack.isEmpty) continue
            inventory.setStackInSlot(index, ItemStack.EMPTY)
            spawnAtLocation(stack, 0.5f)
        }
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
