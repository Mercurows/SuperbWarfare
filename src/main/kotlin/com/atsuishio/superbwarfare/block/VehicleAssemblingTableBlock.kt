package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.entity.VehicleAssemblingTableBlockEntity
import com.atsuishio.superbwarfare.block.property.BlockPart
import com.atsuishio.superbwarfare.entity.vehicle.VehicleAssemblingTableVehicleEntity
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.blockitem.VehicleAssemblingTableBlockItem
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class VehicleAssemblingTableBlock : BaseEntityBlock(
    Properties.of().strength(2f).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.DESTROY)
) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(BLOCK_PART, BlockPart.FLB)
        )
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: BlockGetter?,
        pTooltip: MutableList<Component?>,
        pFlag: TooltipFlag
    ) {
        pTooltip.add(Component.translatable("des.superbwarfare.vehicle_assembly_table").withStyle(ChatFormatting.GRAY))
    }

    override fun isPathfindable(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pType: PathComputationType
    ): Boolean {
        return false
    }

    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return if (pState.getValue(BLOCK_PART) == BlockPart.FLU || pState.getValue(BLOCK_PART) == BlockPart.FRU ||
            pState.getValue(BLOCK_PART) == BlockPart.BLU || pState.getValue(BLOCK_PART) == BlockPart.BRU
        ) {
            box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0)
        } else {
            super.getShape(pState, pLevel, pPos, pContext)
        }
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)

        val facing = state.getValue(FACING)

        var initialPos: BlockPos? = null
        for (part in BlockPart.entries) {
            val blockPos = part.relativeNegative(pos, facing)

            if (VehicleAssemblingTableBlockItem.canPlace(level, blockPos, facing, pos)) {
                initialPos = blockPos
                break
            }
        }

        if (initialPos == null) {
            Mod.LOGGER.error("Unable to find valid position for vehicle assembling table at {}", pos)
            return
        }

        for (part in BlockPart.entries) {
            val blockPos = part.relative(initialPos, facing)

            level.setBlock(blockPos, state.setValue(BLOCK_PART, part), 3)
            level.blockUpdated(initialPos, Blocks.AIR)
            state.updateNeighbourShapes(level, initialPos, 3)
        }
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        val facing = state.getValue(FACING)
        val originalPos = state.getValue(BLOCK_PART).relativeNegative(pos, facing)

        for (part in BlockPart.entries) {
            val relativePos = part.relative(originalPos, facing)
            if (relativePos != neighborPos) continue

            if (neighborState.block !== this || neighborState.getValue(BLOCK_PART) != part) {
                return Blocks.AIR.defaultBlockState()
            }
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos)
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player) {
        if (!level.isClientSide && player.isCreative) {
            val facing = state.getValue(FACING)
            val part = state.getValue(BLOCK_PART)

            val originalPos = part.relativeNegative(pos, facing)

            for (blockPart in BlockPart.entries) {
                val blockPos = blockPart.relative(originalPos, facing)
                val blockState = level.getBlockState(blockPos)
                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 35)
                level.levelEvent(player, 2001, blockPos, getId(blockState))
            }
        }

        super.playerWillDestroy(level, pos, state, player)
    }

    override fun use(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pPlayer: Player,
        pHand: InteractionHand,
        pHit: BlockHitResult
    ): InteractionResult {
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS
        } else {
            val stack = pPlayer.getItemInHand(pHand)
            if (stack.`is`(ModTags.Items.TOOLS_CROWBAR) && pLevel is ServerLevel) {
                val facing = pState.getValue(FACING)
                val part = pState.getValue(BLOCK_PART)
                val originalPos = part.relativeNegative(pPos, facing)

                val vehicle: VehicleAssemblingTableVehicleEntity = createVehicle(pLevel, facing, originalPos)

                pLevel.addFreshEntity(vehicle)

                for (p in BlockPart.entries) {
                    pLevel.destroyBlock(p.relative(originalPos, facing), false)
                }

                return InteractionResult.SUCCESS
            }

            val blockEntity = pLevel.getBlockEntity(pPos)
            if (blockEntity is VehicleAssemblingTableBlockEntity) {
                pPlayer.openMenu(blockEntity)
            }
            return InteractionResult.CONSUME
        }
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block?, BlockState?>) {
        pBuilder.add(FACING).add(BLOCK_PART)
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return this.defaultBlockState()
            .setValue(FACING, pContext.horizontalDirection.opposite)
    }

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity {
        return VehicleAssemblingTableBlockEntity(pPos, pState)
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    companion object {
        @JvmField
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        @JvmField
        val BLOCK_PART: EnumProperty<BlockPart> = EnumProperty.create("block_part", BlockPart::class.java)

        private fun createVehicle(
            server: ServerLevel,
            facing: Direction,
            originalPos: BlockPos
        ): VehicleAssemblingTableVehicleEntity {
            val vehicle = VehicleAssemblingTableVehicleEntity(server)

            val xOffset = when (facing) {
                Direction.WEST, Direction.UP, Direction.DOWN, Direction.SOUTH -> 1
                Direction.NORTH, Direction.EAST -> 0
            }

            val zOffset = when (facing) {
                Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.EAST -> 0
                Direction.NORTH, Direction.WEST -> 1
            }

            vehicle.setPos(
                (originalPos.x + xOffset).toDouble(),
                originalPos.y.toDouble(),
                (originalPos.z + zOffset).toDouble()
            )
            val deg = vehicle.rotate(
                when (facing) {
                    Direction.SOUTH, Direction.UP, Direction.DOWN -> Rotation.NONE
                    Direction.WEST -> Rotation.CLOCKWISE_90
                    Direction.NORTH -> Rotation.CLOCKWISE_180
                    Direction.EAST -> Rotation.COUNTERCLOCKWISE_90
                }
            )

            vehicle.yRotO = deg
            vehicle.yRot = deg
            vehicle.serverYaw = deg

            return vehicle
        }
    }
}
