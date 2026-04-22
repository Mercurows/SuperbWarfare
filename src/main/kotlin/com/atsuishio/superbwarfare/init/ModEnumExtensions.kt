package com.atsuishio.superbwarfare.init

import net.minecraft.ChatFormatting
import net.minecraft.client.model.HumanoidModel.ArmPose
import net.minecraft.network.chat.Style
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.Rarity
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.fml.common.asm.enumextension.EnumProxy
import net.neoforged.neoforge.client.IArmPoseTransformer
import java.util.function.UnaryOperator

object ModEnumExtensions {
    @JvmField
    val SUPERBWARFARE_LEGENDARY: EnumProxy<Rarity> = EnumProxy(
        Rarity::class.java,
        -1,
        "superbwarfare:legendary",
        ChatFormatting.GOLD
    )

    val legendary: Rarity
        get() = SUPERBWARFARE_LEGENDARY.getValue()

    @JvmField
    val SUPERBWARFARE_SUPERB: EnumProxy<Rarity> = EnumProxy(
        Rarity::class.java,
        -1,
        "superbwarfare:superb",
        ChatFormatting.RED
    )

    val superb: Rarity
        get() = SUPERBWARFARE_SUPERB.getValue()

    @JvmField
    val SUPERBWARFARE_VIRTUAL: EnumProxy<Rarity> = EnumProxy(
        Rarity::class.java,
        -1,
        "superbwarfare:virtual",
        UnaryOperator { style: Style -> style.withColor(0xFF9AAF) }
    )

    val virtual: Rarity
        get() = SUPERBWARFARE_VIRTUAL.getValue()

    @OnlyIn(Dist.CLIENT)
    object Client {
        @JvmField
        val SUPERBWARFARE_LUNGE_MINE_POSE: EnumProxy<ArmPose> = EnumProxy(
            ArmPose::class.java,
            false,
            IArmPoseTransformer { model, _, arm ->
                if (arm != HumanoidArm.LEFT) {
                    model.rightArm.xRot = 20f * Mth.DEG_TO_RAD + model.head.xRot
                    model.rightArm.yRot = -12f * Mth.DEG_TO_RAD
                    model.leftArm.xRot = -45f * Mth.DEG_TO_RAD + model.head.xRot
                    model.leftArm.yRot = 40f * Mth.DEG_TO_RAD
                }
            }
        )

        val lungeMinePose: ArmPose
            get() = SUPERBWARFARE_LUNGE_MINE_POSE.getValue()

        @JvmField
        val SUPERBWARFARE_MINIGUN_POSE: EnumProxy<ArmPose> = EnumProxy(
            ArmPose::class.java,
            false,
            IArmPoseTransformer { model, _, arm ->
                if (arm != HumanoidArm.LEFT) {
                    model.rightArm.xRot = 22.5f * Mth.DEG_TO_RAD + model.head.xRot
                    model.rightArm.yRot = model.head.yRot
                    model.leftArm.xRot = Mth.clamp(
                        -45f * Mth.DEG_TO_RAD + model.head.xRot,
                        -67.5f * Mth.DEG_TO_RAD,
                        0f * Mth.DEG_TO_RAD
                    )
                    model.leftArm.yRot =
                        Mth.clamp(45f * Mth.DEG_TO_RAD + model.head.yRot, 45f * Mth.DEG_TO_RAD, 80f * Mth.DEG_TO_RAD)
                }
            }
        )

        @JvmStatic
        val minigunPose: ArmPose
            get() = SUPERBWARFARE_MINIGUN_POSE.getValue()

        @JvmField
        val SUPERBWARFARE_M2_POSE: EnumProxy<ArmPose> = EnumProxy(
            ArmPose::class.java,
            false,
            IArmPoseTransformer { model, _, arm ->
                if (arm != HumanoidArm.LEFT) {
                    model.rightArm.xRot = 45f * Mth.DEG_TO_RAD + model.head.xRot
                    model.rightArm.yRot = model.head.yRot
                    model.leftArm.xRot = Mth.clamp(
                        -45f * Mth.DEG_TO_RAD + model.head.xRot,
                        -67.5f * Mth.DEG_TO_RAD,
                        0f * Mth.DEG_TO_RAD
                    )
                    model.leftArm.yRot =
                        Mth.clamp(45f * Mth.DEG_TO_RAD + model.head.yRot, 45f * Mth.DEG_TO_RAD, 80f * Mth.DEG_TO_RAD)
                }
            }
        )

        @JvmStatic
        val m2Pose: ArmPose
            get() = SUPERBWARFARE_M2_POSE.getValue()

        @JvmField
        val SUPERBWARFARE_SUPER_STAR_SHOOTER_POSE: EnumProxy<ArmPose> = EnumProxy(
            ArmPose::class.java,
            false,
            IArmPoseTransformer { model, _, arm ->
                if (arm != HumanoidArm.LEFT) {
                    model.rightArm.xRot = -70f * Mth.DEG_TO_RAD + model.head.xRot
                    model.rightArm.yRot = 0f
                    model.rightArm.zRot = 0f
                    model.leftArm.xRot = -70f * Mth.DEG_TO_RAD + model.head.xRot
                    model.leftArm.yRot = 0f
                    model.leftArm.zRot = 0f
                }
            }
        )

        val superStarShooterPose: ArmPose
            get() = SUPERBWARFARE_SUPER_STAR_SHOOTER_POSE.getValue()
    }
}
