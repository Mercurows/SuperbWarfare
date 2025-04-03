package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttributes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BulletResistantArmor extends ArmorItem implements GeoItem {

    private final Supplier<GeoArmorRenderer<? extends Item>> renderer;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<? extends Item> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(T livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<T> original) {
                if (this.renderer == null)
                    this.renderer = BulletResistantArmor.this.renderer.get();
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    private float bulletResistance = 0.1f;

    public BulletResistantArmor(Holder<ArmorMaterial> material, Type type, Properties properties, Supplier<GeoArmorRenderer<? extends Item>> renderer) {
        super(material, type, properties);
        this.renderer = renderer;
    }

    public BulletResistantArmor(Holder<ArmorMaterial> material, Type type, Properties properties, float bulletResistance, Supplier<GeoArmorRenderer<? extends Item>> renderer) {
        super(material, type, properties);
        this.bulletResistance = bulletResistance;
        this.renderer = renderer;
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        var modifiers = super.getDefaultAttributeModifiers(stack);
        var list = new ArrayList<>(modifiers.modifiers());
        list.add(new ItemAttributeModifiers.Entry(ModAttributes.BULLET_RESISTANCE, new AttributeModifier(Mod.ATTRIBUTE_MODIFIER,
                this.bulletResistance * Math.max(0, 1 - (double) stack.getDamageValue() / stack.getMaxDamage()), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.bySlot(this.type.getSlot())));
        return new ItemAttributeModifiers(list, true);
    }
}
