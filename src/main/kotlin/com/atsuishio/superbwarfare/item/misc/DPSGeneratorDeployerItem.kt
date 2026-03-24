package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
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
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.HitResult
import java.util.function.Predicate

class DPSGeneratorDeployerItem : Item(Properties()) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.dps_generator_deployer").withStyle(ChatFormatting.GRAY)
                .withStyle(ChatFormatting.ITALIC)
        )
    }

    override fun useOn(pContext: UseOnContext): InteractionResult {
        val level = pContext.level
        if (level !is ServerLevel) {
            return InteractionResult.SUCCESS
        } else {
            val itemstack = pContext.itemInHand
            val blockpos = pContext.clickedPos
            val direction = pContext.clickedFace
            val blockstate = level.getBlockState(blockpos)
            val pos = if (blockstate.getCollisionShape(level, blockpos).isEmpty) {
                blockpos
            } else {
                blockpos.relative(direction)
            }

            // 禁止堆叠
            if (!level.getEntities(
                    null,
                    ModEntities.DPS_GENERATOR.get().getAABB(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5),
                    IS_GENERATOR
                ).isEmpty()
            ) {
                return InteractionResult.FAIL
            }

            if (ModEntities.DPS_GENERATOR.get().spawn(
                    level,
                    itemstack,
                    pContext.player,
                    pos,
                    MobSpawnType.SPAWN_EGG,
                    true,
                    blockpos != pos && direction == Direction.UP
                ) != null
            ) {
                itemstack.shrink(1)
                level.gameEvent(pContext.player, GameEvent.ENTITY_PLACE, blockpos)
            }

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
            } else if (level.mayInteract(player, blockpos) && player.mayUseItemAt(
                    blockpos,
                    hitResult.direction,
                    itemstack
                )
            ) {
                // 禁止堆叠
                if (!level.getEntities(
                        null,
                        ModEntities.DPS_GENERATOR.get().getAABB(blockpos.x + 0.5, blockpos.y + 0.5, blockpos.z + 0.5),
                        IS_GENERATOR
                    ).isEmpty()
                ) {
                    return InteractionResultHolder.fail(itemstack)
                }

                val entity = ModEntities.DPS_GENERATOR.get()
                    .spawn(level, itemstack, player, blockpos, MobSpawnType.SPAWN_EGG, false, false)
                if (entity == null) {
                    return InteractionResultHolder.pass(itemstack)
                } else {
                    if (!player.abilities.instabuild) {
                        itemstack.shrink(1)
                    }

                    player.awardStat(Stats.ITEM_USED.get(this))
                    level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position())
                    return InteractionResultHolder.consume(itemstack)
                }
            } else {
                return InteractionResultHolder.fail(itemstack)
            }
        }
    }

    companion object {
        private val IS_GENERATOR = Predicate { e: Entity? -> e is DPSGeneratorEntity }
    }
}
