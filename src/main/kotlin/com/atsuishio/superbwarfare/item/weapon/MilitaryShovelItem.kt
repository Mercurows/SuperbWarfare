package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.renderer.item.MilitaryShovelRenderer
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.CustomDamageProperty
import com.atsuishio.superbwarfare.tiers.ModItemTier
import com.atsuishio.superbwarfare.tools.mc
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
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
import net.neoforged.neoforge.common.ItemAbilities
import net.neoforged.neoforge.common.ItemAbility

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
    ) {

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

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float {
        val speed = if (state.`is`(ModTags.Blocks.MINEABLE_WITH_MILITARY_SHOVEL)) {
            ModItemTier.CEMENTED_CARBIDE.speed
        } else {
            1f
        }
        return speed * (if (state.`is`(Blocks.COBWEB)) 3f else 1f)
    }

    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState): Boolean {
        return state.`is`(ModTags.Blocks.MINEABLE_WITH_MILITARY_SHOVEL) &&
                !state.`is`(ModItemTier.CEMENTED_CARBIDE.incorrectBlocksForDrops)
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

        // 斧：去皮 / 刮削 / 去蜡（音效已在 getAxeResult 内按原版方式播放）
        getAxeResult(blockstate, context)?.let { return applyModifiedState(context, it) }

        if (player.isShiftKeyDown) {
            // 锄：耕地
            val hoeRes = blockstate.getToolModifiedState(context, ItemAbilities.HOE_TILL, false)
                ?: return InteractionResult.PASS

            level.playSound(player, blockpos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f)
            if (!level.isClientSide) {
                HoeItem.changeIntoState(hoeRes).accept(context)
                context.itemInHand.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.hand))
            }
            return InteractionResult.sidedSuccess(level.isClientSide)
        }

        // 铲：铲平土路 / 熄灭营火
        if (context.clickedFace == Direction.DOWN) {
            return InteractionResult.PASS
        }
        var resultToSet = blockstate.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false)
        if (resultToSet != null && level.isEmptyBlock(blockpos.above())) {
            level.playSound(player, blockpos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F)
        } else {
            resultToSet = blockstate.getToolModifiedState(context, ItemAbilities.SHOVEL_DOUSE, false)
            if (resultToSet != null && !level.isClientSide) {
                level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, blockpos, 0)
            }
        }

        return resultToSet?.let { applyModifiedState(context, it) } ?: InteractionResult.PASS
    }

    /**
     * 真正把方块替换掉，并补上进度触发、游戏事件与耐久消耗。
     *
     * 之前这里是写成 if (resultToSet == null) { ... } 的：斧头分支拿到 resultToSet 后
     * 直接跳过了整段逻辑，所以原木只播了去皮音效、方块根本没变（锄头/铲平正常是因为它们走的是那个分支）。
     */
    private fun applyModifiedState(context: UseOnContext, state: BlockState): InteractionResult {
        val level = context.level
        if (!level.isClientSide) {
            val stack = context.itemInHand
            val player = context.player
            if (player is ServerPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, context.clickedPos, stack)
            }
            level.setBlock(context.clickedPos, state, Block.UPDATE_ALL_IMMEDIATE)
            level.gameEvent(GameEvent.BLOCK_CHANGE, context.clickedPos, GameEvent.Context.of(player, state))
            if (player != null) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.hand))
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

    @EventBusSubscriber
    companion object {
        @SubscribeEvent
        fun registerRenderer(event: RegisterClientExtensionsEvent) {
            event.registerItem(object : IClientItemExtensions {
                private var renderer: BlockEntityWithoutLevelRenderer? = null

                override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                    if (renderer == null) {
                        renderer = MilitaryShovelRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                    }
                    return renderer!!
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
