package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class ChargingStationBlock :
    BaseEntityBlock(Properties.of().sound(SoundType.METAL).strength(3.0f).requiresCorrectToolForDrops()) {

    companion object {
        @JvmStatic
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        @JvmStatic
        val SHOW_RANGE: BooleanProperty = BooleanProperty.create("show_range")

        @JvmStatic
        val CODEC: MapCodec<ChargingStationBlock> = simpleCodec { _ -> ChargingStationBlock() }
    }

    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHOW_RANGE, false)
        )
    }

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

    protected fun openContainer(pLevel: Level, pPos: BlockPos, pPlayer: Player) {
        val blockEntity = pLevel.getBlockEntity(pPos)
        if (blockEntity is ChargingStationBlockEntity) {
            pPlayer.openMenu(blockEntity)
        }
    }

    override fun codec() = CODEC

    public override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity? {
        return ChargingStationBlockEntity(pPos, pState)
    }

    override fun <T : BlockEntity> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (!pLevel.isClientSide) {
            return createTickerHelper(
                pBlockEntityType,
                ModBlockEntities.CHARGING_STATION.get(),
                ChargingStationBlockEntity::serverTick
            )
        }
        return null
    }

    public override fun onRemove(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pNewState: BlockState,
        pMovedByPiston: Boolean
    ) {
        if (!pState.`is`(pNewState.block)) {
            val blockentity = pLevel.getBlockEntity(pPos)
            if (blockentity is ChargingStationBlockEntity) {
                if (pLevel is ServerLevel) {
                    Containers.dropContents(pLevel, pPos, blockentity)
                }
                pLevel.updateNeighbourForOutputSignal(pPos, this)
            }
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston)
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(FACING).add(SHOW_RANGE)
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState {
        return this.defaultBlockState()
            .setValue(FACING, pContext.horizontalDirection.opposite)
            .setValue(SHOW_RANGE, false)
    }

    override fun hasAnalogOutputSignal(state: BlockState): Boolean {
        return true
    }

    override fun getAnalogOutputSignal(
        state: BlockState,
        level: Level,
        pos: BlockPos
    ): Int {
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity is ChargingStationBlockEntity) {
            val energy = blockEntity.getEnergyStorage(null)
            val rate = energy.energyStored / energy.maxEnergyStored.toDouble()
            return (15 * rate).toInt()
        }
        return super.getAnalogOutputSignal(state, level, pos)
    }

    override fun getCloneItemStack(
        state: BlockState,
        target: HitResult,
        level: LevelReader,
        pos: BlockPos,
        player: Player
    ): ItemStack {
        val itemstack = super.getCloneItemStack(state, target, level, pos, player)
        level.getBlockEntity(pos, ModBlockEntities.CHARGING_STATION.get())
            .ifPresent { blockEntity ->
                blockEntity.saveToItem(itemstack, level.registryAccess())
            }
        return itemstack
    }
}
