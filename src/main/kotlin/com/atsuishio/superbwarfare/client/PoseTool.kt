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
    // 不要删这个，纯为了加载本类
    fun init() {
    }

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

    @JvmField
    val MINI_GUN_POSE: ArmPose = ArmPose.create(
        "MiniGunItem",
        false
    ) { model: HumanoidModel<*>, entity: LivingEntity?, arm: HumanoidArm ->
        if (arm != HumanoidArm.LEFT) {
            model.rightArm.xRot = 22.5f * Mth.DEG_TO_RAD + model.head.xRot
            model.rightArm.yRot = model.head.yRot
            model.leftArm.xRot =
                Mth.clamp(-45f * Mth.DEG_TO_RAD + model.head.xRot, -67.5f * Mth.DEG_TO_RAD, 0f * Mth.DEG_TO_RAD)
            model.leftArm.yRot =
                Mth.clamp(45f * Mth.DEG_TO_RAD + model.head.yRot, 45f * Mth.DEG_TO_RAD, 80f * Mth.DEG_TO_RAD)
        }
    }
}