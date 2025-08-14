package com.atsuishio.superbwarfare.block;

import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class VehicleAssemblingTableBlock extends Block implements MenuProvider {

    public VehicleAssemblingTableBlock() {
        super(BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops());
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("哼哼啊啊啊啊啊啊阿");
    }

    @Nullable
    @ParametersAreNonnullByDefault
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new VehicleAssemblingMenu(pContainerId, pPlayerInventory);
    }
}
