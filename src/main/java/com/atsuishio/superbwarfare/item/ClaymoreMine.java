package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.ClaymoreEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

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

    // TODO 发射器发射
//    @Override
//    public DispenseItemBehavior getLaunchBehavior() {
//        return new ProjectileDispenseBehavior() {
//            @Override
//            @ParametersAreNonnullByDefault
//            public @NotNull ItemStack execute(BlockSource pSource, ItemStack pStack) {
//                Level level = pSource.getLevel();
//                Position position = DispenserBlock.getDispensePosition(pSource);
//                Direction direction = pSource.getBlockState().getValue(DispenserBlock.FACING);
//
//                var claymore = new ClaymoreEntity(ModEntities.CLAYMORE.get(), level);
//                claymore.setPos(position.x(), position.y(), position.z());
//
//                var pX = direction.getStepX();
//                var pY = direction.getStepY() + 0.1F;
//                var pZ = direction.getStepZ();
//                Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().scale(0.05);
//                claymore.setDeltaMovement(vec3);
//                double d0 = vec3.horizontalDistance();
//                claymore.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)));
//                claymore.setXRot((float) (Mth.atan2(vec3.y, d0) * (double) (180F / (float) Math.PI)));
//                claymore.yRotO = claymore.getYRot();
//                claymore.xRotO = claymore.getXRot();
//
//                level.addFreshEntity(claymore);
//                pStack.shrink(1);
//                return pStack;
//            }
//        };
//    }
//
//    @Override
//    @ParametersAreNonnullByDefault
//    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
//        return null;
//    }
}
