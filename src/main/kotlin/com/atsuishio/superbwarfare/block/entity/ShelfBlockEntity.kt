package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Containers
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ShelfBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlockEntities.SHELF.get(), pos, state) {
    private val items = NonNullList.withSize(2, ItemStack.EMPTY)

    fun getItem(slot: Int): ItemStack = items[slot]

    fun insertItem(slot: Int, stack: ItemStack, count: Int = stack.count): Int {
        if (stack.isEmpty) return 0
        val stored = items[slot]
        if (!stored.isEmpty && !ItemStack.isSameItemSameComponents(stored, stack)) return 0
        val inserted = minOf(count, stack.count, stack.maxStackSize - stored.count)
        if (inserted <= 0) return 0
        if (stored.isEmpty) items[slot] = stack.copyWithCount(inserted) else stored.grow(inserted)
        markUpdated()
        return inserted
    }

    fun takeItem(slot: Int, inventory: Inventory, count: Int): Int {
        val stored = items[slot]
        if (stored.isEmpty || count <= 0) return 0
        val amount = minOf(count, stored.count, stored.maxStackSize)
        val offered = stored.copyWithCount(amount)
        // Creative inventory insertion can consume a remainder even if it could not store it.
        val before = if (inventory.player.hasInfiniteMaterials()) inventory.countItem(stored.item) else null
        inventory.add(offered)
        val taken = if (before == null) amount - offered.count else inventory.countItem(stored.item) - before
        if (taken > 0) removeItem(slot, taken)
        return taken
    }

    private fun removeItem(slot: Int, count: Int): ItemStack {
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

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        items.clear()
        ContainerHelper.loadAllItems(tag, items, registries)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        ContainerHelper.saveAllItems(tag, items, registries)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = saveWithoutMetadata(registries)

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)
}
