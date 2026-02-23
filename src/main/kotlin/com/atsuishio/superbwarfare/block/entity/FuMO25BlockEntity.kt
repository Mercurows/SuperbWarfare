package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.FuMO25Block
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.tools.SeekTool
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
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
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.EnergyStorage
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar
import software.bernie.geckolib.network.SerializableDataTicket
import software.bernie.geckolib.util.GeckoLibUtil

open class FuMO25BlockEntity(pPos: BlockPos, pBlockState: BlockState) :
    BlockEntity(ModBlockEntities.FUMO_25.get(), pPos, pBlockState), MenuProvider, GeoBlockEntity {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    private var energyHandler: LazyOptional<EnergyStorage>

    var type: FuncType = FuncType.NORMAL
    var time: Int = 0
    var powered: Boolean = false
    var tick: Int = 0
    var yRot0: Float = 0f

    protected val dataAccess: ContainerEnergyData = object : ContainerEnergyData {
        override fun get(index: Int): Long {
            return when (index) {
                0 -> this@FuMO25BlockEntity.energyHandler.map { it.energyStored.toLong() }
                    .orElse(0L)

                1 -> this@FuMO25BlockEntity.type.ordinal.toLong()
                2 -> this@FuMO25BlockEntity.time.toLong()
                3 -> if (this@FuMO25BlockEntity.powered) 1L else 0L
                4 -> this@FuMO25BlockEntity.tick.toLong()
                else -> 0L
            }
        }

        override fun set(index: Int, value: Long) {
            when (index) {
                0 -> this@FuMO25BlockEntity.energyHandler.ifPresent {
                    it.receiveEnergy(
                        value.toInt(),
                        false
                    )
                }

                1 -> this@FuMO25BlockEntity.type = FuncType.entries[value.toInt()]
                2 -> this@FuMO25BlockEntity.time = value.toInt()
                3 -> this@FuMO25BlockEntity.powered = value == 1L
                4 -> this@FuMO25BlockEntity.tick = value.toInt()
            }
        }

        override fun getCount(): Int {
            return MAX_DATA_COUNT
        }
    }

    init {
        this.energyHandler = LazyOptional.of { EnergyStorage(MAX_ENERGY) }
    }

    private fun setGlowEffect() {
        if (this.type != FuncType.GLOW) return
        val level = this.level ?: return
        val pos = this.blockPos
        val entities = SeekTool.getEntitiesWithinRange(pos, level, GLOW_RANGE.toDouble())
        entities.forEach {
            if (it is LivingEntity) {
                it.addEffect(MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false))
            }
        }
    }

    override fun load(pTag: CompoundTag) {
        super.load(pTag)

        if (pTag.contains("Energy")) {
            getCapability(ForgeCapabilities.ENERGY).ifPresent {
                (it as EnergyStorage).deserializeNBT(pTag.get("Energy"))
            }
        }
        this.type = FuncType.entries[Mth.clamp(pTag.getInt("Type"), 0, 3)]
        this.time = pTag.getInt("Time")
        this.powered = pTag.getBoolean("Powered")
    }

    override fun saveAdditional(pTag: CompoundTag) {
        super.saveAdditional(pTag)

        getCapability(ForgeCapabilities.ENERGY).ifPresent {
            pTag.put(
                "Energy",
                (it as EnergyStorage).serializeNBT()
            )
        }
        pTag.putInt("Type", this.type.ordinal)
        pTag.putInt("Time", this.time)
        pTag.putBoolean("Powered", this.powered)
    }

    override fun getDisplayName(): Component {
        return Component.empty()
    }

    override fun createMenu(pContainerId: Int, pPlayerInventory: Inventory, pPlayer: Player): AbstractContainerMenu? {
        if (this.level == null) return null
        return FuMO25Menu(
            pContainerId,
            pPlayerInventory,
            ContainerLevelAccess.create(this.level, this.blockPos),
            this.dataAccess
        )
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun registerControllers(data: ControllerRegistrar?) {
    }

    override fun <T> getCapability(cap: Capability<T?>, side: Direction?): LazyOptional<T?> {
        if (cap === ForgeCapabilities.ENERGY) {
            return energyHandler.cast<T?>()
        }
        return super.getCapability<T?>(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        this.energyHandler.invalidate()
    }

    override fun reviveCaps() {
        super.reviveCaps()
        this.energyHandler = LazyOptional.of { EnergyStorage(MAX_ENERGY) }
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return this.cache
    }

    enum class FuncType {
        NORMAL,
        WIDER,
        GLOW,
        GUIDE
    }

    companion object {
        @JvmField
        var FUMO25_TICK: SerializableDataTicket<Int>? = null

        const val MAX_ENERGY: Int = 1000000

        // 固定距离，以后有人改动这个需要自行解决GUI渲染问题
        const val DEFAULT_RANGE: Int = 96
        const val MAX_RANGE: Int = 128
        const val GLOW_RANGE: Int = 64

        const val DEFAULT_ENERGY_COST: Int = 256
        const val MAX_ENERGY_COST: Int = 1024

        const val DEFAULT_MIN_ENERGY: Int = 64000

        const val MAX_DATA_COUNT: Int = 5

        fun serverTick(pLevel: Level, pPos: BlockPos, pState: BlockState, blockEntity: FuMO25BlockEntity) {
            val energy =
                blockEntity.energyHandler.map { it.energyStored }.orElse(0)

            if (pState.getValue(FuMO25Block.POWERED)) {
                blockEntity.tick++
                blockEntity.setAnimData(FUMO25_TICK, blockEntity.tick)
            }

            val funcType = blockEntity.type
            val energyCost: Int = if (funcType == FuncType.WIDER) {
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
                if (blockEntity.time > 0) {
                    blockEntity.time = 0
                    blockEntity.setChanged()
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
                    blockEntity.energyHandler.ifPresent { it.extractEnergy(energyCost, false) }
                    if (blockEntity.tick == 200) {
                        pLevel.playSound(null, pPos, ModSounds.RADAR_SEARCH_IDLE.get(), SoundSource.BLOCKS, 1f, 1f)
                    }

                    if (blockEntity.time > 0) {
                        if (blockEntity.time % 100 == 0) {
                            blockEntity.setGlowEffect()
                        }
                        blockEntity.time--
                        blockEntity.setChanged()
                    }
                }
            }

            if (blockEntity.tick >= 200) {
                blockEntity.tick = 0
            }

            if (blockEntity.time <= 0 && blockEntity.type != FuncType.NORMAL) {
                blockEntity.type = FuncType.NORMAL
                blockEntity.setChanged()
            }
        }
    }
}
