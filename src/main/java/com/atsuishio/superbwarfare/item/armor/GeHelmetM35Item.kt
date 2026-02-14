package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.client.renderer.armor.GeHelmetM35ArmorRenderer
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import software.bernie.geckolib.renderer.GeoArmorRenderer
import java.util.function.Supplier

class GeHelmetM35Item : BulletResistantArmor(
    ModArmorMaterial.STEEL,
    Type.HELMET,
    Properties().durability(Type.HELMET.getDurability(35))
) {
    override fun getRenderer(): Supplier<GeoArmorRenderer<*>> {
        return Supplier { GeHelmetM35ArmorRenderer() }
    }
}
