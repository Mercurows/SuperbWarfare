package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.inventory.menu.BlueprintResearchTableMenu
import com.atsuishio.superbwarfare.recipe.ResearchingRecipe
import com.atsuishio.superbwarfare.tools.isSameItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.*
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
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

    var tick: Int = 0
    var lastSelectedIndex: Int = 0
    var fuel: Int = 0
    var maxProcessTick: Int = 100
        get() = field.coerceAtLeast(1)
    var activated: Boolean = false

    protected val dataAccess: ContainerData = object : ContainerData {
        override fun get(index: Int): Int {
            return when (index) {
                0 -> this@BlueprintResearchTableBlockEntity.tick
                1 -> this@BlueprintResearchTableBlockEntity.lastSelectedIndex
                2 -> this@BlueprintResearchTableBlockEntity.fuel
                3 -> this@BlueprintResearchTableBlockEntity.maxProcessTick
                4 -> if (this@BlueprintResearchTableBlockEntity.activated) 1 else 0
                else -> 0
            }
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                0 -> this@BlueprintResearchTableBlockEntity.tick = value
                1 -> this@BlueprintResearchTableBlockEntity.lastSelectedIndex = value
                2 -> this@BlueprintResearchTableBlockEntity.fuel = value
                3 -> this@BlueprintResearchTableBlockEntity.maxProcessTick = value
                4 -> this@BlueprintResearchTableBlockEntity.activated = value == 1
            }
        }

        override fun getCount(): Int {
            return MAX_DATA_COUNT
        }
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)

        this.tick = tag.getInt("Tick")
        this.lastSelectedIndex = tag.getInt("LastSelectedIndex")
        this.fuel = tag.getInt("Fuel")
        this.activated = tag.getBoolean("Activated")

        ContainerHelper.loadAllItems(tag, this.items)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)

        tag.putInt("Tick", this.tick)
        tag.putInt("LastSelectedIndex", this.lastSelectedIndex)
        tag.putInt("Fuel", this.fuel)
        tag.putBoolean("Activated", this.activated)

        ContainerHelper.saveAllItems(tag, this.items)
    }

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
            this.resetProgress()
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
        return BlueprintResearchTableMenu(pContainerId, pPlayerInventory, this, this.dataAccess)
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

        if (recipe.get().result.isRandom() && !this.items[SLOT_OUTPUT].isEmpty) {
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

        val result = recipe.get().result
        val item = if (result.isRandom()) result.rollItem() else result.getResult()

        val input = this.items[SLOT_INPUT]
        input.shrink(1)

        val output = this.items[SLOT_OUTPUT]
        this.items[SLOT_OUTPUT] = ItemStack(item.item, output.count + result.count)
    }

    fun resetProgress() {
        this.tick = 0
        this.lastSelectedIndex = 0
        this.maxProcessTick = 100
        this.activated = false
        this.setChanged()
    }

    companion object {
        const val SLOT_FUEL = 0
        const val SLOT_INPUT = 1
        const val SLOT_BASE = 2
        const val SLOT_ADDITION = 3
        const val SLOT_SPECIAL = 4
        const val SLOT_OUTPUT = 5

        const val MAX_DATA_COUNT = 5

        @JvmField
        val MAX_FUEL: Int = MiscConfig.BLUEPRINT_RESEARCH_TABLE_MAX_FUEL.get()

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, entity: BlueprintResearchTableBlockEntity) {
            if (entity.fuel < MAX_FUEL) {
                val fuelItem = entity.getItem(SLOT_FUEL)
                if (fuelItem.isEmpty || !fuelItem.`is`(ModTags.Items.RESEARCH_FUEL)) return

                fuelItem.shrink(1)
                entity.fuel++
                entity.setChanged()
            }
            // TODO 想办法判断一下是否处于手动模式
            if (entity.fuel > 0 && entity.hasRecipe()) {
                if (!entity.activated) return

                val recipe = entity.getCurrentRecipe()
                if (recipe.isEmpty) {
                    entity.activated = false
                    return
                }
                entity.maxProcessTick = recipe.get().time

                if (entity.tick < entity.maxProcessTick) {
                    entity.tick++
                } else {
                    entity.craftItem()
                    entity.resetProgress()
                    entity.fuel--
                    entity.setChanged()
                }
            } else {
                if (entity.activated) {
                    entity.activated = false
                    entity.setChanged()
                }

                if (entity.maxProcessTick != 100) {
                    entity.resetProgress()
                }
            }
        }
    }
}