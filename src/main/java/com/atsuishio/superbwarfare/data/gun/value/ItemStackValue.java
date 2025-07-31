package com.atsuishio.superbwarfare.data.gun.value;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemStackValue {
    private final CompoundTag tag;
    private final String name;
    private final ItemStack defaultValue;

    private ItemStack cache;

    public ItemStackValue(CompoundTag tag, String name, ItemStack defaultValue) {
        this.tag = tag;
        this.name = name;
        this.defaultValue = defaultValue.copy();
        this.cache = defaultValue.copy();
    }

    public ItemStackValue(CompoundTag tag, String name) {
        this(tag, name, ItemStack.EMPTY);
    }

    public ItemStack get() {
        if (!this.cache.isEmpty()) {
            return this.cache;
        }

        if (tag.contains(name)) {
            return ItemStack.of(tag.getCompound(name));
        }
        return defaultValue;
    }

    public void set(@NotNull ItemStack value) {
        if (ItemStack.isSameItemSameTags(value, defaultValue)) {
            tag.remove(name);
        } else {
            tag.put(name, value.save(new CompoundTag()));
        }

        this.cache = value.copy();
    }

    public void reset() {
        set(defaultValue);
    }
}
