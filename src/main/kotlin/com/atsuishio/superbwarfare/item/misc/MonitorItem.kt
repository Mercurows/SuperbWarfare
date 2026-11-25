package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.base.RCEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.remote.RemoteControl
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.world.saveddata.RemoteBindingData
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

@RegistryName("monitor")
open class MonitorItem : Item(Properties().stacksTo(1)) {
    open fun bindTo(vehicle: RCEntity, player: Player): InteractionResult {
        if (player.isSpectator || player.distanceToSqr(vehicle) > 36.0) return InteractionResult.FAIL
        if (player is ServerPlayer) {
            if (!vehicle.canBindRemoteControl(player) || !RemoteBindingData.get(player.server).bind(player, vehicle.uuid)) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.denied"), true)
                return InteractionResult.FAIL
            }
            player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.linked"), true)
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (player is ServerPlayer) {
            if (RemoteControl.endSession(player)) return InteractionResultHolder.consume(stack)
            val bindings = RemoteBindingData.get(player.server)
            if (player.isShiftKeyDown && !player.isPassenger) {
                if (bindings.unbind(player)) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.unlinked"), true)
                }
                return InteractionResultHolder.consume(stack)
            }
            val vehicleId = bindings.getVehicle(player.uuid)
            if (vehicleId == null) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.needs_binding"), true)
                return InteractionResultHolder.consume(stack)
            }
            val target = player.serverLevel().getEntity(vehicleId) as? RCEntity

            if (target == null) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.unavailable"), true)
            } else if (!RemoteControl.startSession(player, target)) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.denied"), true)
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun getAttributeModifiers(slot: EquipmentSlot, stack: ItemStack): Multimap<Attribute, AttributeModifier> {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack)
        return ImmutableMultimap.builder<Attribute, AttributeModifier>()
            .putAll(super.getAttributeModifiers(slot, stack))
            .put(
                Attributes.ATTACK_DAMAGE,
                AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Item modifier", 2.0, AttributeModifier.Operation.ADDITION)
            )
            .build()
    }

    override fun shouldCauseReequipAnimation(oldStack: ItemStack?, newStack: ItemStack?, slotChanged: Boolean) = false

    companion object {
        @JvmStatic
        fun isCarrierMonitor(player: Player, stack: ItemStack): Boolean =
            player.vehicle is VehicleEntity && player.vehicle !is RCEntity && stack.item is MonitorItem
    }
}
