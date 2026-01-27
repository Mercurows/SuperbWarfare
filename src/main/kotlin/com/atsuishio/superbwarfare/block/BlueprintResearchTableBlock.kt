package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION")
class BlueprintResearchTableBlock : Block(Properties.of().strength(2f)), EntityBlock {
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        } else {
            this.openContainer(level, pos, player)
            return InteractionResult.CONSUME
        }
    }

    private fun openContainer(level: Level, pos: BlockPos, player: Player) {
        val entity = level.getBlockEntity(pos) as? BlueprintResearchTableBlockEntity ?: return
        player.openMenu(entity)
    }

    override fun newBlockEntity(
        pPos: BlockPos,
        pState: BlockState
    ): BlockEntity {
        return BlueprintResearchTableBlockEntity(pPos, pState)
    }
}