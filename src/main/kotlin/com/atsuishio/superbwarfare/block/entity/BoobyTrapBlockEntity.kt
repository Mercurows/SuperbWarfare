package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.BoobyTrapBlock
import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class BoobyTrapBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlockEntities.BOOBY_TRAP.get(), pos, state) {
    var camouflage: BlockState? = null
        private set
    internal var detonating = false

    fun applyCamouflage(state: BlockState?) {
        val world = level ?: return
        if (world.isClientSide || isRemoved) return
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

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        camouflage?.let { tag.put("Camouflage", NbtUtils.writeBlockState(it)) }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        camouflage = if (tag.contains("Camouflage")) NbtUtils.readBlockState(
            registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("Camouflage"))
            .takeUnless { it.isAir || it.block is BoobyTrapBlock } else null
    }

    // Keep the removal update nonempty so clients also clear their cached camouflage.
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = saveWithFullMetadata(registries)
    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)
}
