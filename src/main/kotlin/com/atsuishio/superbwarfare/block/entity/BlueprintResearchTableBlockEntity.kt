package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.menu.BlueprintResearchTableMenu
import com.atsuishio.superbwarfare.recipe.ResearchingRecipe
import com.atsuishio.superbwarfare.tools.isSameItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.world.*
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.*


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

    // TODO 完成slot相关设定
    override fun canPlaceItemThroughFace(
        pIndex: Int,
        pItemStack: ItemStack,
        pDirection: Direction?
    ): Boolean {
        return false
    }

    override fun canTakeItemThroughFace(
        pIndex: Int,
        pStack: ItemStack?,
        pDirection: Direction?
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
    ): AbstractContainerMenu {
        return BlueprintResearchTableMenu(pContainerId, pPlayerInventory, this)
    }

    private fun getCurrentRecipe(): Optional<ResearchingRecipe> {
        if (this.level == null) {
            return Optional.empty()
        }

        val inventory = SimpleContainer(4)
        inventory.setItem(0, this.items[SLOT_INPUT])
        inventory.setItem(1, this.items[SLOT_BASE])
        inventory.setItem(2, this.items[SLOT_ADDITION])
        inventory.setItem(3, this.items[SLOT_SPECIAL])

        return this.level!!.recipeManager.getRecipeFor(
            ModRecipes.RESEARCHING_TYPE.get(),
            inventory,
            level
        )
    }

    private fun hasRecipe(): Boolean {
        if (this.level == null) return false

        val recipe = getCurrentRecipe()
        if (recipe.isEmpty) {
            return false
        }

        val result = recipe.get().result.getResult()
        return canInsertAmountIntoOutputSlot(result.count) && canInsertItemIntoOutputSlot(result.item)
    }

    private fun canInsertItemIntoOutputSlot(item: Item): Boolean {
        return this.items[SLOT_OUTPUT].isEmpty || this.items[SLOT_OUTPUT].`is`(item)
    }

    private fun canInsertAmountIntoOutputSlot(count: Int): Boolean {
        return this.items[SLOT_OUTPUT].count + count <= this.items[SLOT_OUTPUT].maxStackSize
    }

    private fun craftItem() {
        val recipe = getCurrentRecipe()
        if (recipe.isEmpty) {
            return
        }

        val result: ItemStack = recipe.get().result.getResult()

        val input = this.items[SLOT_INPUT]
        input.shrink(1)

        val output = this.items[SLOT_OUTPUT]
        this.items[SLOT_OUTPUT] = ItemStack(result.item, output.count + result.count)
    }

    companion object {
        const val SLOT_FUEL = 0
        const val SLOT_INPUT = 1
        const val SLOT_BASE = 2
        const val SLOT_ADDITION = 3
        const val SLOT_SPECIAL = 4
        const val SLOT_OUTPUT = 5

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, entity: BlueprintResearchTableBlockEntity) {
            if (entity.hasRecipe()) {
                val recipe = entity.getCurrentRecipe()
                if (recipe.isEmpty) return
//                val result = recipe.get().result
//                val item = if (result.isRandom()) {
//                    result.rollItem()
//                } else {
//                    result.getResult()
//                }
//                println("result=${item}")
            }
        }
    }
}