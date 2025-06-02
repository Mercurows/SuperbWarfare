package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.Tm62ItemRenderer;
import com.atsuishio.superbwarfare.entity.Tm62Entity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Tm62 extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Tm62() {
        super(new Properties().stacksTo(8));
    }

    @SubscribeEvent
    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {

            private final BlockEntityWithoutLevelRenderer renderer = new Tm62ItemRenderer();

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, ModItems.TM_62);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            float randomRot = (float) Mth.clamp((2 * Math.random() - 1) * 180, -180, 180);
            Tm62Entity entity = new Tm62Entity(player, level, player.isShiftKeyDown());
            entity.moveTo(player.getX(), player.getY() + 1.1, player.getZ(), randomRot, 0);
            entity.setYBodyRot(randomRot);
            entity.setYHeadRot(randomRot);
            entity.setDeltaMovement(0.5 * player.getLookAngle().x, 0.5 * player.getLookAngle().y, 0.5 * player.getLookAngle().z);

            level.addFreshEntity(entity);
        }

        player.getCooldowns().addCooldown(this, 20);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.success(stack);
    }

    public static class Tm62DispenseBehavior extends DefaultDispenseItemBehavior {
        @Override
        @ParametersAreNonnullByDefault
        protected @NotNull ItemStack execute(BlockSource blockSource, ItemStack stack) {
            Level level = blockSource.level();
            Position position = DispenserBlock.getDispensePosition(blockSource);
            Direction direction = blockSource.state().getValue(DispenserBlock.FACING);

            var tm62 = new Tm62Entity(ModEntities.TM_62.get(), level);
            tm62.setPos(position.x(), position.y(), position.z());
            float randomRot = (float) Mth.clamp((2 * Math.random() - 1) * 180, -180, 180);

            var pX = direction.getStepX();
            var pY = direction.getStepY();
            var pZ = direction.getStepZ();
            tm62.shoot(pX, pY, pZ, 0.2f, 25);
            tm62.setYRot(randomRot);
            tm62.yRotO = tm62.getYRot();

            level.addFreshEntity(tm62);
            stack.shrink(1);
            return stack;
        }
    }
}