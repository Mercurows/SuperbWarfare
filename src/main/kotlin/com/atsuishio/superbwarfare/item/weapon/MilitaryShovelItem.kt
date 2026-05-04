package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.renderer.item.MilitaryShovelRenderer
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.CustomDamageProperty
import com.atsuishio.superbwarfare.tiers.ModItemTier
import net.minecraft.ChatFormatting
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.*
import net.minecraft.world.item.component.Tool
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
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

open class MilitaryShovelItem :
    DiggerItem(
        ModItemTier.CEMENTED_CARBIDE,
        ModTags.Blocks.MINEABLE_WITH_MILITARY_SHOVEL,
        CustomDamageProperty(810).rarity(Rarity.RARE)
            .component(
                DataComponents.TOOL, Tool(
                    listOf(
                        Tool.Rule.deniesDrops(ModItemTier.CEMENTED_CARBIDE.incorrectBlocksForDrops),
                        Tool.Rule.minesAndDrops(
                            ModTags.Blocks.MINEABLE_WITH_MILITARY_SHOVEL,
                            ModItemTier.CEMENTED_CARBIDE.speed
                        )
                    ),
                    1f, 1
                )
            )
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
        val level = context.level
        val blockpos = context.clickedPos
        val player = context.player ?: return InteractionResult.PASS

        val blockstate = level.getBlockState(blockpos)
        var resultToSet = getAxeResult(blockstate, context)

        if (resultToSet == null) {
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
                val foundResult = blockstate.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false)
                if (foundResult != null && level.isEmptyBlock(blockpos.above())) {
                    level.playSound(player, blockpos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F)
                    resultToSet = foundResult
                } else {
                    resultToSet = blockstate.getToolModifiedState(context, ItemAbilities.SHOVEL_DOUSE, false)
                    if (resultToSet != null && !level.isClientSide) {
                        level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, blockpos, 0)
                    }
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
                    level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, resultToSet))
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.hand))
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    private fun getAxeResult(state: BlockState, context: UseOnContext): BlockState? {
        val level = context.level
        val pos = context.clickedPos
        val player = context.player
        var resultToSet = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false)
        if (resultToSet != null) {
            level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F)
            return resultToSet
        }
        resultToSet = state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false)
        if (resultToSet != null) {
            level.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F)
            level.levelEvent(player, LevelEvent.PARTICLES_SCRAPE, pos, 0)
            return resultToSet
        }
        resultToSet = state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false)
        if (resultToSet != null) {
            level.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F)
            level.levelEvent(player, LevelEvent.PARTICLES_WAX_OFF, pos, 0)
            return resultToSet
        }
        return null
    }

    override fun getEnchantmentValue(): Int {
        return ModItemTier.CEMENTED_CARBIDE.enchantmentValue
    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {}

    override fun getAnimatableInstanceCache() = this.cache

    @EventBusSubscriber
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