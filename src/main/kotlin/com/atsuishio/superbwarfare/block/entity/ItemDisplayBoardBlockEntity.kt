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

    var item: ItemStack = ItemStack.EMPTY
        private set

    fun setItem(stack: ItemStack) {
        this.item = if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
        this.markUpdated()
    }

    fun clearItem(sync: Boolean = true) {
        this.item = ItemStack.EMPTY
        if (sync) {
            this.markUpdated()
        } else {
            this.setChanged()
        }
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
        this.item = if (pTag.contains("DisplayItem")) {
            ItemStack.parseOptional(registries, pTag.getCompound("DisplayItem"))
        } else {
            ItemStack.EMPTY
        }
    }

    override fun saveAdditional(pTag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(pTag, registries)
        if (!this.item.isEmpty) {
            pTag.put("DisplayItem", this.item.save(registries))
        } else {
            pTag.remove("DisplayItem")
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return this.saveWithoutMetadata(registries)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}
