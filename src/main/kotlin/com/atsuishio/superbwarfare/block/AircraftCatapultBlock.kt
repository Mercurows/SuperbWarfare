package com.atsuishio.superbwarfare.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.Vec3
import kotlin.math.max

@Suppress("OVERRIDE_DEPRECATION")
open class AircraftCatapultBlock :
    Block(Properties.of().sound(SoundType.METAL).strength(3.0f).requiresCorrectToolForDrops()) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWER, 0)
                .setValue(UPDATING, false)
        )
    }

    override fun onPlace(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        pOldState: BlockState,
        pMovedByPiston: Boolean
    ) {
        if (level is ServerLevel) {
            val receivedPower = level.getBestNeighborSignal(pos)
            val maxNeighborPower = this.getFacingPower(level, pos, state)
            val newPower = max(receivedPower, maxNeighborPower)
            level.setBlock(pos, state.setValue(POWER, newPower), 3)
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING).add(POWER).add(UPDATING)
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return this.defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
    }

    override fun neighborChanged(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pBlock: Block,
        pFromPos: BlockPos,
        pIsMoving: Boolean
    ) {
        if (!pLevel.isClientSide && !pState.getValue(UPDATING)) {
            pLevel.scheduleTick(pPos, this, 1)
        }
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, pRandom: RandomSource) {
        this.updateSignal(state, level, pos)
    }

    private fun updateSignal(state: BlockState, level: ServerLevel, pos: BlockPos) {
        if (state.getValue(UPDATING)) return  // 防止重入

        // 标记正在更新
        level.setBlock(pos, state.setValue(UPDATING, true), 2)

        // 计算新能量
        val receivedPower = level.getBestNeighborSignal(pos)
        val maxNeighborPower = this.getFacingPower(level, pos, state)
        val newPower = max(receivedPower, maxNeighborPower)

        // 仅当能量变化时更新
        if (newPower != state.getValue(POWER)) {
            val newState = level.getBlockState(pos)
            level.setBlock(pos, newState.setValue(POWER, newPower), 3)
        }

        // 清除更新标记
        val newState = level.getBlockState(pos)
        level.setBlock(pos, newState.setValue(UPDATING, false), 2)
    }

    private fun getFacingPower(level: Level, pos: BlockPos, state: BlockState): Int {
        var max = 0
        val relative = pos.relative(state.getValue(FACING))
        val blockState = level.getBlockState(relative)
        if (blockState.block is AircraftCatapultBlock) {
            max = max(max, blockState.getValue(POWER))
        }
        return max
    }

    override fun stepOn(pLevel: Level, pPos: BlockPos, pState: BlockState, pEntity: Entity) {
        super.stepOn(pLevel, pPos, pState, pEntity)
        val direction = pState.getValue(FACING)
        val power = pState.getValue(POWER)
        if (power == 0) return

        var rate = power / 400f
        if (pEntity is LivingEntity) {
            rate = power / 50f
        }
        if (pEntity.deltaMovement
                .dot(Vec3(direction.stepX.toDouble(), 0.0, direction.stepZ.toDouble())) < 0.2 * power
        ) {
            pEntity.addDeltaMovement(
                Vec3(
                    (direction.stepX * rate).toDouble(),
                    0.0,
                    (direction.stepZ * rate).toDouble()
                )
            )
        }
    }

    companion object {
        @JvmField
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        @JvmField
        val POWER: IntegerProperty = BlockStateProperties.POWER

        @JvmField
        val UPDATING: BooleanProperty = BooleanProperty.create("updating")
    }
}
