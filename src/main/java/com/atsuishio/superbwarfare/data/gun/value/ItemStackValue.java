package com.atsuishio.superbwarfare.data.gun.value;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class ItemStackValue {
    private final CompoundTag tag;
    private final String name;
    private final ItemStack defaultValue;

    public ItemStackValue(CompoundTag tag, String name, ItemStack defaultValue) {
        this.tag = tag;
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public ItemStackValue(CompoundTag tag, String name) {
        this(tag, name, ItemStack.EMPTY);
    }

    public ItemStack get() {
        if (tag.contains(name)) {
            return ItemStack.of(tag.getCompound(name));
        }
        return defaultValue;
    }

    public void set(ItemStack value) {
        if (value == defaultValue) {
            tag.remove(name);
        } else {
            tag.put(name, value.save(new CompoundTag()));
        }
    }

    public void reset() {
        set(ItemStack.EMPTY);
    }
}
