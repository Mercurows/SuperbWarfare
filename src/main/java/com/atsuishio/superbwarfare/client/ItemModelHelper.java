package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.subdata.Attachment;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class ItemModelHelper {

    /**
     * 旧 GeckoLib 枪模型里"枪口配件位"的骨骼前缀（`Barrel1` / `Barrel2` 这类）。
     *
     * **刻意不跟着 [AttachmentType.MUZZLE] 走**：这个前缀是**模型资产**里的骨骼名，
     * 写在 `geo/*.geo.json`（vector / ql_1031 / m_98b / trachelium）里，改名就得连着改模型，
     * 漏一处就是静默不渲染且不报错。GeckoLib 这条渲染路径**以后要删除**，不值得为它动模型，
     * 所以这里把前缀写成常量，与槽位标识解耦：槽位叫 muzzle，旧模型骨骼仍叫 Barrel。
     */
    private static final String LEGACY_MUZZLE_BONE_PREFIX = "Barrel";

    public static void handleGunAttachments(GeoBone bone, ItemStack stack, String name) {
        var attachments = GunData.from(stack).attachment;

        splitBoneName(bone, name, attachments, AttachmentType.SCOPE);
        splitBoneName(bone, name, attachments, AttachmentType.MAGAZINE);
        splitBoneName(bone, name, attachments, AttachmentType.MUZZLE, LEGACY_MUZZLE_BONE_PREFIX);
        splitBoneName(bone, name, attachments, AttachmentType.STOCK);
        splitBoneName(bone, name, attachments, AttachmentType.GRIP);
        splitBoneName(bone, name, GunData.from(stack).selectedAmmoType.get());
    }

    private static void splitBoneName(GeoBone bone, String boneName, Attachment attachment, AttachmentType type) {
        splitBoneName(bone, boneName, attachment, type, type.getAttachmentName());
    }

    private static void splitBoneName(GeoBone bone, String boneName, Attachment attachment, AttachmentType type, String bonePrefix) {
        try {
            if (boneName.startsWith(bonePrefix)) {
                String[] parts = boneName.split("(?<=\\D)(?=\\d)");
                if (parts.length == 2) {
                    int index = Integer.parseInt(parts[1]);
                    bone.setHidden(attachment.get(type) != index);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static void splitBoneName(GeoBone bone, String boneName, int ammoType) {
        try {
            if (boneName.startsWith("AmmoType")) {
                String[] parts = boneName.split("(?<=\\D)(?=\\d)");
                if (parts.length == 2) {
                    int index = Integer.parseInt(parts[1]);
                    bone.setHidden(ammoType != index);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public static void hideAllAttachments(GeoBone bone, String name) {
        splitAndHideBone(bone, name, "Scope");
        splitAndHideBone(bone, name, "Magazine");
        // 同上：旧模型里枪口配件位那颗骨骼叫 `Barrel`，不是 `Muzzle`
        splitAndHideBone(bone, name, LEGACY_MUZZLE_BONE_PREFIX);
        splitAndHideBone(bone, name, "Stock");
        splitAndHideBone(bone, name, "Grip");
        splitAndHideBoneAmmoType(bone, name);
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

    private static void splitAndHideBoneAmmoType(GeoBone bone, String boneName) {
        try {
            if (boneName.startsWith("AmmoType")) {
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
