package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.init.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SuperbItemInterfaceMenu extends AbstractContainerMenu {

    public static final int CONTAINER_SIZE = 5;
    private final Container container;

    public SuperbItemInterfaceMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE));
    }

    public SuperbItemInterfaceMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenuTypes.SUPERB_ITEM_INTERFACE_MENU.get(), containerId);
        this.container = container;
        checkContainerSize(container, CONTAINER_SIZE);
        container.startOpen(playerInventory.player);

        for (int j = 0; j < CONTAINER_SIZE; j++) {
            this.addSlot(new Slot(container, j, 44 + j * 18, 20));
        }

        for (int l = 0; l < 3; l++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, l * 18 + 51));
            }
        }

        for (int i1 = 0; i1 < 9; i1++) {
            this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 109));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var stack = ItemStack.EMPTY;
        var slot = this.slots.get(index);
        if (slot.hasItem()) {
            var slotItem = slot.getItem();
            stack = slotItem.copy();
            if (index < this.container.getContainerSize()) {
                if (!this.moveItemStackTo(slotItem, this.container.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotItem, 0, this.container.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (slotItem.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return stack;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
