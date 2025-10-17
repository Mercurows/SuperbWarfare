package com.atsuishio.superbwarfare.item.gun.vehicle;

import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// TODO 实现基于VehicleGun的开火控制
public class VehicleGun extends GunItem {

    public VehicleGun() {
        super(new Properties());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.vehicle_gun").withStyle(ChatFormatting.RED));
    }
}