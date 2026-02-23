package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.SuperbItemInterfaceBlock
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.inventory.menu.SuperbItemInterfaceMenu
import com.atsuishio.superbwarfare.tools.isSameItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.wrapper.InvWrapper

open class SuperbItemInterfaceBlockEntity(type: BlockEntityType<*>, pPos: BlockPos, pBlockState: BlockState) :
    BaseContainerBlockEntity(type, pPos, pBlockState) {
    private var items: NonNullList<ItemStack> = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY)
    private var cooldownTime = -1

    constructor(pPos: BlockPos, pBlockState: BlockState) : this(
        ModBlockEntities.SUPERB_ITEM_INTERFACE.get(),
        pPos,
        pBlockState
    )

    protected open val isCreative: Boolean
        get() = false

    override fun load(pTag: CompoundTag) {
        super.load(pTag)
        this.items = NonNullList.withSize(this.containerSize, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(pTag, this.items)
        this.cooldownTime = pTag.getInt("TransferCooldown")
    }

    override fun saveAdditional(pTag: CompoundTag) {
        super.saveAdditional(pTag)
        ContainerHelper.saveAllItems(pTag, this.items)
        pTag.putInt("TransferCooldown", this.cooldownTime)
    }

    override fun getDefaultName(): Component {
        return Component.translatable("container.superbwarfare.superb_item_interface")
    }

    override fun createMenu(pContainerId: Int, pInventory: Inventory): AbstractContainerMenu {
        return SuperbItemInterfaceMenu(pContainerId, pInventory, this)
    }

    override fun getContainerSize(): Int {
        return this.items.size
    }

    override fun isEmpty(): Boolean {
        return this.items.stream().allMatch { it.isEmpty }
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
        this.items[pSlot] = pStack
        if (pStack.count > this.maxStackSize) {
            pStack.count = this.maxStackSize
        }
    }

    override fun stillValid(pPlayer: Player): Boolean {
        return Container.stillValidBlockEntity(this, pPlayer)
    }

    override fun clearContent() {
        this.items.clear()
    }

    fun setCooldown(pCooldownTime: Int) {
        this.cooldownTime = pCooldownTime
    }

    private val isOnCooldown: Boolean
        get() = this.cooldownTime > 0

    override fun <T> getCapability(cap: Capability<T?>, side: Direction?): LazyOptional<T?> {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty<T?>(
            cap,
            LazyOptional.of { InvWrapper(this) }
        )
    }

    companion object {
        const val TRANSFER_COOLDOWN: Int = 20
        const val CONTAINER_SIZE: Int = 5

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: SuperbItemInterfaceBlockEntity) {
            --blockEntity.cooldownTime
            if (blockEntity.isOnCooldown) return
            blockEntity.setCooldown(TRANSFER_COOLDOWN)

            if (blockEntity.isEmpty) return
            if (!state.getValue(SuperbItemInterfaceBlock.ENABLED)) return

            val facing = state.getValue(SuperbItemInterfaceBlock.FACING)

            // find entities
            val x = pos.x + facing.stepX
            val y = pos.y + facing.stepY
            val z = pos.z + facing.stepZ

            val list = level.getEntities(
                null,
                AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5)
            ) {
                it.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent
            }

            if (list.isEmpty()) return
            val target = list[level.random.nextInt(list.size)]

            // item transfer
            for (i in blockEntity.items.indices) {
                val stack = blockEntity.items[i]
                if (stack.isEmpty) continue

                val originalStack = stack.copy()

                val itemHandler = target.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get()

                var totalInserted = 0
                for (ii in 0..<itemHandler.slots) {
                    var inserted: Int = stack.count
                    while (inserted > 0) {
                        val insertedStack = itemHandler.insertItem(ii, stack.copyWithCount(inserted), true)
                        if (insertedStack.count != inserted || !isSameItemStack(insertedStack, stack)) {
                            break
                        }
                        inserted--
                    }

                    if (inserted > 0) {
                        itemHandler.insertItem(ii, stack.copyWithCount(inserted), false)
                        stack.shrink(inserted)
                        totalInserted += inserted
                    }
                }

                if (!blockEntity.isCreative) {
                    blockEntity.items[i] = stack
                    blockEntity.setChanged()
                } else {
                    blockEntity.items[i] = originalStack
                }

                // 只尝试进行一次单格物品传输
                if (totalInserted > 0) {
                    break
                }
            }
        }
    }
}
