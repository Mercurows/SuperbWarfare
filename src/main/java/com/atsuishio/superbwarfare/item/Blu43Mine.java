package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.Blu43Entity;
import com.atsuishio.superbwarfare.init.ModEntities;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;

import javax.annotation.ParametersAreNonnullByDefault;

public class Blu43Mine extends Item {
    public Blu43Mine() {
        super(new Properties());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            float randomRot = (float) Mth.clamp((2 * Math.random() - 1) * 180, -180, 180);
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

        return InteractionResultHolder.consume(stack);
    }

    public static class Blu43MineDispenseBehavior extends DefaultDispenseItemBehavior {
        @Override
        @ParametersAreNonnullByDefault
        protected @NotNull ItemStack execute(BlockSource blockSource, ItemStack stack) {
            Level level = blockSource.level();
            Position position = DispenserBlock.getDispensePosition(blockSource);
            Direction direction = blockSource.state().getValue(DispenserBlock.FACING);

            var blu43 = new Blu43Entity(ModEntities.BLU_43.get(), level);
            blu43.setPos(position.x(), position.y(), position.z());
            float randomRot = (float) Mth.clamp((2 * Math.random() - 1) * 180, -180, 180);

            var pX = direction.getStepX();
            var pY = direction.getStepY() + 0.1F;
            var pZ = direction.getStepZ();
            Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().scale(0.05);
            blu43.setDeltaMovement(vec3);
            double d0 = vec3.horizontalDistance();
            blu43.setYRot(randomRot);
            blu43.setXRot((float) (Mth.atan2(vec3.y, d0) * (double) (180F / (float) Math.PI)));
            blu43.yRotO = blu43.getYRot();
            blu43.xRotO = blu43.getXRot();

            level.addFreshEntity(blu43);
            stack.shrink(1);
            return stack;
        }
    }
}
