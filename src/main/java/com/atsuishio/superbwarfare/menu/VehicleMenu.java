package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.init.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class VehicleMenu extends AbstractContainerMenu {

    private final Container container;
    private final int containerRows;
    private final int containerCols;

    public static final int X_OFFSET = 97;
    public static final int Y_OFFSET = 20;

    public VehicleMenu(MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, int row, int col) {
        this(pType, pContainerId, pPlayerInventory, new SimpleContainer(row * col), row, col);
    }

    public static VehicleMenu mini(int pContainerId, Inventory pPlayerInventory) {
        return new VehicleMenu(ModMenuTypes.VEHICLE_MENU_MINI.get(), pContainerId, pPlayerInventory, 1, 9);
    }

    public static VehicleMenu small(int pContainerId, Inventory pPlayerInventory) {
        return new VehicleMenu(ModMenuTypes.VEHICLE_MENU_SMALL.get(), pContainerId, pPlayerInventory, 3, 9);
    }

    public static VehicleMenu medium(int pContainerId, Inventory pPlayerInventory) {
        return new VehicleMenu(ModMenuTypes.VEHICLE_MENU_MEDIUM.get(), pContainerId, pPlayerInventory, 6, 9);
    }

    public static VehicleMenu large(int pContainerId, Inventory pPlayerInventory) {
        return new VehicleMenu(ModMenuTypes.VEHICLE_MENU_LARGE.get(), pContainerId, pPlayerInventory, 6, 13);
    }

    public static VehicleMenu huge(int pContainerId, Inventory pPlayerInventory) {
        return new VehicleMenu(ModMenuTypes.VEHICLE_MENU_HUGE.get(), pContainerId, pPlayerInventory, 6, 17);
    }

    public VehicleMenu(MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, Container pContainer, int row, int col) {
        super(pType, pContainerId);

        checkContainerSize(pContainer, row * col);

        this.container = pContainer;
        this.containerRows = row;
        this.containerCols = col;

        pContainer.startOpen(pPlayerInventory.player);
        int i = (this.containerRows - 4) * 18;

        for (int j = 0; j < this.containerRows; ++j) {
            for (int k = 0; k < this.containerCols; ++k) {
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
            if (pIndex < this.containerRows * this.containerCols) {
                if (!this.moveItemStackTo(stack, this.containerRows * this.containerCols, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, this.containerRows * this.containerCols, false)) {
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
