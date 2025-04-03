package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.RuHelmet6b47ArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;

public class RuHelmet6b47 extends BulletResistantArmor {
    public RuHelmet6b47() {
        super(ModArmorMaterials.CEMENTED_CARBIDE,
                Type.HELMET,
                new Properties().durability(Type.HELMET.getDurability(50)),
                0.2f,
                RuHelmet6b47ArmorRenderer::new
        );
    }
}
