package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.ChargingStationBlock;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.inventory.menu.ChargingStationMenu;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import com.atsuishio.superbwarfare.tools.MinecraftUtil;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Energy Data Slot Code based on @GoryMoon's Chargers
 */
public class ChargingStationBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    protected static final int SLOT_FUEL = 0;
    protected static final int SLOT_CHARGE = 1;
    public static final int MAX_DATA_COUNT = 4;
    protected NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);

    public static final int MAX_ENERGY = MiscConfig.CHARGING_STATION_MAX_ENERGY.get();
    public static final int DEFAULT_FUEL_TIME = MiscConfig.CHARGING_STATION_DEFAULT_FUEL_TIME.get();
    public static final int CHARGE_SPEED = MiscConfig.CHARGING_STATION_GENERATE_SPEED.get();
    public static final int CHARGE_OTHER_SPEED = MiscConfig.CHARGING_STATION_TRANSFER_SPEED.get();
    public static final int CHARGE_RADIUS = MiscConfig.CHARGING_STATION_CHARGE_RADIUS.get();

    private LazyOptional<EnergyStorage> energyHandler;
    private LazyOptional<?>[] itemHandlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    public int fuelTick = 0;
    public int maxFuelTick = DEFAULT_FUEL_TIME;
    public boolean showRange = false;

    protected final ContainerEnergyData dataAccess = new ContainerEnergyData() {
        public long get(int index) {
            return switch (index) {
                case 0 -> ChargingStationBlockEntity.this.fuelTick;
                case 1 -> ChargingStationBlockEntity.this.maxFuelTick;
                case 2 -> {
                    int[] energy = {0};
                    ChargingStationBlockEntity.this.getCapability(ForgeCapabilities.ENERGY).ifPresent(consumer -> energy[0] = consumer.getEnergyStored());
                    yield energy[0];
                }
                case 3 -> ChargingStationBlockEntity.this.showRange ? 1 : 0;
                default -> 0;
            };
        }

        public void set(int index, long value) {
            switch (index) {
                case 0:
                    ChargingStationBlockEntity.this.fuelTick = (int) value;
                    break;
                case 1:
                    ChargingStationBlockEntity.this.maxFuelTick = (int) value;
                    break;
                case 2:
                    ChargingStationBlockEntity.this.getCapability(ForgeCapabilities.ENERGY).ifPresent(consumer -> consumer.receiveEnergy((int) value, false));
                    break;
                case 3:
                    ChargingStationBlockEntity.this.showRange = value == 1;
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

        if (blockEntity.fuelTick > 0) {
            blockEntity.fuelTick--;
            blockEntity.energyHandler.ifPresent(handler -> {
                int energy = handler.getEnergyStored();
                if (energy < handler.getMaxEnergyStored()) {
                    handler.receiveEnergy(CHARGE_SPEED, false);
                }
            });
        } else if (!blockEntity.getItem(SLOT_FUEL).isEmpty()) {
            boolean[] flag = {false};
            blockEntity.energyHandler.ifPresent(handler -> {
                if (handler.getEnergyStored() >= handler.getMaxEnergyStored()) {
                    flag[0] = true;
                }
            });
            if (flag[0]) return;

            ItemStack fuel = blockEntity.getItem(SLOT_FUEL);
            int burnTime = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);

            if (fuel.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                // 优先当作电池处理
                fuel.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> blockEntity.energyHandler.ifPresent(energy -> {
                    var energyToExtract = Math.min(CHARGE_OTHER_SPEED, energy.getMaxEnergyStored() - energy.getEnergyStored());
                    if (itemEnergy.canExtract() && energy.canReceive()) {
                        energy.receiveEnergy(itemEnergy.extractEnergy(energyToExtract, false), false);
                    }
                }));

                blockEntity.setChanged();
            } else if (burnTime > 0) {
                // 其次尝试作为燃料处理
                blockEntity.fuelTick = burnTime;
                blockEntity.maxFuelTick = burnTime;

                if (fuel.hasCraftingRemainingItem()) {
                    if (fuel.getCount() <= 1) {
                        blockEntity.setItem(SLOT_FUEL, fuel.getCraftingRemainingItem());
                    } else {
                        ItemStack copy = fuel.getCraftingRemainingItem().copy();
                        copy.setCount(1);

                        ItemEntity itemEntity = new ItemEntity(pLevel,
                                pPos.getX() + 0.5,
                                pPos.getY() + 0.2,
                                pPos.getZ() + 0.5,
                                copy);
                        pLevel.addFreshEntity(itemEntity);

                        fuel.shrink(1);
                    }
                } else {
                    fuel.shrink(1);
                }

                blockEntity.setChanged();
            } else if (fuel.getItem().isEdible()) {
                // 最后作为食物处理
                var properties = fuel.getFoodProperties(null);
                if (properties == null) return;

                int nutrition = properties.getNutrition();
                float saturation = properties.getSaturationModifier() * 2.0f * nutrition;
                int tick = nutrition * 80 + (int) (saturation * 200);

                if (fuel.hasCraftingRemainingItem()) {
                    tick += 400;
                }

                fuel.shrink(1);

                blockEntity.fuelTick = tick;
                blockEntity.maxFuelTick = tick;
                blockEntity.setChanged();
            }
        }
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
        return pIndex == SLOT_FUEL;
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
        boolean flag = !pStack.isEmpty() && MinecraftUtil.isSameItemStack(itemstack, pStack);
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
