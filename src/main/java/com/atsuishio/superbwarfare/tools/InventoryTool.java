package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.Ammo;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.ammo.AmmoBoxItem;
import com.atsuishio.superbwarfare.item.ammo.AmmoSupplierItem;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
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

    public static int countAmmoItem(@Nullable IItemHandler handler, @Nullable Ammo type) {
        if (handler == null || type == null) return 0;

        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            var stack = handler.getStackInSlot(i);

            // AmmoSupplier Item
            if (stack.getItem() instanceof AmmoSupplierItem ammoSupplierItem && ammoSupplierItem.getType() == type) {
                count += ammoSupplierItem.getAmmoToAdd() * stack.getCount();
            }

            // AmmoBox
            if (stack.getItem() instanceof AmmoBoxItem) {
                var stackAmmo = type.get(stack);
                if (stackAmmo > 0) {
                    count += stackAmmo;
                }
            }
        }

        return count;
    }

    public static int countAmmoItem(@Nullable Entity entity, @Nullable Ammo type) {
        if (entity == null || type == null) return 0;
        var cap = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        if (cap == null) return 0;

        return countAmmoItem(cap, type);
    }

    public static int consumeAmmoItem(@Nullable Entity entity, @Nullable Ammo type, int count) {
        if (entity == null || type == null || count <= 0) return 0;
        var cap = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        if (cap == null) return 0;

        return consumeAmmoItem(cap, type, count);
    }

    public static int consumeAmmoItem(@Nullable IItemHandler handler, @Nullable Ammo type, int count) {
        if (handler == null || type == null) return 0;

        int initialCount = count;
        for (int i = 0; i < handler.getSlots(); i++) {
            var stack = handler.getStackInSlot(i);

            // AmmoBox
            if (stack.getItem() instanceof AmmoBoxItem) {
                var stackAmmo = type.get(stack);
                if (stackAmmo > 0) {
                    var maxConsumable = Math.min(stackAmmo, count);
                    type.set(stack, stackAmmo - maxConsumable);
                    count -= maxConsumable;
                    continue;
                }
            }

            // AmmoSupplier Item
            if (!(stack.getItem() instanceof AmmoSupplierItem ammoSupplierItem && ammoSupplierItem.getType() == type))
                continue;

            var supplyCount = ammoSupplierItem.getAmmoToAdd();
            var required = (count % supplyCount == 0) ? count / supplyCount : count / supplyCount + 1;

            var countToShrink = Math.min(stack.getCount(), required);
            handler.extractItem(i, countToShrink, false);
//            stack.shrink(countToShrink);
            count -= countToShrink * supplyCount;
            if (count <= 0) break;
        }

        return initialCount - count;
    }

    /**
     * 判断实体物品栏内是否有指定物品
     *
     * @param entity 实体
     * @param item   物品类型
     */
    public static boolean hasItem(@Nullable Entity entity, @NotNull Item item) {
        return !findFirst(entity, item).isEmpty();
    }

    /**
     * 判断物品列表内是否有指定物品
     *
     * @param itemList 物品列表
     * @param item     物品类型
     */
    public static boolean hasItem(@Nullable NonNullList<ItemStack> itemList, @NotNull Item item) {
        return !findFirst(itemList, item).isEmpty();
    }

    public static ItemStack findFirst(@Nullable Entity entity, @NotNull Item item) {
        if (entity == null) return ItemStack.EMPTY;

        return findFirst(entity.getCapability(Capabilities.ItemHandler.ENTITY), stack -> stack.is(item));
    }

    public static ItemStack findFirst(@Nullable IItemHandler handler, @NotNull Item item) {
        return findFirst(handler, stack -> stack.is(item));
    }

    public static ItemStack findFirst(@Nullable NonNullList<ItemStack> list, @NotNull Item item) {
        return findFirst(list, stack -> stack.is(item));
    }

    public static ItemStack findFirst(@Nullable NonNullList<ItemStack> list, @NotNull Predicate<ItemStack> predicate) {
        if (list == null) return ItemStack.EMPTY;

        return list.stream().filter(predicate).findFirst().orElse(ItemStack.EMPTY);
    }

    public static ItemStack findFirst(@Nullable IItemHandler handler, @NotNull Predicate<ItemStack> predicate) {
        if (handler == null) return ItemStack.EMPTY;

        for (int i = 0; i < handler.getSlots(); i++) {
            var stack = handler.getStackInSlot(i);
            if (predicate.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }


    public static boolean hasCreativeAmmoBox(@Nullable IItemHandler handler) {
        return !findFirst(handler, ModItems.CREATIVE_AMMO_BOX.get()).isEmpty();
    }

    /**
     * 判断物品列表内是否有创造模式弹药盒
     *
     * @param itemList 物品列表
     */
    public static boolean hasCreativeAmmoBox(@Nullable NonNullList<ItemStack> itemList) {
        return !findFirst(itemList, ModItems.CREATIVE_AMMO_BOX.get()).isEmpty();
    }

    /**
     * 判断实体物品栏内是否有创造模式弹药盒
     *
     * @param entity 实体
     */
    public static boolean hasCreativeAmmoBox(@Nullable Entity entity) {
        if (entity instanceof VehicleEntity vehicle) {
            return hasCreativeAmmoBoxForVehicle(vehicle);
        } else {
            return hasItem(entity, ModItems.CREATIVE_AMMO_BOX.get());
        }
    }

    public static boolean hasCreativeAmmoBoxForVehicle(@NotNull VehicleEntity vehicle) {
        var passengers = vehicle.getPassengers();
        boolean flag = passengers.stream().anyMatch(e -> InventoryTool.hasItem(e, ModItems.CREATIVE_AMMO_BOX.get())) && vehicle.data().compute().usePassengerCreativeAmmoBox;
        return flag || hasItem(vehicle, ModItems.CREATIVE_AMMO_BOX.get());
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

    /**
     * 消耗生物物品列表内指定物品
     *
     * @param living 物品类型
     * @param item   物品类型
     * @param count  要消耗的数量
     */
    public static void consumeItem(LivingEntity living, Item item, int count) {
        var cap = living.getCapability(Capabilities.ItemHandler.ENTITY);
        if (cap == null) return;

        consumeItem(cap, item, count);
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
//            stack.shrink(countToShrink);
            handler.extractItem(i, countToShrink, false);
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

            if (MinecraftUtil.isSameItemStack(stack, currentStack) && currentStack.getCount() < maxStackSize) {
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

    public static int insertItem(IItemHandler handler, ItemStack stack, int count) {
        int inserted = 0;
        while (count > 0) {
            var limit = stack.getMaxStackSize();
            var toInsert = java.lang.Math.min(limit, count);
            var result = ItemHandlerHelper.insertItemStacked(handler, stack.copyWithCount(toInsert), false);

            count -= toInsert - result.getCount();
            inserted += toInsert - result.getCount();

            if (!result.isEmpty()) {
                Mod.LOGGER.warn("trying to withdraw ammo {} with count {}, but only {} is inserted", stack, count, inserted);
                break;
            }
        }

        return inserted;
    }

    public static int insertItem(Player player, ItemStack stack, int count) {
        int inserted = 0;
        while (count > 0) {
            var limit = stack.getMaxStackSize();
            var toInsert = java.lang.Math.min(limit, count);
            ItemHandlerHelper.giveItemToPlayer(player, stack.copyWithCount(toInsert));
            count -= toInsert;
            inserted += toInsert;
        }

        return inserted;
    }
}
