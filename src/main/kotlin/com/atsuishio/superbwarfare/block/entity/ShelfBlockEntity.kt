package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Containers
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ShelfBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlockEntities.SHELF.get(), pos, state) {
    private val items = NonNullList.withSize(2, ItemStack.EMPTY)

    fun getItem(slot: Int): ItemStack = items[slot]

    fun insertItem(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty || !items[slot].isEmpty) return false
        items[slot] = stack.copy()
        markUpdated()
        return true
    }

    fun removeItem(slot: Int, count: Int): ItemStack {
        val removed = ContainerHelper.removeItem(items, slot, count)
        if (!removed.isEmpty) markUpdated()
        return removed
    }

    fun dropContents() {
        val level = level ?: return
        Containers.dropContents(level, blockPos, items)
        items.clear()
        setChanged()
    }

    private fun markUpdated() {
        setChanged()
        val level = level ?: return
        if (!level.isClientSide) level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        items.clear()
        ContainerHelper.loadAllItems(tag, items)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        ContainerHelper.saveAllItems(tag, items)
    }

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)
}
