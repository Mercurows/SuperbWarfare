package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.FuMO25Block
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.VectorTool
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
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
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.energy.EnergyStorage
import net.neoforged.neoforge.energy.IEnergyStorage
import java.util.*
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.abs

open class FuMO25BlockEntity(pPos: BlockPos, pBlockState: BlockState) :
    BlockEntity(ModBlockEntities.FUMO_25.get(), pPos, pBlockState), MenuProvider {

    private val energyStorage: IEnergyStorage = EnergyStorage(MAX_ENERGY)

    var type: FuncType = FuncType.NORMAL
    var powered: Boolean = false
    var tickO: Int = 0
    var tick: Int = 0
    var ownerUUID: UUID? = null

    protected val dataAccess: ContainerEnergyData = object : ContainerEnergyData {
        override fun get(index: Int): Long {
            return when (index) {
                0 -> this@FuMO25BlockEntity.energyStorage.energyStored
                1 -> this@FuMO25BlockEntity.type.ordinal
                2 -> if (this@FuMO25BlockEntity.powered) 1 else 0
                3 -> this@FuMO25BlockEntity.tick
                4 -> this@FuMO25BlockEntity.tickO
                else -> 0
            }.toLong()
        }

        override fun set(index: Int, value: Long) {
            when (index) {
                0 -> this@FuMO25BlockEntity.energyStorage.receiveEnergy(value.toInt(), false)
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

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        val energyTag = tag.get("Energy")
        if (energyTag != null) {
            (energyStorage as EnergyStorage).deserializeNBT(registries, energyTag)
        }
        this.type = FuncType.entries[tag.getInt("Type").coerceIn(0, 3)]
        this.powered = tag.getBoolean("Powered")
        this.tick = tag.getInt("Tick")
        this.tickO = tag.getInt("TickO")

        if (tag.contains("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID")
        }
    }

    @ParametersAreNonnullByDefault
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        tag.put("Energy", (energyStorage as EnergyStorage).serializeNBT(registries))
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

    fun getEnergyStorage() = this.energyStorage

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
            if (!MiscConfig.SYNC_ENTITY_OVER_RANGE.get()) return
            val energyStorage = blockEntity.getEnergyStorage()
            val energy = energyStorage.energyStored

            blockEntity.tickO = blockEntity.tick

            if (state.getValue(FuMO25Block.POWERED)) {
                blockEntity.tick++
                blockEntity.sync()
            }

            val funcType = blockEntity.type
            val energyCost = if (funcType == FuncType.WIDER) {
                MAX_ENERGY_COST
            } else {
                DEFAULT_ENERGY_COST
            }

            val f = Mth.sin(blockEntity.tick * (Math.PI.toFloat() / 180f)).toDouble()
            val f1 = -Mth.cos(blockEntity.tick * (Math.PI.toFloat() / 180f)).toDouble()

            val direct = Vec3(f, 0.0, f1)

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
                    energyStorage.extractEnergy(energyCost, false)
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
                            scanEntities(level, pos, blockEntity, owner, direct)
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
            vec3: Vec3
        ) {
            if (level.server.tickCount % MiscConfig.SYNC_ENTITY_INTERVAL.get() != 0) return

            val range = if (blockEntity.type == FuncType.WIDER) 2048 else 1024
            val hostileList = level.allEntities.asSequence().mapNotNull {
                val seekRange =
                    range * range * if (it is VehicleEntity && !it.isWreck) it.computed().trackDistanceMultiply * it.computed().trackDistanceMultiply else 1.0
                val flag = (it is VehicleEntity || VehicleConfig.inScanList(it.type))
                        && SeekTool.NOT_IN_SMOKE.test(it)
                        && it.distanceToSqr(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) <= seekRange
                        && !SeekTool.IS_FRIENDLY.test(player, it)
                        && SeekTool.calculateAngle(
                    Vec3(
                        pos.x.toDouble() + 0.5,
                        pos.y.toDouble() + 2.5,
                        pos.z.toDouble() + 0.5
                    ), vec3, it
                ) < 60 && VectorTool.checkNoClip(
                    Vec3(
                        pos.x.toDouble() + 0.5,
                        pos.y.toDouble() + 2.5,
                        pos.z.toDouble() + 0.5
                    ), it.eyePosition, level
                )
                if (!flag) return@mapNotNull null
                EntitySyncMessage.SyncedEntity(
                    it.id,
                    BuiltInRegistries.ENTITY_TYPE.getKey(it.type),
                    it.position(),
                    it.deltaMovement,
                    CompoundTag().also { tag -> it.saveWithoutId(tag) }
                )
            }.toList()

            level.players()
                .asSequence()
                .filter { SeekTool.IS_FRIENDLY.test(player, it) }
                .forEach { sendPacketTo(it, EntitySyncMessage(level.dimension().location(), hostileList, false)) }
        }
    }
}
