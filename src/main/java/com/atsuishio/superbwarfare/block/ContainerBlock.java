package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
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

public class ContainerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");

    public ContainerBlock() {
        this(Properties.of().sound(SoundType.METAL).strength(3.0f).noOcclusion().requiresCorrectToolForDrops());
    }

    public ContainerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPENED, false));
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide
                || state.getValue(OPENED)
                || !(level.getBlockEntity(pos) instanceof ContainerBlockEntity containerBlockEntity)
        ) return ItemInteractionResult.FAIL;

        if (!stack.is(ModItems.CROWBAR.get())) {
            player.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.crowbar"), true);
            return ItemInteractionResult.FAIL;
        }

        if (!hasEntity(level, pos)) {
            player.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.empty"), true);
            return ItemInteractionResult.FAIL;
        }

        if (canOpen(level, pos, containerBlockEntity.entityType)) {
            level.setBlockAndUpdate(pos, state.setValue(OPENED, true));
            level.playSound(null, BlockPos.containing(pos.getX(), pos.getY(), pos.getZ()), ModSounds.OPEN.get(), SoundSource.BLOCKS, 1, 1);

            return ItemInteractionResult.SUCCESS;
        } else {
            player.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.open"), true);
            return ItemInteractionResult.FAIL;
        }
    }

    public boolean hasEntity(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ContainerBlockEntity containerBlockEntity)) return false;
        return containerBlockEntity.entityTag != null || containerBlockEntity.entityType != null;
    }

    public static boolean canOpen(Level level, BlockPos pos, EntityType<?> entityType) {
        boolean flag = true;

        int w = 0;
        int h = 0;

        if (entityType != null) {
            w = (int) (entityType.getDimensions().width() / 2 + 1);
            h = (int) (entityType.getDimensions().height() + 1);
        }

        for (int i = -w; i < w + 1; i++) {
            for (int j = 0; j < h; j++) {
                for (int k = -w; k < w + 1; k++) {
                    if (i == 0 && j == 0 && k == 0) {
                        continue;
                    }

                    var state = level.getBlockState(pos.offset(i, j, k));
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> pBlockEntityType) {
        if (!level.isClientSide) {
            return createTickerHelper(pBlockEntityType, ModBlockEntities.CONTAINER.get(), ContainerBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
        if (tag.contains("EntityType")) {
            String s = getEntityTranslationKey(tag.getString("EntityType"));
            tooltipComponents.add(Component.translatable(s == null ? "des.superbwarfare.container.empty" : s).withStyle(ChatFormatting.GRAY));

            var entityType = EntityType.byString(tag.getString("EntityType")).orElse(null);
            if (entityType != null) {
                float w = 0;
                int h = 0;

                Level level = null;
                try {
                    level = context.level();
                } catch (Exception ignored) {
                }

                if (level instanceof Level && tag.contains("Entity")) {
                    var entity = entityType.create(level);
                    if (entity != null) {
                        entity.load(tag.getCompound("Entity"));
                        w = (float) Math.ceil(entity.getType().getDimensions().width() / 2);
                        h = (int) (entity.getType().getDimensions().height() + 1);
                    }
                } else {
                    w = (float) Math.ceil(entityType.getDimensions().width() / 2);
                    h = (int) (entityType.getDimensions().height() + 1);
                }
                if (w != 0 && h != 0) {
                    w *= 2;
                    if ((int) w % 2 == 0) w++;
                    tooltipComponents.add(Component.literal((int) w + " x " + (int) w + " x " + h).withStyle(ChatFormatting.YELLOW));
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
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ContainerBlock::new);
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
    public @NotNull ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        var itemStack = super.getCloneItemStack(state, target, level, pos, player);
        level.getBlockEntity(pos, ModBlockEntities.CONTAINER.get()).ifPresent((blockEntity) -> blockEntity.saveToItem(itemStack, level.registryAccess()));
        return itemStack;
    }
}

