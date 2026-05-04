package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModAttributes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.GeoArmorRenderer
import net.minecraft.client.model.HumanoidModel
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
import kotlin.math.max

class UsHelmetPasgtItem :
    ArmorItem(ModArmorMaterial.CEMENTED_CARBIDE, Type.HELMET, Properties().durability(Type.HELMET.getDurability(50))) {
    @EventBusSubscriber
    companion object {
        val TEXTURE = loc("textures/bedrock/armor/us_helmet_pasgt.png")

        @SubscribeEvent
        fun registerRender(event: RegisterClientExtensionsEvent) {
            event.registerItem(object : IClientItemExtensions {
                private var renderer: GeoArmorRenderer? = null

                override fun getHumanoidArmorModel(
                    livingEntity: LivingEntity,
                    itemStack: ItemStack,
                    equipmentSlot: EquipmentSlot,
                    original: HumanoidModel<*>
                ): HumanoidModel<*> {
                    if (this.renderer == null) {
                        this.renderer = GeoArmorRenderer(
                            BedrockModelLoader.getArmorModel(BedrockModelLoader.US_HELMET_PASGT_MODEL),
                            TEXTURE
                        )
                    }

                    this.renderer!!.preparePose(livingEntity, itemStack, equipmentSlot, original)
                    return this.renderer!!
                }
            }, ModItems.US_HELMET_PASGT)
        }
    }

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val modifiers = super.getDefaultAttributeModifiers(stack)
        val list = ArrayList<ItemAttributeModifiers.Entry>(modifiers.modifiers())
        list.add(
            ItemAttributeModifiers.Entry(
                ModAttributes.BULLET_RESISTANCE, AttributeModifier(
                    Mod.ATTRIBUTE_MODIFIER,
                    0.2 * max(0.0, 1 - stack.damageValue.toDouble() / stack.maxDamage),
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.bySlot(this.type.slot)
            )
        )
        return ItemAttributeModifiers(list, true)
    }
}