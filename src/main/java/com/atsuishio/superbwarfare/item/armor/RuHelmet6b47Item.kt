package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.client.renderer.armor.RuHelmet6b47ArmorRenderer
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import software.bernie.geckolib.renderer.GeoArmorRenderer
import java.util.function.Supplier

class RuHelmet6b47Item : BulletResistantArmor(
    ModArmorMaterial.CEMENTED_CARBIDE,
    Type.HELMET,
    Properties().durability(Type.HELMET.getDurability(50)),
    0.2f
) {
    @OnlyIn(Dist.CLIENT)
    override fun getRenderer(): Supplier<GeoArmorRenderer<*>> {
        return Supplier { RuHelmet6b47ArmorRenderer() }
    }
}
