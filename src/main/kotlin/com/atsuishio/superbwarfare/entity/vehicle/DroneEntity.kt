package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.RCEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class DroneEntity(type: EntityType<out DroneEntity>, level: Level) : RCEntity(type, level) {
    private var hasSavedEnergy = false

    override fun onAddedToWorld() {
        super.onAddedToWorld()
        if (!level().isClientSide && !hasSavedEnergy && hasEnergyStorage()) {
            energy = maxEnergy
            hasSavedEnergy = true
        }
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        hasSavedEnergy = compound.contains("Energy")
    }
}
