package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.init.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

// TODO 图标和简介等
public class VehicleDeployerBlockItem extends BlockItem {
    public VehicleDeployerBlockItem() {
        super(ModBlocks.VEHICLE_DEPLOYER.get(), new Properties().stacksTo(1));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("test"));
    }
}
