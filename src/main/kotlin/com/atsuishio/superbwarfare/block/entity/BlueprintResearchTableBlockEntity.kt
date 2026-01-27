package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.menu.BlueprintResearchTableMenu
import com.atsuishio.superbwarfare.tools.isSameItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

open class BlueprintResearchTableBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.BLUEPRINT_RESEARCH_TABLE.get(), pos, state), GeoBlockEntity,
    WorldlyContainer, MenuProvider {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    protected val items: NonNullList<ItemStack> = NonNullList.withSize(6, ItemStack.EMPTY)

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return cache
    }

    override fun getSlotsForFace(pSide: Direction): IntArray {
        return intArrayOf(0)
    }

    override fun canPlaceItemThroughFace(
        pIndex: Int,
        pItemStack: ItemStack,
        pDirection: Direction?
    ): Boolean {
        return false
    }

    override fun canTakeItemThroughFace(
        pIndex: Int,
        pStack: ItemStack,
        pDirection: Direction
    ): Boolean {
        return pIndex == SLOT_OUTPUT
    }

    override fun getContainerSize(): Int {
        return this.items.size
    }

    override fun isEmpty(): Boolean {
        for (item in this.items) {
            if (!item.isEmpty) return false
        }
        return true
    }

    override fun getItem(pSlot: Int): ItemStack {
        return this.items[pSlot]
    }

    override fun removeItem(pSlot: Int, pAmount: Int): ItemStack {
        return ContainerHelper.removeItem(this.items, pSlot, pAmount)
    }

    override fun removeItemNoUpdate(pSlot: Int): ItemStack {
        return ContainerHelper.takeItem(this.items, pSlot)
    }

    override fun setItem(pSlot: Int, pStack: ItemStack) {
        val itemstack = this.items[pSlot]
        val flag = !pStack.isEmpty && isSameItemStack(itemstack, pStack)
        this.items[pSlot] = pStack
        if (pStack.count > this.maxStackSize) {
            pStack.count = this.maxStackSize
        }

        if (pSlot != SLOT_FUEL && pSlot != SLOT_OUTPUT && !flag) {
            this.setChanged()
        }
    }

    override fun stillValid(pPlayer: Player): Boolean {
        return Container.stillValidBlockEntity(this, pPlayer)
    }

    override fun clearContent() {
        this.items.clear()
    }

    override fun getDisplayName(): Component {
        return Component.translatable("container.superbwarfare.blueprint_research_table")
    }

    override fun createMenu(
        pContainerId: Int,
        pPlayerInventory: Inventory,
        pPlayer: Player
    ): AbstractContainerMenu? {
        return BlueprintResearchTableMenu(pContainerId, pPlayerInventory, this)
    }

    companion object {
        const val SLOT_FUEL = 0
        const val SLOT_INPUT = 1
        const val SLOT_INPUT_BASE = 2
        const val SLOT_INPUT_DYE = 3
        const val SLOT_SPECIAL = 4
        const val SLOT_OUTPUT = 5
    }
}