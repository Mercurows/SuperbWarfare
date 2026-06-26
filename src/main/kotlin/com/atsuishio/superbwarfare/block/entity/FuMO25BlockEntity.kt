package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.FuMO25Block
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.tools.RadarScanner
import com.atsuishio.superbwarfare.tools.SeekTool
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
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
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.EnergyStorage
import java.util.*
import kotlin.math.abs

open class FuMO25BlockEntity(pPos: BlockPos, pBlockState: BlockState) :
    BlockEntity(ModBlockEntities.FUMO_25.get(), pPos, pBlockState), MenuProvider {
    private var energyHandler: LazyOptional<EnergyStorage> = LazyOptional.of { EnergyStorage(MAX_ENERGY) }

    var type: FuncType = FuncType.NORMAL
    var powered: Boolean = false
    var tickO: Int = 0
    var tick: Int = 0
    var ownerUUID: UUID? = null

    protected val dataAccess: ContainerEnergyData = object : ContainerEnergyData {
        override fun get(index: Int): Long {
            return when (index) {
                0 -> this@FuMO25BlockEntity.energyHandler.map { it.energyStored.toLong() }
                    .orElse(0L)

                1 -> this@FuMO25BlockEntity.type.ordinal.toLong()
                2 -> if (this@FuMO25BlockEntity.powered) 1L else 0L
                3 -> this@FuMO25BlockEntity.tick.toLong()
                4 -> this@FuMO25BlockEntity.tickO.toLong()
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
                2 -> this@FuMO25BlockEntity.powered = value == 1L
                3 -> this@FuMO25BlockEntity.tick = value.toInt()
                4 -> this@FuMO25BlockEntity.tickO = value.toInt()
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

    override fun load(tag: CompoundTag) {
        super.load(tag)

        if (tag.contains("Energy")) {
            getCapability(ForgeCapabilities.ENERGY).ifPresent {
                (it as EnergyStorage).deserializeNBT(tag.get("Energy"))
            }
        }
        this.type = FuncType.entries[tag.getInt("Type").coerceIn(0, 3)]
        this.powered = tag.getBoolean("Powered")
        this.tick = tag.getInt("Tick")
        this.tickO = tag.getInt("TickO")

        if (tag.contains("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID")
        }
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)

        getCapability(ForgeCapabilities.ENERGY).ifPresent {
            tag.put(
                "Energy",
                (it as EnergyStorage).serializeNBT()
            )
        }
        tag.putInt("Type", this.type.ordinal)
        tag.putBoolean("Powered", this.powered)
        tag.putInt("Tick", this.tick)
        tag.putInt("TickO", this.tickO)

        this.ownerUUID?.let { tag.putUUID("OwnerUUID", it) }
    }

    override fun getDisplayName(): Component {
        return Component.empty()
    }

    override fun createMenu(pContainerId: Int, pPlayerInventory: Inventory, pPlayer: Player): AbstractContainerMenu? {
        if (this.level == null) return null
        return FuMO25Menu(
            pContainerId,
            pPlayerInventory,
            ContainerLevelAccess.create(this.level!!, this.blockPos),
            this.dataAccess
        )
    }

    fun sync() {
        val level = this.level ?: return
        if (level.isClientSide) return
        this.setChanged()
        level.sendBlockUpdated(this.worldPosition, this.blockState, this.blockState, 3)
    }

    override fun getUpdateTag(): CompoundTag {
        val tag = CompoundTag()
        this.saveAdditional(tag)
        return tag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun handleUpdateTag(tag: CompoundTag?) {
        tag?.let { this.load(it) }
    }

    override fun onDataPacket(
        net: Connection,
        pkt: ClientboundBlockEntityDataPacket
    ) {
        this.handleUpdateTag(pkt.tag)
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
        const val MAX_ENERGY_COST: Int = 1024

        const val DEFAULT_MIN_ENERGY: Int = 64000

        const val MAX_DATA_COUNT: Int = 5

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: FuMO25BlockEntity) {
            val energy =
                blockEntity.energyHandler.map { it.energyStored }.orElse(0)

            blockEntity.tickO = blockEntity.tick

            if (state.getValue(FuMO25Block.POWERED)) {
                blockEntity.tick++
                blockEntity.sync()
            }

            val funcType = blockEntity.type
            val energyCost: Int = if (funcType == FuncType.WIDER) {
                MAX_ENERGY_COST
            } else {
                DEFAULT_ENERGY_COST
            }

            if (energy < energyCost) {
                if (state.getValue(FuMO25Block.POWERED)) {
                    level.setBlockAndUpdate(pos, state.setValue(FuMO25Block.POWERED, false))
                    level.playSound(null, pos, ModSounds.RADAR_SEARCH_END.get(), SoundSource.BLOCKS, 1f, 1f)
                    blockEntity.powered = false
                    setChanged(level, pos, state)
                }
            } else {
                if (!state.getValue(FuMO25Block.POWERED)) {
                    if (energy >= DEFAULT_MIN_ENERGY) {
                        level.setBlockAndUpdate(pos, state.setValue(FuMO25Block.POWERED, true))
                        level.playSound(null, pos, ModSounds.RADAR_SEARCH_START.get(), SoundSource.BLOCKS, 1f, 1f)
                        blockEntity.powered = true
                        setChanged(level, pos, state)
                    }
                } else {
                    blockEntity.energyHandler.ifPresent { it.extractEnergy(energyCost, false) }
                    if (blockEntity.tick == 360) {
                        level.playSound(null, pos, ModSounds.RADAR_SEARCH_IDLE.get(), SoundSource.BLOCKS, 1f, 1f)
                    }

                    if (blockEntity.tick % 100 == 0) {
                        blockEntity.setGlowEffect()
                    }

                    val uuid = blockEntity.ownerUUID
                    if (uuid != null) {
                        val owner = level.getPlayerByUUID(uuid)
                        if (owner != null && level is ServerLevel) {
                            // 每 tick 同步雷达配置到客户端（自旋模式保证旋转流畅）
                            val range = if (blockEntity.type == FuncType.WIDER) 2048 else 1024
                            val sourceId = "block_${pos.x}_${pos.y}_${pos.z}"
                            RadarScanner.sendRadarConfig(
                                RadarScanner.RadarConfig(
                                    owner = owner,
                                    center = Vec3(pos.x + 0.5, pos.y + 2.5, pos.z + 0.5),
                                    radius = range.toDouble(),
                                    sweepAngle = 120.0,
                                    yRot = blockEntity.tick.toDouble(),
                                    searchType = RadarScanner.SearchType.VEHICLES,
                                    sourceId = sourceId,
                                ), level
                            )
                            // 每 SYNC_ENTITY_INTERVAL 扫描实体
                            scanEntities(level, pos, blockEntity, owner)
                        }
                    }
                }
            }

            val deltaT = abs(blockEntity.tick - blockEntity.tickO)
            while (blockEntity.tick > 360) {
                blockEntity.tick -= 360
                blockEntity.tickO = blockEntity.tick - deltaT
            }
            while (blockEntity.tick <= 0) {
                blockEntity.tick += 360
                blockEntity.tickO = deltaT + blockEntity.tick
            }


//            // 测试粒子
//            if (level is ServerLevel) {
//
//                val f2 = Mth.sin((blockEntity.tick - 60) * (Math.PI.toFloat() / 180f)).toDouble()
//                val f3 = -Mth.cos((blockEntity.tick - 60) * (Math.PI.toFloat() / 180f)).toDouble()
//
//                val dir1 = Vec3(f2, 0.0, f3)
//
//                val f4 = Mth.sin((blockEntity.tick + 60) * (Math.PI.toFloat() / 180f)).toDouble()
//                val f5 = -Mth.cos((blockEntity.tick + 60) * (Math.PI.toFloat() / 180f)).toDouble()
//
//                val dir2 = Vec3(f4, 0.0, f5)
//
//                ParticleTool.sendParticle(
//                    level,
//                    ModParticleTypes.FIRE_STAR.get(),
//                    pos.x.toDouble() + 0.5,
//                    pos.y.toDouble() + 2.5,
//                    pos.z.toDouble() + 0.5,
//                    0,
//                    dir1.x,
//                    dir1.y,
//                    dir1.z,
//                    2.0,
//                    false
//                )
//                ParticleTool.sendParticle(
//                    level,
//                    ModParticleTypes.FIRE_STAR.get(),
//                    pos.x.toDouble() + 0.5,
//                    pos.y.toDouble() + 2.5,
//                    pos.z.toDouble() + 0.5,
//                    0,
//                    dir2.x,
//                    dir2.y,
//                    dir2.z,
//                    2.0,
//                    false
//                )
//            }
        }

        fun scanEntities(
            level: ServerLevel,
            pos: BlockPos,
            blockEntity: FuMO25BlockEntity,
            player: Player,
        ) {
            if (level.server.tickCount % MiscConfig.SYNC_ENTITY_INTERVAL.get() != 0) return

            val range = if (blockEntity.type == FuncType.WIDER) 2048 else 1024
            val radarPos = Vec3(pos.x + 0.5, pos.y + 2.5, pos.z + 0.5)

            val sourceId = "block_${pos.x}_${pos.y}_${pos.z}"
            val config = RadarScanner.RadarConfig(
                owner = player,
                center = radarPos,
                radius = range.toDouble(),
                sweepAngle = 120.0,
                yRot = blockEntity.tick.toDouble(),
                searchType = RadarScanner.SearchType.VEHICLES,
                sourceId = sourceId,
            )

            val result = RadarScanner.scan(level, config)
            result.sendToClients(player, level, config.shareWithTeammates)

            val rangeLiving = if (blockEntity.type == FuncType.WIDER) 96 else 128
            val configLiving = RadarScanner.RadarConfig(
                owner = player,
                center = radarPos,
                radius = rangeLiving.toDouble(),
                sweepAngle = 120.0,
                yRot = blockEntity.tick.toDouble(),
                searchType = RadarScanner.SearchType.LIVING,
                sourceId = sourceId,
            )

            val resultLiving = RadarScanner.scan(level, configLiving)
            resultLiving.sendToClients(player, level, configLiving.shareWithTeammates)
        }
    }
}
