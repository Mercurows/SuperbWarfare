package com.atsuishio.superbwarfare.item.armor;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;

public abstract class BulletResistantArmor extends ArmorItem {

    private float bulletResistance = 0.1f;

    public BulletResistantArmor(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public BulletResistantArmor(Holder<ArmorMaterial> material, Type type, Properties properties, float bulletResistance) {
        super(material, type, properties);
        this.bulletResistance = bulletResistance;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var modifiers = super.getDefaultAttributeModifiers(stack);
        var list = new ArrayList<>(modifiers.modifiers());
        list.add(new ItemAttributeModifiers.Entry(ModAttributes.BULLET_RESISTANCE, new AttributeModifier(Mod.ATTRIBUTE_MODIFIER,
                this.bulletResistance * Math.max(0, 1 - (double) stack.getDamageValue() / stack.getMaxDamage()), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.bySlot(this.type.getSlot())));
        return new ItemAttributeModifiers(list, true);
    }
}
