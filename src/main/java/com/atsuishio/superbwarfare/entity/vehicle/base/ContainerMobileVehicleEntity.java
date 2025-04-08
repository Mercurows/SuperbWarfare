package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;

public abstract class ContainerMobileVehicleEntity extends MobileVehicleEntity implements HasCustomInventoryScreen, ContainerEntity {

    public static final int CONTAINER_SIZE = 102;

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public ContainerMobileVehicleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        ContainerHelper.saveAllItems(compound, this.getItemStacks(), level().registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        ContainerHelper.loadAllItems(compound, this.getItemStacks(), level().registryAccess());
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getVehicle() == this) return InteractionResult.PASS;

        ItemStack stack = player.getMainHandItem();
        if (player.isShiftKeyDown() && !stack.is(ModItems.CROWBAR.get())) {
            player.openMenu(this);
            return !player.level().isClientSide ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
        }

        return super.interact(player, hand);
    }

    @Override
    public void remove(@NotNull RemovalReason pReason) {
        if (!this.level().isClientSide && pReason != RemovalReason.DISCARDED) {
            Containers.dropContents(this.level(), this, this);
        }
        super.remove(pReason);
    }

    @Override
    public void baseTick() {
        super.baseTick();

        for (var stack : this.getItemStacks()) {
            int neededEnergy = this.getMaxEnergy() - this.getEnergy();
            if (neededEnergy <= 0) break;

            var energyCap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (energyCap == null) continue;

            var stored = energyCap.getEnergyStored();
            if (stored <= 0) continue;

            int energyToExtract = Math.min(stored, neededEnergy);
            energyCap.extractEnergy(energyToExtract, false);
            this.setEnergy(this.getEnergy() + energyToExtract);
        }
        this.refreshDimensions();
    }

    @Override
    public void openCustomInventoryScreen(Player pPlayer) {
        pPlayer.openMenu(this);
        if (!pPlayer.level().isClientSide) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, pPlayer);
        }
    }

    @Override
    public ResourceKey<LootTable> getLootTable() {
        return null;
    }

    @Override
    public void setLootTable(@Nullable ResourceKey<LootTable> lootTable) {
    }

    @Override
    public long getLootTableSeed() {
        return 0;
    }

    @Override
    public void setLootTableSeed(long pLootTableSeed) {
    }

    @Override
    public @NotNull NonNullList<ItemStack> getItemStacks() {
        return this.items;
    }

    /**
     * 计算当前载具内指定物品的数量
     *
     * @param item 物品类型
     * @return 物品数量
     */
    public int countItem(@Nullable Item item) {
        if (item == null) return 0;
        return InventoryTool.countItem(this.getItemStacks(), item);
    }

    /**
     * 判断载具内是否包含指定物品
     *
     * @param item 物品类型
     */
    public boolean hasItem(Item item) {
        return countItem(item) > 0;
    }

    /**
     * 消耗载具内指定物品
     *
     * @param item  物品类型
     * @param count 要消耗的数量
     * @return 成功消耗的物品数量
     */
    public int consumeItem(Item item, int count) {
        return InventoryTool.consumeItem(this.getItemStacks(), item, count);
    }

    /**
     * 尝试插入指定物品指定数量，如果载具内已满则生成掉落物
     *
     * @param item  物品类型
     * @param count 要插入的数量
     */
    public void insertItem(Item item, int count) {
        var rest = InventoryTool.insertItem(this.getItemStacks(), item, count);

        if (rest > 0) {
            var stackToDrop = new ItemStack(item, rest);
            this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), stackToDrop));
        }
    }

    @Override
    public void clearItemStacks() {
        this.items.clear();
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public @NotNull ItemStack getItem(int pSlot) {
        return this.items.get(pSlot);
    }

    @Override
    public @NotNull ItemStack removeItem(int pSlot, int pAmount) {
        return ContainerHelper.removeItem(this.items, pSlot, pAmount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pSlot) {
        ItemStack itemstack = this.getItemStacks().get(pSlot);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.getItemStacks().set(pSlot, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int pSlot, @NotNull ItemStack pStack) {
        this.getItemStacks().set(pSlot, pStack);
        if (!pStack.isEmpty() && pStack.getCount() > this.getMaxStackSize()) {
            pStack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return !this.isRemoved() && this.position().closerThan(pPlayer.position(), 8.0D);
    }

    @Override
    public void clearContent() {
        this.getItemStacks().clear();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, Player pPlayer) {
        if (pPlayer.isSpectator()) {
            return null;
        } else {
            return new VehicleMenu(pContainerId, pPlayerInventory, this);
        }
    }

    @Override
    public void stopOpen(@NotNull Player pPlayer) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(pPlayer));
    }
}
