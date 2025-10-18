package com.atsuishio.superbwarfare.inventory.menu.vehicle;

import com.atsuishio.superbwarfare.data.vehicle.VehicleProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Code based on TartaricAcid's <a href="https://github.com/TartaricAcid/TouhouLittleMaid">TouhouLittleMaid</a>
 */
public abstract class AbstractVehicleContainer extends AbstractContainerMenu {

    public static final int PLAYER_INVENTORY_SIZE = 36;

    protected final VehicleEntity vehicle;

    protected AbstractVehicleContainer(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory inventory, int entityId) {
        super(pMenuType, pContainerId);
        this.vehicle = (VehicleEntity) inventory.player.level().getEntity(entityId);
        if (vehicle != null) {
            this.addPlayerInventory(inventory);
            this.addVehicleInventory();
            this.addUpgradeInventory();
        }
    }

    @Nullable
    public VehicleEntity getVehicle() {
        return vehicle;
    }

    protected abstract void addVehicleInventory();

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 16 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inventory, col, 16 + col * 18, 142));
        }
    }

    private void addUpgradeInventory() {
        var hasSlots = this.vehicle.data().get(VehicleProp.HAS_UPGRADE_SLOTS);
        if (!hasSlots) return;

        var upgrades = this.vehicle.getUpgradesInventory();

        for (int m = 0; m < 3; ++m) {
            this.addSlot(new SlotItemHandler(upgrades, m, -4, 84 + m * 18));
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        if (this.vehicle == null) {
            return false;
        }
        return this.vehicle.hasContainer() && !this.vehicle.isRemoved() && this.vehicle.position().closerThan(pPlayer.position(), 8.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack stack1 = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        //noinspection ConstantValue
        if (slot != null && slot.hasItem()) {
            ItemStack stack2 = slot.getItem();
            stack1 = stack2.copy();
            if (index < PLAYER_INVENTORY_SIZE) {
                if (!this.moveItemStackTo(stack2, PLAYER_INVENTORY_SIZE, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack2, 0, PLAYER_INVENTORY_SIZE, true)) {
                return ItemStack.EMPTY;
            }
            if (stack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack1;
    }
}
