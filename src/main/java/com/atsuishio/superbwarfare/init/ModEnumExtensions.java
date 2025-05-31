package com.atsuishio.superbwarfare.init;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public class ModEnumExtensions {

    public static final EnumProxy<Rarity> SUPERBWARFARE_LEGENDARY = new EnumProxy<>(
            Rarity.class,
            -1,
            "superbwarfare:legendary",
            ChatFormatting.GOLD
    );

    public static Rarity getLegendary() {
        return SUPERBWARFARE_LEGENDARY.getValue();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Client {

        public static final EnumProxy<HumanoidModel.ArmPose> SUPERBWARFARE_LUNGE_MINE_POSE = new EnumProxy<>(
                HumanoidModel.ArmPose.class,
                false,
                (IArmPoseTransformer) (model, entity, arm) -> {
                    if (arm != HumanoidArm.LEFT) {
                        model.rightArm.xRot = 20f * Mth.DEG_TO_RAD + model.head.xRot;
                        model.rightArm.yRot = -12f * Mth.DEG_TO_RAD;
                        model.leftArm.xRot = -45f * Mth.DEG_TO_RAD + model.head.xRot;
                        model.leftArm.yRot = 40f * Mth.DEG_TO_RAD;
                    }
                }
        );

        public static HumanoidModel.ArmPose getLungeMinePose() {
            return SUPERBWARFARE_LUNGE_MINE_POSE.getValue();
        }

        public static final EnumProxy<HumanoidModel.ArmPose> SUPERBWARFARE_AURELIA_SCEPTRE_POSE = new EnumProxy<>(
                HumanoidModel.ArmPose.class,
                false,
                (IArmPoseTransformer) (model, entity, arm) -> {
                    if (arm != HumanoidArm.LEFT) {
                        model.rightArm.xRot = -67.5f * Mth.DEG_TO_RAD + model.head.xRot + 0.2f * model.rightArm.xRot;
                        model.rightArm.yRot = 5f * Mth.DEG_TO_RAD;
                    }
                }
        );

        public static HumanoidModel.ArmPose getAureliaSceptrePose() {
            return SUPERBWARFARE_AURELIA_SCEPTRE_POSE.getValue();
        }

        public static final EnumProxy<HumanoidModel.ArmPose> SUPERBWARFARE_MINIGUN_POSE = new EnumProxy<>(
                HumanoidModel.ArmPose.class,
                false,
                (IArmPoseTransformer) (model, entity, arm) -> {
                    if (arm != HumanoidArm.LEFT) {
                        model.rightArm.xRot = -22.5f * Mth.DEG_TO_RAD + model.head.xRot;
                        model.rightArm.yRot = -10f * Mth.DEG_TO_RAD;
                        model.leftArm.xRot = -45f * Mth.DEG_TO_RAD + model.head.xRot;
                        model.leftArm.yRot = 40f * Mth.DEG_TO_RAD;
                    }
                }
        );

        public static HumanoidModel.ArmPose getMinigunPose() {
            return SUPERBWARFARE_MINIGUN_POSE.getValue();
        }
    }
}
