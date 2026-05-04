package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.CreativeChargingStationBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.neoforge.capabilities.Capabilities

class CreativeChargingStationBlock(properties: Properties) : BaseEntityBlock(properties) {
    constructor() : this(Properties.of().sound(SoundType.METAL).strength(3.0f).requiresCorrectToolForDrops()) {
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    init {
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun codec(): MapCodec<out BaseEntityBlock> {
        return CODEC
    }

    public override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity {
        return CreativeChargingStationBlockEntity(pPos, pState)
    }

    override fun <T : BlockEntity> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (!pLevel.isClientSide) {
            return createTickerHelper<CreativeChargingStationBlockEntity, T>(
                pBlockEntityType, ModBlockEntities.CREATIVE_CHARGING_STATION.get(),
                CreativeChargingStationBlockEntity::serverTick
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
            val blockEntity = pLevel.getBlockEntity(pPos)
            if (blockEntity is CreativeChargingStationBlockEntity) {
                pLevel.updateNeighbourForOutputSignal(pPos, this)
            }
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston)
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
        if (stack.isEmpty) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

        val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return ItemInteractionResult.FAIL

        if (cap.canReceive() && cap.energyStored < cap.maxEnergyStored) {
            cap.receiveEnergy(Int.MAX_VALUE, false)
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.translatable("des.superbwarfare.creative_charging_station.charge.success")
                        .withStyle(ChatFormatting.GREEN), true
                )
            }
        } else if (cap.canExtract()) {
            cap.extractEnergy(Int.MAX_VALUE, false)
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.translatable("des.superbwarfare.creative_charging_station.extract.success")
                        .withStyle(ChatFormatting.GREEN), true
                )
            }
        } else {
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.translatable("des.superbwarfare.creative_charging_station.fail")
                        .withStyle(ChatFormatting.RED), true
                )
            }
            return ItemInteractionResult.FAIL
        }

        return ItemInteractionResult.SUCCESS
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity is CreativeChargingStationBlockEntity) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS
            }

            blockEntity.showRange = !blockEntity.showRange
            level.sendBlockUpdated(pos, state, state, UPDATE_CLIENTS)
            return InteractionResult.SUCCESS
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(FACING)
    }

    override fun getStateForPlacement(pContext: BlockPlaceContext): BlockState? {
        return this.defaultBlockState()
            .setValue(FACING, pContext.horizontalDirection.opposite)
    }

    override fun hasAnalogOutputSignal(state: BlockState): Boolean {
        return true
    }

    override fun getAnalogOutputSignal(
        state: BlockState,
        level: Level,
        pos: BlockPos
    ): Int {
        return 15
    }

    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        private val CODEC: MapCodec<CreativeChargingStationBlock> = simpleCodec(::CreativeChargingStationBlock)
    }
}