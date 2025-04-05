package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PotionMortarShell extends MortarShell {

    public PotionMortarShell() {
        super();
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack itemstack = super.getDefaultInstance();
        itemstack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
        return itemstack;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        PotionContents potioncontents = stack.get(DataComponents.POTION_CONTENTS);
        if (potioncontents != null) {
            potioncontents.addPotionTooltip(tooltipComponents::add, 0.125F, context.tickRate());
        }
    }

    @SubscribeEvent
    public static void onRegisterColorHandlers(final RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 1 ? FastColor.ARGB32.opaque(stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor()) : -1,
                ModItems.POTION_MORTAR_SHELL.get());
    }
}
