package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.GeoArmorRenderer
import net.minecraft.client.model.HumanoidModel
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent

class UsChestIotvItem : ArmorItem(ModArmorMaterial.CEMENTED_CARBIDE, Type.CHESTPLATE, Properties()) {

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    companion object {
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
                            BedrockModelLoader.usChestIotvModel,
                            BedrockModelLoader.US_CHEST_IOTV_TEXTURE
                        )
                    }

                    this.renderer!!.preparePose(livingEntity, itemStack, equipmentSlot, original)
                    return this.renderer!!
                }
            }, ModItems.US_CHEST_IOTV)
        }
    }

    // TODO attributeModifier
//    override fun getAttributeModifiers(
//        slot: EquipmentSlot,
//        stack: ItemStack
//    ): Multimap<Attribute, AttributeModifier> {
//        var map = super.getDefaultAttributeModifiers(slot)
//        val uuid = UUID(slot.toString().hashCode().toLong(), 0)
//        if (slot == EquipmentSlot.CHEST) {
//            map = HashMultimap.create<Attribute, AttributeModifier>(map)
//            map.put(
//                ModAttributes.BULLET_RESISTANCE.get(), AttributeModifier(
//                    uuid,
//                    Mod.ATTRIBUTE_MODIFIER,
//                    0.5 * max(0.0, 1 - stack.damageValue.toDouble() / stack.maxDamage),
//                    AttributeModifier.Operation.ADDITION
//                )
//            )
//        }
//        return map
//    }
}
