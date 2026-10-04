package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.client.model.HumanoidModel
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object PoseTool {
    @JvmStatic
    fun pose(entityLiving: LivingEntity, hand: InteractionHand?, stack: ItemStack): HumanoidModel.ArmPose {
        val data = GunData.from(stack)
        return if ((entityLiving.isSprinting && entityLiving.onGround())
            || data.reload.empty()
            || data.reload.normal()
            || data.reloading()
        ) {
            HumanoidModel.ArmPose.CROSSBOW_CHARGE
        } else {
            HumanoidModel.ArmPose.BOW_AND_ARROW
        }
    }
}