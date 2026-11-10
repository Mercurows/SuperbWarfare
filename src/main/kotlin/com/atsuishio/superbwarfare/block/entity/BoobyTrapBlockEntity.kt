package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.BoobyTrapBlock
import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class BoobyTrapBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlockEntities.BOOBY_TRAP.get(), pos, state) {
    var camouflage: BlockState? = null
        private set

    fun applyCamouflage(state: BlockState?) {
        val world = level ?: return
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
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        camouflage?.let { tag.put("Camouflage", NbtUtils.writeBlockState(it)) }
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        camouflage = if (tag.contains("Camouflage")) NbtUtils.readBlockState(
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Camouflage"))
            .takeUnless { it.isAir || it.block is BoobyTrapBlock } else null
    }

    // Empty tags become null in ClientboundBlockEntityDataPacket and never reach load().
    override fun getUpdateTag(): CompoundTag = saveWithFullMetadata()
    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)
}
