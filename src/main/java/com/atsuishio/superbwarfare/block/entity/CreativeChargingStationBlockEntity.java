package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.block.CreativeChargingStationBlock;
import com.atsuishio.superbwarfare.capability.energy.InfinityEnergyStorage;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Energy Data Slot Code based on @GoryMoon's Chargers
 */
public class CreativeChargingStationBlockEntity extends BlockEntity {

    public static final int CHARGE_RADIUS = 8;

    public boolean showRange = false;

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowRange", this.showRange);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        this.showRange = packet.getTag().getBoolean("ShowRange");
    }

    public CreativeChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_CHARGING_STATION.get(), pos, state);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, CreativeChargingStationBlockEntity blockEntity) {
        if (blockEntity.showRange != pState.getValue(CreativeChargingStationBlock.SHOW_RANGE)) {
            pLevel.setBlockAndUpdate(pPos, pState.setValue(CreativeChargingStationBlock.SHOW_RANGE, blockEntity.showRange));
            setChanged(pLevel, pPos, pState);
        }

        blockEntity.chargeEntity();
        blockEntity.chargeBlock();
    }

    private void chargeEntity() {
        if (this.level == null) return;
        if (this.level.getGameTime() % 20 != 0) return;

        List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, new AABB(this.getBlockPos()).inflate(CHARGE_RADIUS));
        entities.forEach(entity -> {
            var cap = entity.getCapability(Capabilities.EnergyStorage.ENTITY, null);
            if (cap == null || !cap.canReceive()) return;

            cap.receiveEnergy(Integer.MAX_VALUE, false);
        });
    }

    private void chargeBlock() {
        if (this.level == null) return;

        for (Direction direction : Direction.values()) {
            var blockEntity = this.level.getBlockEntity(this.getBlockPos().relative(direction));
            if (blockEntity == null) continue;

            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, blockEntity.getBlockPos(), direction);
            if (energy == null || blockEntity instanceof CreativeChargingStationBlockEntity) continue;

            if (energy.canReceive() && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                energy.receiveEnergy(Integer.MAX_VALUE, false);
                blockEntity.setChanged();
            }
        }
    }

    private final IEnergyStorage energyStorage = new InfinityEnergyStorage();

    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energyStorage;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.showRange = tag.getBoolean("ShowRange");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("ShowRange", this.showRange);
    }
}
