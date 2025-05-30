package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.subdata.Attachment;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class ItemModelHelper {

    public static void handleGunAttachments(GeoBone bone, ItemStack stack, String name) {
        var attachments = GunData.from(stack).attachment;

        splitBoneName(bone, name, attachments, AttachmentType.SCOPE);
        splitBoneName(bone, name, attachments, AttachmentType.MAGAZINE);
        splitBoneName(bone, name, attachments, AttachmentType.BARREL);
        splitBoneName(bone, name, attachments, AttachmentType.STOCK);
        splitBoneName(bone, name, attachments, AttachmentType.GRIP);
    }

    private static void splitBoneName(GeoBone bone, String boneName, Attachment attachment, AttachmentType type) {
        try {
            if (boneName.startsWith(type.getName())) {
                String[] parts = boneName.split("(?<=\\D)(?=\\d)");
                if (parts.length == 2) {
                    int index = Integer.parseInt(parts[1]);
                    bone.setHidden(attachment.get(type) != index);
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
