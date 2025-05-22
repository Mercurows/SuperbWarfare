package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.ClaymoreEntity;
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

import javax.annotation.ParametersAreNonnullByDefault;

public class ClaymoreMine extends Item {
    public ClaymoreMine() {
        super(new Properties());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            ClaymoreEntity entity = new ClaymoreEntity(player, level);
            entity.moveTo(player.getX(), player.getY() + 1.1, player.getZ(), player.getYRot(), 0);
            entity.setYBodyRot(player.getYRot());
            entity.setYHeadRot(player.getYRot());
            entity.setDeltaMovement(0.5 * player.getLookAngle().x, 0.5 * player.getLookAngle().y, 0.5 * player.getLookAngle().z);

            level.addFreshEntity(entity);
        }

        player.getCooldowns().addCooldown(this, 20);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.consume(stack);
    }

    public static class ClaymoreDispenseBehavior extends DefaultDispenseItemBehavior {
        @Override
        @ParametersAreNonnullByDefault
        protected @NotNull ItemStack execute(BlockSource blockSource, ItemStack stack) {
            Level level = blockSource.level();
            Position position = DispenserBlock.getDispensePosition(blockSource);
            Direction direction = blockSource.state().getValue(DispenserBlock.FACING);

            var claymore = new ClaymoreEntity(ModEntities.CLAYMORE.get(), level);
            claymore.setPos(position.x(), position.y(), position.z());

            var pX = direction.getStepX();
            var pY = direction.getStepY() + 0.1F;
            var pZ = direction.getStepZ();
            Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().scale(0.05);
            claymore.setDeltaMovement(vec3);
            double d0 = vec3.horizontalDistance();
            claymore.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)));
            claymore.setXRot((float) (Mth.atan2(vec3.y, d0) * (double) (180F / (float) Math.PI)));
            claymore.yRotO = claymore.getYRot();
            claymore.xRotO = claymore.getXRot();

            level.addFreshEntity(claymore);
            stack.shrink(1);
            return stack;
        }
    }
}
