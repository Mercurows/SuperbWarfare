package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.BlueprintResearchTableBlock
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.inventory.menu.BlueprintResearchTableMenu
import com.atsuishio.superbwarfare.recipe.ResearchingRecipe
import com.atsuishio.superbwarfare.tools.isSameItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BedPart
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.wrapper.RecipeWrapper
import java.util.*

open class BlueprintResearchTableBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.BLUEPRINT_RESEARCH_TABLE.get(), pos, state),
    WorldlyContainer, MenuProvider {
    protected val items: NonNullList<ItemStack> = NonNullList.withSize(6, ItemStack.EMPTY)

    var tick: Int = 0
    var lastSelectedIndex: Int = 0
    var fuel: Int = 0
    var maxProcessTick: Int = DEFAULT_TIME
        get() = field.coerceAtLeast(1)
    var activated: Boolean = false
    var crafting: Boolean = false

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

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        this.tick = tag.getInt("Tick")
        this.lastSelectedIndex = tag.getInt("LastSelectedIndex")
        this.fuel = tag.getInt("Fuel")
        this.activated = tag.getBoolean("Activated")
        this.crafting = tag.getBoolean("Crafting")

        ContainerHelper.loadAllItems(tag, this.items, registries)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        tag.putInt("Tick", this.tick)
        tag.putInt("LastSelectedIndex", this.lastSelectedIndex)
        tag.putInt("Fuel", this.fuel)
        tag.putBoolean("Activated", this.activated)
        tag.putBoolean("Crafting", this.crafting)

        ContainerHelper.saveAllItems(tag, this.items, registries)
    }

    override fun getSlotsForFace(side: Direction): IntArray {
        if (this.blockState.getValue(BlueprintResearchTableBlock.PART) == BedPart.HEAD) return intArrayOf()
        return when (side) {
            Direction.DOWN -> intArrayOf(SLOT_OUTPUT)
            Direction.NORTH -> intArrayOf(SLOT_INPUT)
            Direction.EAST -> intArrayOf(SLOT_BASE)
            Direction.SOUTH -> intArrayOf(SLOT_ADDITION)
            else -> intArrayOf(SLOT_FUEL)
        }
    }

    override fun canPlaceItemThroughFace(
        index: Int,
        stack: ItemStack,
        side: Direction?
    ): Boolean {
        if (this.blockState.getValue(BlueprintResearchTableBlock.PART) == BedPart.HEAD) return false

        return when (side) {
            Direction.DOWN -> index == SLOT_OUTPUT
            Direction.NORTH -> index == SLOT_INPUT
            Direction.EAST -> index == SLOT_BASE
            Direction.SOUTH -> index == SLOT_ADDITION
            else -> index == SLOT_FUEL
        }
    }

    override fun canTakeItemThroughFace(
        pIndex: Int,
        pStack: ItemStack,
        pDirection: Direction
    ): Boolean {
        if (this.blockState.getValue(BlueprintResearchTableBlock.PART) == BedPart.HEAD) return false
        return pIndex == SLOT_OUTPUT && pDirection == Direction.DOWN
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

    private fun getCurrentRecipe(): Optional<RecipeHolder<ResearchingRecipe>> {
        val level = this.level ?: return Optional.empty()

        val inventory = ItemStackHandler(4)
        inventory.setStackInSlot(0, this.items[SLOT_INPUT])
        inventory.setStackInSlot(1, this.items[SLOT_BASE])
        inventory.setStackInSlot(2, this.items[SLOT_ADDITION])
        inventory.setStackInSlot(3, this.items[SLOT_SPECIAL])

        val recipe = level.recipeManager.getRecipeFor(
            ModRecipes.RESEARCHING_TYPE.get(),
            RecipeWrapper(inventory),
            level
        )
        return recipe
    }

    private fun hasRecipe(): Boolean {
        if (this.level == null) return false

        val recipe = getCurrentRecipe()
        if (recipe.isEmpty) {
            return false
        }

        val holder = recipe.get()
        val value = holder.value ?: return false

        if (value.result.isRandom() && !this.items[SLOT_OUTPUT].isEmpty) {
            return false
        }

        val result = value.result.getResult()
        return canInsertAmountIntoOutputSlot(result.count) && canInsertItemIntoOutputSlot(result.item)
    }

    private fun canInsertItemIntoOutputSlot(item: Item): Boolean {
        return this.items[SLOT_OUTPUT].isEmpty || this.items[SLOT_OUTPUT].`is`(item)
    }

    private fun canInsertAmountIntoOutputSlot(count: Int): Boolean {
        val output = this.items[SLOT_OUTPUT]
        val size = if (output.isEmpty) 64 else output.maxStackSize
        return output.count + count <= size
    }

    private fun craftItem() {
        val recipe = getCurrentRecipe()
        if (recipe.isEmpty) {
            return
        }

        val holder = recipe.get()
        val value = holder.value ?: return

        val result = value.result
        val item = if (value.selectable) {
            result.getItemByIndex(this.lastSelectedIndex)
        } else if (result.isRandom()) {
            result.rollItem()
        } else {
            result.getResult()
        }

        val input = this.items[SLOT_INPUT]
        input.shrink(1)
        val base = this.items[SLOT_BASE]
        base.shrink(1)
        val addition = this.items[SLOT_ADDITION]
        addition.shrink(1)

        val output = this.items[SLOT_OUTPUT]
        this.items[SLOT_OUTPUT] = ItemStack(item.item, output.count + result.count)
    }

    fun resetProgress() {
        this.tick = 0
        this.maxProcessTick = 100
        this.activated = false
        this.crafting = false
        this.setChanged()
    }

    fun sync() {
        val level = this.level ?: return
        if (level.isClientSide) return
        this.setChanged()
        level.sendBlockUpdated(this.worldPosition, this.blockState, this.blockState, 3)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        this.saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun handleUpdateTag(tag: CompoundTag, lookupProvider: HolderLookup.Provider) {
        tag.let { this.loadAdditional(it, lookupProvider) }
    }

    override fun onDataPacket(
        net: Connection,
        pkt: ClientboundBlockEntityDataPacket,
        lookupProvider: HolderLookup.Provider
    ) {
        this.handleUpdateTag(pkt.tag, lookupProvider)
    }

    companion object {
        const val SLOT_FUEL = 0
        const val SLOT_INPUT = 1
        const val SLOT_BASE = 2
        const val SLOT_ADDITION = 3
        const val SLOT_SPECIAL = 4
        const val SLOT_OUTPUT = 5

        const val MAX_DATA_COUNT = 5
        const val DEFAULT_TIME = 1200

        @JvmField
        val MAX_FUEL: Int = MiscConfig.BLUEPRINT_RESEARCH_TABLE_MAX_FUEL.get()

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, entity: BlueprintResearchTableBlockEntity) {
            if (entity.fuel < MAX_FUEL) {
                val fuelItem = entity.getItem(SLOT_FUEL)
                if (!fuelItem.isEmpty && fuelItem.`is`(ModTags.Items.RESEARCH_FUEL)) {
                    fuelItem.shrink(1)
                    entity.fuel++
                    entity.setChanged()
                }
            }

            if (entity.fuel > 0 && entity.hasRecipe()) {
                if (state.getValue(BlueprintResearchTableBlock.ENABLED)) {
                    entity.activated = true
                }

                if (!entity.activated) return

                val recipe = entity.getCurrentRecipe()
                if (recipe.isEmpty) {
                    entity.activated = false
                    return
                }

                if (!entity.crafting) {
                    entity.crafting = true
                }

                entity.maxProcessTick = recipe.get().value.time

                if (entity.tick < entity.maxProcessTick) {
                    entity.tick++
                } else {
                    entity.craftItem()
                    entity.resetProgress()
                    entity.fuel--
                    entity.setChanged()
                }

                entity.sync()
            } else {
                if (entity.activated) {
                    entity.activated = false
                    entity.lastSelectedIndex = 0
                    entity.setChanged()
                }

                if (entity.maxProcessTick != DEFAULT_TIME) {
                    entity.lastSelectedIndex = 0
                    entity.resetProgress()
                }
            }
        }
    }
}