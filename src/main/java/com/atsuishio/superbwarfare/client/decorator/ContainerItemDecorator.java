package com.atsuishio.superbwarfare.client.decorator;

import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.IItemDecorator;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ContainerItemDecorator implements IItemDecorator {

    private static final Map<String, ResourceLocation> icons = new HashMap<>();

    @Override
    @ParametersAreNonnullByDefault
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (!(stack.getItem() instanceof ContainerBlockItem)) return false;
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return false;
        var tag = data.copyTag();

        ResourceLocation icon = null;
        if (tag.contains("EntityType")) {
            var typeString = tag.getString("EntityType");

            if (icons.containsKey(typeString)) {
                icon = icons.get(typeString);
            } else {
                EntityType<?> entityType;
                entityType = EntityType.byString(typeString).orElse(null);
                if (entityType == null) return false;

                Minecraft mc = Minecraft.getInstance();
                var level = mc.level;
                if (level == null) return false;

                var entity = entityType.create(level);
                if (!(entity instanceof VehicleEntity vehicle)) return false;

                icon = vehicle.getVehicleItemIcon();
                icons.put(typeString, icon);
            }
        }
        if (icon == null) return false;

        var pose = guiGraphics.pose();
        pose.pushPose();

        RenderHelper.preciseBlit(guiGraphics, icon, xOffset, yOffset, 200, 0, 0, 8, 8, 8, 8);

        pose.popPose();

        return true;
    }
}
