package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity;
import com.atsuishio.superbwarfare.init.ModMenuTypes;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import com.atsuishio.superbwarfare.network.dataslot.SimpleEnergyData;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;

public class ChargingStationMenu extends EnergyMenu {

    private final Container container;
    private final ContainerEnergyData containerData;
    protected final Level level;

    public static final int X_OFFSET = 0;
    public static final int Y_OFFSET = 0;

    public ChargingStationMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(2), new SimpleEnergyData(ChargingStationBlockEntity.MAX_DATA_COUNT));
    }

    public ChargingStationMenu(int id, Inventory inventory, Container container, ContainerEnergyData containerData) {
        super(ModMenuTypes.CHARGING_STATION_MENU.get(), id, containerData);

        checkContainerSize(container, 2);

        this.container = container;
        this.containerData = containerData;
        this.level = inventory.player.level();

        this.addSlot(new Slot(container, 0, 44, 54));
        this.addSlot(new ChargingSlot(container, 1, 116, 54));
        this.addDataSlots(containerData);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18 + X_OFFSET, 84 + i * 18 + Y_OFFSET));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inventory, k, 8 + k * 18 + X_OFFSET, 142 + Y_OFFSET));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (pIndex == 1) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex != 0) {
                var itemCapability = itemstack1.getCapability(Capabilities.EnergyStorage.ITEM);
                if (itemCapability != null) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.getBurnTime(RecipeType.SMELTING) > 0 || itemstack1.getFoodProperties(null) != null) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex >= 2 && pIndex < 29) {
                    if (!this.moveItemStackTo(itemstack1, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex >= 29 && pIndex < 38 && !this.moveItemStackTo(itemstack1, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 2, 38, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    public long getFuelTick() {
        return this.containerData.get(0);
    }

    public long getMaxFuelTick() {
        return this.containerData.get(1);
    }

    public long getEnergy() {
        return this.containerData.get(2);
    }

    public boolean showRange() {
        return this.containerData.get(3) == 1;
    }

    public void setShowRange(boolean showRange) {
        this.containerData.set(3, showRange ? 1 : 0);
    }

    static class ChargingSlot extends Slot {

        public ChargingSlot(Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack pStack) {
            return super.mayPlace(pStack);
        }
    }
}
