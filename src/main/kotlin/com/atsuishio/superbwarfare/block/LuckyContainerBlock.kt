package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.entity.LuckyContainerBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import javax.annotation.ParametersAreNonnullByDefault

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
class LuckyContainerBlock :
    BaseEntityBlock(Properties.of().sound(SoundType.METAL).strength(3.0f).noOcclusion().requiresCorrectToolForDrops()) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPENED, false)
        )
    }

    override fun use(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pPlayer: Player,
        pHand: InteractionHand,
        pHit: BlockHitResult
    ): InteractionResult {
        if (pLevel.isClientSide
            || pState.getValue(OPENED)
            || pLevel.getBlockEntity(pPos) !is LuckyContainerBlockEntity
        ) return InteractionResult.PASS

        val stack = pPlayer.getItemInHand(pHand)
        if (!stack.`is`(ModTags.Items.TOOLS_CROWBAR)) {
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.crowbar"), true)
            return InteractionResult.PASS
        }

        pLevel.setBlockAndUpdate(pPos, pState.setValue(OPENED, true))
        pLevel.playSound(
            null,
            BlockPos.containing(pPos.x.toDouble(), pPos.y.toDouble(), pPos.z.toDouble()),
            ModSounds.OPEN.get(),
            SoundSource.BLOCKS,
            1f,
            1f
        )

        return InteractionResult.SUCCESS
    }

    override fun <T : BlockEntity?> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T?>
    ): BlockEntityTicker<T?>? {
        if (!pLevel.isClientSide) {
            return createTickerHelper<LuckyContainerBlockEntity?, T?>(
                pBlockEntityType,
                ModBlockEntities.LUCKY_CONTAINER.get()
            ) { pLevel, pPos, pState, blockEntity ->
                LuckyContainerBlockEntity.serverTick(
                    pLevel,
                    pPos,
                    pState,
                    blockEntity
                )
            }
        }
        return null
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: BlockGetter?,
        pTooltip: MutableList<Component?>,
        pFlag: TooltipFlag
    ) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag)
        val tag = BlockItem.getBlockEntityData(pStack)
        if (tag != null) {
            var location = tag.getString("Location")
            if (location.startsWith(Mod.MODID)) {
                val split: Array<String?> =
                    location.split((Mod.MODID + ":").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (split.size == 2) {
                    location = "location." + split[1]
                }
                pTooltip.add(
                    Component.translatable("des.superbwarfare.lucky_container.$location")
                        .withStyle(ChatFormatting.GRAY)
                )
            }
        } else {
            pTooltip.add(Component.translatable("des.superbwarfare.small_container").withStyle(ChatFormatting.GRAY))
        }
    }

    @ParametersAreNonnullByDefault
    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return if (state.getValue(OPENED)) box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0)
        else box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0)
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity {
        return LuckyContainerBlockEntity(blockPos, blockState)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING).add(OPENED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return this.defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(OPENED, false)
    }

    @ParametersAreNonnullByDefault
    override fun getCloneItemStack(pLevel: BlockGetter, pPos: BlockPos, pState: BlockState): ItemStack {
        val itemstack = super.getCloneItemStack(pLevel, pPos, pState)
        pLevel.getBlockEntity(pPos, ModBlockEntities.LUCKY_CONTAINER.get()).ifPresent { it.saveToItem(itemstack) }
        return itemstack
    }

    companion object {
        @JvmField
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        @JvmField
        val OPENED: BooleanProperty = BooleanProperty.create("opened")
    }
}
