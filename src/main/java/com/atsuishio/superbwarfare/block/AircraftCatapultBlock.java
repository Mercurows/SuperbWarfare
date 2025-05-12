package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.block.entity.AircraftCatapultBlockEntity;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class AircraftCatapultBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty UPDATING = BooleanProperty.create("updating");

    public AircraftCatapultBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3.0f).requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, 0).setValue(UPDATING, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new AircraftCatapultBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        if (!pLevel.isClientSide) {
            return createTickerHelper(pBlockEntityType, ModBlockEntities.AIRCRAFT_CATAPULT.get(), AircraftCatapultBlockEntity::serverTick);
        } else {
            return createTickerHelper(pBlockEntityType, ModBlockEntities.AIRCRAFT_CATAPULT.get(), AircraftCatapultBlockEntity::clientTick);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public void onPlace(@NotNull BlockState state, Level level, BlockPos pos, @NotNull BlockState pOldState, boolean pMovedByPiston) {
        if (level instanceof ServerLevel) {
            int receivedPower = level.getBestNeighborSignal(pos);
            int maxNeighborPower = this.getFacingPower(level, pos, state);
            int newPower = Math.max(receivedPower, maxNeighborPower);
            level.setBlock(pos, state.setValue(POWER, newPower), 3);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING).add(POWER).add(UPDATING);
    }

    public static final MapCodec<AircraftCatapultBlock> CODEC = simpleCodec((prop) -> new AircraftCatapultBlock());

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        if (!pLevel.isClientSide && !pState.getValue(UPDATING)) {
            pLevel.scheduleTick(pPos, this, 1);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource pRandom) {
        this.updateSignal(state, level, pos);
    }

    private void updateSignal(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getValue(UPDATING)) return; // 防止重入

        // 标记正在更新
        level.setBlock(pos, state.setValue(UPDATING, true), 2);

        // 计算新能量
        int receivedPower = level.getBestNeighborSignal(pos);
        int maxNeighborPower = this.getFacingPower(level, pos, state);
        int newPower = Math.max(receivedPower, maxNeighborPower);

        // 仅当能量变化时更新
        if (newPower != state.getValue(POWER)) {
            var newState = level.getBlockState(pos);
            level.setBlock(pos, newState.setValue(POWER, newPower), 3);
        }

        // 清除更新标记
        var newState = level.getBlockState(pos);
        level.setBlock(pos, newState.setValue(UPDATING, false), 2);
    }

    private int getFacingPower(Level level, BlockPos pos, BlockState state) {
        int max = 0;
        BlockPos relative = pos.relative(state.getValue(FACING));
        BlockState blockState = level.getBlockState(relative);
        if (blockState.getBlock() instanceof AircraftCatapultBlock) {
            max = Math.max(max, blockState.getValue(POWER));
        }
        return max;
    }
}
