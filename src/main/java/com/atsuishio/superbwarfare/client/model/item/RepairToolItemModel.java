package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.special.RepairToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animation.AnimationState;

public class RepairToolItemModel extends CustomGunModel<RepairToolItem> {

    @Override
    public ResourceLocation getAnimationResource(RepairToolItem animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(RepairToolItem animatable) {
        return Mod.loc("geo/repair_tool.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RepairToolItem animatable) {
        return Mod.loc("textures/item/repair_tool.png");
    }

    @Override
    public void setCustomAnimations(RepairToolItem animatable, long instanceId, AnimationState<RepairToolItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;
        ClientEventHandler.gunRootMove(getAnimationProcessor(), 3, 0, 0, false);
    }
}
