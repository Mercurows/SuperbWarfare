package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.IVehicleInteract
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.*
import net.minecraft.world.entity.decoration.HangingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraftforge.entity.PartEntity
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

open class TowlineItem : Item(Properties().stacksTo(1)), IVehicleInteract {

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        val tag = stack.tag
        val target = tag?.getString(TAG_TOW_TARGET)
        if (!target.isNullOrBlank()) {
            tooltip.add(
                Component.translatable("des.superbwarfare.towline.target_selected")
                    .withStyle(ChatFormatting.GOLD)
            )
        } else {
            tooltip.add(
                Component.translatable("des.superbwarfare.towline.hint")
                    .withStyle(ChatFormatting.GRAY)
            )
        }
    }

    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult? {
        if (player.level().isClientSide) return InteractionResult.SUCCESS

        val tag = stack.getOrCreateTag()
        val existingTarget = tag.getString(TAG_TOW_TARGET)

        // Shift+right-click: clear towing / clear stored target
        if (player.isShiftKeyDown) {
            val towedBy = vehicle.towedByUUID
            val towing = vehicle.towingUUID

            if (towedBy.isNotBlank() || towing.isNotBlank()) {
                vehicle.clearTowingInfo()

                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.towline.unlinked")
                        .withStyle(ChatFormatting.YELLOW),
                    true
                )
                player.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f)
            }

            // Also clear stored target if present
            if (existingTarget.isNotBlank()) {
                clearTowTargetTag(stack)
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.towline.selection_cleared")
                        .withStyle(ChatFormatting.GRAY),
                    true
                )
            }

            return InteractionResult.SUCCESS
        }

        if (existingTarget.isBlank()) {
            // First click: select the towing vehicle
            tag.putString(TAG_TOW_TARGET, vehicle.stringUUID)
            player.displayClientMessage(
                Component.translatable(
                    "tips.superbwarfare.towline.select_towing",
                    vehicle.displayName
                ).withStyle(ChatFormatting.GREEN),
                true
            )
            player.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f)
            return InteractionResult.SUCCESS
        }

        // Second click: link the stored vehicle with this vehicle
        return linkTowTarget(stack, player, vehicle, existingTarget)
    }

    /**
     * Handle right-click on a living entity as the towed target.
     * Only works when a towing vehicle has already been selected (first click on a vehicle).
     */
    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        interactionTarget: LivingEntity,
        hand: InteractionHand
    ): InteractionResult {
        if (player.level().isClientSide) return InteractionResult.SUCCESS

        val tag = stack.getOrCreateTag()
        val existingTarget = tag.getString(TAG_TOW_TARGET)

        // First click must be on a vehicle; ignore if no vehicle stored yet
        if (existingTarget.isBlank()) return InteractionResult.PASS

        // Exclude creative and spectator players from being towed
        if (interactionTarget is Player && (interactionTarget.isCreative || interactionTarget.isSpectator)) {
            return InteractionResult.PASS
        }

        // Shift+right-click on living entity: clear towing relationship / stored target
        if (player.isShiftKeyDown) {
            // First check if this entity is being towed by a vehicle
            val towedByUUID = interactionTarget.persistentData.getString(TOWED_BY_TAG_KEY)
            if (towedByUUID.isNotBlank()) {
                val towingVehicle = EntityFindUtil.findEntity(interactionTarget.level(), towedByUUID) as? VehicleEntity
                towingVehicle?.clearTowingInfo()
                interactionTarget.persistentData.remove(TOWED_BY_TAG_KEY)

                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.towline.unlinked")
                        .withStyle(ChatFormatting.YELLOW),
                    true
                )
                player.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f)
                return InteractionResult.SUCCESS
            }

            // Also clear stored target if present
            if (existingTarget.isNotBlank()) {
                clearTowTargetTag(stack)
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.towline.selection_cleared")
                        .withStyle(ChatFormatting.GRAY),
                    true
                )
            }
            return InteractionResult.SUCCESS
        }

        return linkTowTarget(stack, player, interactionTarget, existingTarget)
    }

    /**
     * Link the stored towing vehicle with the given target entity as the towed entity.
     */
    fun linkTowTarget(
        stack: ItemStack,
        player: Player,
        targetEntity: Entity,
        existingTarget: String
    ): InteractionResult {
        val firstVehicle = EntityFindUtil.findEntity(targetEntity.level(), existingTarget) as? VehicleEntity

        if (firstVehicle == null) {
            clearTowTargetTag(stack)
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.towline.target_lost")
                    .withStyle(ChatFormatting.RED),
                true
            )
            return InteractionResult.FAIL
        }

        if (firstVehicle === targetEntity) {
            clearTowTargetTag(stack)
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.towline.same_entity")
                    .withStyle(ChatFormatting.RED),
                true
            )
            return InteractionResult.FAIL
        }

        // Check if the towing vehicle is being towed (can't tow while being towed)
        if (firstVehicle.towedByUUID.isNotBlank()) {
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.towline.already_linked")
                    .withStyle(ChatFormatting.RED),
                true
            )
            clearTowTargetTag(stack)
            return InteractionResult.FAIL
        }

        // If the target is a vehicle, check if it's already being towed
        if (targetEntity is VehicleEntity) {
            if (targetEntity.towedByUUID.isNotBlank()) {
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.towline.already_linked")
                        .withStyle(ChatFormatting.RED),
                    true
                )
                clearTowTargetTag(stack)
                return InteractionResult.FAIL
            }
        }

        // Distance check
        val dist = targetEntity.distanceTo(firstVehicle)
        val maxDist = VehicleConfig.TOW_MAX_DISTANCE.get().toDouble()
        if (dist > maxDist) {
            player.displayClientMessage(
                Component.translatable(
                    "tips.superbwarfare.towline.too_far",
                    String.format("%.1f", dist),
                    maxDist.toInt()
                ).withStyle(ChatFormatting.RED),
                true
            )
            clearTowTargetTag(stack)
            return InteractionResult.FAIL
        }

        // Link: firstVehicle tows targetEntity
        val currentList = firstVehicle.towingUUIDs
        currentList.add(targetEntity.stringUUID)
        firstVehicle.towingUUIDs = currentList
        if (targetEntity is VehicleEntity) {
            targetEntity.towedByUUID = firstVehicle.stringUUID
        } else {
            // For non-vehicle entities, store the towing vehicle's UUID in persistent data
            targetEntity.persistentData.putString(TOWED_BY_TAG_KEY, firstVehicle.stringUUID)
        }
        clearTowTargetTag(stack)

        player.displayClientMessage(
            Component.translatable(
                "tips.superbwarfare.towline.linked",
                firstVehicle.displayName,
                targetEntity.displayName
            ).withStyle(ChatFormatting.GREEN),
            true
        )
        player.playSound(SoundEvents.CHAIN_PLACE, 1.0f, 1.0f)

        return InteractionResult.SUCCESS
    }

    private fun clearTowTargetTag(stack: ItemStack) {
        val tag = stack.tag ?: return
        tag.remove(TAG_TOW_TARGET)
        if (tag.isEmpty) {
            stack.tag = null
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    companion object {
        private const val TAG_TOW_TARGET = "TowTarget"
        const val TOWED_BY_TAG_KEY = "TowedByUUID"

        @SubscribeEvent
        fun onEntityInteract(event: PlayerInteractEvent.EntityInteract) {
            val player = event.entity
            val stack = event.itemStack
            val originalTarget = event.target
            val target = if (originalTarget is PartEntity<*>) originalTarget.parent else originalTarget

            if (player.level().isClientSide) return

            val item = stack.item as? TowlineItem ?: return
            if (target is VehicleEntity) return // Let onInteractVehicle handle
            if (target is LivingEntity) return  // Let interactLivingEntity handle
            if (target is Display
                || target is HangingEntity
                || target is AreaEffectCloud
                || target is LightningBolt
            ) return
            if (VehicleConfig.inConfigList(target.type, VehicleConfig.TOW_BLACK_LIST.get())) return

            // Shift+right-click on non-vehicle, non-living entity: clear towing relationship
            if (player.isShiftKeyDown) {
                val towedByUUID = target.persistentData.getString(TOWED_BY_TAG_KEY)
                if (towedByUUID.isNotBlank()) {
                    val towingVehicle = EntityFindUtil.findEntity(target.level(), towedByUUID) as? VehicleEntity
                    towingVehicle?.clearTowingInfo()
                    target.persistentData.remove(TOWED_BY_TAG_KEY)

                    event.isCanceled = true
                    player.displayClientMessage(
                        Component.translatable("tips.superbwarfare.towline.unlinked")
                            .withStyle(ChatFormatting.YELLOW),
                        true
                    )
                    player.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f)
                }
                return
            }

            val tag = stack.getOrCreateTag()
            val existingTarget = tag.getString(TAG_TOW_TARGET)
            if (existingTarget.isBlank()) return

            item.linkTowTarget(stack, player, target, existingTarget)
            event.isCanceled = true
        }
    }
}
