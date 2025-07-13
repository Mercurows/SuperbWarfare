package com.atsuishio.superbwarfare.item.common.container;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.LuckyContainerBlockItemRenderer;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Supplier;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class LuckyContainerBlockItem extends BlockItem implements GeoItem {

    public static final List<Supplier<ItemStack>> LUCKY_CONTAINERS = List.of(
            () -> LuckyContainerBlockItem.createInstance(Mod.loc("mobile_vehicles"), Mod.loc("textures/gui/vehicle/type/civilian.png")),
            () -> LuckyContainerBlockItem.createInstance(Mod.loc("land_vehicles"), Mod.loc("textures/gui/vehicle/type/land.png")),
            () -> LuckyContainerBlockItem.createInstance(Mod.loc("aircraft"), Mod.loc("textures/gui/vehicle/type/aircraft.png")),
            () -> LuckyContainerBlockItem.createInstance(Mod.loc("controllable_turrets"), Mod.loc("textures/gui/vehicle/type/defense.png"))
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LuckyContainerBlockItem() {
        super(ModBlocks.LUCKY_CONTAINER.get(), new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        BlockHitResult playerPOVHitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.WATER);
        if (playerPOVHitResult.getType() == HitResult.Type.MISS) {
            return super.use(level, player, hand);
        }
        BlockHitResult blockHitResult = playerPOVHitResult.withPosition(playerPOVHitResult.getBlockPos().above());
        InteractionResult interactionresult = super.useOn(new UseOnContext(player, hand, blockHitResult));
        return new InteractionResultHolder<>(interactionresult, player.getItemInHand(hand));
    }

    private PlayState predicate(AnimationState<LuckyContainerBlockItem> event) {
        return PlayState.CONTINUE;
    }

    @SubscribeEvent
    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new LuckyContainerBlockItemRenderer();

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, ModItems.LUCKY_CONTAINER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static ItemStack createInstance(ResourceLocation location, @Nullable ResourceLocation icon) {
        ItemStack stack = new ItemStack(ModBlocks.LUCKY_CONTAINER.get());
        CompoundTag tag = new CompoundTag();

        tag.putString("Location", location.toString());
        BlockItem.setBlockEntityData(stack, ModBlockEntities.LUCKY_CONTAINER.get(), tag);
        if (icon != null) {
            var iconTag = NBTTool.getTag(stack);
            iconTag.putString("Icon", icon.toString());
            NBTTool.saveTag(stack, iconTag);
        }
        return stack;
    }

    public static ItemStack createInstance(ResourceLocation location) {
        return createInstance(location, null);
    }
}
