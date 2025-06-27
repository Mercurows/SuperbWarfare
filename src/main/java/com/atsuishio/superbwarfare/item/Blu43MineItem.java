package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.Blu43Entity;
import com.atsuishio.superbwarfare.init.ModEntities;
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
import org.jetbrains.annotations.NotNull;
import org.joml.Math;

import javax.annotation.ParametersAreNonnullByDefault;

public class Blu43MineItem extends Item implements DispenserLaunchable {

    public Blu43MineItem() {
        super(new Properties());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            float randomRot = (float) Mth.clamp((2 * Math.random() - 1) * 180 , -180, 180);
            Blu43Entity entity = new Blu43Entity(player, level);
            entity.moveTo(player.getX(), player.getY() + 1.1, player.getZ(), randomRot, 0);
            entity.setYBodyRot(randomRot);
            entity.setYHeadRot(randomRot);
            entity.setDeltaMovement(0.5 * player.getLookAngle().x, 0.5 * player.getLookAngle().y, 0.5 * player.getLookAngle().z);

            level.addFreshEntity(entity);
        }

        player.getCooldowns().addCooldown(this, 4);

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

                var blu43 = new Blu43Entity(ModEntities.BLU_43.get(), level);
                blu43.setPos(position.x(), position.y(), position.z());
                float randomRot = (float) Mth.clamp((2 * Math.random() - 1) * 180 , -180, 180);

                var pX = direction.getStepX();
                var pY = direction.getStepY();
                var pZ = direction.getStepZ();
                blu43.shoot(pX, pY, pZ, 0.4f, 10);
                blu43.setYRot(randomRot);
                blu43.yRotO = blu43.getYRot();

                level.addFreshEntity(blu43);
                pStack.shrink(1);
                return pStack;
            }
        };
    }
}
