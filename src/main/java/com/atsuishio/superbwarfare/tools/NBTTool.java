package com.atsuishio.superbwarfare.tools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

// From Botania
public final class NBTTool {
    public static boolean verifyExistence(ItemStack stack, String key) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty() && data != null && data.contains(key);
    }

    public static CompoundTag getTag(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) return data.copyTag();

        return new CompoundTag();
    }

    public static void setBoolean(ItemStack stack, String key, boolean b) {
        var tag = getTag(stack);
        tag.putBoolean(key, b);
        saveTag(stack, tag);
    }

    public static boolean getBoolean(ItemStack stack, String key, boolean defaultExpected) {
        return verifyExistence(stack, key) ? getTag(stack).getBoolean(key) : defaultExpected;
    }

    public static void setFloat(ItemStack stack, String key, float f) {
        var tag = getTag(stack);
        tag.putFloat(key, f);
        saveTag(stack, tag);
    }

    public static float getFloat(ItemStack stack, String key, float f) {
        return verifyExistence(stack, key) ? getTag(stack).getFloat(key) : f;
    }

    public static void setInt(ItemStack stack, String key, int num) {
        var tag = getTag(stack);
        tag.putInt(key, num);
        saveTag(stack, tag);
    }

    public static int getInt(ItemStack stack, String key, int num) {
        return verifyExistence(stack, key) ? getTag(stack).getInt(key) : num;
    }

    public static void setLong(ItemStack stack, String key, long num) {
        var tag = getTag(stack);
        tag.putLong(key, num);
        saveTag(stack, tag);
    }

    public static long getLong(ItemStack stack, String key, long num) {
        return verifyExistence(stack, key) ? getTag(stack).getLong(key) : num;
    }

    public static void setDouble(ItemStack stack, String key, double num) {
        getTag(stack).putDouble(key, num);
    }

    public static double getDouble(ItemStack stack, String key, double num) {
        return verifyExistence(stack, key) ? getTag(stack).getDouble(key) : num;
    }

    public static void saveTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}