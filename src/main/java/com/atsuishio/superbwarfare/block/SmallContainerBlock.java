package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.SmallContainerBlockEntity;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class SmallContainerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");

    public SmallContainerBlock() {
        this(Properties.of().sound(SoundType.METAL).strength(3.0f).noOcclusion().requiresCorrectToolForDrops());
    }

    public SmallContainerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide || state.getValue(OPENED) || !(level.getBlockEntity(pos) instanceof SmallContainerBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(player.getUsedItemHand());
        if (!stack.is(ModItems.CROWBAR.get())) {
            player.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.crowbar"), true);
            return InteractionResult.PASS;
        }

        blockEntity.setPlayer(player);

        level.setBlockAndUpdate(pos, state.setValue(OPENED, true));
        level.playSound(null, BlockPos.containing(pos.getX(), pos.getY(), pos.getZ()), ModSounds.OPEN.get(), SoundSource.BLOCKS, 1, 1);

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> pBlockEntityType) {
        if (!level.isClientSide) {
            return createTickerHelper(pBlockEntityType, ModBlockEntities.SMALL_CONTAINER.get(), SmallContainerBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        var data = stack.get(DataComponents.CONTAINER_LOOT);
        if (data != null) {
            String lootTable = data.lootTable().location().toString();
            if (lootTable.startsWith(Mod.MODID + ":containers/")) {
                var split = lootTable.split(Mod.MODID + ":containers/");
                if (split.length == 2) {
                    lootTable = "loot." + split[1];
                }
                tooltipComponents.add(Component.translatable("des.superbwarfare.small_container." + lootTable).withStyle(ChatFormatting.GRAY));
            } else {
                long seed = data.seed();
                if (seed != 0 && seed % 205 == 0) {
                    tooltipComponents.add(Component.translatable("des.superbwarfare.small_container.special").withStyle(ChatFormatting.GRAY));
                } else {
                    tooltipComponents.add(Component.translatable("des.superbwarfare.small_container.random").withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            tooltipComponents.add(Component.translatable("des.superbwarfare.small_container").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(FACING) == Direction.NORTH || state.getValue(FACING) == Direction.SOUTH) {
            return state.getValue(OPENED) ? box(1, 0, 2, 15, 12, 14) : box(0, 0, 1, 16, 13.5, 15);
        } else return state.getValue(OPENED) ? box(2, 0, 1, 14, 12, 15) : box(1, 0, 0, 15, 13.5, 16);
    }

    private static final MapCodec<SmallContainerBlock> CODEC = BlockBehaviour.simpleCodec(SmallContainerBlock::new);

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    public @NotNull ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        var stack = super.getCloneItemStack(state, target, level, pos, player);

        level.getBlockEntity(pos, ModBlockEntities.SMALL_CONTAINER.get()).ifPresent((blockEntity) -> blockEntity.saveToItem(stack, level.registryAccess()));
        return stack;
    }
}

