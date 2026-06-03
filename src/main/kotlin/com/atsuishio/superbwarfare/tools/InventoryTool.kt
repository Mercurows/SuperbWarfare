package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.ammo.AmmoBoxItem
import com.atsuishio.superbwarfare.item.ammo.AmmoSupplierItem
import net.minecraft.core.NonNullList
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemHandlerHelper
import org.joml.Math
import java.util.function.Predicate
import kotlin.math.min

object InventoryTool {
    @JvmStatic
    fun countItem(handler: IItemHandler?, item: Item): Int {
        return countItem(handler) { it.`is`(item) }
    }

    @JvmStatic
    fun countItem(handler: IItemHandler?, item: TagKey<Item>): Int {
        return countItem(handler) { it.`is`(item) }
    }

    @JvmStatic
    fun countItem(handler: IItemHandler?, predicate: Predicate<ItemStack>): Int {
        if (handler == null) return 0

        var count = 0
        for (i in 0..<handler.slots) {
            val stack = handler.getStackInSlot(i)
            if (predicate.test(stack)) {
                count += stack.count
            }
        }
        return count
    }


    /**
     * 计算物品列表内指定物品的数量
     * 
     * @param itemList 物品列表
     * @param item     物品类型
     */
    @JvmStatic
    fun countItem(itemList: NonNullList<ItemStack>?, item: Item): Int {
        if (itemList == null) return 0

        return itemList.stream()
            .filter { it.`is`(item) }
            .mapToInt { it.count }
            .sum()
    }

    /**
     * 计算实体物品栏内指定物品的数量
     * 
     * @param entity 实体
     * @param item   物品类型
     */
    @JvmStatic
    fun countItem(entity: Entity?, item: Item): Int {
        if (entity == null) return 0

        return entity.getCapability(Capabilities.ItemHandler.ENTITY)
            ?.let { countItem(it, item) }
            ?: 0
    }

    @JvmStatic
    fun countAmmoItem(handler: IItemHandler?, type: Ammo?): Int {
        if (handler == null || type == null) return 0

        var count = 0
        for (i in 0..<handler.slots) {
            val stack = handler.getStackInSlot(i)
            val item = stack.item

            // AmmoSupplier Item
            if (item is AmmoSupplierItem && item.type == type) {
                count += item.ammoToAdd * stack.count
            }

            // AmmoBox
            if (item is AmmoBoxItem) {
                val stackAmmo = type.get(stack)
                if (stackAmmo > 0) {
                    count += stackAmmo
                }
            }
        }

        return count
    }

    @JvmStatic
    fun countAmmoItem(entity: Entity?, type: Ammo?): Int {
        if (entity == null || type == null) return 0

        return entity.getCapability(Capabilities.ItemHandler.ENTITY)
            ?.let { countAmmoItem(it, type) }
            ?: 0
    }

    @JvmStatic
    fun consumeAmmoItem(entity: Entity?, type: Ammo?, count: Int): Int {
        if (entity == null || type == null || count <= 0) return 0

        return entity.getCapability(Capabilities.ItemHandler.ENTITY)
            ?.let { consumeAmmoItem(it, type, count) }
            ?: 0
    }

    @JvmStatic
    fun consumeAmmoItem(handler: IItemHandler?, type: Ammo?, count: Int): Int {
        var count = count
        if (handler == null || type == null) return 0

        val initialCount = count
        for (i in 0..<handler.slots) {
            val stack = handler.getStackInSlot(i)
            val item = stack.item

            // AmmoBox
            if (item is AmmoBoxItem) {
                val stackAmmo = type.get(stack)
                if (stackAmmo > 0) {
                    val maxConsumable = Math.min(stackAmmo, count)
                    type.set(stack, stackAmmo - maxConsumable)
                    count -= maxConsumable
                    continue
                }
            }

            // AmmoSupplier Item
            if (!(item is AmmoSupplierItem && item.type == type)) continue

            val supplyCount = item.ammoToAdd
            val required = if (count % supplyCount == 0) count / supplyCount else count / supplyCount + 1

            val countToShrink = Math.min(stack.count, required)
            handler.extractItem(i, countToShrink, false)
            count -= countToShrink * supplyCount
            if (count <= 0) break
        }

        return initialCount - count
    }

    /**
     * 判断实体物品栏内是否有指定物品
     * 
     * @param entity 实体
     * @param item   物品类型
     */
    @JvmStatic
    fun hasItem(entity: Entity?, item: Item): Boolean {
        return !findFirst(entity, item).isEmpty()
    }

    /**
     * 判断物品列表内是否有指定物品
     * 
     * @param itemList 物品列表
     * @param item     物品类型
     */
    @JvmStatic
    fun hasItem(itemList: NonNullList<ItemStack>?, item: Item): Boolean {
        return !findFirst(itemList, item).isEmpty
    }

    @JvmStatic
    fun findFirst(entity: Entity?, item: Item): ItemStack {
        if (entity == null) return ItemStack.EMPTY

        return findFirst(
            entity.getCapability(Capabilities.ItemHandler.ENTITY)
        ) { it.`is`(item) }
    }

    @JvmStatic
    fun findFirst(handler: IItemHandler?, item: Item): ItemStack {
        return findFirst(handler) { it.`is`(item) }
    }

    @JvmStatic
    fun findFirst(list: NonNullList<ItemStack>?, item: Item): ItemStack {
        return findFirst(list) { it.`is`(item) }
    }

    @JvmStatic
    fun findFirst(list: NonNullList<ItemStack>?, predicate: Predicate<ItemStack>): ItemStack {
        if (list == null) return ItemStack.EMPTY

        return list.stream().filter(predicate).findFirst().orElseGet { ItemStack.EMPTY }
    }

    @JvmStatic
    fun findFirst(handler: IItemHandler?, predicate: Predicate<ItemStack>): ItemStack {
        if (handler == null) return ItemStack.EMPTY

        for (i in 0..<handler.slots) {
            val stack = handler.getStackInSlot(i)
            if (predicate.test(stack)) {
                return stack
            }
        }
        return ItemStack.EMPTY
    }

    @JvmStatic
    fun hasCreativeAmmoBox(handler: IItemHandler?): Boolean {
        return !findFirst(handler, ModItems.CREATIVE_AMMO_BOX.get()).isEmpty
    }

    /**
     * 判断物品列表内是否有创造模式弹药盒
     * 
     * @param itemList 物品列表
     */
    @JvmStatic
    fun hasCreativeAmmoBox(itemList: NonNullList<ItemStack>?): Boolean {
        return !findFirst(itemList, ModItems.CREATIVE_AMMO_BOX.get()).isEmpty
    }

    /**
     * 判断实体物品栏内是否有创造模式弹药盒
     * 
     * @param entity 实体
     */
    @JvmStatic
    fun hasCreativeAmmoBox(entity: Entity?): Boolean {
        return if (entity is VehicleEntity) {
            hasCreativeAmmoBoxForVehicle(entity)
        } else {
            hasItem(entity, ModItems.CREATIVE_AMMO_BOX.get())
        }
    }

    @JvmStatic
    fun hasCreativeAmmoBoxForVehicle(vehicle: VehicleEntity): Boolean {
        val passengers = vehicle.getPassengers()
        val flag = passengers.stream()
            .anyMatch { hasItem(it, ModItems.CREATIVE_AMMO_BOX.get()) }
                && vehicle.data().compute().usePassengerCreativeAmmoBox
        return flag || hasItem(vehicle, ModItems.CREATIVE_AMMO_BOX.get())
    }

    /**
     * 消耗物品列表内指定物品
     * 
     * @param item  物品类型
     * @param count 要消耗的数量
     * @return 成功消耗的物品数量
     */
    @JvmStatic
    fun consumeItem(itemList: NonNullList<ItemStack>?, item: Item, count: Int): Int {
        return consumeItem(itemList, { it.`is`(item) }, count)
    }

    /**
     * 消耗生物物品列表内指定物品
     * 
     * @param living 物品类型
     * @param item   物品类型
     * @param count  要消耗的数量
     */
    @JvmStatic
    fun consumeItem(living: LivingEntity, item: Item, count: Int) {
        living.getCapability(Capabilities.ItemHandler.ENTITY)
            ?.let { consumeItem(it, item, count) }
    }

    @JvmStatic
    fun consumeItem(itemList: NonNullList<ItemStack>?, predicate: Predicate<ItemStack>, count: Int): Int {
        var count = count
        if (itemList == null || count <= 0) return 0

        val initialCount = count
        val items = itemList.stream().filter(predicate).toList()
        for (stack in items) {
            val countToShrink = Math.min(stack.count, count)
            stack.shrink(countToShrink)
            count -= countToShrink
            if (count <= 0) break
        }
        return initialCount - count
    }

    @JvmStatic
    fun consumeItem(handler: IItemHandler?, item: Item, count: Int): Int {
        return consumeItem(handler, { it.`is`(item) }, count)
    }

    @JvmStatic
    fun consumeItem(handler: IItemHandler?, predicate: Predicate<ItemStack>, count: Int): Int {
        var count = count
        if (handler == null || count <= 0) return 0
        val initialCount = count

        for (i in 0..<handler.slots) {
            val stack = handler.getStackInSlot(i)
            if (!predicate.test(stack)) continue

            val countToShrink = Math.min(stack.count, count)
            handler.extractItem(i, countToShrink, false)
            count -= countToShrink
            if (count <= 0) break
        }

        return initialCount - count
    }

    /**
     * 尝试插入指定物品指定数量
     * 
     * @param item  物品类型
     * @param count 要插入的数量
     * @return 未能成功插入的物品数量
     */
    @JvmStatic
    fun insertItem(itemList: NonNullList<ItemStack>?, item: Item, count: Int, maxStackSize: Int): Int {
        var count = count
        var maxStackSize = maxStackSize
        if (itemList == null || count <= 0) return count

        val defaultStack = ItemStack(item)
        maxStackSize = Math.min(maxStackSize, item.getMaxStackSize(defaultStack))

        for (i in itemList.indices) {
            val stack = itemList.get(i)

            if (stack.`is`(item) && stack.count < maxStackSize) {
                val countToAdd = Math.min(maxStackSize - stack.count, count)
                stack.grow(countToAdd)
                count -= countToAdd
            } else if (stack.isEmpty) {
                val countToAdd = Math.min(maxStackSize, count)
                itemList[i] = ItemStack(item, countToAdd)
                count -= countToAdd
            }

            if (count <= 0) break
        }

        return count
    }

    @JvmStatic
    fun insertItem(itemList: NonNullList<ItemStack>?, stack: ItemStack): Int {
        if (itemList == null) return stack.count

        val maxStackSize = stack.item.getMaxStackSize(stack)
        val originalCount = stack.count

        for (i in itemList.indices) {
            val currentStack = itemList[i]

            if (isSameItemStack(stack, currentStack) && currentStack.count < maxStackSize) {
                val countToAdd = Math.min(maxStackSize - currentStack.count, stack.count)
                currentStack.grow(countToAdd)
                stack.count -= countToAdd
            } else if (currentStack.isEmpty) {
                itemList[i] = stack
                return stack.count
            }

            if (stack.count <= 0) break
        }

        return originalCount - stack.count
    }

    @JvmStatic
    fun insertItem(handler: IItemHandler?, stack: ItemStack, count: Int): Int {
        if (handler == null) return 0
        var count = count
        var inserted = 0
        while (count > 0) {
            val limit = stack.maxStackSize
            val toInsert = min(limit, count)
            val result = ItemHandlerHelper.insertItemStacked(handler, stack.copyWithCount(toInsert), false)

            count -= toInsert - result.count
            inserted += toInsert - result.count

            if (!result.isEmpty) {
                Mod.LOGGER.warn(
                    "trying to withdraw ammo {} with count {}, but only {} is inserted",
                    stack,
                    count,
                    inserted
                )
                break
            }
        }

        return inserted
    }

    @JvmStatic
    fun insertItem(player: Player?, stack: ItemStack, count: Int): Int {
        if (player == null) return 0
        var count = count
        var inserted = 0
        while (count > 0) {
            val limit = stack.maxStackSize
            val toInsert = min(limit, count)
            ItemHandlerHelper.giveItemToPlayer(player, stack.copyWithCount(toInsert))
            count -= toInsert
            inserted += toInsert
        }

        return inserted
    }
}
