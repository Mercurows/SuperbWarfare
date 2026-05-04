package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
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
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.common.capabilities.ForgeCapabilities

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class ChargingStationBlock :
    BaseEntityBlock(Properties.of().sound(SoundType.METAL).strength(3.0f).requiresCorrectToolForDrops()) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHOW_RANGE, false)
        )
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: BlockGetter?,
        pTooltip: MutableList<Component?>,
        pFlag: TooltipFlag
    ) {
        pTooltip.add(Component.translatable("des.superbwarfare.charging_station").withStyle(ChatFormatting.GRAY))
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
            this.openContainer(pLevel, pPos, pPlayer)
            return InteractionResult.CONSUME
        }
    }

    protected fun openContainer(pLevel: Level, pPos: BlockPos, pPlayer: Player) {
        val blockEntity = pLevel.getBlockEntity(pPos)
        if (blockEntity is ChargingStationBlockEntity) {
            pPlayer.openMenu(blockEntity)
        }
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
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
            return createTickerHelper<ChargingStationBlockEntity, T>(
                pBlockEntityType,
                ModBlockEntities.CHARGING_STATION.get(),
                ChargingStationBlockEntity::serverTick
            )
        }
        return null
    }

    override fun onRemove(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pNewState: BlockState,
        pMovedByPiston: Boolean
    ) {
        if (!pState.`is`(pNewState.block)) {
            val blockEntity = pLevel.getBlockEntity(pPos)
            if (blockEntity is ChargingStationBlockEntity) {
                if (pLevel is ServerLevel) {
                    Containers.dropContents(pLevel, pPos, blockEntity)
                }
                pLevel.updateNeighbourForOutputSignal(pPos, this)
            }
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston)
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block?, BlockState?>) {
        pBuilder.add(FACING).add(SHOW_RANGE)
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return this.defaultBlockState()
            .setValue(FACING, pContext.horizontalDirection.opposite)
            .setValue(SHOW_RANGE, false)
    }

    override fun getCloneItemStack(pLevel: BlockGetter, pPos: BlockPos, pState: BlockState): ItemStack {
        val itemstack = super.getCloneItemStack(pLevel, pPos, pState)
        pLevel.getBlockEntity(pPos, ModBlockEntities.CHARGING_STATION.get()).ifPresent { it.saveToItem(itemstack) }
        return itemstack
    }

    override fun getAnalogOutputSignal(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos
    ): Int {
        val blockEntity = pLevel.getBlockEntity(pPos)
        if (blockEntity is ChargingStationBlockEntity) {
            val rate = blockEntity.getCapability(ForgeCapabilities.ENERGY)
                .map { it.energyStored / it.maxEnergyStored.toDouble() }.orElse(0.0)
            return (15 * rate).toInt()
        }
        return super.getAnalogOutputSignal(pState, pLevel, pPos)
    }

    override fun hasAnalogOutputSignal(pState: BlockState): Boolean {
        return true
    }

    companion object {
        @JvmField
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        @JvmField
        val SHOW_RANGE: BooleanProperty = BooleanProperty.create("show_range")
    }
}
