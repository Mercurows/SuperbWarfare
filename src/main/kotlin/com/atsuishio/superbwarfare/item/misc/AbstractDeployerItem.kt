package com.atsuishio.superbwarfare.item.misc

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.Shapes

abstract class AbstractDeployerItem(properties: Properties) : Item(properties) {
    abstract fun spawnDeployedEntity(level: Level, player: Player): Entity

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        if (level !is ServerLevel) {
            return InteractionResult.SUCCESS
        } else {
            val stack = context.itemInHand
            val clickedPos = context.clickedPos
            val direction = context.clickedFace
            val player = context.player ?: return InteractionResult.PASS

            val blockstate = level.getBlockState(clickedPos)
            val pos = if (blockstate.getCollisionShape(level, clickedPos).isEmpty) {
                clickedPos
            } else {
                clickedPos.relative(direction)
            }

            val entity = this.spawnDeployedEntity(level, player)
            entity.setPos(pos.x.toDouble() + 0.5, (pos.y + 1).toDouble(), pos.z.toDouble() + 0.5)
            val yOffset =
                this.getYOffset(level, pos, clickedPos != pos && direction == Direction.UP, entity.boundingBox)
            entity.moveTo(pos.x.toDouble() + 0.5, pos.y + yOffset, pos.z.toDouble() + 0.5)
            level.addFreshEntity(entity)

            if (!player.abilities.instabuild) {
                stack.shrink(1)
            }
            level.gameEvent(player, GameEvent.ENTITY_PLACE, clickedPos)

            return InteractionResult.CONSUME
        }
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemstack = player.getItemInHand(hand)
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY)
        if (hitResult.type != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack)
        } else if (level !is ServerLevel) {
            return InteractionResultHolder.success(itemstack)
        } else {
            val blockpos = hitResult.blockPos
            if (level.getBlockState(blockpos).block !is LiquidBlock) {
                return InteractionResultHolder.pass(itemstack)
            } else if (level.mayInteract(player, blockpos)
                && player.mayUseItemAt(blockpos, hitResult.direction, itemstack)
            ) {
                val entity = this.spawnDeployedEntity(level, player)
                entity.setPos(
                    blockpos.x.toDouble() + 0.5,
                    blockpos.y.toDouble(),
                    blockpos.z.toDouble() + 0.5
                )
                level.addFreshEntity(entity)

                if (!player.abilities.instabuild) {
                    itemstack.shrink(1)
                }

                player.awardStat(Stats.ITEM_USED.get(this))
                level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position())
                return InteractionResultHolder.consume(itemstack)
            } else {
                return InteractionResultHolder.fail(itemstack)
            }
        }
    }

    fun getYOffset(pLevel: LevelReader, pPos: BlockPos, pShouldOffsetYMore: Boolean, pBox: AABB): Double {
        var aabb = AABB(pPos)
        if (pShouldOffsetYMore) {
            aabb = aabb.expandTowards(0.0, -1.0, 0.0)
        }

        val iterable = pLevel.getCollisions(null, aabb)
        return 1 + Shapes.collide(
            Direction.Axis.Y, pBox, iterable,
            (if (pShouldOffsetYMore) -2 else -1).toDouble()
        )
    }
}