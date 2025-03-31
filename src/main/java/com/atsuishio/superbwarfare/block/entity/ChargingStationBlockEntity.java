package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.ChargingStationBlock;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.menu.ChargingStationMenu;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Energy Data Slot Code based on @GoryMoon's Chargers
 */
public class ChargingStationBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    protected static final int SLOT_FUEL = 0;
    protected static final int SLOT_CHARGE = 1;

    public static final int MAX_ENERGY = 4000000;
    public static final int MAX_DATA_COUNT = 4;
    public static final int DEFAULT_FUEL_TIME = 1600;
    public static final int CHARGE_SPEED = 128;
    public static final int CHARGE_OTHER_SPEED = 100000;
    public static final int CHARGE_RADIUS = 8;

    protected NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);

    private LazyOptional<EnergyStorage> energyHandler;
    private LazyOptional<?>[] itemHandlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    public int fuelTick = DEFAULT_FUEL_TIME;
    public int maxFuelTick = DEFAULT_FUEL_TIME;
    public boolean showRange = false;

    protected final ContainerEnergyData dataAccess = new ContainerEnergyData() {
        public long get(int pIndex) {
            return switch (pIndex) {
                case 0 -> ChargingStationBlockEntity.this.fuelTick;
                case 1 -> ChargingStationBlockEntity.this.maxFuelTick;
                case 2 -> {
                    AtomicInteger energy = new AtomicInteger();
                    ChargingStationBlockEntity.this.getCapability(ForgeCapabilities.ENERGY).ifPresent(consumer -> energy.set(consumer.getEnergyStored()));
                    yield energy.get();
                }
                case 3 -> ChargingStationBlockEntity.this.showRange ? 1 : 0;
                default -> 0;
            };
        }

        public void set(int pIndex, long pValue) {
            switch (pIndex) {
                case 0:
                    ChargingStationBlockEntity.this.fuelTick = (int) pValue;
                    break;
                case 1:
                    ChargingStationBlockEntity.this.maxFuelTick = (int) pValue;
                    break;
                case 2:
                    ChargingStationBlockEntity.this.getCapability(ForgeCapabilities.ENERGY).ifPresent(consumer -> consumer.receiveEnergy((int) pValue, false));
                    break;
                case 3:
                    ChargingStationBlockEntity.this.showRange = pValue == 1;
                    break;
            }
        }

        public int getCount() {
            return MAX_DATA_COUNT;
        }
    };

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_STATION.get(), pos, state);

        this.energyHandler = LazyOptional.of(() -> new EnergyStorage(MAX_ENERGY));
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, ChargingStationBlockEntity blockEntity) {
        if (blockEntity.showRange != pState.getValue(ChargingStationBlock.SHOW_RANGE)) {
            pLevel.setBlockAndUpdate(pPos, pState.setValue(ChargingStationBlock.SHOW_RANGE, blockEntity.showRange));
            setChanged(pLevel, pPos, pState);
        }

        blockEntity.energyHandler.ifPresent(handler -> {
            int energy = handler.getEnergyStored();
            if (energy > 0) {
                blockEntity.chargeEntity(handler);
            }
            if (handler.getEnergyStored() > 0) {
                blockEntity.chargeItemStack(handler);
            }
            if (handler.getEnergyStored() > 0) {
                blockEntity.chargeBlock(handler);
            }
        });

        // 持续供电
        blockEntity.energyHandler.ifPresent(handler -> {
            int energy = handler.getEnergyStored();
            if (energy < handler.getMaxEnergyStored()) {
                handler.receiveEnergy(CHARGE_SPEED, false);
            }
        });
    }

    private void chargeEntity(EnergyStorage handler) {
        if (this.level == null) return;
        if (this.level.getGameTime() % 20 != 0) return;

        List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, new AABB(this.getBlockPos()).inflate(CHARGE_RADIUS));
        entities.forEach(entity -> entity.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
            if (cap.canReceive()) {
                int charged = cap.receiveEnergy(Math.min(handler.getEnergyStored(), CHARGE_OTHER_SPEED * 20), false);
                handler.extractEnergy(charged, false);
            }
        }));
        this.setChanged();
    }

    private void chargeItemStack(EnergyStorage handler) {
        ItemStack stack = this.getItem(SLOT_CHARGE);
        if (stack.isEmpty()) return;

        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(consumer -> {
            if (consumer.getEnergyStored() < consumer.getMaxEnergyStored()) {
                int charged = consumer.receiveEnergy(Math.min(CHARGE_OTHER_SPEED, handler.getEnergyStored()), false);
                handler.extractEnergy(Math.min(charged, handler.getEnergyStored()), false);
            }
        });
        this.setChanged();
    }

    private void chargeBlock(EnergyStorage handler) {
        if (this.level == null) return;

        for (Direction direction : Direction.values()) {
            var blockEntity = this.level.getBlockEntity(this.getBlockPos().relative(direction));
            if (blockEntity == null || !blockEntity.getCapability(ForgeCapabilities.ENERGY).isPresent() || blockEntity instanceof ChargingStationBlockEntity) {
                continue;
            }

            blockEntity.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
                if (energy.canReceive() && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                    int receiveEnergy = energy.receiveEnergy(Math.min(handler.getEnergyStored(), CHARGE_OTHER_SPEED), false);
                    handler.extractEnergy(receiveEnergy, false);

                    blockEntity.setChanged();
                    this.setChanged();
                }
            });
        }
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);

        if (pTag.contains("Energy")) {
            getCapability(ForgeCapabilities.ENERGY).ifPresent(handler -> ((EnergyStorage) handler).deserializeNBT(pTag.get("Energy")));
        }
        this.fuelTick = pTag.getInt("FuelTick");
        this.maxFuelTick = pTag.getInt("MaxFuelTick");
        this.showRange = pTag.getBoolean("ShowRange");
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.items);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);

        getCapability(ForgeCapabilities.ENERGY).ifPresent(handler -> pTag.put("Energy", ((EnergyStorage) handler).serializeNBT()));
        pTag.putInt("FuelTick", this.fuelTick);
        pTag.putInt("MaxFuelTick", this.maxFuelTick);
        pTag.putBoolean("ShowRange", this.showRange);
        ContainerHelper.saveAllItems(pTag, this.items);
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction pSide) {
        return new int[]{SLOT_FUEL};
    }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, @NotNull ItemStack pItemStack, @Nullable Direction pDirection) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int pIndex, @NotNull ItemStack pStack, @NotNull Direction pDirection) {
        return false;
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
    public @NotNull ItemStack getItem(int pSlot) {
        return this.items.get(pSlot);
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
    public void setItem(int pSlot, ItemStack pStack) {
        ItemStack itemstack = this.items.get(pSlot);
        boolean flag = !pStack.isEmpty() && ItemStack.isSameItemSameTags(itemstack, pStack);
        this.items.set(pSlot, pStack);
        if (pStack.getCount() > this.getMaxStackSize()) {
            pStack.setCount(this.getMaxStackSize());
        }

        if (pSlot == 0 && !flag) {
            this.setChanged();
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

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.superbwarfare.charging_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new ChargingStationMenu(pContainerId, pPlayerInventory, this, this.dataAccess);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag compoundtag = new CompoundTag();
        ContainerHelper.saveAllItems(compoundtag, this.items, true);
        compoundtag.putBoolean("ShowRange", this.showRange);
        return compoundtag;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        if (!this.remove && side != null && cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.UP) {
                return itemHandlers[0].cast();
            } else if (side == Direction.DOWN) {
                return itemHandlers[1].cast();
            } else {
                return itemHandlers[2].cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<?> itemHandler : itemHandlers) itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.itemHandlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
        this.energyHandler = LazyOptional.of(() -> new EnergyStorage(MAX_ENERGY));
    }

    @Override
    public void saveToItem(@NotNull ItemStack pStack) {
        CompoundTag tag = new CompoundTag();
        this.getCapability(ForgeCapabilities.ENERGY).ifPresent(handler -> tag.put("Energy", ((EnergyStorage) handler).serializeNBT()));
        BlockItem.setBlockEntityData(pStack, this.getType(), tag);
    }
}
