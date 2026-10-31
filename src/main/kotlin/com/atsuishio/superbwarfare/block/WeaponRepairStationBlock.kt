package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.WeaponRepairStationBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class WeaponRepairStationBlock : BaseEntityBlock(
    Properties.of().sound(SoundType.METAL).strength(3f).requiresCorrectToolForDrops()
        .pushReaction(PushReaction.BLOCK)
) {
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = WeaponRepairStationBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState) = RenderShape.MODEL

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(FACING)))

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) (level.getBlockEntity(pos) as? WeaponRepairStationBlockEntity)?.let {
            player.openMenu(
                it
            )
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (level.isClientSide) null else createTickerHelper(
            type,
            ModBlockEntities.WEAPON_REPAIR_STATION.get(),
            WeaponRepairStationBlockEntity::serverTick
        )

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moving: Boolean) {
        if (!state.`is`(newState.block) && !level.isClientSide) {
            (level.getBlockEntity(pos) as? WeaponRepairStationBlockEntity)?.let {
                Containers.dropContents(
                    level,
                    pos,
                    it
                )
            }
        }
        super.onRemove(state, level, pos, newState, moving)
    }

    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
    }
}
