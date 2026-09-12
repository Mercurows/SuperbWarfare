package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.HumanoidModel.ArmPose
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object PoseTool {
    @JvmStatic
    fun pose(entityLiving: LivingEntity, hand: InteractionHand?, stack: ItemStack): HumanoidModel.ArmPose {
        val data = GunData.from(stack)
        return if ((entityLiving.isSprinting && entityLiving.onGround())
            || data.reload.empty()
            || data.reload.normal()
            || data.reloading()
            || data.charging()
        ) {
            HumanoidModel.ArmPose.CROSSBOW_CHARGE
        } else {
            HumanoidModel.ArmPose.BOW_AND_ARROW
        }
    }

    @JvmField
    val SUPERBWARFARE_SUPER_STAR_SHOOTER_POSE: ArmPose = ArmPose.create(
        "SuperStarShooterItem",
        false
    ) { model: HumanoidModel<*>, entity: LivingEntity?, arm: HumanoidArm ->
        if (arm != HumanoidArm.LEFT) {
            model.rightArm.xRot = -70f * Mth.DEG_TO_RAD + model.head.xRot
            model.rightArm.yRot = 0f
            model.rightArm.zRot = 0f
            model.leftArm.xRot = -70f * Mth.DEG_TO_RAD + model.head.xRot
            model.leftArm.yRot = 0f
            model.leftArm.zRot = 0f
        }
    }
}