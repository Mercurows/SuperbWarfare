package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;

import java.util.function.Predicate;

public class InventoryTool {

    public static int countItem(@Nullable IItemHandler handler, @NotNull Item item) {
        return countItem(handler, stack -> stack.is(item));
    }

    public static int countItem(@Nullable IItemHandler handler, @NotNull TagKey<Item> item) {
        return countItem(handler, stack -> stack.is(item));
    }

    public static int countItem(@Nullable IItemHandler handler, @NotNull Predicate<ItemStack> predicate) {
        if (handler == null) return 0;

        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            var stack = handler.getStackInSlot(i);
            if (predicate.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }


    /**
     * 计算物品列表内指定物品的数量
     *
     * @param itemList 物品列表
     * @param item     物品类型
     */
    public static int countItem(@Nullable NonNullList<ItemStack> itemList, @NotNull Item item) {
        if (itemList == null) return 0;

        return itemList.stream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    /**
     * 计算实体物品栏内指定物品的数量
     *
     * @param entity 实体
     * @param item   物品类型
     */
    public static int countItem(@Nullable Entity entity, @NotNull Item item) {
        if (entity == null) return 0;
        var cap = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        if (cap == null) return 0;

        return countItem(cap, item);
    }

    /**
     * 判断实体物品栏内是否有指定物品
     *
     * @param entity 实体
     * @param item   物品类型
     */
    public static boolean hasItem(@Nullable Entity entity, @NotNull Item item) {
        return countItem(entity, item) > 0;
    }

    /**
     * 判断物品列表内是否有指定物品
     *
     * @param itemList 物品列表
     * @param item     物品类型
     */
    public static boolean hasItem(@Nullable NonNullList<ItemStack> itemList, @NotNull Item item) {
        return countItem(itemList, item) > 0;
    }

    public static boolean hasCreativeAmmoBox(@Nullable IItemHandler handler) {
        return countItem(handler, ModItems.CREATIVE_AMMO_BOX.get()) > 0;
    }

    /**
     * 判断物品列表内是否有创造模式弹药盒
     *
     * @param itemList 物品列表
     */
    public static boolean hasCreativeAmmoBox(@Nullable NonNullList<ItemStack> itemList) {
        return countItem(itemList, ModItems.CREATIVE_AMMO_BOX.get()) > 0;
    }

    /**
     * 判断实体物品栏内是否有创造模式弹药盒
     *
     * @param entity 实体
     */
    public static boolean hasCreativeAmmoBox(@Nullable Entity entity) {
        return hasItem(entity, ModItems.CREATIVE_AMMO_BOX.get());
    }

    /**
     * 消耗物品列表内指定物品
     *
     * @param item  物品类型
     * @param count 要消耗的数量
     * @return 成功消耗的物品数量
     */
    public static int consumeItem(@Nullable NonNullList<ItemStack> itemList, Item item, int count) {
        return consumeItem(itemList, stack -> stack.is(item), count);
    }


    public static int consumeItem(@Nullable NonNullList<ItemStack> itemList, Predicate<ItemStack> predicate, int count) {
        if (itemList == null || count <= 0) return 0;

        int initialCount = count;
        var items = itemList.stream().filter(predicate).toList();
        for (var stack : items) {
            var countToShrink = Math.min(stack.getCount(), count);
            stack.shrink(countToShrink);
            count -= countToShrink;
            if (count <= 0) break;
        }
        return initialCount - count;
    }

    public static int consumeItem(@Nullable IItemHandler handler, Item item, int count) {
        return consumeItem(handler, stack -> stack.is(item), count);
    }

    public static int consumeItem(@Nullable IItemHandler handler, Predicate<ItemStack> predicate, int count) {
        if (handler == null || count <= 0) return 0;
        int initialCount = count;

        for (int i = 0; i < handler.getSlots(); i++) {
            var stack = handler.getStackInSlot(i);
            if (!predicate.test(stack)) continue;

            var countToShrink = Math.min(stack.getCount(), count);
            stack.shrink(countToShrink);
            count -= countToShrink;
            if (count <= 0) break;
        }

        return initialCount - count;
    }

    /**
     * 尝试插入指定物品指定数量
     *
     * @param item  物品类型
     * @param count 要插入的数量
     * @return 未能成功插入的物品数量
     */
    public static int insertItem(@Nullable NonNullList<ItemStack> itemList, Item item, int count, int maxStackSize) {
        if (itemList == null || count <= 0) return count;

        var defaultStack = new ItemStack(item);
        maxStackSize = Math.min(maxStackSize, item.getMaxStackSize(defaultStack));

        for (int i = 0; i < itemList.size(); i++) {
            var stack = itemList.get(i);

            if (stack.is(item) && stack.getCount() < maxStackSize) {
                var countToAdd = Math.min(maxStackSize - stack.getCount(), count);
                stack.grow(countToAdd);
                count -= countToAdd;
            } else if (stack.isEmpty()) {
                var countToAdd = Math.min(maxStackSize, count);
                itemList.set(i, new ItemStack(item, countToAdd));
                count -= countToAdd;
            }

            if (count <= 0) break;
        }

        return count;
    }

    public static int insertItem(@Nullable NonNullList<ItemStack> itemList, ItemStack stack) {
        if (itemList == null) return stack.getCount();

        var maxStackSize = stack.getItem().getMaxStackSize(stack);
        var originalCount = stack.getCount();

        for (int i = 0; i < itemList.size(); i++) {
            var currentStack = itemList.get(i);

            if (ItemStack.isSameItemSameComponents(stack, currentStack) && currentStack.getCount() < maxStackSize) {
                var countToAdd = Math.min(maxStackSize - currentStack.getCount(), stack.getCount());
                currentStack.grow(countToAdd);
                stack.setCount(stack.getCount() - countToAdd);
            } else if (currentStack.isEmpty()) {
                itemList.set(i, stack);
                return stack.getCount();
            }

            if (stack.getCount() <= 0) break;
        }

        return originalCount - stack.getCount();
    }
}
