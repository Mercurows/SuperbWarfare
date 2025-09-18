package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.data.gun.Ammo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class SniperAmmoBox extends AmmoSupplierItem {

    public SniperAmmoBox() {
        super(Ammo.SNIPER, 12, new Item.Properties());
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);
        list.add(Component.translatable("des.superbwarfare.sniper_ammo_box").withStyle(ChatFormatting.GRAY));
    }
}
