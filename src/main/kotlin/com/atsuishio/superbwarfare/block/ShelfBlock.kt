package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.ShelfBlockEntity
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.StringRepresentable
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.util.TriState
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import java.util.*

class ShelfBlock(properties: Properties) : BaseEntityBlock(properties) {
    constructor() : this(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.5f).noOcclusion())

    override fun codec(): MapCodec<ShelfBlock> = CODEC

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
            .setValue(BASE, false).setValue(PART, Part.SINGLE)
            .setValue(SEPARATE_LEFT, false).setValue(SEPARATE_RIGHT, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, BASE, PART, SEPARATE_LEFT, SEPARATE_RIGHT)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        placementState(context.level, context.clickedPos, context.horizontalDirection.opposite, context.isSecondaryUseActive)

    private fun placementState(level: BlockGetter, pos: BlockPos, facing: Direction, separate: Boolean): BlockState {
        var state = defaultBlockState().setValue(FACING, facing)
        if (separate) {
            fun hasNeighbor(direction: Direction): Boolean {
                val neighbor = level.getBlockState(pos.relative(direction))
                return neighbor.`is`(this) && neighbor.getValue(FACING) == facing
            }
            state = state.setValue(SEPARATE_LEFT, hasNeighbor(facing.clockWise))
                .setValue(SEPARATE_RIGHT, hasNeighbor(facing.counterClockWise))
        }
        return connectedState(state, level, pos)
    }

    override fun updateShape(
        state: BlockState, direction: Direction, neighborState: BlockState,
        level: LevelAccessor, pos: BlockPos, neighborPos: BlockPos
    ): BlockState = connectedState(state, level, pos)

    private fun connectedState(state: BlockState, level: BlockGetter, pos: BlockPos): BlockState {
        val facing = state.getValue(FACING)
        val base = level.getBlockState(pos.above()).`is`(this)
        fun connects(direction: Direction, separate: BooleanProperty, neighborSeparate: BooleanProperty): Boolean {
            if (state.getValue(separate)) return false
            val neighborPos = pos.relative(direction)
            val neighbor = level.getBlockState(neighborPos)
            return neighbor.`is`(this) && neighbor.getValue(FACING) == facing &&
                    !neighbor.getValue(neighborSeparate) &&
                    level.getBlockState(neighborPos.above()).`is`(this) == base
        }
        val left = connects(facing.clockWise, SEPARATE_LEFT, SEPARATE_RIGHT)
        val right = connects(facing.counterClockWise, SEPARATE_RIGHT, SEPARATE_LEFT)
        val part = when {
            left && right -> Part.MIDDLE
            left -> Part.RIGHT
            right -> Part.LEFT
            else -> Part.SINGLE
        }
        return state.setValue(PART, part).setValue(BASE, base)
    }

    override fun useItemOn(
        held: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player,
        hand: InteractionHand, hit: BlockHitResult
    ): ItemInteractionResult {
        val pass = ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
        if (hit.direction != state.getValue(FACING) || player.isSpectator) return pass
        if (player.isShiftKeyDown && isShelfItem(held)) return pass
        val shelf = level.getBlockEntity(pos) as? ShelfBlockEntity ?: return pass
        val slot = clickedSlot(pos, hit)
        val take = player.isShiftKeyDown || held.isEmpty
        val stored = shelf.getItem(slot)
        if (take && stored.isEmpty || !take && !stored.isEmpty) return pass
        if (level.isClientSide) return ItemInteractionResult.SUCCESS
        if (take) {
            val removed = shelf.removeItem(slot, if (player.isShiftKeyDown) stored.count else 1)
            if (!player.inventory.add(removed)) player.drop(removed, false)
        } else {
            if (!shelf.insertItem(slot, held)) return pass
            if (!player.abilities.instabuild) player.setItemInHand(hand, ItemStack.EMPTY)
        }
        level.playSound(null, pos, if (take) SoundEvents.ITEM_FRAME_REMOVE_ITEM else SoundEvents.ITEM_FRAME_ADD_ITEM,
            SoundSource.BLOCKS, 1f, 1f)
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos)
        return ItemInteractionResult.CONSUME
    }

    private fun isShelfItem(stack: ItemStack): Boolean = (stack.item as? BlockItem)?.block === this

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moving: Boolean) {
        if (!state.`is`(newState.block) && !level.isClientSide) {
            (level.getBlockEntity(pos) as? ShelfBlockEntity)?.dropContents()
        }
        super.onRemove(state, level, pos, newState, moving)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = ShelfBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState) = RenderShape.ENTITYBLOCK_ANIMATED

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        val part = if (mirror == Mirror.NONE) state.getValue(PART) else when (state.getValue(PART)) {
            Part.LEFT -> Part.RIGHT
            Part.RIGHT -> Part.LEFT
            else -> state.getValue(PART)
        }
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(PART, part)
            .setValue(SEPARATE_LEFT, state.getValue(if (mirror == Mirror.NONE) SEPARATE_LEFT else SEPARATE_RIGHT))
            .setValue(SEPARATE_RIGHT, state.getValue(if (mirror == Mirror.NONE) SEPARATE_RIGHT else SEPARATE_LEFT))
    }

    enum class Part : StringRepresentable {
        SINGLE, LEFT, MIDDLE, RIGHT;

        override fun getSerializedName() = name.lowercase(Locale.ROOT)
    }

    @EventBusSubscriber
    companion object {
        val CODEC: MapCodec<ShelfBlock> = simpleCodec(::ShelfBlock)
        val FACING = BlockStateProperties.HORIZONTAL_FACING
        val BASE = BooleanProperty.create("base")
        val PART: EnumProperty<Part> = EnumProperty.create("part", Part::class.java)
        val SEPARATE_LEFT = BooleanProperty.create("separate_left")
        val SEPARATE_RIGHT = BooleanProperty.create("separate_right")

        private fun clickedSlot(pos: BlockPos, hit: BlockHitResult): Int =
            if (hit.location.y - pos.y >= 9.0 / 16.0) 1 else 0

        @SubscribeEvent
        fun onRightClick(event: PlayerInteractEvent.RightClickBlock) {
            val player = event.entity
            if (player.isSpectator || !player.isShiftKeyDown) return
            val state = event.level.getBlockState(event.pos)
            val shelf = state.block as? ShelfBlock ?: return
            if (event.hitVec.direction != state.getValue(FACING) || shelf.isShelfItem(player.getItemInHand(event.hand))) return
            val blockEntity = event.level.getBlockEntity(event.pos) as? ShelfBlockEntity ?: return
            if (blockEntity.getItem(clickedSlot(event.pos, event.hitVec)).isEmpty) return

            event.useBlock = TriState.TRUE
            event.useItem = TriState.FALSE
        }
    }
}
