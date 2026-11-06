package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

open class ItemDisplayBoardBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.ITEM_DISPLAY_BOARD.get(), pos, state) {

    private val displayItems = arrayOf(ItemStack.EMPTY, ItemStack.EMPTY)

    val itemCount: Int
        get() = displayItems.count { !it.isEmpty }

    val hasItem: Boolean
        get() = displayItems.any { !it.isEmpty }

    fun getItem(slot: Int): ItemStack = displayItems[slot]

    fun firstEmptySlot(): Int = displayItems.indexOfFirst { it.isEmpty }

    fun setItem(slot: Int, stack: ItemStack) {
        displayItems[slot] = if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
        this.markUpdated()
    }

    fun clearItem(slot: Int, sync: Boolean = true) {
        displayItems[slot] = ItemStack.EMPTY
        if (sync) {
            this.markUpdated()
        } else {
            this.setChanged()
        }
    }

    fun takeLastItem(): ItemStack {
        val slot = displayItems.indexOfLast { !it.isEmpty }
        if (slot < 0) return ItemStack.EMPTY
        val stack = displayItems[slot]
        clearItem(slot)
        return stack
    }

    private fun markUpdated() {
        this.setChanged()
        val level = this.level ?: return
        if (!level.isClientSide) {
            level.sendBlockUpdated(this.blockPos, this.blockState, this.blockState, Block.UPDATE_ALL)
        }
    }

    override fun loadAdditional(pTag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(pTag, registries)
        for (slot in displayItems.indices) {
            displayItems[slot] = ItemStack.parseOptional(registries, pTag.getCompound("DisplayItem$slot"))
        }
        // 兼容只有一个物品的旧展示板；更新包也会清空未携带的槽位。
        if (!hasItem && pTag.contains("DisplayItem")) {
            displayItems[0] = ItemStack.parseOptional(registries, pTag.getCompound("DisplayItem"))
        }
    }

    override fun saveAdditional(pTag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(pTag, registries)
        pTag.remove("DisplayItem")
        for (slot in displayItems.indices) {
            val stack = displayItems[slot]
            if (stack.isEmpty) pTag.remove("DisplayItem$slot")
            else pTag.put("DisplayItem$slot", stack.save(registries))
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return this.saveWithoutMetadata(registries)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}
