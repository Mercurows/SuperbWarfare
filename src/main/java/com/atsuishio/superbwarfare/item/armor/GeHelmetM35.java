package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.GeHelmetM35ArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Supplier;

public class GeHelmetM35 extends BulletResistantArmor implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GeHelmetM35() {
        super(ModArmorMaterials.STEEL,
                Type.HELMET,
                new Properties().durability(Type.HELMET.getDurability(35))
        );
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Supplier<GeoArmorRenderer<? extends Item>> getRenderer() {
        return GeHelmetM35ArmorRenderer::new;
    }
}
