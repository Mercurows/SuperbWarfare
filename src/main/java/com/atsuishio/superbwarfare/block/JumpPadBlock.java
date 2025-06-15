package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("deprecation")
public class JumpPadBlock extends Block {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");

    private static final int ACTIVATION_TICKS = 4;

    public JumpPadBlock() {
        super(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).noCollission().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(ACTIVATED, false));
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            default ->
                    Shapes.or(box(0, 0, 0, 16, 3, 16), box(-0.25, -0.1, -0.25, 2, 3.25, 2), box(14, -0.1, -0.25, 16.25, 3.25, 2), box(14, -0.1, 14, 16.25, 3.25, 16.25), box(-0.25, -0.1, 14, 2, 3.25, 16.25), box(1, 3, 1, 15, 4, 15));
            case NORTH ->
                    Shapes.or(box(0, 0, 0, 16, 3, 16), box(14, -0.1, 14, 16.25, 3.25, 16.25), box(-0.25, -0.1, 14, 2, 3.25, 16.25), box(-0.25, -0.1, -0.25, 2, 3.25, 2), box(14, -0.1, -0.25, 16.25, 3.25, 2), box(1, 3, 1, 15, 4, 15));
            case EAST ->
                    Shapes.or(box(0, 0, 0, 16, 3, 16), box(-0.25, -0.1, 14, 2, 3.25, 16.25), box(-0.25, -0.1, -0.25, 2, 3.25, 2), box(14, -0.1, -0.25, 16.25, 3.25, 2), box(14, -0.1, 14, 16.25, 3.25, 16.25), box(1, 3, 1, 15, 4, 15));
            case WEST ->
                    Shapes.or(box(0, 0, 0, 16, 3, 16), box(14, -0.1, -0.25, 16.25, 3.25, 2), box(14, -0.1, 14, 16.25, 3.25, 16.25), box(-0.25, -0.1, 14, 2, 3.25, 16.25), box(-0.25, -0.1, -0.25, 2, 3.25, 2), box(1, 3, 1, 15, 4, 15));
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, ACTIVATED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean flag = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, flag).setValue(ACTIVATED, false);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public @NotNull BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public @NotNull BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return super.updateShape(state, facing, facingState, world, currentPos, facingPos);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        // 禁止套娃
        if (entity instanceof TargetEntity || entity instanceof CannonEntity) return;

        if (entity.isShiftKeyDown()) {
            if (entity.onGround()) {
                entity.setDeltaMovement(new Vec3(5 * entity.getLookAngle().x, 1.5, 5 * entity.getLookAngle().z));
            } else {
                entity.setDeltaMovement(new Vec3(1.8 * entity.getLookAngle().x, 1.5, 1.8 * entity.getLookAngle().z));
            }
        } else {
            entity.setDeltaMovement(new Vec3(0.7 * entity.getDeltaMovement().x(), 1.7, 0.7 * entity.getDeltaMovement().z()));
        }

        if (!level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) {
            setOutputPower(level, state, pos, ACTIVATION_TICKS);
        }

        if (!level.isClientSide()) {
            level.playSound(null, BlockPos.containing(pos.getX(), pos.getY(), pos.getZ()), ModSounds.JUMP.get(), SoundSource.BLOCKS, 1, 1);
        } else {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), ModSounds.JUMP.get(), SoundSource.BLOCKS, 1, 1, false);
        }

        // 谁说载具就不能二段跳了（）
        entity.passengers.stream()
                .filter(e -> e instanceof Player player && player.level().isClientSide)
                .findFirst()
                .ifPresent(player -> Mod.queueClientWork(2, () -> ClientEventHandler.canDoubleJump = true));

        if (entity instanceof Player player && player.level().isClientSide) {
            Mod.queueClientWork(2, () -> ClientEventHandler.canDoubleJump = true);
        }
    }

    private static void setOutputPower(LevelAccessor pLevel, BlockState pState, BlockPos pPos, int pWaitTime) {
        pLevel.setBlock(pPos, pState.setValue(ACTIVATED, true), 3);
        pLevel.scheduleTick(pPos, pState.getBlock(), pWaitTime);
    }

    @Override
    public boolean isSignalSource(@NotNull BlockState pState) {
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public int getSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
        return pState.getValue(ACTIVATED) ? 15 : 0;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(ACTIVATED)) {
            pLevel.setBlock(pPos, pState.setValue(ACTIVATED, false), 3);
        }
    }
}
