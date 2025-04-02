package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.GeHelmetM35ArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;
import com.atsuishio.superbwarfare.item.CustomRendererArmor;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GeHelmetM35 extends BulletResistantArmor implements GeoItem, CustomRendererArmor {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GeHelmetM35() {
        super(ModArmorMaterials.STEEL, Type.HELMET, new Properties().durability(Type.HELMET.getDurability(35)));
    }

    @Override
    public GeoArmorRenderer<? extends Item> getRenderer() {
        return new GeHelmetM35ArmorRenderer();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
