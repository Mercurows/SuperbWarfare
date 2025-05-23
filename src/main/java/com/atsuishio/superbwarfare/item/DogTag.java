package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.tooltip.component.DogTagImageComponent;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.menu.DogTagEditorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class DogTag extends Item implements ICurioItem {

    public DogTag() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        LivingEntity livingEntity = slotContext.entity();
        AtomicBoolean flag = new AtomicBoolean(true);
        CuriosApi.getCuriosInventory(livingEntity).flatMap(c -> c.findFirstCurio(this)).ifPresent(s -> flag.set(false));

        return flag.get();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        } else {
            player.openMenu(new SimpleMenuProvider((i, inventory, p) ->
                    new DogTagEditorMenu(i, ContainerLevelAccess.create(level, p.getOnPos()), stack), Component.empty()));
            return InteractionResultHolder.consume(stack);
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new DogTagImageComponent(pStack));
    }

    public static short[][] getColors(ItemStack stack) {
        short[][] colors = new short[16][16];
        for (var el : colors) {
            Arrays.fill(el, (short) -1);
        }

        var data = stack.get(ModDataComponents.DOG_TAG_IMAGE);
        if (data == null) return colors;

        var index = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                colors[i][j] = data.get(index);
                index++;
            }
        }

        return colors;
    }
}
