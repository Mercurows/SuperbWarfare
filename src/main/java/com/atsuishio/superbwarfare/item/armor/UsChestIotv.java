package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.UsChestIotvArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import java.util.function.Supplier;

public class UsChestIotv extends BulletResistantArmor {

    public UsChestIotv() {
        super(ModArmorMaterials.CEMENTED_CARBIDE,
                Type.CHESTPLATE,
                new Properties().durability(Type.CHESTPLATE.getDurability(50)),
                0.5f
        );
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Supplier<GeoArmorRenderer<? extends Item>> getRenderer() {
        return UsChestIotvArmorRenderer::new;
    }
}
