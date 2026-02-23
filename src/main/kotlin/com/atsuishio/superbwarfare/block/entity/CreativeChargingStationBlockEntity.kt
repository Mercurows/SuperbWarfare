package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.capability.energy.InfinityEnergyStorage
import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.IEnergyStorage

/**
 * Energy Data Slot Code based on @GoryMoon's Chargers
 */
open class CreativeChargingStationBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.CREATIVE_CHARGING_STATION.get(), pos, state) {
    private var energyHandler: LazyOptional<IEnergyStorage>

    init {
        this.energyHandler = LazyOptional.of { InfinityEnergyStorage() }
    }

    private fun chargeEntity() {
        if (this.level == null) return
        if (this.level!!.gameTime % 20 != 0L) return

        val entities = this.level!!.getEntitiesOfClass<Entity>(
            Entity::class.java, AABB(this.blockPos)
                .inflate(CHARGE_RADIUS.toDouble())
        )
        entities.forEach { entity ->
            entity.getCapability(ForgeCapabilities.ENERGY)
                .ifPresent { cap ->
                    if (cap.canReceive()) {
                        cap.receiveEnergy(Int.MAX_VALUE, false)
                    }
                }
        }
    }

    private fun chargeBlock() {
        if (this.level == null) return

        for (direction in Direction.entries) {
            val blockEntity = this.level!!.getBlockEntity(this.blockPos.relative(direction))
            if (blockEntity == null
                || !blockEntity.getCapability(ForgeCapabilities.ENERGY).isPresent
                || blockEntity is CreativeChargingStationBlockEntity
            ) continue

            blockEntity.getCapability(ForgeCapabilities.ENERGY)
                .ifPresent {
                    if (it.canReceive() && it.energyStored < it.maxEnergyStored) {
                        it.receiveEnergy(Int.MAX_VALUE, false)
                        blockEntity.setChanged()
                    }
                }
        }
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun <T> getCapability(cap: Capability<T?>, side: Direction?): LazyOptional<T?> {
        return ForgeCapabilities.ENERGY.orEmpty<T?>(cap, energyHandler)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        energyHandler.invalidate()
    }

    override fun reviveCaps() {
        super.reviveCaps()
        this.energyHandler = LazyOptional.of { InfinityEnergyStorage() }
    }

    companion object {
        const val CHARGE_RADIUS: Int = 8

        fun serverTick(blockEntity: CreativeChargingStationBlockEntity) {
            if (blockEntity.level == null) return

            blockEntity.energyHandler.ifPresent { _ ->
                blockEntity.chargeEntity()
                blockEntity.chargeBlock()
            }
        }
    }
}
