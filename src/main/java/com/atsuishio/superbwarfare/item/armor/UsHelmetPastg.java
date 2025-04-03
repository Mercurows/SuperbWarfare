package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.UsHelmetPastgArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;

public class UsHelmetPastg extends BulletResistantArmor {
    public UsHelmetPastg() {
        super(ModArmorMaterials.CEMENTED_CARBIDE,
                Type.HELMET,
                new Properties().durability(Type.HELMET.getDurability(50)),
                0.2f,
                UsHelmetPastgArmorRenderer::new
        );
    }
}
