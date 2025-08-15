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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;

public class VehicleAssemblingTableBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VehicleAssemblingTableBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get(), pPos, pBlockState);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.empty();
    }

    @Nullable
    @ParametersAreNonnullByDefault
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new VehicleAssemblingMenu(pContainerId, pPlayerInventory);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // TODO ???
//    @Override
//    public AABB getRenderBoundingBox() {
//        // 创建一个更大的边界框（示例：覆盖从方块底部到顶部上方2格的范围）
//        double expansion = 2.0; // 根据模型实际大小调整
//        return new AABB(
//                worldPosition.getX() - 1,
//                worldPosition.getY(),
//                worldPosition.getZ() - 1,
//                worldPosition.getX() + 2,
//                worldPosition.getY() + expansion,
//                worldPosition.getZ() + 2
//        );
//    }
}
