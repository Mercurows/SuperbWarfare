package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.model.item.HuntingRifleItemModel;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class HuntingRifleItem extends GunGeoItem {

    public HuntingRifleItem() {
        super(new Item.Properties().rarity(Rarity.RARE));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(HuntingRifleItemModel::new);
    }
}