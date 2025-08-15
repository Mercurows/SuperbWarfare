package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class VehicleAssemblingMenu extends AbstractContainerMenu {

    public VehicleAssemblingMenu(int pContainerId, Inventory inventory) {
        super(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), pContainerId);

    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.isAlive();
    }
}
