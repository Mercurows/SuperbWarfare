package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.client.renderer.armor.UsHelmetPastgArmorRenderer;
import com.atsuishio.superbwarfare.init.ModArmorMaterials;
import com.atsuishio.superbwarfare.item.CustomRendererArmor;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class UsHelmetPastg extends ArmorItem implements GeoItem, CustomRendererArmor {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public UsHelmetPastg() {
        super(ModArmorMaterials.CEMENTED_CARBIDE, Type.HELMET, new Properties());
    }

    @Override
    public GeoArmorRenderer<? extends Item> getRenderer() {
        return new UsHelmetPastgArmorRenderer();
    }

    // todo attribute
//    @Override
//    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
//        Multimap<Attribute, AttributeModifier> map = super.getDefaultAttributeModifiers(slot);
//        UUID uuid = new UUID(slot.toString().hashCode(), 0);
//        if (slot == EquipmentSlot.HEAD) {
//            map = HashMultimap.create(map);
//            map.put(ModAttributes.BULLET_RESISTANCE.get(), new AttributeModifier(uuid, ModUtils.ATTRIBUTE_MODIFIER,
//                    0.2 * Math.max(0, 1 - (double) stack.getDamageValue() / stack.getMaxDamage()), AttributeModifier.Operation.ADDITION));
//        }
//        return map;
//    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
