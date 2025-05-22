package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class C4Bomb extends Item {

    public static final String TAG_CONTROL = "Control";

    public C4Bomb() {
        super(new Properties());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            boolean flag = NBTTool.getTag(stack).getBoolean(TAG_CONTROL);

            C4Entity entity = new C4Entity(player, level, flag);
            entity.setPos(player.getX() + 0.25 * player.getLookAngle().x, player.getEyeY() - 0.2f + 0.25 * player.getLookAngle().y, player.getZ() + 0.25 * player.getLookAngle().z);
            entity.setDeltaMovement(0.5 * player.getLookAngle().x, 0.5 * player.getLookAngle().y, 0.5 * player.getLookAngle().z);
            entity.setOwnerUUID(player.getUUID());

            level.addFreshEntity(entity);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.C4_THROW.get(), SoundSource.PLAYERS, 1, 1);
        }

        player.getCooldowns().addCooldown(this, 20);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (NBTTool.getTag(stack).getBoolean(TAG_CONTROL)) {
            tooltipComponents.add(Component.translatable("des.superbwarfare.c4_bomb.control").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("des.superbwarfare.c4_bomb.time").withStyle(ChatFormatting.GRAY));
        }
    }

    public static ItemStack makeInstance() {
        ItemStack stack = new ItemStack(ModItems.C4_BOMB.get());
        final var tag = NBTTool.getTag(stack);
        tag.putBoolean(TAG_CONTROL, true);
        NBTTool.saveTag(stack, tag);
        return stack;
    }

    public static class C4DispenseItemBehavior extends DefaultDispenseItemBehavior {
        @Override
        @ParametersAreNonnullByDefault
        protected @NotNull ItemStack execute(BlockSource blockSource, ItemStack stack) {
            Level level = blockSource.level();
            Position position = DispenserBlock.getDispensePosition(blockSource);
            Direction direction = blockSource.state().getValue(DispenserBlock.FACING);

            var entity = new C4Entity(ModEntities.C_4.get(), level);
            entity.setPos(position.x(), position.y(), position.z());

            var pX = direction.getStepX();
            var pY = direction.getStepY() + 0.1F;
            var pZ = direction.getStepZ();
            Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().scale(0.05);
            entity.setDeltaMovement(vec3);
            double d0 = vec3.horizontalDistance();
            entity.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)));
            entity.setXRot((float) (Mth.atan2(vec3.y, d0) * (double) (180F / (float) Math.PI)));
            entity.yRotO = entity.getYRot();
            entity.xRotO = entity.getXRot();

            level.addFreshEntity(entity);
            stack.shrink(1);
            return stack;
        }
    }
}
