package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

// From Botania
public final class NBTTool {
    public static boolean verifyExistence(ItemStack stack, String tag) {
        var data = stack.get(ModDataComponents.GUN_DATA);
        return !stack.isEmpty() && data != null && data.contains(tag);
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        var data = stack.get(ModDataComponents.GUN_DATA);
        if (data != null) return data;

        var newTag = new CompoundTag();
        stack.set(ModDataComponents.GUN_DATA, newTag);
        return newTag;
    }

    public static void setBoolean(ItemStack stack, String tag, boolean b) {
        getOrCreateTag(stack).putBoolean(tag, b);
    }

    public static boolean getBoolean(ItemStack stack, String tag, boolean defaultExpected) {
        return verifyExistence(stack, tag) ? getOrCreateTag(stack).getBoolean(tag) : defaultExpected;
    }

    public static void setFloat(ItemStack stack, String tag, float f) {
        getOrCreateTag(stack).putFloat(tag, f);
    }

    public static float getFloat(ItemStack stack, String tag, float f) {
        return verifyExistence(stack, tag) ? getOrCreateTag(stack).getFloat(tag) : f;
    }

    public static void setInt(ItemStack stack, String tag, int num) {
        getOrCreateTag(stack).putInt(tag, num);
    }

    public static int getInt(ItemStack stack, String tag, int num) {
        return verifyExistence(stack, tag) ? getOrCreateTag(stack).getInt(tag) : num;
    }

    public static void setLong(ItemStack stack, String tag, long num) {
        getOrCreateTag(stack).putLong(tag, num);
    }

    public static long getLong(ItemStack stack, String tag, long num) {
        return verifyExistence(stack, tag) ? getOrCreateTag(stack).getLong(tag) : num;
    }

    public static void setDouble(ItemStack stack, String tag, double num) {
        getOrCreateTag(stack).putDouble(tag, num);
    }

    public static double getDouble(ItemStack stack, String tag, double num) {
        return verifyExistence(stack, tag) ? getOrCreateTag(stack).getDouble(tag) : num;
    }
}