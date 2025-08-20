package com.atsuishio.superbwarfare.tools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Consumer;

public final class NBTTool {
    public static CompoundTag getTag(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) return data.copyTag();

        return new CompoundTag();
    }

    /**
     * 警告：请勿使用该方法保存任何枪械NBT数据！请统一使用GunData.save()保存枪械数据
     */
    public static void saveTag(ItemStack stack, CompoundTag tag) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        var oldTag = data != null ? data.copyTag() : new CompoundTag();
        var newTag = oldTag.merge(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag));
    }

    public static ItemStack withTag(DeferredHolder<Item, ? extends Item> item, int count, Consumer<CompoundTag> setter) {
        return withTag(new ItemStack(item, count), setter);
    }

    public static ItemStack withTag(DeferredHolder<Item, ? extends Item> item, Consumer<CompoundTag> setter) {
        return withTag(item, 1, setter);
    }

    public static ItemStack withTag(ItemStack stack, Consumer<CompoundTag> setter) {
        var tag = new CompoundTag();
        setter.accept(tag);
        saveTag(stack, tag);
        return stack;
    }
}