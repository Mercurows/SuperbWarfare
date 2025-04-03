package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.UsChestIotvArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;

public class UsChestIotv extends BulletResistantArmor {
    public UsChestIotv() {
        super(ModArmorMaterials.CEMENTED_CARBIDE,
                Type.CHESTPLATE,
                new Properties().durability(Type.CHESTPLATE.getDurability(50)),
                0.5f,
                UsChestIotvArmorRenderer::new
        );
    }
}
