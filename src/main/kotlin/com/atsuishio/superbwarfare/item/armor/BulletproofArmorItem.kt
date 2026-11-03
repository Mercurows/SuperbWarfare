package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModAttributes
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.Consumer

abstract class BulletproofArmorItem(
    material: DeferredHolder<ArmorMaterial, ArmorMaterial>,
    type: Type,
    private val bulletResistance: Double,
) : ArmorItem(material, type, Properties()) {
    override fun <T : LivingEntity?> damageItem(
        stack: ItemStack,
        amount: Int,
        entity: T?,
        onBroken: Consumer<Item>
    ): Int {
        if (!MiscConfig.MOD_ARMOR_UNBREAKABLE.get()) return super.damageItem(stack, amount, entity, onBroken)
        val maxDamage = stack.maxDamage - 1
        if (stack.damageValue > maxDamage) stack.damageValue = maxDamage
        return amount.coerceIn(0, maxDamage - stack.damageValue)
    }

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val modifiers = super.getDefaultAttributeModifiers(stack)
        val list = ArrayList<ItemAttributeModifiers.Entry>(modifiers.modifiers())

        if (MiscConfig.MOD_ARMOR_UNBREAKABLE.get() && stack.damageValue >= stack.maxDamage - 1) {
            return modifiers
        }

        list.add(
            ItemAttributeModifiers.Entry(
                ModAttributes.BULLET_RESISTANCE, AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    bulletResistance * (1 - stack.damageValue.toDouble() / stack.maxDamage),
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.bySlot(this.type.slot)
            )
        )

        return ItemAttributeModifiers(list, true)

    }
}
