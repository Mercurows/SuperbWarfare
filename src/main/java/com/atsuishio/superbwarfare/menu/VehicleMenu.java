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

public class VehicleMenu extends AbstractContainerMenu {

    private final Container container;
    private final int containerRows;

    public static final int DEFAULT_SIZE = 102;

    public static final int X_OFFSET = 97;
    public static final int Y_OFFSET = 20;

    public VehicleMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, new SimpleContainer(DEFAULT_SIZE));
    }

    public VehicleMenu(int pContainerId, Inventory pPlayerInventory, Container pContainer) {
        super(ModMenuTypes.VEHICLE_MENU.get(), pContainerId);

        checkContainerSize(pContainer, DEFAULT_SIZE);
        this.container = pContainer;
        this.containerRows = 6;
        pContainer.startOpen(pPlayerInventory.player);
        int i = (this.containerRows - 4) * 18;

        for (int j = 0; j < this.containerRows; ++j) {
            for (int k = 0; k < 17; ++k) {
                this.addSlot(new Slot(pContainer, k + j * 17, 8 + k * 18 + 25, 18 + j * 18));
            }
        }

        for (int l = 0; l < 3; ++l) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(pPlayerInventory, j + l * 9 + 9, 8 + j * 18 + X_OFFSET, 84 + l * 18 + Y_OFFSET + i));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(pPlayerInventory, k, 8 + k * 18 + X_OFFSET, 142 + Y_OFFSET + i));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (pIndex < this.containerRows * 17 + 3) {
                if (!this.moveItemStackTo(stack, this.containerRows * 17 + 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, this.containerRows * 17, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }
}
