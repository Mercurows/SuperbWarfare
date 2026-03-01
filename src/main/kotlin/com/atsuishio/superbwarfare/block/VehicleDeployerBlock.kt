package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.VehicleDeployerBlockEntity
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class VehicleDeployerBlock :
    BaseEntityBlock(Properties.of().sound(SoundType.METAL).strength(3.0f).requiresCorrectToolForDrops()) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TRIGGERED, false)
        )
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: BlockGetter?,
        pTooltip: MutableList<Component>,
        pFlag: TooltipFlag
    ) {
        pTooltip.add(Component.translatable("des.superbwarfare.vehicle_deployer").withStyle(ChatFormatting.GRAY))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING).add(TRIGGERED)
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val blockEntity = level.getBlockEntity(pos)
        if (level.isClientSide || blockEntity !is VehicleDeployerBlockEntity) return InteractionResult.SUCCESS

        if (!player.isCreative) return InteractionResult.FAIL

        val stack = player.getItemInHand(hand)
        if (stack.item !== ModItems.CONTAINER.get()) {
            player.displayClientMessage(
                Component.translatable("des.superbwarfare.vehicle_deployer.fail").withStyle(ChatFormatting.RED), true
            )
            return InteractionResult.FAIL
        }

        blockEntity.writeEntityInfo(stack)
        player.displayClientMessage(
            Component.translatable("des.superbwarfare.vehicle_deployer.success").withStyle(ChatFormatting.GREEN), true
        )

        return InteractionResult.SUCCESS
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighborBlock: Block,
        neighborPos: BlockPos,
        pMovedByPiston: Boolean
    ) {
        val charged = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above())
        val triggered = state.getValue(TRIGGERED)

        if (charged && !triggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, true), 4)
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is VehicleDeployerBlockEntity) {
                blockEntity.deploy(state)
            }
        } else if (!charged && triggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 4)
        }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return VehicleDeployerBlockEntity(pos, state)
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return this.defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
    }

    companion object {
        @JvmField
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        @JvmField
        val TRIGGERED: BooleanProperty = BlockStateProperties.TRIGGERED
    }
}
