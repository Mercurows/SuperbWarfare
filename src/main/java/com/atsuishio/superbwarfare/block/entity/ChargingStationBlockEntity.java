package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.ChargingStationBlock;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.menu.ChargingStationMenu;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
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


    public int fuelTick = 0;
    public int maxFuelTick = DEFAULT_FUEL_TIME;
    public boolean showRange = false;

    protected final ContainerEnergyData dataAccess = new ContainerEnergyData() {
        public int get(int pIndex) {
            return switch (pIndex) {
                case 0 -> ChargingStationBlockEntity.this.fuelTick;
                case 1 -> ChargingStationBlockEntity.this.maxFuelTick;
                case 2 -> {
                    var level = ChargingStationBlockEntity.this.level;
                    if (level == null) yield 0;

                    var cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, ChargingStationBlockEntity.this.getBlockPos(), null);
                    if (cap == null) yield 0;

                    yield cap.getEnergyStored();
                }
                case 3 -> ChargingStationBlockEntity.this.showRange ? 1 : 0;
                default -> 0;
            };
        }

        public void set(int pIndex, int pValue) {
            switch (pIndex) {
                case 0:
                    ChargingStationBlockEntity.this.fuelTick = pValue;
                    break;
                case 1:
                    ChargingStationBlockEntity.this.maxFuelTick = pValue;
                    break;
                case 2:
                    var level = ChargingStationBlockEntity.this.level;
                    if (level == null) return;

                    var cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, ChargingStationBlockEntity.this.getBlockPos(), null);
                    if (cap == null) return;

                    cap.receiveEnergy(pValue, false);
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
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, ChargingStationBlockEntity blockEntity) {
        if (blockEntity.showRange != pState.getValue(ChargingStationBlock.SHOW_RANGE)) {
            pLevel.setBlockAndUpdate(pPos, pState.setValue(ChargingStationBlock.SHOW_RANGE, blockEntity.showRange));
            setChanged(pLevel, pPos, pState);
        }

        var handler = blockEntity.getEnergyStorage(null);
        if (handler == null) return;

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

        if (blockEntity.fuelTick > 0) {
            blockEntity.fuelTick--;
            if (energy < handler.getMaxEnergyStored()) {
                handler.receiveEnergy(CHARGE_SPEED, false);
            }
        } else if (!blockEntity.getItem(SLOT_FUEL).isEmpty()) {
            if (handler.getEnergyStored() >= handler.getMaxEnergyStored()) return;

            ItemStack fuel = blockEntity.getItem(SLOT_FUEL);
            int burnTime = fuel.getBurnTime(RecipeType.SMELTING);

            var fuelEnergy = fuel.getCapability(Capabilities.EnergyStorage.ITEM);

            if (fuelEnergy != null) {
                // 优先当作电池处理
                var energyToExtract = Math.min(CHARGE_OTHER_SPEED, handler.getMaxEnergyStored() - handler.getEnergyStored());
                if (fuelEnergy.canExtract() && handler.canReceive()) {
                    handler.receiveEnergy(fuelEnergy.extractEnergy(energyToExtract, false), false);
                }

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
            } else if (fuel.get(DataComponents.FOOD) != null) {
                // 最后作为食物处理
                var foodComponent = fuel.get(DataComponents.FOOD);
                if (foodComponent == null) return;

                int nutrition = foodComponent.nutrition();
                float saturation = foodComponent.saturation() * 2.0f * nutrition;
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

    private void chargeEntity(IEnergyStorage handler) {
        if (this.level == null) return;
        if (this.level.getGameTime() % 20 != 0) return;

        List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, new AABB(this.getBlockPos()).inflate(CHARGE_RADIUS));
        entities.forEach(entity -> {
            var cap = entity.getCapability(Capabilities.EnergyStorage.ENTITY, null);
            if (cap == null || !cap.canReceive()) return;

            int charged = cap.receiveEnergy(Math.min(handler.getEnergyStored(), CHARGE_OTHER_SPEED * 20), false);
            handler.extractEnergy(charged, false);
        });
        this.setChanged();
    }

    private void chargeItemStack(IEnergyStorage handler) {
        ItemStack stack = this.getItem(SLOT_CHARGE);
        if (stack.isEmpty()) return;

        var consumer = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (consumer != null) {
            if (consumer.getEnergyStored() < consumer.getMaxEnergyStored()) {
                int charged = consumer.receiveEnergy(Math.min(CHARGE_OTHER_SPEED, handler.getEnergyStored()), false);
                handler.extractEnergy(Math.min(charged, handler.getEnergyStored()), false);
            }
        }
        this.setChanged();
    }

    private void chargeBlock(IEnergyStorage handler) {
        if (this.level == null) return;

        for (Direction direction : Direction.values()) {
            var blockEntity = this.level.getBlockEntity(this.getBlockPos().relative(direction));
            if (blockEntity == null) continue;

            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, blockEntity.getBlockPos(), direction);
            if (energy == null || blockEntity instanceof ChargingStationBlockEntity) return;

            if (energy.canReceive() && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                int receiveEnergy = energy.receiveEnergy(Math.min(handler.getEnergyStored(), CHARGE_OTHER_SPEED), false);
                handler.extractEnergy(receiveEnergy, false);

                blockEntity.setChanged();
                this.setChanged();
            }
        }
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void applyImplicitComponents(@NotNull DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);

        if (this.level != null) {
            ((EnergyStorage) this.energyStorage).deserializeNBT(level.registryAccess(), IntTag.valueOf(componentInput.getOrDefault(ModDataComponents.ENERGY, 0)));
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder components) {
        super.collectImplicitComponents(components);

        components.set(ModDataComponents.ENERGY, this.energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("Energy")) {
            var energy = tag.get("Energy");
            if (energy instanceof IntTag) {
                ((EnergyStorage) this.energyStorage).deserializeNBT(registries, energy);
            }
        }
        this.fuelTick = tag.getInt("FuelTick");
        this.maxFuelTick = tag.getInt("MaxFuelTick");
        this.showRange = tag.getBoolean("ShowRange");
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putInt("Energy", this.energyStorage.getEnergyStored());

        tag.putInt("FuelTick", this.fuelTick);
        tag.putInt("MaxFuelTick", this.maxFuelTick);
        tag.putBoolean("ShowRange", this.showRange);
        ContainerHelper.saveAllItems(tag, this.items, registries);
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
        boolean flag = !pStack.isEmpty() && ItemStack.isSameItem(itemstack, pStack);
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
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag compoundtag = new CompoundTag();
        ContainerHelper.saveAllItems(compoundtag, this.items, registries);
        compoundtag.putBoolean("ShowRange", this.showRange);
        return compoundtag;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (this.level != null) {
            tag.put("Energy", ((EnergyStorage) energyStorage).serializeNBT(registries));
        }
        BlockItem.setBlockEntityData(stack, this.getType(), tag);
    }

    private final IEnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY);

    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energyStorage;
    }
}
