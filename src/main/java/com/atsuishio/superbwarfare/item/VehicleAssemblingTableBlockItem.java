package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.block.property.BlockPart;
import com.atsuishio.superbwarfare.client.renderer.item.VehicleAssemblingTableBlockItemRenderer;
import com.atsuishio.superbwarfare.init.ModBlocks;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class VehicleAssemblingTableBlockItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VehicleAssemblingTableBlockItem() {
        super(ModBlocks.VEHICLE_ASSEMBLING_TABLE.get(), new Properties());
    }

    @Override
    public @NotNull InteractionResult place(@NotNull BlockPlaceContext context) {
        var pos = context.getClickedPos();
        var direction = context.getHorizontalDirection().getOpposite();

        for (var part : BlockPart.values()) {
            var detectPos = part.relative(pos, direction);
            if (!context.getLevel().getBlockState(detectPos).canBeReplaced(context)) {
                return InteractionResult.FAIL;
            }
        }

        return super.place(context);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new VehicleAssemblingTableBlockItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
