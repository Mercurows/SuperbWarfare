package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VehicleAssemblingTableBlockEntity extends BlockEntity implements MenuProvider {

    public VehicleAssemblingTableBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get(), pPos, pBlockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new VehicleAssemblingMenu(pContainerId, pPlayerInventory);
    }
}
