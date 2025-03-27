package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.CustomRendererArmor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import javax.annotation.ParametersAreNonnullByDefault;

public class ArmorRendererRegister {
    @SubscribeEvent
    private static void registerArmorExtensions(RegisterClientExtensionsEvent event) {

        for (var item : ModItems.ITEMS.getEntries()) {
            if (!(item instanceof CustomRendererArmor armor)) continue;
            event.registerItem(new IClientItemExtensions() {

                private GeoArmorRenderer<?> renderer;

                @Override
                @ParametersAreNonnullByDefault
                public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                    if (this.renderer == null)
                        this.renderer = armor.getRenderer();

                    // TODO other params?
                    this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                    return this.renderer;
                }

            }, item);
        }
    }
}
