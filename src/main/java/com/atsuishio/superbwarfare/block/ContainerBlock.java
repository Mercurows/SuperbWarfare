package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity;
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
import net.minecraft.world.entity.EntityType;
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
import java.util.List;

@SuppressWarnings("deprecation")
public class ContainerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");

    public ContainerBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3.0f).noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPENED, false));
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide
                || pState.getValue(OPENED)
                || !(pLevel.getBlockEntity(pPos) instanceof ContainerBlockEntity containerBlockEntity)
        ) return InteractionResult.PASS;

        ItemStack stack = pPlayer.getItemInHand(pHand);
        if (!stack.is(ModTags.Items.CROWBAR)) {
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.crowbar"), true);
            return InteractionResult.PASS;
        }

        if (!hasEntity(pLevel, pPos)) {
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.empty"), true);
            return InteractionResult.PASS;
        }

        if (canOpen(pLevel, pPos, containerBlockEntity.entityType, containerBlockEntity.entityTag)) {
            pLevel.setBlockAndUpdate(pPos, pState.setValue(OPENED, true));
            pLevel.playSound(null, BlockPos.containing(pPos.getX(), pPos.getY(), pPos.getZ()), ModSounds.OPEN.get(), SoundSource.BLOCKS, 1, 1);

            return InteractionResult.SUCCESS;
        } else {
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.open"), true);
            return InteractionResult.PASS;
        }
    }

    public boolean hasEntity(Level pLevel, BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (!(blockEntity instanceof ContainerBlockEntity containerBlockEntity)) return false;
        return containerBlockEntity.entityTag != null || containerBlockEntity.entityType != null;
    }

    public static boolean canOpen(Level pLevel, BlockPos pPos, EntityType<?> entityType, CompoundTag tag) {
        if (entityType == null) return false;

        var entity = entityType.create(pLevel);
        if (entity != null && tag != null) {
            entity.load(tag);
        }

        boolean flag = true;

        int w = (int) (entityType.getDimensions().width / 2 + 1);
        int h = (int) (entityType.getDimensions().height + 1);

        if (entity != null) {
            w = (int) (entity.getType().getDimensions().width / 2 + 1);
            h = (int) (entity.getType().getDimensions().height + 1);
        }

        for (int i = -w; i < w + 1; i++) {
            for (int j = 0; j < h; j++) {
                for (int k = -w; k < w + 1; k++) {
                    if (i == 0 && j == 0 && k == 0) {
                        continue;
                    }

                    var state = pLevel.getBlockState(pPos.offset(i, j, k));
                    if (state.canOcclude() && !state.is(Blocks.SNOW)) {
                        flag = false;
                    }
                }
            }
        }

        return flag;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        if (!pLevel.isClientSide) {
            return createTickerHelper(pBlockEntityType, ModBlockEntities.CONTAINER.get(), ContainerBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable BlockGetter pLevel, @NotNull List<Component> pTooltip, @NotNull TooltipFlag pFlag) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
        CompoundTag tag = BlockItem.getBlockEntityData(pStack);
        if (tag != null && tag.contains("EntityType")) {
            String s = getEntityTranslationKey(tag.getString("EntityType"));
            pTooltip.add(Component.translatable(s == null ? "des.superbwarfare.container.empty" : s).withStyle(ChatFormatting.GRAY));

            var entityType = EntityType.byString(tag.getString("EntityType")).orElse(null);
            if (entityType != null) {
                float w = 0;
                int h = 0;
                if (pLevel instanceof Level level && tag.contains("Entity")) {
                    var entity = entityType.create(level);
                    if (entity != null) {
                        entity.load(tag.getCompound("Entity"));
                        w = (float) Math.ceil(entity.getType().getDimensions().width / 2);
                        h = (int) (entity.getType().getDimensions().height + 1);
                    }
                } else {
                    w = (float) Math.ceil(entityType.getDimensions().width / 2);
                    h = (int) (entityType.getDimensions().height + 1);
                }
                if (w != 0 && h != 0) {
                    w *= 2;
                    if ((int) w % 2 == 0) w++;
                    pTooltip.add(Component.literal((int) w + " x " + (int) w + " x " + h).withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }

    @Nullable
    public static String getEntityTranslationKey(String path) {
        String[] parts = path.split(":");
        if (parts.length > 1) {
            return "entity." + parts[0] + "." + parts[1];
        } else {
            return null;
        }
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
        return new ContainerBlockEntity(blockPos, blockState);
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
        pLevel.getBlockEntity(pPos, ModBlockEntities.CONTAINER.get()).ifPresent((blockEntity) -> blockEntity.saveToItem(itemstack));
        return itemstack;
    }

}

