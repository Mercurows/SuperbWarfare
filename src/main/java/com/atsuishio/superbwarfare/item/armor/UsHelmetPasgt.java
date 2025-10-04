package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.UsHelmetPasgtArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import java.util.function.Supplier;

public class UsHelmetPasgt extends BulletResistantArmor {

    public UsHelmetPasgt() {
        super(ModArmorMaterials.CEMENTED_CARBIDE,
                Type.HELMET,
                new Properties().durability(Type.HELMET.getDurability(50)),
                0.2f
        );
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Supplier<GeoArmorRenderer<? extends Item>> getRenderer() {
        return UsHelmetPasgtArmorRenderer::new;
    }
}
