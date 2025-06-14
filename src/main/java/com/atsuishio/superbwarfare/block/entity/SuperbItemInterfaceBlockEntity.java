package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

public class SuperbItemInterfaceBlockEntity extends BaseContainerBlockEntity {

    public static final int MOVE_ITEM_SPEED = 64;
    public static final int CONTAINER_SIZE = 5;
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int cooldownTime = -1;
    private long tickedGameTime;


    public SuperbItemInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SUPERB_ITEM_INTERFACE.get(), pPos, pBlockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SuperbItemInterfaceBlockEntity blockEntity) {
        --blockEntity.cooldownTime;
        blockEntity.tickedGameTime = level.getGameTime();
        if (!blockEntity.isOnCooldown()) {
            blockEntity.setCooldown(0);
//            tryMoveItems(level, pos, state, blockEntity, () -> suckInItems(level, blockEntity));
        }
    }

    private static boolean tryMoveItems(Level pLevel, BlockPos pPos, BlockState pState, SuperbItemInterfaceBlockEntity pBlockEntity, BooleanSupplier pValidator) {
        if (!pLevel.isClientSide) {
            if (!pBlockEntity.isOnCooldown() && pState.getValue(HopperBlock.ENABLED)) {
                boolean flag = false;
                if (!pBlockEntity.isEmpty()) {
                    flag = ejectItems(pLevel, pPos, pState, pBlockEntity);
                }

                if (!pBlockEntity.inventoryFull()) {
                    flag |= pValidator.getAsBoolean();
                }

                if (flag) {
                    pBlockEntity.setCooldown(8);
                    setChanged(pLevel, pPos, pState);
                    return true;
                }
            }

        }
        return false;
    }

    private static boolean ejectItems(Level pLevel, BlockPos pPos, BlockState pState, SuperbItemInterfaceBlockEntity pSourceContainer) {
//        if (net.minecraftforge.items.VanillaInventoryCodeHooks.insertHook(pSourceContainer)) return true;
//        Container container = getAttachedContainer(pLevel, pPos, pState);
//        if (container == null) {
//            return false;
//        } else {
        // TODO 替换成开启吸物品的directions
//            Direction direction = pState.getValue(HopperBlock.FACING).getOpposite();
//            if (!isFullContainer(container, direction)) {
//                for (int i = 0; i < pSourceContainer.getContainerSize(); ++i) {
//                    if (!pSourceContainer.getItem(i).isEmpty()) {
//                        ItemStack itemstack = pSourceContainer.getItem(i).copy();
//                        ItemStack itemstack1 = addItem(pSourceContainer, container, pSourceContainer.removeItem(i, 1), direction);
//                        if (itemstack1.isEmpty()) {
//                            container.setChanged();
//                            return true;
//                        }
//
//                        pSourceContainer.setItem(i, itemstack);
//                    }
//                }
//            }
        return false;
//        }
    }

    private static IntStream getSlots(Container pContainer, Direction pDirection) {
        return pContainer instanceof WorldlyContainer ? IntStream.of(((WorldlyContainer) pContainer).getSlotsForFace(pDirection)) : IntStream.range(0, pContainer.getContainerSize());
    }

    private static boolean isFullContainer(Container pContainer, Direction pDirection) {
        return getSlots(pContainer, pDirection).allMatch((p_59379_) -> {
            ItemStack itemstack = pContainer.getItem(p_59379_);
            return itemstack.getCount() >= itemstack.getMaxStackSize();
        });
    }

    private boolean inventoryFull() {
        for (ItemStack itemstack : this.items) {
            if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.cooldownTime = tag.getInt("TransferCooldown");
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("TransferCooldown", this.cooldownTime);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.superbwarfare.superb_item_interface");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pInventory) {
        return null;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int pSlot, int pAmount) {
        return ContainerHelper.removeItem(this.items, pSlot, pAmount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pSlot) {
        return ContainerHelper.takeItem(this.items, pSlot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return Container.stillValidBlockEntity(this, pPlayer);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    public void setCooldown(int pCooldownTime) {
        this.cooldownTime = pCooldownTime;
    }

    private boolean isOnCooldown() {
        return this.cooldownTime > 0;
    }

    public long getLastUpdateTime() {
        return this.tickedGameTime;
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(@NotNull NonNullList<ItemStack> items) {
        this.items = items;
    }
}
