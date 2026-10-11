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
            model.rightArm.xRot = model.head.xRot
            model.rightArm.yRot = model.head.yRot
            model.leftArm.xRot =
                Mth.clamp(-45f * Mth.DEG_TO_RAD + model.head.xRot, -67.5f * Mth.DEG_TO_RAD, 0f * Mth.DEG_TO_RAD)
            model.leftArm.yRot =
                Mth.clamp(45f * Mth.DEG_TO_RAD + model.head.yRot, 45f * Mth.DEG_TO_RAD, 80f * Mth.DEG_TO_RAD)
        }
    }

    /**
     * 修理工具：单手平举，和别的枪的 `PoseTool.pose` 都不一样 —— 它是一把工具，不是端着的枪。
     *
     * `0.05f * model.rightArm.xRot` 是**自引用**旧值（先把角度算成"低头角度 + 5% 当前俯角"），
     * 和迁移前 `RepairToolItem` 里那份完全相同，不要顺手改成 `head.xRot`。
     */
    @JvmField
    val REPAIR_TOOL_POSE: ArmPose = ArmPose.create(
        "RepairTool",
        false
    ) { model: HumanoidModel<*>, entity: LivingEntity?, arm: HumanoidArm ->
        if (arm != HumanoidArm.LEFT) {
            model.rightArm.xRot = -67.5f * Mth.DEG_TO_RAD + model.head.xRot + 0.05f * model.rightArm.xRot
            model.rightArm.yRot = 5f * Mth.DEG_TO_RAD + model.head.yRot
        }
    }
}