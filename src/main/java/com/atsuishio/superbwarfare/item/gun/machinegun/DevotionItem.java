package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.client.renderer.gun.DevotionItemRenderer;
import com.atsuishio.superbwarfare.init.RegistryName;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

@RegistryName("devotion")
public class DevotionItem extends GunGeoItem {

    public DevotionItem() {
        super(new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return DevotionItemRenderer::new;
    }

}
