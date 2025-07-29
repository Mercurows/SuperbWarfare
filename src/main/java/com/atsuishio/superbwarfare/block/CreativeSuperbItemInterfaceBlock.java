package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.block.entity.CreativeSuperbItemInterfaceBlockEntity;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class CreativeSuperbItemInterfaceBlock extends SuperbItemInterfaceBlock {

    public CreativeSuperbItemInterfaceBlock() {
        super();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.creative_superb_item_interface").withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    @ParametersAreNonnullByDefault
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeSuperbItemInterfaceBlockEntity(pos, state);
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? null : createTickerHelper(pBlockEntityType, ModBlockEntities.CREATIVE_SUPERB_ITEM_INTERFACE.get(), CreativeSuperbItemInterfaceBlockEntity::serverTick);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof CreativeSuperbItemInterfaceBlockEntity entity) {
                player.openMenu(entity);
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void onRemove(BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof CreativeSuperbItemInterfaceBlockEntity entity) {
                Containers.dropContents(pLevel, pPos, entity);
                pLevel.updateNeighbourForOutputSignal(pPos, this);
            }

            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        }
    }

    private static final MapCodec<CreativeSuperbItemInterfaceBlock> CODEC = BlockBehaviour.simpleCodec(prop -> new CreativeSuperbItemInterfaceBlock());

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
