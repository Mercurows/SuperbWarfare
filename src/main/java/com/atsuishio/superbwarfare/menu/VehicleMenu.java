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

public class VehicleMenu extends AbstractContainerMenu {

    private final Container container;
    private final int containerRows;
    private final int containerCols;
    private final boolean hasUpgradeSlots;

    public static final int X_OFFSET = 97;
    public static final int Y_OFFSET = 20;

    public VehicleMenu(MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, int row, int col, boolean hasUpgradeSlots) {
        this(pType, pContainerId, pPlayerInventory, new SimpleContainer(row * col), row, col, hasUpgradeSlots);
    }

    public static VehicleMenu mini(int pContainerId, Inventory pPlayerInventory, boolean hasUpgradeSlots) {
        return new VehicleMenu(hasUpgradeSlots ? ModMenuTypes.VEHICLE_MENU_MINI_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_MINI.get(),
                pContainerId, pPlayerInventory, 1, 9, hasUpgradeSlots);
    }

    public static VehicleMenu small(int pContainerId, Inventory pPlayerInventory, boolean hasUpgradeSlots) {
        return new VehicleMenu(hasUpgradeSlots ? ModMenuTypes.VEHICLE_MENU_SMALL_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_SMALL.get(),
                pContainerId, pPlayerInventory, 3, 9, hasUpgradeSlots);
    }

    public static VehicleMenu medium(int pContainerId, Inventory pPlayerInventory, boolean hasUpgradeSlots) {
        return new VehicleMenu(hasUpgradeSlots ? ModMenuTypes.VEHICLE_MENU_MEDIUM_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_MEDIUM.get(),
                pContainerId, pPlayerInventory, 6, 9, hasUpgradeSlots);
    }

    public static VehicleMenu large(int pContainerId, Inventory pPlayerInventory, boolean hasUpgradeSlots) {
        return new VehicleMenu(hasUpgradeSlots ? ModMenuTypes.VEHICLE_MENU_LARGE_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_LARGE.get(),
                pContainerId, pPlayerInventory, 6, 13, hasUpgradeSlots);
    }

    public static VehicleMenu huge(int pContainerId, Inventory pPlayerInventory, boolean hasUpgradeSlots) {
        return new VehicleMenu(hasUpgradeSlots ? ModMenuTypes.VEHICLE_MENU_HUGE_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_HUGE.get(),
                pContainerId, pPlayerInventory, 6, 17, hasUpgradeSlots);
    }

    public VehicleMenu(MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, Container pContainer, int row, int col, boolean hasUpgradeSlots) {
        super(pType, pContainerId);

        int size = row * col;
        if (hasUpgradeSlots) {
            size += 3;
        }

        checkContainerSize(pContainer, size);

        this.container = pContainer;
        this.containerRows = row;
        this.containerCols = col;
        this.hasUpgradeSlots = hasUpgradeSlots;

        pContainer.startOpen(pPlayerInventory.player);
        int i = (this.containerRows - 4) * 18;

        int colOffset = (this.containerCols - 9) / 2 * 18;

        for (int j = 0; j < this.containerRows; ++j) {
            for (int k = 0; k < this.containerCols; ++k) {
                this.addSlot(new Slot(pContainer, k + j * this.containerCols, 16 + k * 18, 18 + j * 18));
            }
        }

        if (this.hasUpgradeSlots) {
            for (int m = 0; m < 3; ++m) {
                this.addSlot(new Slot(pContainer, m, colOffset - 4, 84 + m * 18 + Y_OFFSET + i));
            }
        }

        for (int l = 0; l < 3; ++l) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(pPlayerInventory, j + l * 9 + 9, 16 + j * 18 + colOffset, 84 + l * 18 + Y_OFFSET + i));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(pPlayerInventory, k, 16 + k * 18 + colOffset, 142 + Y_OFFSET + i));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // TODO 在有upgrade slots的情况下，完成对于载具perk的移动判断
            if (pIndex < this.containerRows * this.containerCols) {
                if (!this.moveItemStackTo(itemstack1, this.containerRows * this.containerCols, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.containerRows * this.containerCols, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    public int getContainerRows() {
        return containerRows;
    }

    public int getContainerCols() {
        return containerCols;
    }

    public boolean hasUpgradeSlots() {
        return hasUpgradeSlots;
    }
}
