package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.block.entity.LuckyContainerBlockEntity;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("deprecation")
public class LuckyContainerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");

    public LuckyContainerBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3.0f).noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPENED, false));
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide
                || pState.getValue(OPENED)
                || !(pLevel.getBlockEntity(pPos) instanceof LuckyContainerBlockEntity blockEntity)
        ) return InteractionResult.PASS;

        ItemStack stack = pPlayer.getItemInHand(pHand);
        if (!stack.is(ModTags.Items.CROWBAR)) {
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.crowbar"), true);
            return InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        if (!pLevel.isClientSide) {
            return createTickerHelper(pBlockEntityType, ModBlockEntities.LUCKY_CONTAINER.get(), LuckyContainerBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(OPENED) ? box(1, 0, 1, 15, 14, 15) : box(0, 0, 0, 16, 15, 16);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new LuckyContainerBlockEntity(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(OPENED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(OPENED, false);
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack getCloneItemStack(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        ItemStack itemstack = super.getCloneItemStack(pLevel, pPos, pState);
        pLevel.getBlockEntity(pPos, ModBlockEntities.LUCKY_CONTAINER.get()).ifPresent((blockEntity) -> blockEntity.saveToItem(itemstack));
        return itemstack;
    }
}
