package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.GeHelmetM35ArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;

public class GeHelmetM35 extends BulletResistantArmor {
    public GeHelmetM35() {
        super(ModArmorMaterials.STEEL,
                Type.HELMET,
                new Properties().durability(Type.HELMET.getDurability(35)),
                GeHelmetM35ArmorRenderer::new
        );
    }
}
