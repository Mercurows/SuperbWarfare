package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.entity.SmallContainerBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.mojang.serialization.MapCodec
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
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
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class SmallContainerBlock :
    BaseEntityBlock(Properties.of().sound(SoundType.METAL).strength(3.0f).noOcclusion().requiresCorrectToolForDrops()) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPENED, false)
        )
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        val blockEntity = level.getBlockEntity(pos)
        if (level.isClientSide || state.getValue(OPENED) || (blockEntity !is SmallContainerBlockEntity)) {
            return ItemInteractionResult.FAIL
        }

        if (!stack.`is`(ModTags.Items.TOOLS_CROWBAR)) {
            player.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.crowbar"), true)
            return ItemInteractionResult.FAIL
        }

        blockEntity.player = player

        level.setBlockAndUpdate(pos, state.setValue(OPENED, true))
        level.playSound(
            null,
            BlockPos.containing(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()),
            ModSounds.OPEN.get(),
            SoundSource.BLOCKS,
            1f,
            1f
        )

        return ItemInteractionResult.SUCCESS
    }

    override fun <T : BlockEntity?> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T?>
    ): BlockEntityTicker<T?>? {
        if (!pLevel.isClientSide) {
            return createTickerHelper<SmallContainerBlockEntity?, T?>(
                pBlockEntityType,
                ModBlockEntities.SMALL_CONTAINER.get(),
                SmallContainerBlockEntity::serverTick
            )
        }
        return null
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)

        val data = stack.get(DataComponents.CONTAINER_LOOT)
        if (data != null) {
            var lootTable = data.lootTable().location().toString()
            if (lootTable.startsWith(Mod.MODID + ":containers/")) {
                val split = lootTable.split((Mod.MODID + ":containers/").toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (split.size == 2) {
                    lootTable = "loot." + split[1]
                }
                tooltipComponents.add(
                    Component.translatable("des.superbwarfare.small_container.$lootTable")
                        .withStyle(ChatFormatting.GRAY)
                )
            } else {
                val seed = data.seed()
                if (seed != 0L && seed % 205 == 0L) {
                    tooltipComponents.add(
                        Component.translatable("des.superbwarfare.small_container.special")
                            .withStyle(ChatFormatting.GRAY)
                    )
                } else {
                    tooltipComponents.add(
                        Component.translatable("des.superbwarfare.small_container.random")
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            }
        } else {
            tooltipComponents.add(
                Component.translatable("des.superbwarfare.small_container").withStyle(ChatFormatting.GRAY)
            )
        }
    }

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return if (state.getValue(FACING) == Direction.NORTH || state.getValue(FACING) == Direction.SOUTH) {
            if (state.getValue(OPENED)) box(1.0, 0.0, 2.0, 15.0, 12.0, 14.0)
            else box(0.0, 0.0, 1.0, 16.0, 13.5, 15.0)
        } else if (state.getValue(OPENED)) box(2.0, 0.0, 1.0, 14.0, 12.0, 15.0)
        else box(1.0, 0.0, 0.0, 15.0, 13.5, 16.0)
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity? {
        return SmallContainerBlockEntity(blockPos, blockState)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING).add(OPENED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return this.defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(OPENED, false)
    }

    override fun getCloneItemStack(
        state: BlockState,
        target: HitResult,
        level: LevelReader,
        pos: BlockPos,
        player: Player
    ): ItemStack {
        val stack = super.getCloneItemStack(state, target, level, pos, player)

        level.getBlockEntity(pos, ModBlockEntities.SMALL_CONTAINER.get())
            .ifPresent { blockEntity ->
                blockEntity.saveToItem(stack, level.registryAccess())
            }
        return stack
    }


    override fun codec(): MapCodec<out BaseEntityBlock?> {
        return CODEC
    }

    companion object {
        @JvmField
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        @JvmField
        val OPENED: BooleanProperty = BooleanProperty.create("opened")

        @JvmField
        val CODEC: MapCodec<SmallContainerBlock> = simpleCodec { _ -> SmallContainerBlock() }
    }
}

