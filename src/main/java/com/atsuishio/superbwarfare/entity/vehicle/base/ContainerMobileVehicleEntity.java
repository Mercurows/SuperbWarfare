package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
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
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        ContainerHelper.saveAllItems(compound, this.getItemStacks());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        ContainerHelper.loadAllItems(compound, this.getItemStacks());
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getVehicle() == this) return InteractionResult.PASS;

        if (this.hasMenu() && player.isShiftKeyDown() && !player.getMainHandItem().is(ModTags.Items.CROWBAR)) {
            player.openMenu(this);
            return InteractionResult.sidedSuccess(player.level().isClientSide);
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

                var energyCap = stack.getCapability(ForgeCapabilities.ENERGY).resolve();
                if (energyCap.isEmpty()) continue;

                var energyStorage = energyCap.get();
                var stored = energyStorage.getEnergyStored();
                if (stored <= 0) continue;

                int energyToExtract = Math.min(stored, neededEnergy);
                energyStorage.extractEnergy(energyToExtract, false);
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

    @Nullable
    @Override
    public ResourceLocation getLootTable() {
        return null;
    }

    @Override
    public void setLootTable(@Nullable ResourceLocation pLootTable) {
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
