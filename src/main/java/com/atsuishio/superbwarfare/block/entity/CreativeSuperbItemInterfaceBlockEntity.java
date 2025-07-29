package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeSuperbItemInterfaceBlockEntity extends SuperbItemInterfaceBlockEntity {
    public CreativeSuperbItemInterfaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CREATIVE_SUPERB_ITEM_INTERFACE.get(), pos, blockState);
    }

    @Override
    protected boolean isCreative() {
        return true;
    }
}
