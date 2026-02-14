package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.client.renderer.armor.RuChest6b43ArmorRenderer
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import software.bernie.geckolib.renderer.GeoArmorRenderer
import java.util.function.Supplier

class RuChest6b43Item : BulletResistantArmor(
    ModArmorMaterial.CEMENTED_CARBIDE,
    Type.CHESTPLATE,
    Properties().durability(Type.CHESTPLATE.getDurability(50)),
    0.5f
) {
    @OnlyIn(Dist.CLIENT)
    override fun getRenderer(): Supplier<GeoArmorRenderer<*>> {
        return Supplier { RuChest6b43ArmorRenderer() }
    }
}
