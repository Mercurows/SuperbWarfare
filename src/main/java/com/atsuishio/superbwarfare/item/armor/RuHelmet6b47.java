package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.RuHelmet6b47ArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;
import com.atsuishio.superbwarfare.item.CustomRendererArmor;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RuHelmet6b47 extends BulletResistantArmor implements GeoItem, CustomRendererArmor {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RuHelmet6b47() {
        super(ModArmorMaterials.CEMENTED_CARBIDE, Type.HELMET, new Properties().durability(Type.HELMET.getDurability(50)), 0.2f);
    }

    @Override
    public GeoArmorRenderer<? extends Item> getRenderer() {
        return new RuHelmet6b47ArmorRenderer();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
