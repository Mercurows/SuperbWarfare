package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.renderer.item.MilitaryShovelRenderer
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.CustomDamageProperty
import com.atsuishio.superbwarfare.tiers.ModItemTier
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
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
import net.neoforged.neoforge.common.ItemAbilities
import net.neoforged.neoforge.common.ItemAbility
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.*

open class MilitaryShovelItem :
    AxeItem(
        ModItemTier.CEMENTED_CARBIDE,
        CustomDamageProperty(810).rarity(Rarity.RARE)
            .attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, 2f, -2.6f))
    ), GeoItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.military_shovel").withStyle(ChatFormatting.GRAY)
        )
    }

    // TODO
//    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float {
//        val speed = if (state.`is`(BlockTags.MINEABLE_WITH_SHOVEL)
//            || state.`is`(BlockTags.MINEABLE_WITH_AXE)
//            || state.`is`(BlockTags.MINEABLE_WITH_HOE)
//        ) this.speed else 1f
//        return speed * (if (state.`is`(Blocks.COBWEB)) 3 else 1)
//    }
//

    fun isCorrectToolForDrops(state: BlockState): Boolean {
        return state.`is`(Blocks.COBWEB) || state.`is`(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.`is`(BlockTags.MINEABLE_WITH_AXE) || state.`is`(BlockTags.MINEABLE_WITH_HOE)
        // TODO
//                && TierSortingRegistry.isCorrectTierForDrops(tier, state)
    }

    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState): Boolean {
        return this.isCorrectToolForDrops(state)
    }

    override fun canPerformAction(
        stack: ItemStack,
        itemAbility: ItemAbility
    ): Boolean {
        return TOOL_ACTIONS.contains(itemAbility)
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
            val hoeRes = level.getBlockState(blockpos).getToolModifiedState(context, ItemAbilities.HOE_TILL, false)
                ?: return InteractionResult.PASS

            level.playSound(player, blockpos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f)
            if (!level.isClientSide) {
                HoeItem.changeIntoState(hoeRes).accept(context)
            }
        } else {
            if (context.clickedFace == Direction.DOWN) {
                return InteractionResult.PASS
            }
            // TODO shovelRes
//            val shovelRes = blockstate.getToolModifiedState(
//                context,
//                CustomDamageProperty(500).rarity(Rarity.RARE)
//                    .attributes(createAttributes(ModItemTier.STEEL, 5f, -2.6f)).SHOVEL_FLATTEN, false
//            )
            val shovelRes = null

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

                // TODO hurtAndBreak
//                stack.hurtAndBreak(1, player) { it.broadcastBreakEvent(context.hand) }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    // TODO canApplyAtEnchantingTable
//    override fun canApplyAtEnchantingTable(stack: ItemStack?, enchantment: Enchantment): Boolean {
//        return enchantment.category === EnchantmentCategory.BREAKABLE || enchantment.category === EnchantmentCategory.VANISHABLE || enchantment.category === EnchantmentCategory.DIGGER || enchantment.category === EnchantmentCategory.WEAPON
//    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {}

    override fun getAnimatableInstanceCache() = this.cache

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
    companion object {
        @SubscribeEvent
        fun registerRenderer(event: RegisterClientExtensionsEvent) {
            event.registerItem(object : IClientItemExtensions {
                private val renderer: BlockEntityWithoutLevelRenderer = MilitaryShovelRenderer()

                override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                    return renderer
                }
            }, ModItems.MILITARY_SHOVEL.get())
        }

        private val TOOL_ACTIONS = buildSet {
            addAll(ItemAbilities.DEFAULT_HOE_ACTIONS)
            addAll(ItemAbilities.DEFAULT_SHOVEL_ACTIONS)
            addAll(ItemAbilities.DEFAULT_AXE_ACTIONS)
            add(ItemAbilities.SWORD_SWEEP)
        }
    }
}