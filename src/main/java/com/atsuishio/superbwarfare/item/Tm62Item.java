package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.renderer.item.Tm62ItemRenderer;
import com.atsuishio.superbwarfare.entity.Tm62Entity;
import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

public class Tm62Item extends Item implements GeoItem, DispenserLaunchable {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Tm62Item() {
        super(new Properties().stacksTo(8));
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new Tm62ItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
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

    @Override
    public DispenseItemBehavior getLaunchBehavior() {
        return new DefaultDispenseItemBehavior() {
            @Override
            @ParametersAreNonnullByDefault
            public @NotNull ItemStack execute(BlockSource pSource, ItemStack pStack) {
                Level level = pSource.getLevel();
                Position position = DispenserBlock.getDispensePosition(pSource);
                Direction direction = pSource.getBlockState().getValue(DispenserBlock.FACING);

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
                pStack.shrink(1);
                return pStack;
            }
        };
    }
}