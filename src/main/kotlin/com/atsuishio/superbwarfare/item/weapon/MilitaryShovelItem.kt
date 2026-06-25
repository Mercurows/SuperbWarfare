package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.renderer.item.MilitaryShovelRenderer
import com.atsuishio.superbwarfare.tiers.ModItemTier
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.ChatFormatting
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.*
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentCategory
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import net.minecraftforge.common.TierSortingRegistry
import net.minecraftforge.common.ToolAction
import net.minecraftforge.common.ToolActions
import java.util.function.Consumer

open class MilitaryShovelItem :
    AxeItem(
        ModItemTier.CEMENTED_CARBIDE, 2f, -2.6f,
        Properties().rarity(Rarity.RARE).durability(810)
    ) {

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.military_shovel").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float {
        val speed = if (state.`is`(BlockTags.MINEABLE_WITH_SHOVEL)
            || state.`is`(BlockTags.MINEABLE_WITH_AXE)
            || state.`is`(BlockTags.MINEABLE_WITH_HOE)
        ) this.speed else 1f
        return speed * (if (state.`is`(Blocks.COBWEB)) 3 else 1)
    }

    @Deprecated("Deprecated in Java")
    override fun isCorrectToolForDrops(state: BlockState): Boolean {
        return state.`is`(Blocks.COBWEB) || state.`is`(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.`is`(BlockTags.MINEABLE_WITH_AXE) || state.`is`(BlockTags.MINEABLE_WITH_HOE)
                && TierSortingRegistry.isCorrectTierForDrops(tier, state)
    }

    @Suppress("DEPRECATION")
    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState): Boolean {
        return this.isCorrectToolForDrops(state)
    }

    override fun canPerformAction(stack: ItemStack, action: ToolAction): Boolean {
        return TOOL_ACTIONS.contains(action)
    }

    /**
     * Code Based on Mekanism-Tools
     */
    override fun useOn(context: UseOnContext): InteractionResult {
        val axeResult = super.useOn(context)
        if (axeResult != InteractionResult.PASS) {
            return axeResult
        }

        val level = context.level
        val blockpos = context.clickedPos
        val player = context.player ?: return InteractionResult.PASS

        val blockstate = level.getBlockState(blockpos)
        var resultToSet: BlockState? = null

        if (player.isShiftKeyDown) {
            val hoeRes = level.getBlockState(blockpos).getToolModifiedState(context, ToolActions.HOE_TILL, false)
                ?: return InteractionResult.PASS

            level.playSound(player, blockpos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f)
            if (!level.isClientSide) {
                HoeItem.changeIntoState(hoeRes).accept(context)
            }
        } else {
            if (context.clickedFace == Direction.DOWN) {
                return InteractionResult.PASS
            }
            val shovelRes = blockstate.getToolModifiedState(context, ToolActions.SHOVEL_FLATTEN, false)
            if (shovelRes != null && level.isEmptyBlock(blockpos.above())) {
                level.playSound(player, blockpos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0f, 1.0f)
                resultToSet = shovelRes
            } else if (blockstate.block is CampfireBlock && blockstate.getValue(CampfireBlock.LIT)) {
                if (!level.isClientSide) {
                    level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, blockpos, 0)
                }
                CampfireBlock.dowse(player, level, blockpos, blockstate)
                resultToSet = blockstate.setValue(CampfireBlock.LIT, false)
            }
            if (resultToSet == null) {
                return InteractionResult.PASS
            }
            if (!level.isClientSide) {
                val stack = context.itemInHand
                if (player is ServerPlayer) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockpos, stack)
                }
                level.setBlock(blockpos, resultToSet, Block.UPDATE_ALL_IMMEDIATE)
                stack.hurtAndBreak(1, player) { it.broadcastBreakEvent(context.hand) }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun canApplyAtEnchantingTable(stack: ItemStack?, enchantment: Enchantment): Boolean {
        return enchantment.category === EnchantmentCategory.BREAKABLE || enchantment.category === EnchantmentCategory.VANISHABLE || enchantment.category === EnchantmentCategory.DIGGER || enchantment.category === EnchantmentCategory.WEAPON
    }

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private var renderer: BlockEntityWithoutLevelRenderer? = null

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                if (renderer == null) {
                    renderer = MilitaryShovelRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                }
                return renderer!!
            }
        })
    }

    companion object {
        private val TOOL_ACTIONS = buildSet {
            addAll(ToolActions.DEFAULT_HOE_ACTIONS)
            addAll(ToolActions.DEFAULT_SHOVEL_ACTIONS)
            addAll(ToolActions.DEFAULT_AXE_ACTIONS)
            add(ToolActions.SWORD_SWEEP)
        }
    }
}
