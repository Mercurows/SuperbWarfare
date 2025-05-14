package com.atsuishio.superbwarfare.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class AircraftCatapultBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty UPDATING = BooleanProperty.create("updating");

    public AircraftCatapultBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3.0f).requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, 0).setValue(UPDATING, false));
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

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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

    @Override
    @ParametersAreNonnullByDefault
    public void stepOn(Level pLevel, BlockPos pPos, BlockState pState, Entity pEntity) {
        super.stepOn(pLevel, pPos, pState, pEntity);
        var direction = pState.getValue(AircraftCatapultBlock.FACING);
        int power = pState.getValue(AircraftCatapultBlock.POWER);
        if (power == 0) return;

        float rate = power / 400f;
        if (pEntity instanceof LivingEntity) {
            rate = power / 50f;
        }
        if (pEntity.getDeltaMovement().dot(new Vec3(direction.getStepX(), 0, direction.getStepZ())) < 0.2 * power) {
            pEntity.addDeltaMovement(new Vec3(direction.getStepX() * rate, 0, direction.getStepZ() * rate));
        }
    }
}
