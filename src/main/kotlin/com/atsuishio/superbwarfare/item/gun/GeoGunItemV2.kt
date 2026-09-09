package com.atsuishio.superbwarfare.item.gun

import com.atsuishio.superbwarfare.client.PoseTool
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer
import net.minecraft.client.model.HumanoidModel
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent

// TODO 替换掉之前的GunGeoItem，给这个的V2去掉
open class GeoGunItemV2(properties: Properties) : GunItem(properties) {

    @EventBusSubscriber
    companion object {
        @SubscribeEvent
        private fun registerGunExtensions(event: RegisterClientExtensionsEvent) {
            for (item in BuiltInRegistries.ITEM.filterIsInstance<GeoGunItemV2>()) {
                event.registerItem(object : IClientItemExtensions {
                    private val renderer by lazy { GeoGunRenderer() }

                    override fun getCustomRenderer() = renderer

                    override fun getArmPose(
                        entityLiving: LivingEntity,
                        hand: InteractionHand,
                        itemStack: ItemStack
                    ) = item.armPose(entityLiving, hand, itemStack)
                }, item)
            }
        }

    }

    @OnlyIn(Dist.CLIENT)
    open fun armPose(
        entityLiving: LivingEntity,
        hand: InteractionHand,
        itemStack: ItemStack
    ): HumanoidModel.ArmPose {
        return PoseTool.pose(entityLiving, hand, itemStack)
    }
}