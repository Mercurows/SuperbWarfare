package com.atsuishio.superbwarfare.item.gun

import com.atsuishio.superbwarfare.client.PoseTool
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer
import net.minecraft.client.model.HumanoidModel
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

// TODO 替换掉之前的GunGeoItem，给这个的V2去掉
open class GeoGunItemV2(properties: Properties) : GunItem(properties) {

    @OnlyIn(Dist.CLIENT)
    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private val renderer by lazy { GeoGunRenderer() }

            override fun getCustomRenderer() = renderer

            override fun getArmPose(
                entityLiving: LivingEntity,
                hand: InteractionHand,
                itemStack: ItemStack
            ): HumanoidModel.ArmPose {
                return armPose(entityLiving, hand, itemStack)
            }
        })
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