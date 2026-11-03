package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.resource.model.ArmorModelReloadListener
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.GeoArmorRendererV2
import net.minecraft.client.model.HumanoidModel
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("us_chest_iotv")
class UsChestIotvItem : BulletproofArmorItem(ModArmorMaterial.CEMENTED_CARBIDE, Type.CHESTPLATE, 0.5) {
    companion object {
        val TEXTURE = loc("textures/bedrock/armor/us_chest_iotv.png")
        val MODEL = loc("models/bedrock/armor/us_chest_iotv.geo.json")
    }

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        consumer.accept(object : IClientItemExtensions {
            private var renderer: GeoArmorRendererV2? = null

            override fun getHumanoidArmorModel(
                livingEntity: LivingEntity?,
                itemStack: ItemStack?,
                equipmentSlot: EquipmentSlot?,
                original: HumanoidModel<*>?
            ): HumanoidModel<*> {
                if (this.renderer == null) {
                    this.renderer = GeoArmorRendererV2(
                        ArmorModelReloadListener.getModel(MODEL)!!,
                        equipmentSlot!!,
                        TEXTURE
                    )
                }

                this.renderer!!.preparePose(livingEntity, itemStack, equipmentSlot, original)
                return this.renderer!!
            }
        })
    }
}
