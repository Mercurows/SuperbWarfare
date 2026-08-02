package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.compat.valkyrienskies.ValkyrienSkiesCompat
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.misc.CatapultShuttleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleMotionUtils
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

open class TowBarItem : Item(Properties().stacksTo(1)), IVehicleInteract {

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
                Component.translatable("des.superbwarfare.tow_bar.target_selected")
                    .withStyle(ChatFormatting.GOLD)
            )
        } else {
            tooltip.add(
                Component.translatable("des.superbwarfare.tow_bar.hint")
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
            if (vehicle.towedByUUID.isNotBlank()) {
                // Check if this vehicle is towed by a shuttle
                val tower = EntityFindUtil.findEntity(vehicle.level(), vehicle.towedByUUID)
                if (tower is CatapultShuttleEntity) {
                    tower.clearTowingInfo()
                } else {
                    vehicle.clearTowingInfo()
                }
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.tow_bar.unlinked")
                        .withStyle(ChatFormatting.YELLOW),
                    true
                )
                player.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f)
            }

            if (existingTarget.isNotBlank()) {
                clearTowTargetTag(stack)
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.tow_bar.selection_cleared")
                        .withStyle(ChatFormatting.GRAY),
                    true
                )
            }

            return InteractionResult.SUCCESS
        }

        if (existingTarget.isBlank()) {
            // No shuttle selected yet, prevent sitting in vehicle
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.tow_bar.select_shuttle_first")
                    .withStyle(ChatFormatting.GRAY),
                true
            )
            return InteractionResult.SUCCESS
        }

        // Second click: link the stored shuttle with this vehicle
        return linkTowTarget(stack, player, vehicle, existingTarget)
    }

    /**
     * Handle right-click on a living entity as the towed target.
     * Only works when a shuttle has already been selected.
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

        // Shift+right-click on living entity: clear towing relationship / stored target
        if (player.isShiftKeyDown) {
            val towedByShuttle =
                interactionTarget.persistentData.getString(CatapultShuttleEntity.TOWED_BY_SHUTTLE_TAG_KEY)
            if (towedByShuttle.isNotBlank()) {
                val shuttle =
                    EntityFindUtil.findEntity(interactionTarget.level(), towedByShuttle) as? CatapultShuttleEntity
                shuttle?.clearTowingInfo()
                interactionTarget.persistentData.remove(CatapultShuttleEntity.TOWED_BY_SHUTTLE_TAG_KEY)

                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.tow_bar.unlinked")
                        .withStyle(ChatFormatting.YELLOW),
                    true
                )
                player.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f)
                return InteractionResult.SUCCESS
            }

            if (existingTarget.isNotBlank()) {
                clearTowTargetTag(stack)
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.tow_bar.selection_cleared")
                        .withStyle(ChatFormatting.GRAY),
                    true
                )
            }
            return InteractionResult.SUCCESS
        }

        // First click must be on a shuttle; show hint if no shuttle selected
        if (existingTarget.isBlank()) {
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.tow_bar.select_shuttle_first")
                    .withStyle(ChatFormatting.GRAY),
                true
            )
            return InteractionResult.SUCCESS
        }

        // Exclude creative and spectator players from being towed
        if (interactionTarget is Player && (interactionTarget.isCreative || interactionTarget.isSpectator)) {
            return InteractionResult.PASS
        }

        return linkTowTarget(stack, player, interactionTarget, existingTarget)
    }

    /**
     * Link the stored shuttle with the given target entity as the towed entity.
     */
    fun linkTowTarget(
        stack: ItemStack,
        player: Player,
        targetEntity: Entity,
        existingTarget: String
    ): InteractionResult {
        val shuttle = EntityFindUtil.findEntity(targetEntity.level(), existingTarget) as? CatapultShuttleEntity

        if (shuttle == null) {
            clearTowTargetTag(stack)
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.tow_bar.target_lost")
                    .withStyle(ChatFormatting.RED),
                true
            )
            return InteractionResult.FAIL
        }

        if (shuttle === targetEntity) {
            clearTowTargetTag(stack)
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.tow_bar.same_entity")
                    .withStyle(ChatFormatting.RED),
                true
            )
            return InteractionResult.FAIL
        }

        // Check if the shuttle is already towing something
        if (shuttle.towingUUID.isNotBlank()) {
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.tow_bar.already_linked")
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
                    Component.translatable("tips.superbwarfare.tow_bar.already_linked")
                        .withStyle(ChatFormatting.RED),
                    true
                )
                clearTowTargetTag(stack)
                return InteractionResult.FAIL
            }
        } else {
            // Check if target entity is already being towed
            val towedBy = targetEntity.persistentData.getString(CatapultShuttleEntity.TOWED_BY_SHUTTLE_TAG_KEY)
            if (towedBy.isNotBlank()) {
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.tow_bar.already_linked")
                        .withStyle(ChatFormatting.RED),
                    true
                )
                clearTowTargetTag(stack)
                return InteractionResult.FAIL
            }
        }

        // Distance check — use world-space shuttle position for VS ship compatibility
        val shuttleWorldPos = if (ValkyrienSkiesCompat.hasMod())
            ValkyrienSkiesCompat.toWorldSpace(shuttle)
        else shuttle.position()

        // Don't allow connecting if target is in front of the shuttle
        val worldLookAngle = if (ValkyrienSkiesCompat.hasMod())
            ValkyrienSkiesCompat.toWorldDirection(shuttle, shuttle.lookAngle)
        else shuttle.lookAngle
        if (shuttleWorldPos.vectorTo(targetEntity.position()).dot(worldLookAngle) > 0) {
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.tow_bar.target_in_front")
                    .withStyle(ChatFormatting.RED),
                true
            )
            clearTowTargetTag(stack)
            return InteractionResult.FAIL
        }

        val dist = targetEntity.position().distanceTo(shuttleWorldPos)
        val longestSide = VehicleMotionUtils.calculateLongestSide(targetEntity)

        val maxDist = VehicleConfig.TOW_BAR_EXTRA_LENGTH.get().toDouble() + 1.5 + longestSide
        if (dist > maxDist) {
            player.displayClientMessage(
                Component.translatable(
                    "tips.superbwarfare.tow_bar.too_far",
                    String.format("%.1f", dist),
                    maxDist.toInt()
                ).withStyle(ChatFormatting.RED),
                true
            )
            clearTowTargetTag(stack)
            return InteractionResult.FAIL
        }

        // Link: shuttle tows targetEntity
        shuttle.towingUUID = targetEntity.stringUUID
        if (targetEntity is VehicleEntity) {
            targetEntity.towedByUUID = shuttle.stringUUID
        } else {
            // For non-vehicle entities, store the shuttle's UUID in persistent data
            targetEntity.persistentData.putString(CatapultShuttleEntity.TOWED_BY_SHUTTLE_TAG_KEY, shuttle.stringUUID)
        }
        clearTowTargetTag(stack)

        player.displayClientMessage(
            Component.translatable(
                "tips.superbwarfare.tow_bar.linked",
                shuttle.displayName,
                targetEntity.displayName
            ).withStyle(ChatFormatting.GREEN),
            true
        )
        player.playSound(SoundEvents.CHAIN_PLACE, 1.0f, 1.0f)

        return InteractionResult.SUCCESS
    }

    internal fun clearTowTargetTag(stack: ItemStack) {
        val tag = stack.tag ?: return
        tag.remove(TAG_TOW_TARGET)
        if (tag.isEmpty) {
            stack.tag = null
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    companion object {
        private const val TAG_TOW_TARGET = "TowTarget"

        @SubscribeEvent
        fun onTowBarEntityInteract(event: PlayerInteractEvent.EntityInteract) {
            val player = event.entity
            val stack = event.itemStack
            val originalTarget = event.target
            val target = if (originalTarget is PartEntity<*>) originalTarget.parent else originalTarget

            if (player.level().isClientSide) return

            val item = stack.item as? TowBarItem ?: return

            // --- Shift+right-click on CatapultShuttleEntity: clear towing relationship ---
            if (target is CatapultShuttleEntity && player.isShiftKeyDown) {
                if (target.towingUUID.isNotBlank()) {
                    target.clearTowingInfo()
                    player.displayClientMessage(
                        Component.translatable("tips.superbwarfare.tow_bar.unlinked")
                            .withStyle(ChatFormatting.YELLOW),
                        true
                    )
                    player.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f)
                    event.isCanceled = true
                }

                val tag = stack.tag
                if (tag != null && tag.getString(TAG_TOW_TARGET).isNotBlank()) {
                    item.clearTowTargetTag(stack)
                    player.displayClientMessage(
                        Component.translatable("tips.superbwarfare.tow_bar.selection_cleared")
                            .withStyle(ChatFormatting.GRAY),
                        true
                    )
                }
                return
            }

            // --- First click on CatapultShuttleEntity: select it ---
            if (target is CatapultShuttleEntity) {
                val tag = stack.getOrCreateTag()
                val existingTarget = tag.getString(TAG_TOW_TARGET)

                if (existingTarget.isBlank()) {
                    tag.putString(TAG_TOW_TARGET, target.stringUUID)
                    player.displayClientMessage(
                        Component.translatable(
                            "tips.superbwarfare.tow_bar.select_shuttle",
                            target.displayName
                        ).withStyle(ChatFormatting.GREEN),
                        true
                    )
                    player.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f)
                    event.isCanceled = true
                }
                return
            }

            // --- VehicleEntity and LivingEntity are handled by onInteractVehicle / interactLivingEntity ---
            if (target is VehicleEntity) return
            if (target is LivingEntity) return

            // Exclude certain entity types
            if (target is Display
                || target is HangingEntity
                || target is AreaEffectCloud
                || target is LightningBolt
            ) return
            if (VehicleConfig.inConfigList(target.type, VehicleConfig.TOW_BLACK_LIST.get())) return

            // --- Shift+right-click on non-vehicle, non-living entity: clear towing relationship ---
            if (player.isShiftKeyDown) {
                val towedByShuttle = target.persistentData.getString(CatapultShuttleEntity.TOWED_BY_SHUTTLE_TAG_KEY)
                if (towedByShuttle.isNotBlank()) {
                    val shuttle = EntityFindUtil.findEntity(target.level(), towedByShuttle) as? CatapultShuttleEntity
                    shuttle?.clearTowingInfo()
                    target.persistentData.remove(CatapultShuttleEntity.TOWED_BY_SHUTTLE_TAG_KEY)

                    event.isCanceled = true
                    player.displayClientMessage(
                        Component.translatable("tips.superbwarfare.tow_bar.unlinked")
                            .withStyle(ChatFormatting.YELLOW),
                        true
                    )
                    player.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f)
                    return
                }

                val tag = stack.tag
                if (tag != null && tag.getString(TAG_TOW_TARGET).isNotBlank()) {
                    item.clearTowTargetTag(stack)
                    player.displayClientMessage(
                        Component.translatable("tips.superbwarfare.tow_bar.selection_cleared")
                            .withStyle(ChatFormatting.GRAY),
                        true
                    )
                }
                return
            }

            // --- Right-click on other entity: link with stored shuttle ---
            val tag = stack.getOrCreateTag()
            val existingTarget = tag.getString(TAG_TOW_TARGET)
            if (existingTarget.isBlank()) {
                event.isCanceled = true
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.tow_bar.select_shuttle_first")
                        .withStyle(ChatFormatting.GRAY),
                    true
                )
                return
            }

            item.linkTowTarget(stack, player, target, existingTarget)
            event.isCanceled = true
        }
    }
}
