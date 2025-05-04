package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.SmallContainerBlockItemRenderer;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Supplier;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class SmallContainerBlockItem extends BlockItem implements GeoItem {

    public static final List<Supplier<ItemStack>> SMALL_CONTAINER_LOOT_TABLES = List.of(
            () -> SmallContainerBlockItem.createInstance(Mod.loc("containers/blueprints")),
            () -> SmallContainerBlockItem.createInstance(Mod.loc("containers/common"))
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SmallContainerBlockItem() {
        super(ModBlocks.SMALL_CONTAINER.get(), new Properties().stacksTo(1));
    }

    private PlayState predicate(AnimationState<SmallContainerBlockItem> event) {
        return PlayState.CONTINUE;
    }

    @SubscribeEvent
    private static void registerArmorExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {

            private final BlockEntityWithoutLevelRenderer renderer = new SmallContainerBlockItemRenderer();

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

        }, ModItems.SMALL_CONTAINER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static ItemStack createInstance(ResourceLocation lootTable) {
        return createInstance(ResourceKey.create(Registries.LOOT_TABLE, lootTable), 0L);
    }

    public static ItemStack createInstance(ResourceKey<LootTable> lootTable, long lootTableSeed) {
        ItemStack stack = new ItemStack(ModBlocks.SMALL_CONTAINER.get());
        stack.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(lootTable, lootTableSeed));
        return stack;
    }
}
