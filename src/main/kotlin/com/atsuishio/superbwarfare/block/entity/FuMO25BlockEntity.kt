package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.FuMO25Block
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.tools.SeekTool
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.sounds.SoundSource
import net.minecraft.world.MenuProvider
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.energy.EnergyStorage
import net.neoforged.neoforge.energy.IEnergyStorage
import javax.annotation.ParametersAreNonnullByDefault

open class FuMO25BlockEntity(pPos: BlockPos, pBlockState: BlockState) :
    BlockEntity(ModBlockEntities.FUMO_25.get(), pPos, pBlockState), MenuProvider {

    private val energyStorage: IEnergyStorage? = null

    var type: FuncType = FuncType.NORMAL
    var powered: Boolean = false
    var tick: Int = 0

    protected val dataAccess: ContainerEnergyData = object : ContainerEnergyData {
        override fun get(index: Int): Long {
            return when (index) {
                0 -> this@FuMO25BlockEntity.energyStorage?.energyStored ?: 0

                1 -> this@FuMO25BlockEntity.type.ordinal
                2 -> if (this@FuMO25BlockEntity.powered) 1 else 0
                3 -> this@FuMO25BlockEntity.tick
                else -> 0
            }.toLong()
        }

        override fun set(index: Int, value: Long) {
            when (index) {
                0 -> this@FuMO25BlockEntity.energyStorage?.receiveEnergy(value.toInt(), false)
                1 -> this@FuMO25BlockEntity.type = FuncType.entries[value.toInt()]
                2 -> this@FuMO25BlockEntity.powered = value == 1L
                3 -> this@FuMO25BlockEntity.tick = value.toInt()
            }
        }

        override fun getCount(): Int {
            return MAX_DATA_COUNT
        }
    }

    private fun setGlowEffect() {
        if (this.type != FuncType.GLOW) return
        val level = this.level ?: return
        val pos = this.blockPos
        val entities = SeekTool.getEntitiesWithinRange(pos, level, GLOW_RANGE.toDouble())
        entities.forEach {
            if (it is LivingEntity) {
                it.addEffect(MobEffectInstance(MobEffects.GLOWING, 110, 0, true, false))
            }
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        val energyTag = tag.get("Energy")
        if (energyTag != null) {
            (energyStorage as EnergyStorage).deserializeNBT(registries, energyTag)
        }
        this.type = FuncType.entries[tag.getInt("Type").coerceIn(0, 3)]
        this.powered = tag.getBoolean("Powered")
        this.tick = tag.getInt("Tick")
    }

    @ParametersAreNonnullByDefault
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        tag.put("Energy", (energyStorage as EnergyStorage).serializeNBT(registries))
        tag.putInt("Type", this.type.ordinal)
        tag.putBoolean("Powered", this.powered)
        tag.putInt("Tick", this.tick)
    }

    override fun getDisplayName(): Component {
        return Component.empty()
    }

    override fun createMenu(pContainerId: Int, pPlayerInventory: Inventory, pPlayer: Player): AbstractContainerMenu? {
        val level = this.level ?: return null
        return FuMO25Menu(
            pContainerId,
            pPlayerInventory,
            ContainerLevelAccess.create(level, this.blockPos),
            this.dataAccess
        )
    }

    fun sync() {
        val level = this.level ?: return
        if (level.isClientSide) return
        this.setChanged()
        level.sendBlockUpdated(this.worldPosition, this.blockState, this.blockState, 3)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        this.saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun handleUpdateTag(tag: CompoundTag, lookupProvider: HolderLookup.Provider) {
        tag.let { this.loadAdditional(it, lookupProvider) }
    }

    override fun onDataPacket(
        net: Connection,
        pkt: ClientboundBlockEntityDataPacket,
        lookupProvider: HolderLookup.Provider
    ) {
        this.handleUpdateTag(pkt.tag, lookupProvider)
    }

    fun getEnergyStorage() = this.energyStorage!!

    enum class FuncType {
        NORMAL,
        WIDER,
        GLOW,
        GUIDE
    }

    companion object {
        const val MAX_ENERGY: Int = 1000000

        // 固定距离，以后有人改动这个需要自行解决GUI渲染问题
        const val DEFAULT_RANGE: Int = 96
        const val MAX_RANGE: Int = 128
        const val GLOW_RANGE: Int = 64

        const val DEFAULT_ENERGY_COST: Int = 256
        const val MAX_ENERGY_COST: Int = 512

        const val DEFAULT_MIN_ENERGY: Int = 64000

        const val MAX_DATA_COUNT: Int = 4

        fun serverTick(pLevel: Level, pPos: BlockPos, pState: BlockState, blockEntity: FuMO25BlockEntity) {
            if (pState.getValue(FuMO25Block.POWERED)) {
                blockEntity.tick++
                blockEntity.sync()
            }

            val energyStorage = blockEntity.getEnergyStorage()
            val energy = energyStorage.energyStored
            blockEntity.tick++

            val funcType = blockEntity.type
            val energyCost = if (funcType == FuncType.WIDER) {
                MAX_ENERGY_COST
            } else {
                DEFAULT_ENERGY_COST
            }

            if (energy < energyCost) {
                if (pState.getValue(FuMO25Block.POWERED)) {
                    pLevel.setBlockAndUpdate(pPos, pState.setValue(FuMO25Block.POWERED, false))
                    pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_END.get(), SoundSource.BLOCKS, 1f, 1f)
                    blockEntity.powered = false
                    setChanged(pLevel, pPos, pState)
                }
            } else {
                if (!pState.getValue(FuMO25Block.POWERED)) {
                    if (energy >= DEFAULT_MIN_ENERGY) {
                        pLevel.setBlockAndUpdate(pPos, pState.setValue(FuMO25Block.POWERED, true))
                        pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_START.get(), SoundSource.BLOCKS, 1f, 1f)
                        blockEntity.powered = true
                        setChanged(pLevel, pPos, pState)
                    }
                } else {
                    energyStorage.extractEnergy(energyCost, false)
                    if (blockEntity.tick == 300) {
                        pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_IDLE.get(), SoundSource.BLOCKS, 1f, 1f)
                    }

                    if (blockEntity.tick % 100 == 0) {
                        blockEntity.setGlowEffect()
                    }
                }
            }

            if (blockEntity.tick >= 300) {
                blockEntity.tick = 0
            }
        }
    }
}
