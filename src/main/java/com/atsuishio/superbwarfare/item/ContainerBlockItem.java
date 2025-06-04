package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.atsuishio.superbwarfare.client.renderer.item.ContainerBlockItemRenderer;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
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

import javax.annotation.ParametersAreNonnullByDefault;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ContainerBlockItem extends BlockItem implements GeoItem {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void registerContainers(RegisterContainersEvent event) {
        event.add(ModEntities.WHEEL_CHAIR);
        event.add(ModEntities.MK_42);
        event.add(ModEntities.MLE_1934);
        event.add(ModEntities.HPJ_11);
        event.add(ModEntities.ANNIHILATOR);
        event.add(ModEntities.LASER_TOWER);
        event.add(ModEntities.SPEEDBOAT);
        event.add(ModEntities.LAV_150);
        event.add(ModEntities.BMP_2);
        event.add(ModEntities.PRISM_TANK);
        event.add(ModEntities.YX_100);
        event.add(ModEntities.AH_6);
        event.add(ModEntities.TOM_6);
        event.add(ModEntities.A_10A);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ContainerBlockItem() {
        super(ModBlocks.CONTAINER.get(), new Properties().stacksTo(1));
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

    @Override
    public @NotNull InteractionResult place(BlockPlaceContext pContext) {
        ItemStack stack = pContext.getItemInHand();
        Player player = pContext.getPlayer();
        var res = super.place(pContext);

        if (player != null) {
            var tag = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (tag != null && tag.copyTag().get("Entity") != null) {
                if (player.level().isClientSide && res == InteractionResult.SUCCESS) {
                    player.getInventory().removeItem(stack);
                }
                if (!player.level().isClientSide && res == InteractionResult.CONSUME) {
                    player.getInventory().removeItem(stack);
                }
            }
        }
        return res;
    }

    private PlayState predicate(AnimationState<ContainerBlockItem> event) {
        return PlayState.CONTINUE;
    }


    @SubscribeEvent
    private static void registerArmorExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {

            private final BlockEntityWithoutLevelRenderer renderer = new ContainerBlockItemRenderer();

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

        }, ModItems.CONTAINER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static ItemStack createInstance(Entity entity) {
        ItemStack stack = new ItemStack(ModBlocks.CONTAINER.get());

        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        var tag = data != null ? data.copyTag() : new CompoundTag();

        var entityTag = new CompoundTag();
        var encodedId = entity.getEncodeId();
        if (encodedId != null) {
            entityTag.putString("id", encodedId);
        }
        entity.saveWithoutId(entityTag);
        tag.put("Entity", entityTag);

        tag.putString("EntityType", EntityType.getKey(entity.getType()).toString());
        BlockItem.setBlockEntityData(stack, ModBlockEntities.CONTAINER.get(), tag);
        return stack;
    }

    public static ItemStack createInstance(EntityType<?> entityType) {
        ItemStack stack = new ItemStack(ModBlocks.CONTAINER.get());
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        var tag = data != null ? data.copyTag() : new CompoundTag();

        tag.putString("EntityType", EntityType.getKey(entityType).toString());
        BlockItem.setBlockEntityData(stack, ModBlockEntities.CONTAINER.get(), tag);
        return stack;
    }
}
