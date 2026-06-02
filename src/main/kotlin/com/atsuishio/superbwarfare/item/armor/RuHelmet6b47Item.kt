package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModAttributes
import com.atsuishio.superbwarfare.resource.model.ArmorModelReloadListener
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.GeoArmorRenderer
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import net.minecraft.client.model.HumanoidModel
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.*
import java.util.function.Consumer
import kotlin.math.max

class RuHelmet6b47Item : ArmorItem(ModArmorMaterial.CEMENTED_CARBIDE, Type.HELMET, Properties()) {
    companion object {
        val TEXTURE = loc("textures/bedrock/armor/ru_helmet_6b47.png")
        val MODEL = loc("models/bedrock/armor/ru_helmet_6b47.geo.json")
    }

    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        consumer.accept(object : IClientItemExtensions {
            private var renderer: GeoArmorRenderer? = null

            override fun getHumanoidArmorModel(
                livingEntity: LivingEntity?,
                itemStack: ItemStack?,
                equipmentSlot: EquipmentSlot?,
                original: HumanoidModel<*>?
            ): HumanoidModel<*> {
                if (this.renderer == null) {
                    this.renderer = GeoArmorRenderer(
                        ArmorModelReloadListener.getModel(MODEL),
                        TEXTURE
                    )
                }

                this.renderer!!.preparePose(livingEntity, itemStack, equipmentSlot, original)
                return this.renderer!!
            }
        })
    }

    override fun getAttributeModifiers(
        slot: EquipmentSlot,
        stack: ItemStack
    ): Multimap<Attribute, AttributeModifier> {
        var map = super.getDefaultAttributeModifiers(slot)
        val uuid = UUID(slot.toString().hashCode().toLong(), 0)
        if (slot == EquipmentSlot.HEAD) {
            map = HashMultimap.create<Attribute, AttributeModifier>(map)
            map.put(
                ModAttributes.BULLET_RESISTANCE.get(), AttributeModifier(
                    uuid,
                    Mod.ATTRIBUTE_MODIFIER,
                    0.2 * max(0.0, 1 - stack.damageValue.toDouble() / stack.maxDamage),
                    AttributeModifier.Operation.ADDITION
                )
            )
        }
        return map
    }
}
