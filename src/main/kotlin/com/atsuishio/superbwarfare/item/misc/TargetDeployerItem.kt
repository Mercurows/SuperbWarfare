package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.HitResult
import java.util.function.Predicate

class TargetDeployerItem : Item(Properties()) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        if (level !is ServerLevel) {
            return InteractionResult.SUCCESS
        } else {
            val itemstack = context.itemInHand
            val blockpos = context.clickedPos
            val direction = context.clickedFace
            val blockstate = level.getBlockState(blockpos)
            val pos = if (blockstate.getCollisionShape(level, blockpos).isEmpty) {
                blockpos
            } else {
                blockpos.relative(direction)
            }

            // 禁止堆叠
            if (!level.getEntities(
                    null,
                    ModEntities.TARGET.get().getAABB(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5),
                    IS_TARGET
                ).isEmpty()
            ) {
                return InteractionResult.FAIL
            }

            if (ModEntities.TARGET.get().spawn(
                    level,
                    itemstack,
                    context.player,
                    pos,
                    MobSpawnType.SPAWN_EGG,
                    true,
                    blockpos != pos && direction == Direction.UP
                ) != null
            ) {
                itemstack.shrink(1)
                level.gameEvent(context.player, GameEvent.ENTITY_PLACE, blockpos)
            }

            return InteractionResult.CONSUME
        }
    }

    override fun use(pLevel: Level, pPlayer: Player, pHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemstack = pPlayer.getItemInHand(pHand)
        val hitResult = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.SOURCE_ONLY)
        if (hitResult.type != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack)
        } else if (pLevel !is ServerLevel) {
            return InteractionResultHolder.success(itemstack)
        } else {
            val blockpos = hitResult.blockPos
            if (pLevel.getBlockState(blockpos).block !is LiquidBlock) {
                return InteractionResultHolder.pass(itemstack)
            } else if (pLevel.mayInteract(pPlayer, blockpos)
                && pPlayer.mayUseItemAt(blockpos, hitResult.direction, itemstack)
            ) {
                // 禁止堆叠
                if (!pLevel.getEntities(
                        null,
                        ModEntities.TARGET.get().getAABB(blockpos.x + 0.5, blockpos.y + 0.5, blockpos.z + 0.5),
                        IS_TARGET
                    ).isEmpty()
                ) {
                    return InteractionResultHolder.fail(itemstack)
                }

                val entity = ModEntities.TARGET.get()
                    .spawn(pLevel, itemstack, pPlayer, blockpos, MobSpawnType.SPAWN_EGG, false, false)
                if (entity == null) {
                    return InteractionResultHolder.pass(itemstack)
                } else {
                    if (!pPlayer.abilities.instabuild) {
                        itemstack.shrink(1)
                    }

                    pPlayer.awardStat(Stats.ITEM_USED.get(this))
                    pLevel.gameEvent(pPlayer, GameEvent.ENTITY_PLACE, entity.position())
                    return InteractionResultHolder.consume(itemstack)
                }
            } else {
                return InteractionResultHolder.fail(itemstack)
            }
        }
    }

    companion object {
        private val IS_TARGET = Predicate { e: Entity? -> e is TargetEntity }
    }
}
