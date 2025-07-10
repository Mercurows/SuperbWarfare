package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen;
import com.atsuishio.superbwarfare.client.tooltip.component.DogTagImageComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class DogTag extends Item implements ICurioItem, ItemScreenProvider {

    public DogTag() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        LivingEntity livingEntity = slotContext.entity();
        AtomicBoolean flag = new AtomicBoolean(true);
        CuriosApi.getCuriosInventory(livingEntity).ifPresent(c -> c.findFirstCurio(this).ifPresent(s -> flag.set(false)));

        return flag.get();
    }

//    @Override
//    @ParametersAreNonnullByDefault
//    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
//        ItemStack stack = pPlayer.getItemInHand(pUsedHand);
//        if (pLevel.isClientSide) {
//            return InteractionResultHolder.success(stack);
//        } else {
//            pPlayer.openMenu(new SimpleMenuProvider((i, inventory, player) ->
//                    new DogTagEditorMenu(i, ContainerLevelAccess.create(pLevel, pPlayer.getOnPos()), stack), Component.empty()));
//            return InteractionResultHolder.consume(stack);
//        }
//    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new DogTagImageComponent(pStack));
    }

    public static short[][] getColors(ItemStack stack) {
        short[][] colors = new short[16][16];
        for (var el : colors) {
            Arrays.fill(el, (short) -1);
        }

        if (stack.getTag() == null) return colors;
        CompoundTag tag = stack.getTag().getCompound("Colors");
        for (int i = 0; i < 16; i++) {
            int[] color = tag.getIntArray("Color" + i);
            for (int j = 0; j < color.length; j++) {
                colors[i][j] = (short) color[j];
            }
        }

        return colors;
    }

    @Override
    public @Nullable Screen getItemScreen(ItemStack stack, Player player) {
        return new DogTagEditorScreen(stack);
    }
}
