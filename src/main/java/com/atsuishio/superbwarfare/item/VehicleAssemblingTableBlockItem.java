package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock;
import com.atsuishio.superbwarfare.block.property.BlockPart;
import com.atsuishio.superbwarfare.client.renderer.item.VehicleAssemblingTableBlockItemRenderer;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class VehicleAssemblingTableBlockItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VehicleAssemblingTableBlockItem() {
        super(ModBlocks.VEHICLE_ASSEMBLING_TABLE.get(), new Properties());
    }

    // 根据当前状态尝试找到合适的初始放置位置
    public static @Nullable BlockPos findInitialPos(@NotNull BlockPlaceContext context, BlockPos currentPos, Direction facing) {
        BlockPart availablePart = null;
        for (var part : BlockPart.values()) {
            var placePos = part.relativeNegative(currentPos, facing);
            if (canPlace(context, placePos, facing)) {
                availablePart = part;
                break;
            }
        }

        if (availablePart == null) return null;
        return availablePart.relativeNegative(currentPos, facing);
    }

    // 多方块额外碰撞检测
    @Override
    protected boolean canPlace(@NotNull BlockPlaceContext context, @NotNull BlockState state) {
        var facing = state.getValue(VehicleAssemblingTableBlock.FACING);
        var initialPos = findInitialPos(context, context.getClickedPos(), facing);
        if (initialPos == null) return false;

        var player = context.getPlayer();
        var collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);

        // 检测是否所有位置都不会被实体挡住
        for (var blockPart : BlockPart.values()) {
            var blockPos = blockPart.relative(initialPos, facing);
            if (!context.getLevel().isUnobstructed(state, blockPos, collisionContext)) {
                return false;
            }
        }

        return super.canPlace(context, state);
    }

    public static boolean canPlace(@NotNull BlockPlaceContext context, BlockPos pos, Direction direction) {
        for (var part : BlockPart.values()) {
            var detectPos = part.relative(pos, direction);
            if (!context.getLevel().getBlockState(detectPos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canPlace(@NotNull Level level, BlockPos pos, Direction direction, BlockPos skipPos) {
        for (var part : BlockPart.values()) {
            var detectPos = part.relative(pos, direction);
            if (detectPos.equals(skipPos)) continue;

            if (!level.getBlockState(detectPos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @SubscribeEvent
    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new VehicleAssemblingTableBlockItemRenderer();

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, ModItems.VEHICLE_ASSEMBLING_TABLE);
    }
}
