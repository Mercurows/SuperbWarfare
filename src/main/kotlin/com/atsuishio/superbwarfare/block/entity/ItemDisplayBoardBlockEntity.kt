package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB

open class ItemDisplayBoardBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.ITEM_DISPLAY_BOARD.get(), pos, state) {

    private val displayItems: Array<ItemStack> = arrayOf(ItemStack.EMPTY, ItemStack.EMPTY)

    val itemCount: Int
        get() = (if (displayItems[0].isEmpty) 0 else 1) + (if (displayItems[1].isEmpty) 0 else 1)

    val hasItem: Boolean
        get() = !displayItems[0].isEmpty || !displayItems[1].isEmpty

    fun getItem(slot: Int): ItemStack {
        return displayItems[slot]
    }


    fun firstEmptySlot(): Int {
        if (displayItems[0].isEmpty) return 0
        if (displayItems[1].isEmpty) return 1
        return -1
    }

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
        val slot = if (!displayItems[1].isEmpty) 1 else if (!displayItems[0].isEmpty) 0 else -1
        if (slot < 0) return ItemStack.EMPTY

        val stack = displayItems[slot]
        this.clearItem(slot)
        return stack
    }

    override fun getRenderBoundingBox(): AABB {
        return AABB(this.blockPos).inflate(0.5)
    }

    private fun markUpdated() {
        this.setChanged()
        val level = this.level ?: return
        if (!level.isClientSide) {
            level.sendBlockUpdated(this.blockPos, this.blockState, this.blockState, Block.UPDATE_ALL)
        }
    }

    override fun load(pTag: CompoundTag) {
        super.load(pTag)
        displayItems[0] = ItemStack.EMPTY
        displayItems[1] = ItemStack.EMPTY

        if (pTag.contains("DisplayItem0")) {
            displayItems[0] = ItemStack.of(pTag.getCompound("DisplayItem0"))
        }
        if (pTag.contains("DisplayItem1")) {
            displayItems[1] = ItemStack.of(pTag.getCompound("DisplayItem1"))
        }
        if (!this.hasItem && pTag.contains("DisplayItem")) {
            displayItems[0] = ItemStack.of(pTag.getCompound("DisplayItem"))
        }
    }

    override fun saveAdditional(pTag: CompoundTag) {
        super.saveAdditional(pTag)
        for (slot in 0..1) {
            if (!displayItems[slot].isEmpty) {
                pTag.put("DisplayItem$slot", displayItems[slot].save(CompoundTag()))
            }
        }
    }

    override fun getUpdateTag(): CompoundTag {
        return this.saveWithFullMetadata()
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}
