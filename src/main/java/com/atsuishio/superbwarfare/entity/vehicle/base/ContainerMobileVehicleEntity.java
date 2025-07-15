package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;

public abstract class ContainerMobileVehicleEntity extends MobileVehicleEntity implements HasCustomInventoryScreen, ContainerEntity {

    public static final int DEFAULT_CONTAINER_SIZE = 102;

    @Override
    public int getContainerSize() {
        return DEFAULT_CONTAINER_SIZE;
    }

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
        if (player.isShiftKeyDown() && !stack.is(ModTags.Items.CROWBAR)) {
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

        if (this.hasEnergyStorage() && this.tickCount % 20 == 0) {
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
    public boolean hasMenu() {
        return true;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, Player pPlayer) {
        if (!pPlayer.isSpectator() && this.hasMenu()) {
            return new VehicleMenu(pContainerId, pPlayerInventory, this);
        }
        return null;
    }

    @Override
    public void stopOpen(@NotNull Player pPlayer) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(pPlayer));
    }

    @Override
    public @NotNull NonNullList<ItemStack> getItemStacks() {
        return this.items;
    }

    @Override
    public void clearItemStacks() {
        this.items.clear();
    }
}
