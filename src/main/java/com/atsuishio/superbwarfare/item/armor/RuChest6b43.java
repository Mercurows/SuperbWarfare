package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.RuChest6b43ArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;

public class RuChest6b43 extends BulletResistantArmor {
    public RuChest6b43() {
        super(ModArmorMaterials.CEMENTED_CARBIDE,
                Type.CHESTPLATE,
                new Properties().durability(Type.CHESTPLATE.getDurability(50)),
                0.5f,
                RuChest6b43ArmorRenderer::new
        );
    }
}
