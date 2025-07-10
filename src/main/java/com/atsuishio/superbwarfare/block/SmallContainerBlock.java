package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.SmallContainerBlockEntity;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import java.util.List;

@SuppressWarnings("deprecation")
public class SmallContainerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");

    public SmallContainerBlock() {
        super(Properties.of().sound(SoundType.METAL).strength(3.0f).noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPENED, false));
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide || pState.getValue(OPENED) || !(pLevel.getBlockEntity(pPos) instanceof SmallContainerBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = pPlayer.getItemInHand(pHand);
        if (!stack.is(ModTags.Items.CROWBAR)) {
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.crowbar"), true);
            return InteractionResult.PASS;
        }

        blockEntity.setPlayer(pPlayer);

        pLevel.setBlockAndUpdate(pPos, pState.setValue(OPENED, true));
        pLevel.playSound(null, BlockPos.containing(pPos.getX(), pPos.getY(), pPos.getZ()), ModSounds.OPEN.get(), SoundSource.BLOCKS, 1, 1);

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        if (!pLevel.isClientSide) {
            return createTickerHelper(pBlockEntityType, ModBlockEntities.SMALL_CONTAINER.get(), SmallContainerBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable BlockGetter pLevel, @NotNull List<Component> pTooltip, @NotNull TooltipFlag pFlag) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
        CompoundTag tag = BlockItem.getBlockEntityData(pStack);
        if (tag != null) {
            String lootTable = tag.getString("LootTable");
            if (lootTable.startsWith(Mod.MODID + ":containers/")) {
                var split = lootTable.split(Mod.MODID + ":containers/");
                if (split.length == 2) {
                    lootTable = "loot." + split[1];
                }
                pTooltip.add(Component.translatable("des.superbwarfare.small_container." + lootTable).withStyle(ChatFormatting.GRAY));
            } else {
                long seed = tag.getLong("LootTableSeed");
                if (seed != 0 && seed % 205 == 0) {
                    pTooltip.add(Component.translatable("des.superbwarfare.small_container.special").withStyle(ChatFormatting.GRAY));
                } else {
                    pTooltip.add(Component.translatable("des.superbwarfare.small_container.random").withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            pTooltip.add(Component.translatable("des.superbwarfare.small_container").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(FACING) == Direction.NORTH || state.getValue(FACING) == Direction.SOUTH) {
            return state.getValue(OPENED) ? box(1, 0, 2, 15, 12, 14) : box(0, 0, 1, 16, 13.5, 15);
        } else return state.getValue(OPENED) ? box(2, 0, 1, 14, 12, 15) : box(1, 0, 0, 15, 13.5, 16);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new SmallContainerBlockEntity(blockPos, blockState);
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
        pLevel.getBlockEntity(pPos, ModBlockEntities.SMALL_CONTAINER.get()).ifPresent((blockEntity) -> blockEntity.saveToItem(itemstack));
        return itemstack;
    }
}

