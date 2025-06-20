package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.SuperbItemInterfaceBlock;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class SuperbItemInterfaceBlockEntity extends BaseContainerBlockEntity {

    public static final int TRANSFER_COOLDOWN = 20;
    public static final int CONTAINER_SIZE = 5;
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int cooldownTime = -1;

    private Direction facing;


    public SuperbItemInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SUPERB_ITEM_INTERFACE.get(), pPos, pBlockState);

        this.facing = pBlockState.getValue(SuperbItemInterfaceBlock.FACING);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SuperbItemInterfaceBlockEntity blockEntity) {
        --blockEntity.cooldownTime;
        if (blockEntity.isOnCooldown()) return;
        blockEntity.setCooldown(TRANSFER_COOLDOWN);

        if (blockEntity.isEmpty()) return;

        // find entities
        var x = pos.getX() + blockEntity.facing.getStepX();
        var y = pos.getY() + blockEntity.facing.getStepY();
        var z = pos.getZ() + blockEntity.facing.getStepZ();

        var list = level.getEntities(
                (Entity) null,
                new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5),
                entity -> entity.getCapability(Capabilities.ItemHandler.ENTITY, null) != null
        );
        if (list.isEmpty()) return;
        var target = list.get(level.random.nextInt(list.size()));

        // item transfer

        var index = -1;
        for (int i = 0; i < blockEntity.items.size(); i++) {
            var stack = blockEntity.items.get(i);
            if (!stack.isEmpty()) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        var stack = blockEntity.items.get(index);
        var itemHandler = target.getCapability(Capabilities.ItemHandler.ENTITY, null);
        assert itemHandler != null;

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (stack.isEmpty()) break;

            stack = itemHandler.insertItem(i, stack, false);
        }

        blockEntity.items.set(index, stack);
        blockEntity.setChanged();
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

    // TODO 实现菜单
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
        return this.items.stream().allMatch(ItemStack::isEmpty);
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

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(@NotNull NonNullList<ItemStack> items) {
        this.items = items;
    }
}
