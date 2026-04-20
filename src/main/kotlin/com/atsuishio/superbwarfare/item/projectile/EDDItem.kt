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
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

open class EDDItem : Item(Properties()) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.edd").withStyle(ChatFormatting.GRAY))
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
                corner = this.getCornerFromHit(direction, context.clickLocation)
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

    fun getCornerFromHit(face: Direction, hitVec: Vec3): Int {
        val u: Double
        val v: Double

        when (face) {
            Direction.NORTH, Direction.SOUTH -> {
                u = hitVec.x.toInt() - hitVec.x
                v = hitVec.y.toInt() - hitVec.y
            }

            Direction.EAST, Direction.WEST -> {
                u = hitVec.z.toInt() - hitVec.z
                v = hitVec.y.toInt() - hitVec.y
            }

            else -> return 0
        }

        val left = if (face == Direction.WEST) abs(u) < 0.5 else abs(u) > 0.5
        val top = abs(v) < 0.5

        return when {
            left && top -> 0
            left && !top -> 1
            !left && top -> 2
            else -> 3
        }
    }
}