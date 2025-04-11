package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.block.entity.CreativeChargingStationBlockEntity;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class CreativeChargingStationBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty SHOW_RANGE = BooleanProperty.create("show_range");

    public CreativeChargingStationBlock() {
        this(Properties.of().sound(SoundType.METAL).strength(3.0f).requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHOW_RANGE, false));
    }

    public CreativeChargingStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static final MapCodec<CreativeChargingStationBlock> CODEC = BlockBehaviour.simpleCodec(CreativeChargingStationBlock::new);

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    @ParametersAreNonnullByDefault
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new CreativeChargingStationBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        if (!pLevel.isClientSide) {
            return createTickerHelper(
                    pBlockEntityType, ModBlockEntities.CREATIVE_CHARGING_STATION.get(),
                    CreativeChargingStationBlockEntity::serverTick
            );
        }
        return null;
    }

    @Override
    public void onRemove(BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (!pState.is(pNewState.getBlock())) {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof CreativeChargingStationBlockEntity) {
                pLevel.updateNeighbourForOutputSignal(pPos, this);
            }
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap == null) return ItemInteractionResult.FAIL;

        if (cap.canReceive() && cap.getEnergyStored() < cap.getMaxEnergyStored()) {
            cap.receiveEnergy(Integer.MAX_VALUE, false);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("des.superbwarfare.creative_charging_station.charge.success").withStyle(ChatFormatting.GREEN), true);
            }
        } else if (cap.canExtract()) {
            cap.extractEnergy(Integer.MAX_VALUE, false);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("des.superbwarfare.creative_charging_station.extract.success").withStyle(ChatFormatting.GREEN), true);
            }
        } else {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("des.superbwarfare.creative_charging_station.fail").withStyle(ChatFormatting.RED), true);
            }
            return ItemInteractionResult.FAIL;
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CreativeChargingStationBlockEntity station) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            station.showRange = !station.showRange;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING).add(SHOW_RANGE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(SHOW_RANGE, false);
    }
}
