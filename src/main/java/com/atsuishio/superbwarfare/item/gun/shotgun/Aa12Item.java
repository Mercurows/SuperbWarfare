package com.atsuishio.superbwarfare.item.gun.shotgun;

import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.model.item.Aa12ItemModel;
import com.atsuishio.superbwarfare.init.ModRarities;
import com.atsuishio.superbwarfare.init.RegistryName;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

@RegistryName("aa_12")
public class Aa12Item extends GunGeoItem {

    public Aa12Item() {
        super(new Properties().rarity(ModRarities.LEGENDARY));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(Aa12ItemModel::new);
    }

}
