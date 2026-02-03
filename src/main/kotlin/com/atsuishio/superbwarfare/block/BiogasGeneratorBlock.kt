package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.BiogasGeneratorBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

@Suppress("OVERRIDE_DEPRECATION")
open class BiogasGeneratorBlock :
    BaseEntityBlock(Properties.of().sound(SoundType.METAL).strength(3.0f).requiresCorrectToolForDrops()) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: BlockGetter?,
        pTooltip: MutableList<Component>,
        pFlag: TooltipFlag
    ) {
        pTooltip.add(Component.translatable("des.superbwarfare.biogas_generator").withStyle(ChatFormatting.GRAY))
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity? {
        return BiogasGeneratorBlockEntity(pPos, pState)
    }

    override fun <T : BlockEntity?> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T?>
    ): BlockEntityTicker<T?>? {
        if (!pLevel.isClientSide) {
            return createTickerHelper(
                pBlockEntityType, ModBlockEntities.BIOGAS_GENERATOR.get(),
                BiogasGeneratorBlockEntity::serverTick
            )
        }
        return null
    }
}