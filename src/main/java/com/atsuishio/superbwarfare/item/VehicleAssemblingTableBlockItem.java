package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.property.BlockPart;
import com.atsuishio.superbwarfare.client.renderer.item.VehicleAssemblingTableBlockItemRenderer;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
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
