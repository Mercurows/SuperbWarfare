package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.Mod.Companion.queueClientWork
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class JumpPadBlock : Block(
    Properties.of().forceSolidOn().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE)
        .strength(3f, 8f).noCollission().noOcclusion()
        .isRedstoneConductor { _, _, _ -> false }
) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(ACTIVATED, false)
        )
    }

    override fun propagatesSkylightDown(state: BlockState, reader: BlockGetter, pos: BlockPos): Boolean {
        return true
    }

    override fun getVisualShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return Shapes.empty()
    }

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return when (state.getValue(FACING)) {
            Direction.NORTH -> Shapes.or(
                box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
                box(14.0, -0.1, 14.0, 16.25, 3.25, 16.25),
                box(-0.25, -0.1, 14.0, 2.0, 3.25, 16.25),
                box(-0.25, -0.1, -0.25, 2.0, 3.25, 2.0),
                box(14.0, -0.1, -0.25, 16.25, 3.25, 2.0),
                box(1.0, 3.0, 1.0, 15.0, 4.0, 15.0)
            )

            Direction.EAST -> Shapes.or(
                box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
                box(-0.25, -0.1, 14.0, 2.0, 3.25, 16.25),
                box(-0.25, -0.1, -0.25, 2.0, 3.25, 2.0),
                box(14.0, -0.1, -0.25, 16.25, 3.25, 2.0),
                box(14.0, -0.1, 14.0, 16.25, 3.25, 16.25),
                box(1.0, 3.0, 1.0, 15.0, 4.0, 15.0)
            )

            Direction.WEST -> Shapes.or(
                box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
                box(14.0, -0.1, -0.25, 16.25, 3.25, 2.0),
                box(14.0, -0.1, 14.0, 16.25, 3.25, 16.25),
                box(-0.25, -0.1, 14.0, 2.0, 3.25, 16.25),
                box(-0.25, -0.1, -0.25, 2.0, 3.25, 2.0),
                box(1.0, 3.0, 1.0, 15.0, 4.0, 15.0)
            )

            else -> {
                Shapes.or(
                    box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
                    box(-0.25, -0.1, -0.25, 2.0, 3.25, 2.0),
                    box(14.0, -0.1, -0.25, 16.25, 3.25, 2.0),
                    box(14.0, -0.1, 14.0, 16.25, 3.25, 16.25),
                    box(-0.25, -0.1, 14.0, 2.0, 3.25, 16.25),
                    box(1.0, 3.0, 1.0, 15.0, 4.0, 15.0)
                )
                Shapes.or(
                    box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
                    box(14.0, -0.1, 14.0, 16.25, 3.25, 16.25),
                    box(-0.25, -0.1, 14.0, 2.0, 3.25, 16.25),
                    box(-0.25, -0.1, -0.25, 2.0, 3.25, 2.0),
                    box(14.0, -0.1, -0.25, 16.25, 3.25, 2.0),
                    box(1.0, 3.0, 1.0, 15.0, 4.0, 15.0)
                )
                Shapes.or(
                    box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
                    box(-0.25, -0.1, 14.0, 2.0, 3.25, 16.25),
                    box(-0.25, -0.1, -0.25, 2.0, 3.25, 2.0),
                    box(14.0, -0.1, -0.25, 16.25, 3.25, 2.0),
                    box(14.0, -0.1, 14.0, 16.25, 3.25, 16.25),
                    box(1.0, 3.0, 1.0, 15.0, 4.0, 15.0)
                )
                Shapes.or(
                    box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
                    box(14.0, -0.1, -0.25, 16.25, 3.25, 2.0),
                    box(14.0, -0.1, 14.0, 16.25, 3.25, 16.25),
                    box(-0.25, -0.1, 14.0, 2.0, 3.25, 16.25),
                    box(-0.25, -0.1, -0.25, 2.0, 3.25, 2.0),
                    box(1.0, 3.0, 1.0, 15.0, 4.0, 15.0)
                )
            }
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING, WATERLOGGED, ACTIVATED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val flag = context.level.getFluidState(context.clickedPos).type === Fluids.WATER
        return this.defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(WATERLOGGED, flag)
            .setValue(ACTIVATED, false)
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    override fun rotate(state: BlockState, rot: Rotation): BlockState {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)))
    }

    override fun mirror(state: BlockState, mirrorIn: Mirror): BlockState {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)))
    }

    override fun updateShape(
        state: BlockState,
        facing: Direction,
        facingState: BlockState,
        world: LevelAccessor,
        currentPos: BlockPos,
        facingPos: BlockPos
    ): BlockState {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world))
        }
        return super.updateShape(state, facing, facingState, world, currentPos, facingPos)
    }

    override fun entityInside(state: BlockState, level: Level, pos: BlockPos, entity: Entity) {
        super.entityInside(state, level, pos, entity)

        if (entity is TargetEntity) return

        if (entity.isShiftKeyDown) {
            if (entity.onGround()) {
                entity.deltaMovement = Vec3(5 * entity.lookAngle.x, 1.5, 5 * entity.lookAngle.z)
            } else {
                entity.deltaMovement = Vec3(1.8 * entity.lookAngle.x, 1.5, 1.8 * entity.lookAngle.z)
            }
        } else {
            entity.deltaMovement = Vec3(0.7 * entity.deltaMovement.x(), 1.7, 0.7 * entity.deltaMovement.z())
        }

        if (!level.blockTicks.hasScheduledTick(pos, state.block)) {
            setOutputPower(level, state, pos, ACTIVATION_TICKS)
        }

        if (!level.isClientSide()) {
            level.playSound(
                null,
                BlockPos.containing(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()),
                ModSounds.JUMP.get(),
                SoundSource.BLOCKS,
                1f,
                1f
            )
        } else {
            level.playLocalSound(
                pos.x.toDouble(),
                pos.y.toDouble(),
                pos.z.toDouble(),
                ModSounds.JUMP.get(),
                SoundSource.BLOCKS,
                1f,
                1f,
                false
            )
        }

        // 谁说载具就不能二段跳了（）
        entity.passengers.stream()
            .filter { it is Player && it.level().isClientSide }
            .findFirst()
            .ifPresent { _ ->
                queueClientWork(2) { ClientEventHandler.canDoubleJump = true }
            }

        if (entity is Player && entity.level().isClientSide) {
            queueClientWork(2) { ClientEventHandler.canDoubleJump = true }
        }
    }

    override fun isSignalSource(pState: BlockState): Boolean {
        return true
    }

    override fun getSignal(pState: BlockState, pLevel: BlockGetter, pPos: BlockPos, pDirection: Direction): Int {
        return if (pState.getValue(ACTIVATED)) 15 else 0
    }

    override fun tick(pState: BlockState, pLevel: ServerLevel, pPos: BlockPos, pRandom: RandomSource) {
        if (pState.getValue(ACTIVATED)) {
            pLevel.setBlock(pPos, pState.setValue(ACTIVATED, false), 3)
        }
    }

    companion object {
        @JvmField
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED

        @JvmField
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        @JvmField
        val ACTIVATED: BooleanProperty = BooleanProperty.create("activated")

        private const val ACTIVATION_TICKS = 4

        private fun setOutputPower(pLevel: LevelAccessor, pState: BlockState, pPos: BlockPos, pWaitTime: Int) {
            pLevel.setBlock(pPos, pState.setValue(ACTIVATED, true), 3)
            pLevel.scheduleTick(pPos, pState.block, pWaitTime)
        }
    }
}
