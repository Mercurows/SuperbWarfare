package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.BoobyTrapBlock
import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkStatus
import java.util.*

class BoobyTrapBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlockEntities.BOOBY_TRAP.get(), pos, state) {
    var camouflage: BlockState? = null
        private set
    var ownerUUID: UUID? = null
        private set
    private var ownerName = ""

    fun recordOwner(owner: LivingEntity) {
        ownerUUID = owner.uuid
        ownerName = owner.scoreboardName
        setChanged()
    }

    fun isOwnerOrAlly(player: Player): Boolean {
        val uuid = ownerUUID ?: return false
        if (player.uuid == uuid) return true
        val world = level ?: return false
        val owner = world.getPlayerByUUID(uuid)
        if (owner != null) return owner.isAlliedTo(player)
        val team = world.scoreboard.getPlayersTeam(ownerName) ?: return false
        return player.team?.isAlliedTo(team) == true
    }

    fun applyCamouflage(state: BlockState?) {
        val world = level ?: return
        if (blockState.getValue(BoobyTrapBlock.OFF)) return
        val current = camouflage
        val material = if (state != null && current != null) {
            if (state.block !== current.block) return
            val orientation = current.block.stateDefinition.getProperty("axis")
                ?.takeIf { it.valueClass == Direction.Axis::class.java }
                ?: current.block.stateDefinition.getProperty("facing")
                    ?.takeIf { it.valueClass == Direction::class.java }
                ?: return
            current.cycle(orientation)
        } else state
        camouflage = material
        setChanged()
        val previous = blockState
        val updated = previous.setValue(BoobyTrapBlock.CAMOUFLAGED, material != null)
        world.setBlock(blockPos, updated, 3)
        world.sendBlockUpdated(blockPos, previous, updated, 3)
        refreshLighting()
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        camouflage?.let { tag.put("Camouflage", NbtUtils.writeBlockState(it)) }
        ownerUUID?.let { tag.putUUID("Owner", it) }
        if (ownerName.isNotEmpty()) tag.putString("OwnerName", ownerName)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        ownerUUID = if (tag.hasUUID("Owner")) tag.getUUID("Owner") else null
        ownerName = tag.getString("OwnerName")
        camouflage = if (tag.contains("Camouflage")) NbtUtils.readBlockState(
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Camouflage"))
            .takeUnless { it.isAir || it.block is BoobyTrapBlock } else null
        // The packet may change light opacity without changing the blockstate's camouflage flag.
        level?.takeIf { it.isClientSide }?.let { world ->
            refreshLighting()
            world.sendBlockUpdated(blockPos, blockState, blockState, 2)
        }
    }

    override fun onLoad() {
        super.onLoad()
        refreshLighting()
    }

    private fun refreshLighting() {
        val world = level ?: return
        val chunk = world.chunkSource.getChunk(blockPos.x shr 4, blockPos.z shr 4, ChunkStatus.FULL, false)
        if (chunk != null) {
            // Skylight source columns use local X/Z, while block entities are keyed by world position.
            val view = object : BlockGetter by chunk {
                override fun getBlockEntity(pos: BlockPos) = chunk.getBlockEntity(
                    BlockPos(chunk.pos.minBlockX + (pos.x and 15), pos.y, chunk.pos.minBlockZ + (pos.z and 15)))
            }
            chunk.skyLightSources.update(view, blockPos.x and 15, blockPos.y, blockPos.z and 15)
        }
        world.chunkSource.lightEngine.checkBlock(blockPos)
    }

    // Empty tags become null in ClientboundBlockEntityDataPacket and never reach load().
    override fun getUpdateTag(): CompoundTag = saveWithFullMetadata()
    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)
}
