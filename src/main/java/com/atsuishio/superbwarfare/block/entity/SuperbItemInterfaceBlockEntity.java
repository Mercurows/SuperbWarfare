package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class SuperbItemInterfaceBlockEntity extends BaseContainerBlockEntity {

    public SuperbItemInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SUPERB_ITEM_INTERFACE.get(), pPos, pBlockState);
    }
    
    @Override
    protected @NotNull Component getDefaultName() {
        return Component.empty();
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return null;
    }

    @Override
    protected void setItems(@NotNull NonNullList<ItemStack> items) {

    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pInventory) {
        return null;
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public @NotNull ItemStack removeItem(int pSlot, int pAmount) {
        return null;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pSlot) {
        return null;
    }


    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return false;
    }

    @Override
    public void clearContent() {

    }
}
