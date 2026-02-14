package com.atsuishio.superbwarfare.capability.energy

import net.minecraft.nbt.Tag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraftforge.energy.EnergyStorage

/**
 * 自动同步的实体能量存储能力，会和客户端自动同步实体的当前能量值
 */
open class SyncedEntityEnergyStorage(
    capacity: Int,
    maxReceive: Int,
    maxExtract: Int,
    protected var entityData: SynchedEntityData,
    protected var energyDataAccessor: EntityDataAccessor<Int>
) : EnergyStorage(capacity, maxReceive, maxExtract, 0) {
    /**
     * 自动同步的实体能量存储能力
     * 
     * @param capacity           能量上限
     * @param data               实体的entityData
     * @param energyDataAccessor 能量的EntityDataAccessor
     */
    constructor(capacity: Int, data: SynchedEntityData, energyDataAccessor: EntityDataAccessor<Int>) : this(
        capacity,
        capacity,
        capacity,
        data,
        energyDataAccessor
    )

    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        val received = super.receiveEnergy(maxReceive, simulate)

        if (!simulate) {
            entityData.set(energyDataAccessor, this.energy)
        }

        return received
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        val extracted = super.extractEnergy(maxExtract, simulate)

        if (!simulate) {
            entityData.set(energyDataAccessor, energy)
        }

        return extracted
    }

    override fun getEnergyStored(): Int {
        // 获取同步数据，保证客户端能正确获得能量值
        return entityData.get(energyDataAccessor)
    }

    override fun deserializeNBT(nbt: Tag) {
        super.deserializeNBT(nbt)
        entityData.set(energyDataAccessor, energy)
    }

    fun setEnergy(energy: Int) {
        this.energy = energy
        entityData.set(energyDataAccessor, energy)
    }

    fun setCapacity(capacity: Int) {
        this.capacity = capacity
    }

    fun setMaxExtract(maxExtract: Int) {
        this.maxExtract = maxExtract
    }

    fun setMaxReceive(maxReceive: Int) {
        this.maxReceive = maxReceive
    }
}
