package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.special.RepairToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animation.AnimationState;

public class RepairToolItemModel extends CustomGunModel<RepairToolItem> {

    @Override
    public void setCustomAnimations(RepairToolItem animatable, long instanceId, AnimationState<RepairToolItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;
        ClientEventHandler.gunRootMove(getAnimationProcessor(), 3, 0, 0, false);
    }
}
