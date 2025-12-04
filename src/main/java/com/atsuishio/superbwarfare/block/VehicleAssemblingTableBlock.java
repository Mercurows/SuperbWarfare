package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.VehicleAssemblingTableBlockEntity;
import com.atsuishio.superbwarfare.block.property.BlockPart;
import com.atsuishio.superbwarfare.entity.vehicle.VehicleAssemblingTableVehicleEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.VehicleAssemblingTableBlockItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@EventBusSubscriber(modid = Mod.MODID)
public class VehicleAssemblingTableBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BlockPart> BLOCK_PART = EnumProperty.create("block_part", BlockPart.class);

    public VehicleAssemblingTableBlock() {
        super(BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.DESTROY));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BLOCK_PART, BlockPart.FLB));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.vehicle_assembly_table").withStyle(ChatFormatting.GRAY));
    }

    @Override
    @ParametersAreNonnullByDefault
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (pState.getValue(BLOCK_PART) == BlockPart.FLU || pState.getValue(BLOCK_PART) == BlockPart.FRU || pState.getValue(BLOCK_PART) == BlockPart.BLU || pState.getValue(BLOCK_PART) == BlockPart.BRU) {
            return Block.box(0, 0, 0, 16, 14, 16);
        }
        return super.getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        var facing = state.getValue(FACING);

        BlockPos initialPos = null;
        for (var part : BlockPart.values()) {
            var blockPos = part.relativeNegative(pos, facing);

            if (VehicleAssemblingTableBlockItem.canPlace(level, blockPos, facing, pos)) {
                initialPos = blockPos;
                break;
            }
        }

        if (initialPos == null) {
            Mod.LOGGER.error("Unable to find valid position for vehicle assembling table at {}", pos);
            return;
        }

        for (var part : BlockPart.values()) {
            var blockPos = part.relative(initialPos, facing);

            level.setBlock(blockPos, state.setValue(BLOCK_PART, part), 3);
            level.blockUpdated(initialPos, Blocks.AIR);
            state.updateNeighbourShapes(level, initialPos, 3);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        var facing = state.getValue(FACING);
        var originalPos = state.getValue(BLOCK_PART).relativeNegative(pos, facing);

        for (var part : BlockPart.values()) {
            var relativePos = part.relative(originalPos, facing);
            if (!relativePos.equals(neighborPos)) continue;

            if (neighborState.getBlock() != this || neighborState.getValue(BLOCK_PART) != part) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            var facing = state.getValue(FACING);
            var part = state.getValue(BLOCK_PART);

            var originalPos = part.relativeNegative(pos, facing);

            for (var blockPart : BlockPart.values()) {
                var blockPos = blockPart.relative(originalPos, facing);
                var blockState = level.getBlockState(blockPos);
                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, blockPos, Block.getId(blockState));
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel server && event.getEntity().getMainHandItem().is(ModTags.Items.TOOLS_CROWBAR)) {
            var pos = event.getHitVec().getBlockPos();
            var state = server.getBlockState(pos);
            if (state.getBlock() instanceof VehicleAssemblingTableBlock) {
                var facing = state.getValue(FACING);
                var part = state.getValue(BLOCK_PART);
                var originalPos = part.relativeNegative(pos, facing);

                var vehicle = createVehicle(server, facing, originalPos);
                server.addFreshEntity(vehicle);

                for (var p : BlockPart.values()) {
                    server.destroyBlock(p.relative(originalPos, facing), false);
                }

                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    private static @NotNull VehicleAssemblingTableVehicleEntity createVehicle(ServerLevel server, Direction facing, BlockPos originalPos) {
        var vehicle = new VehicleAssemblingTableVehicleEntity(server);

        var xOffset = switch (facing) {
            case WEST, UP, DOWN, SOUTH -> 1;
            case NORTH, EAST -> 0;
        };

        var zOffset = switch (facing) {
            case UP, DOWN, SOUTH, EAST -> 0;
            case NORTH, WEST -> 1;
        };

        vehicle.setPos(originalPos.getX() + xOffset, originalPos.getY(), originalPos.getZ() + zOffset);
        var deg = vehicle.rotate(switch (facing) {
            case SOUTH, UP, DOWN -> Rotation.NONE;
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
        });

        vehicle.yRotO = deg;
        vehicle.setYRot(deg);
        vehicle.setServerYaw(deg);

        return vehicle;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            if (level.getBlockEntity(pos) instanceof VehicleAssemblingTableBlockEntity blockEntity) {
                player.openMenu(blockEntity);
            }
            return InteractionResult.CONSUME;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING).add(BLOCK_PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @ParametersAreNonnullByDefault
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new VehicleAssemblingTableBlockEntity(pPos, pState);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(prop -> new VehicleAssemblingTableBlock());
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
