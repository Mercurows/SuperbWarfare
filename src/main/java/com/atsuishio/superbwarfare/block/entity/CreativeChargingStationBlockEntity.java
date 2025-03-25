package com.atsuishio.superbwarfare.block.entity;

import com.atsuishio.superbwarfare.capability.energy.InfinityEnergyStorage;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

/**
 * Energy Data Slot Code based on @GoryMoon's Chargers
 */
public class CreativeChargingStationBlockEntity extends BlockEntity {

    public static final int CHARGE_RADIUS = 8;

    private final IEnergyStorage energyHandler;

    public CreativeChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_CHARGING_STATION.get(), pos, state);
        this.energyHandler = new InfinityEnergyStorage();
    }

    public static void serverTick(CreativeChargingStationBlockEntity blockEntity) {
        if (blockEntity.level == null) return;

        if (blockEntity.energyHandler != null) {
            blockEntity.chargeEntity();
            blockEntity.chargeBlock();
        }
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
            if (blockEntity == null) return;

            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, blockEntity.getBlockPos(), direction);
            if (energy == null || blockEntity instanceof CreativeChargingStationBlockEntity) continue;

            if (energy.canReceive() && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                energy.receiveEnergy(Integer.MAX_VALUE, false);
                blockEntity.setChanged();
            }
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
