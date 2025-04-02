package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.RuChest6b43ArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;
import com.atsuishio.superbwarfare.item.CustomRendererArmor;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RuChest6b43 extends BulletResistantArmor implements GeoItem, CustomRendererArmor {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RuChest6b43() {
        super(ModArmorMaterials.CEMENTED_CARBIDE, Type.CHESTPLATE, new Properties().durability(Type.CHESTPLATE.getDurability(50)), 0.5f);
    }

    @Override
    public GeoArmorRenderer<? extends Item> getRenderer() {
        return new RuChest6b43ArmorRenderer();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
