package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.model.item.M1911ItemModel;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class M1911Item extends GunGeoItem {

    public M1911Item() {
        super(new Properties().rarity(Rarity.COMMON));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(M1911ItemModel::new);
    }

}
