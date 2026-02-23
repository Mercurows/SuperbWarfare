package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

open class CreativeSuperbItemInterfaceBlockEntity(pos: BlockPos, blockState: BlockState) :
    SuperbItemInterfaceBlockEntity(ModBlockEntities.CREATIVE_SUPERB_ITEM_INTERFACE.get(), pos, blockState) {
    override val isCreative: Boolean
        get() = true

    companion object {
        fun serverTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: CreativeSuperbItemInterfaceBlockEntity) =
            SuperbItemInterfaceBlockEntity.serverTick(level, pos, state, blockEntity)
    }
}
