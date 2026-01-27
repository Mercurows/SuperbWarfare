package com.atsuishio.superbwarfare.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class BlueprintResearchTableBlock : Block(Properties.of().strength(2f)), EntityBlock {


    override fun newBlockEntity(
        pPos: BlockPos,
        pState: BlockState
    ): BlockEntity? {
        return null
    }
}