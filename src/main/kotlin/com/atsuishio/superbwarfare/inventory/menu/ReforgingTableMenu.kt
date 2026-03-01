package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModMenuTypes
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.item.misc.PerkItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.max
import kotlin.math.min

open class ReforgingTableMenu(
    pContainerId: Int,
    inventory: Inventory,
    container: Container,
    pContainerLevelAccess: ContainerLevelAccess
) : AbstractContainerMenu(ModMenuTypes.REFORGING_TABLE_MENU.get(), pContainerId) {
    protected val container: Container
    protected val access: ContainerLevelAccess

    @JvmField
    val ammoPerkLevel: DataSlot = DataSlot.standalone()

    @JvmField
    val funcPerkLevel: DataSlot = DataSlot.standalone()

    @JvmField
    val damagePerkLevel: DataSlot = DataSlot.standalone()

    constructor(pContainerId: Int, pPlayerInventory: Inventory) : this(
        pContainerId,
        pPlayerInventory,
        SimpleContainer(5),
        ContainerLevelAccess.NULL
    )

    constructor(pContainerId: Int, pPlayerInventory: Inventory, access: ContainerLevelAccess) : this(
        pContainerId,
        pPlayerInventory,
        SimpleContainer(5),
        access
    )

    init {
        checkContainerSize(container, 5)

        this.container = container
        this.access = pContainerLevelAccess

        this.ammoPerkLevel.set(0)
        this.funcPerkLevel.set(0)
        this.damagePerkLevel.set(0)

        this.addDataSlot(ammoPerkLevel)
        this.addDataSlot(funcPerkLevel)
        this.addDataSlot(damagePerkLevel)

        this.addSlot(InputSlot(container, INPUT_SLOT, 20, 22))
        this.addSlot(PerkSlot(container, AMMO_PERK_SLOT, Perk.Type.AMMO, 80, 25))
        this.addSlot(PerkSlot(container, FUNC_PERK_SLOT, Perk.Type.FUNCTIONAL, 80, 45))
        this.addSlot(PerkSlot(container, DAMAGE_PERK_SLOT, Perk.Type.DAMAGE, 80, 65))
        this.addSlot(ResultSlot(container, RESULT_SLOT, 142, 45))

        for (i in 0..2) {
            for (j in 0..8) {
                this.addSlot(Slot(inventory, j + i * 9 + 9, 8 + j * 18 + X_OFFSET, 84 + i * 18 + Y_OFFSET))
            }
        }

        for (k in 0..8) {
            this.addSlot(Slot(inventory, k, 8 + k * 18 + X_OFFSET, 142 + Y_OFFSET))
        }
    }

    override fun quickMoveStack(pPlayer: Player, pIndex: Int): ItemStack {
        var itemstack = ItemStack.EMPTY
        val slot = this.slots[pIndex]
        if (slot.hasItem()) {
            val stack = slot.item
            itemstack = stack.copy()

            if (pIndex == INPUT_SLOT) {
                onTakeGun(stack)
                if (!this.moveItemStackTo(stack, RESULT_SLOT + 1, RESULT_SLOT + 37, false)) {
                    return ItemStack.EMPTY
                }
            } else if (pIndex in AMMO_PERK_SLOT..DAMAGE_PERK_SLOT) {
                onTakePerk(stack)
                if (!this.moveItemStackTo(stack, RESULT_SLOT + 1, RESULT_SLOT + 37, false)) {
                    return ItemStack.EMPTY
                }
            } else if (pIndex == RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, RESULT_SLOT, RESULT_SLOT + 36, false)) {
                    return ItemStack.EMPTY
                }
            } else {
                val item = stack.item
                if (item is GunItem) {
                    if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY
                    }
                } else if (item is PerkItem) {
                    val type = item.perk.type
                    if (type == Perk.Type.AMMO) {
                        if (!this.moveItemStackTo(stack, AMMO_PERK_SLOT, AMMO_PERK_SLOT + 1, false)) {
                            return ItemStack.EMPTY
                        }
                    } else if (type == Perk.Type.FUNCTIONAL) {
                        if (!this.moveItemStackTo(stack, FUNC_PERK_SLOT, FUNC_PERK_SLOT + 1, false)) {
                            return ItemStack.EMPTY
                        }
                    } else if (type == Perk.Type.DAMAGE) {
                        if (!this.moveItemStackTo(stack, DAMAGE_PERK_SLOT, DAMAGE_PERK_SLOT + 1, false)) {
                            return ItemStack.EMPTY
                        }
                    }
                }
            }

            if (stack.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }

            if (stack.count == itemstack.count) {
                return ItemStack.EMPTY
            }

            slot.onTake(pPlayer, stack)
        }

        return itemstack
    }

    override fun stillValid(pPlayer: Player): Boolean {
        return this.access.evaluate({ level, pos ->
            level.getBlockState(pos).`is`(ModBlocks.REFORGING_TABLE.get())
                    && pPlayer.distanceToSqr(
                pos.x.toDouble() + 0.5,
                pos.y.toDouble() + 0.5,
                pos.z.toDouble() + 0.5
            ) <= 64
        }, true)
    }

    val gunStack: ItemStack?
        get() {
            val gun = this.container.getItem(INPUT_SLOT)
            if (gun.item is GunItem) {
                return gun
            }
            return null
        }

    val gunData: GunData?
        get() {
            val gun = this.gunStack
            return if (gun == null) null else GunData.from(gun)
        }

    override fun removed(pPlayer: Player) {
        super.removed(pPlayer)
        this.access.execute { _, _ ->
            val gun = this.container.getItem(INPUT_SLOT)
            val copy = gun.copy()
            for (i in 0..<this.container.containerSize) {
                val itemstack = this.container.getItem(i)
                val item = itemstack.item

                if (copy.item is GunItem
                    && item is PerkItem
                    && !copy.isEmpty && GunData.from(copy).perk.getLevel(item) > 0
                ) continue

                if (!itemstack.isEmpty) {
                    pPlayer.inventory.placeItemBackInInventory(itemstack)
                }

                this.container.removeItemNoUpdate(i)
            }
        }
    }

    fun availableLevel(): Int {
        val data = this.gunData ?: return 0

        var totalLevel = data.level.get()
        totalLevel -= max(0, this.ammoPerkLevel.get() - 1)
        totalLevel -= max(0, this.funcPerkLevel.get() - 1)
        totalLevel -= max(0, this.damagePerkLevel.get() - 1)

        return max(0, totalLevel)
    }

    fun setPerkLevel(type: Perk.Type, upgrade: Boolean, isCreative: Boolean) {
        if (upgrade && availableLevel() <= 0 && !isCreative) {
            return
        }

        if (!upgrade && availableLevel() >= MAX_UPGRADE_POINT && !isCreative) {
            return
        }

        when (type) {
            Perk.Type.AMMO -> this.ammoPerkLevel.set(
                if (upgrade) min(
                    MAX_PERK_LEVEL,
                    this.ammoPerkLevel.get() + 1
                ) else max(1, this.ammoPerkLevel.get() - 1)
            )

            Perk.Type.FUNCTIONAL -> this.funcPerkLevel.set(
                if (upgrade) min(
                    MAX_PERK_LEVEL,
                    this.funcPerkLevel.get() + 1
                ) else max(1, this.funcPerkLevel.get() - 1)
            )

            Perk.Type.DAMAGE -> this.damagePerkLevel.set(
                if (upgrade) min(
                    MAX_PERK_LEVEL,
                    this.damagePerkLevel.get() + 1
                ) else max(1, this.damagePerkLevel.get() - 1)
            )
        }
    }

    /**
     * 根据输入槽的枪械和Perk槽中的物品与等级，生成重铸后的武器，并放入输出槽中
     */
    fun generateResult() {
        val gun = this.gunStack ?: return

        val ammo = this.container.getItem(AMMO_PERK_SLOT)
        val func = this.container.getItem(FUNC_PERK_SLOT)
        val damage = this.container.getItem(DAMAGE_PERK_SLOT)
        if (ammo.isEmpty && func.isEmpty && damage.isEmpty) {
            return
        }

        val result = gun.copy()
        val data = GunData.from(result)

        listOf(ammo, func, damage).forEach { item ->
            val perkItem = item.item
            if (!item.isEmpty && perkItem is PerkItem && GunData.from(container.getItem(INPUT_SLOT)).canApplyPerk(perkItem.perk)) {
                data.perk.set(
                    PerkInstance(
                        perkItem.perk, when (perkItem.perk.type) {
                            Perk.Type.AMMO -> this.ammoPerkLevel.get()
                            Perk.Type.FUNCTIONAL -> this.funcPerkLevel.get()
                            Perk.Type.DAMAGE -> this.damagePerkLevel.get()
                        }.toShort()
                    )
                )
                this.container.setItem(
                    when (perkItem.perk.type) {
                        Perk.Type.AMMO -> AMMO_PERK_SLOT
                        Perk.Type.FUNCTIONAL -> FUNC_PERK_SLOT
                        Perk.Type.DAMAGE -> DAMAGE_PERK_SLOT
                    }, ItemStack.EMPTY
                )
            }
        }

        data.save()

        this.ammoPerkLevel.set(0)
        this.funcPerkLevel.set(0)
        this.damagePerkLevel.set(0)

        this.container.setItem(INPUT_SLOT, ItemStack.EMPTY)
        this.container.setItem(RESULT_SLOT, result)
        this.container.setChanged()
    }

    /**
     * 从Perk槽中取出对应的Perk物品时，根据其类型移除输入槽中枪械的Perk
     * 
     * @param perk Perk物品
     */
    private fun onTakePerk(perk: ItemStack) {
        val gun = this.container.getItem(INPUT_SLOT)
        if (gun.item !is GunItem) {
            return
        }

        val perkItem = perk.item
        if (perkItem is PerkItem) {
            when (perkItem.perk.type) {
                Perk.Type.AMMO -> this.ammoPerkLevel.set(0)
                Perk.Type.FUNCTIONAL -> this.funcPerkLevel.set(0)
                Perk.Type.DAMAGE -> this.damagePerkLevel.set(0)
            }

            val inputData = GunData.from(gun)
            val level = inputData.perk.getLevel(perkItem).toInt()

            if (level <= 0) return

            val output = gun.copy()
            val outputData = GunData.from(output)
            outputData.perk.remove(perkItem.perk)

            outputData.save()
            inputData.save()
            this.container.setItem(INPUT_SLOT, output)
            this.container.setChanged()
        }
    }

    /**
     * 放置perk物品时，将对应位置的level设置为1
     * 
     * @param pStack Perk物品
     */
    private fun onPlacePerk(pStack: ItemStack) {
        val perkItem = pStack.item as? PerkItem ?: return

        when (perkItem.perk.type) {
            Perk.Type.AMMO -> this.ammoPerkLevel.set(1)
            Perk.Type.FUNCTIONAL -> this.funcPerkLevel.set(1)
            Perk.Type.DAMAGE -> this.damagePerkLevel.set(1)
        }
    }

    /**
     * 将枪械放入输入槽中时，根据枪械上已有的Perk生成对应的Perk物品，并将等级调整为当前的等级
     * 
     * @param stack 输入的枪械
     */
    private fun onPlaceGun(stack: ItemStack) {
        if (stack.item !is GunItem) return
        val data = GunData.from(stack)

        for (type in Perk.Type.entries) {
            val list = data.perk.getInstances(type)
            if (!list.isEmpty()) {
                val perkInstance = list[0]
                when (type) {
                    Perk.Type.AMMO -> this.ammoPerkLevel.set(perkInstance.level.toInt())
                    Perk.Type.FUNCTIONAL -> this.funcPerkLevel.set(perkInstance.level.toInt())
                    Perk.Type.DAMAGE -> this.damagePerkLevel.set(perkInstance.level.toInt())
                }

                val ammoPerkItem = perkInstance.perk.getItem().get()
                this.container.setItem(
                    when (type) {
                        Perk.Type.AMMO -> AMMO_PERK_SLOT
                        Perk.Type.FUNCTIONAL -> FUNC_PERK_SLOT
                        Perk.Type.DAMAGE -> DAMAGE_PERK_SLOT
                    }, ammoPerkItem.defaultInstance
                )
            }
        }

        this.container.setChanged()
        this.broadcastChanges()
    }

    /**
     * 拿走输入槽中的枪械时，如果Perk槽中存在放入枪械时生成的Perk物品，则将其移除，如果是没有的Perk则无视
     * 
     * @param stack 输入的枪械
     */
    private fun onTakeGun(stack: ItemStack) {
        if (stack.item !is GunItem) return
        val data = GunData.from(stack)

        for (type in Perk.Type.entries) {
            val perk: Perk? = data.perk.get(type)
            val slot: Int = when (type) {
                Perk.Type.AMMO -> AMMO_PERK_SLOT
                Perk.Type.FUNCTIONAL -> FUNC_PERK_SLOT
                Perk.Type.DAMAGE -> DAMAGE_PERK_SLOT
            }

            val perkItem = this.container.getItem(slot).item
            if (perk != null && perkItem is PerkItem && perkItem.perk == perk) {
                this.container.setItem(slot, ItemStack.EMPTY)
            }
        }

        this.ammoPerkLevel.set(0)
        this.funcPerkLevel.set(0)
        this.damagePerkLevel.set(0)

        val ammo = this.container.getItem(AMMO_PERK_SLOT)
        if (ammo != ItemStack.EMPTY) {
            this.moveItemStackTo(ammo, RESULT_SLOT + 1, RESULT_SLOT + 37, false)
        }

        val func = this.container.getItem(FUNC_PERK_SLOT)
        if (func != ItemStack.EMPTY) {
            this.moveItemStackTo(func, RESULT_SLOT + 1, RESULT_SLOT + 37, false)
        }

        val damage = this.container.getItem(DAMAGE_PERK_SLOT)
        if (damage != ItemStack.EMPTY) {
            this.moveItemStackTo(damage, RESULT_SLOT + 1, RESULT_SLOT + 37, false)
        }

        this.container.setChanged()
    }

    fun getPerkItemBySlot(type: Perk.Type): ItemStack? {
        return when (type) {
            Perk.Type.AMMO -> this.container.getItem(AMMO_PERK_SLOT)
            Perk.Type.FUNCTIONAL -> this.container.getItem(FUNC_PERK_SLOT)
            Perk.Type.DAMAGE -> this.container.getItem(DAMAGE_PERK_SLOT)
        }
    }

    internal inner class InputSlot(pContainer: Container, pSlot: Int, pX: Int, pY: Int) :
        Slot(pContainer, pSlot, pX, pY) {
        override fun mayPlace(pStack: ItemStack): Boolean {
            if (pStack.item is GunItem) {
                val ammoPerk = this.container.getItem(AMMO_PERK_SLOT)
                val funcPerk = this.container.getItem(FUNC_PERK_SLOT)
                val damagePerk = this.container.getItem(DAMAGE_PERK_SLOT)

                val flag1 = ammoPerk.isEmpty
                val flag2 = funcPerk.isEmpty
                val flag3 = damagePerk.isEmpty

                return flag1 && flag2 && flag3
                        && this.container.getItem(RESULT_SLOT).isEmpty
                        && this.container.getItem(INPUT_SLOT).isEmpty
            }
            return false
        }

        override fun getMaxStackSize(): Int {
            return 1
        }

        override fun onTake(pPlayer: Player, pStack: ItemStack) {
            super.onTake(pPlayer, pStack)
            onTakeGun(pStack)
        }

        override fun setByPlayer(pStack: ItemStack) {
            onPlaceGun(pStack)
            super.setByPlayer(pStack)
        }
    }

    internal inner class PerkSlot(pContainer: Container, pSlot: Int, var type: Perk.Type, pX: Int, pY: Int) :
        Slot(pContainer, pSlot, pX, pY) {
        override fun mayPlace(pStack: ItemStack): Boolean {
            val slot = when (type) {
                Perk.Type.AMMO -> AMMO_PERK_SLOT
                Perk.Type.FUNCTIONAL -> FUNC_PERK_SLOT
                Perk.Type.DAMAGE -> DAMAGE_PERK_SLOT
            }

            val perkItem = pStack.item as? PerkItem ?: return false

            return perkItem.perk.type == type && !container.getItem(INPUT_SLOT).isEmpty
                    && container.getItem(INPUT_SLOT).item is GunItem
                    && GunData.from(container.getItem(INPUT_SLOT)).canApplyPerk(perkItem.perk)
                    && container.getItem(slot).isEmpty
        }

        override fun getMaxStackSize(): Int {
            return 1
        }

        @ParametersAreNonnullByDefault
        override fun onTake(pPlayer: Player, pStack: ItemStack) {
            onTakePerk(pStack)
            super.onTake(pPlayer, pStack)
        }

        override fun setByPlayer(pStack: ItemStack) {
            onPlacePerk(pStack)
            super.setByPlayer(pStack)
        }
    }

    internal class ResultSlot(pContainer: Container, pSlot: Int, pX: Int, pY: Int) : Slot(pContainer, pSlot, pX, pY) {
        override fun mayPlace(pStack: ItemStack): Boolean {
            return false
        }

        override fun getMaxStackSize(): Int {
            return 1
        }
    }

    companion object {
        const val INPUT_SLOT: Int = 0
        const val AMMO_PERK_SLOT: Int = 1
        const val FUNC_PERK_SLOT: Int = 2
        const val DAMAGE_PERK_SLOT: Int = 3
        const val RESULT_SLOT: Int = 4

        const val MAX_PERK_LEVEL: Int = 20
        const val MAX_UPGRADE_POINT: Int = 100

        const val X_OFFSET: Int = 0
        const val Y_OFFSET: Int = 11
    }
}