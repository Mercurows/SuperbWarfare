package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.EDDEntity
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3

open class EDDItem : Item(Properties()) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.edd").withStyle(ChatFormatting.GRAY))
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val pos = context.clickedPos
        val direction = context.clickedFace
        val relative = pos.relative(direction)
        val player = context.player
        val stack = context.itemInHand

        if (player != null && !this.mayPlace(player, direction, stack, relative)) {
            return InteractionResult.FAIL
        } else {
            if (direction.axis.isVertical) return InteractionResult.FAIL

            val level = context.level
            val entity = EDDEntity(
                owner = player,
                level = level,
                pos = relative,
                direction = direction,
                corner = this.getCornerFromHit(direction, pos, context.clickLocation)
            )

            if (entity.survives()) {
                if (!level.isClientSide) {
                    entity.playPlacementSound()
                    level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position())
                    level.addFreshEntity(entity)
                }

                stack.shrink(1)
                return InteractionResult.sidedSuccess(level.isClientSide)
            } else {
                return InteractionResult.CONSUME
            }
        }
    }

    open fun mayPlace(
        player: Player,
        direction: Direction,
        stack: ItemStack,
        pos: BlockPos
    ): Boolean {
        return !player.level().isOutsideBuildHeight(pos) && player.mayUseItemAt(pos, direction, stack)
    }

    fun getCornerFromHit(face: Direction, pos: BlockPos, hitVec: Vec3): Int {
        val x = hitVec.x
        val y = hitVec.y
        val z = hitVec.z

        val top = y > pos.y + 0.5
        val left = when (face) {
            Direction.WEST -> z < pos.z + 0.5
            Direction.EAST -> z > pos.z + 0.5
            Direction.SOUTH -> x < pos.x + 0.5
            Direction.NORTH -> x > pos.x + 0.5
            else -> false
        }

        return if (left && top) {
            0
        } else if (left) {
            1
        } else if (!top) {
            2
        } else {
            3
        }
    }
}