package com.atsuishio.superbwarfare.inventory.menu.vehicle;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.SlotItemHandler;

public class MiniVehicleContainer extends AbstractVehicleContainer {

    public static final MenuType<MiniVehicleContainer> TYPE =
            IForgeMenuType.create((windowId, inv, data) -> new MiniVehicleContainer(windowId, inv, data.readInt()));

    protected MiniVehicleContainer(int pContainerId, Inventory inventory, int entityId) {
        super(TYPE, pContainerId, inventory, entityId);
    }

    @Override
    protected void addVehicleInventory() {
        var inv = this.vehicle.getInventory();
        for (int i = 0; i < 9; i++) {
            addSlot(new SlotItemHandler(inv, i, 143 + 18 * i, 59));
        }
    }
}
