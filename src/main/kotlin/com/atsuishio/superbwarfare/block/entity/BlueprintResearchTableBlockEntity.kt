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
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
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
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.wrapper.SidedInvWrapper
import java.util.*

open class BlueprintResearchTableBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.BLUEPRINT_RESEARCH_TABLE.get(), pos, state),
    WorldlyContainer, MenuProvider {
    protected val items: NonNullList<ItemStack> = NonNullList.withSize(6, ItemStack.EMPTY)

    private var itemHandlers =
        SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST)

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

    override fun load(tag: CompoundTag) {
        super.load(tag)

        this.tick = tag.getInt("Tick")
        this.lastSelectedIndex = tag.getInt("LastSelectedIndex")
        this.fuel = tag.getInt("Fuel")
        this.activated = tag.getBoolean("Activated")
        this.crafting = tag.getBoolean("Crafting")

        ContainerHelper.loadAllItems(tag, this.items)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)

        tag.putInt("Tick", this.tick)
        tag.putInt("LastSelectedIndex", this.lastSelectedIndex)
        tag.putInt("Fuel", this.fuel)
        tag.putBoolean("Activated", this.activated)
        tag.putBoolean("Crafting", this.crafting)

        ContainerHelper.saveAllItems(tag, this.items)
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
            level!!
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
        val item = if (recipe.get().selectable) {
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

    override fun <T> getCapability(
        cap: Capability<T?>,
        side: Direction?
    ): LazyOptional<T?> {
        if (!this.remove && side != null && cap == ForgeCapabilities.ITEM_HANDLER) {
            return when (side) {
                Direction.UP -> itemHandlers[0].cast()
                Direction.DOWN -> itemHandlers[1].cast()
                Direction.NORTH -> itemHandlers[2].cast()
                Direction.SOUTH -> itemHandlers[3].cast()
                else -> itemHandlers[4].cast()
            }
        }
        return super.getCapability(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        for (itemHandler in itemHandlers) itemHandler.invalidate()
    }

    override fun reviveCaps() {
        super.reviveCaps()
        this.itemHandlers =
            SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST)
    }

    fun sync() {
        val level = this.level ?: return
        if (level.isClientSide) return
        this.setChanged()
        level.sendBlockUpdated(this.worldPosition, this.blockState, this.blockState, 3)
    }

    override fun getUpdateTag(): CompoundTag {
        val tag = CompoundTag()
        this.saveAdditional(tag)
        return tag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun handleUpdateTag(tag: CompoundTag?) {
        tag?.let { this.load(it) }
    }

    override fun onDataPacket(
        net: Connection,
        pkt: ClientboundBlockEntityDataPacket
    ) {
        this.handleUpdateTag(pkt.tag)
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

                entity.maxProcessTick = recipe.get().time

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

    override fun getRenderBoundingBox(): AABB {
        // 创建一个更大的边界框（示例：覆盖从方块底部到顶部上方2格的范围）
        val expansion = 2.0 // 根据模型实际大小调整
        return AABB(
            (worldPosition.x - 1).toDouble(),
            worldPosition.y.toDouble(),
            (worldPosition.z - 1).toDouble(),
            (worldPosition.x + 2).toDouble(),
            worldPosition.y + expansion,
            (worldPosition.z + 2).toDouble()
        )
    }
}