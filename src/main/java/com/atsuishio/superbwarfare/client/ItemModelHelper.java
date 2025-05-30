package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.data.gun.GunData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class ItemModelHelper {

    public static void handleGunAttachments(GeoBone bone, ItemStack stack, String name) {
        CompoundTag tag = GunData.from(stack).attachment();

        splitBoneName(bone, name, "Scope", tag);
        splitBoneName(bone, name, "Magazine", tag);
        splitBoneName(bone, name, "Barrel", tag);
        splitBoneName(bone, name, "Stock", tag);
        splitBoneName(bone, name, "Grip", tag);
    }

    private static void splitBoneName(GeoBone bone, String boneName, String tagName, CompoundTag tag) {
        try {
            if (boneName.startsWith(tagName)) {
                String[] parts = boneName.split("(?<=\\D)(?=\\d)");
                if (parts.length == 2) {
                    int index = Integer.parseInt(parts[1]);
                    bone.setHidden(tag.getInt(tagName) != index);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public static void hideAllAttachments(GeoBone bone, String name) {
        splitAndHideBone(bone, name, "Scope");
        splitAndHideBone(bone, name, "Magazine");
        splitAndHideBone(bone, name, "Barrel");
        splitAndHideBone(bone, name, "Stock");
        splitAndHideBone(bone, name, "Grip");
    }

    private static void splitAndHideBone(GeoBone bone, String boneName, String tagName) {
        try {
            if (boneName.startsWith(tagName)) {
                String[] parts = boneName.split("(?<=\\D)(?=\\d)");
                if (parts.length == 2) {
                    int index = Integer.parseInt(parts[1]);
                    bone.setHidden(index != 0);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
