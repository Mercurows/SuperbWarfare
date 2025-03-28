package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class Defuser extends Item {

    public Defuser() {
        super(new Properties().durability(8));
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack stack = pPlayer.getItemInHand(pUsedHand);
        if (findBombInSight(pPlayer) != null) {
            pPlayer.startUsingItem(pUsedHand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    private static C4Entity findBombInSight(Player player) {
        Entity target = TraceTool.findLookingEntity(player, 4);
        return target instanceof C4Entity c4Entity ? c4Entity : null;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pRemainingUseDuration) {
        if (!(pLivingEntity instanceof Player player)) return;
        var target = findBombInSight(player);
        if (target == null) return;

        int useTick = pStack.getUseDuration(player) - pRemainingUseDuration;

        if (!pLevel.isClientSide) {
            player.displayClientMessage(Component.literal(
                    FormatTool.format1DZZ((C4Entity.DEFAULT_DEFUSE_PROGRESS - useTick) / 20d, "s")
            ).withStyle(ChatFormatting.GREEN), true);
        }

        if (useTick >= C4Entity.DEFAULT_DEFUSE_PROGRESS) {
            player.stopUsingItem();
            if (pLevel instanceof ServerLevel serverLevel) {
                pStack.hurtAndBreak(1, serverLevel, player, p -> {
                });
            }
            target.defuse();
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }
}
