package com.atsuishio.superbwarfare.init;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ModEnumExtensions {


    @OnlyIn(Dist.CLIENT)
    public static class Client {

        private static final HumanoidModel.ArmPose LUNGE_MINE_POSE = HumanoidModel.ArmPose.create("LungeMine", false, (model, entity, arm) -> {
            if (arm != HumanoidArm.LEFT) {
                model.rightArm.xRot = 20f * Mth.DEG_TO_RAD + model.head.xRot;
                model.rightArm.yRot = -12f * Mth.DEG_TO_RAD;
                model.leftArm.xRot = -45f * Mth.DEG_TO_RAD + model.head.xRot;
                model.leftArm.yRot = 40f * Mth.DEG_TO_RAD;
            }
        });

        public static HumanoidModel.ArmPose getLungeMinePose() {
            return LUNGE_MINE_POSE;
        }

        private static final HumanoidModel.ArmPose AURELIA_SCEPTRE_POSE = HumanoidModel.ArmPose.create("AureliaSceptre", false, (model, entity, arm) -> {
            if (arm != HumanoidArm.LEFT) {
                model.rightArm.xRot = -67.5f * Mth.DEG_TO_RAD + model.head.xRot + 0.05f * model.rightArm.xRot;
                model.rightArm.yRot = 5f * Mth.DEG_TO_RAD + model.head.yRot;
            }
        });

        public static HumanoidModel.ArmPose getAureliaSceptrePose() {
            return AURELIA_SCEPTRE_POSE;
        }

        private static final HumanoidModel.ArmPose MINIGUN_POSE = HumanoidModel.ArmPose.create("Minigun", false, (model, entity, arm) -> {
            if (arm != HumanoidArm.LEFT) {
                model.rightArm.xRot = 22.5f * Mth.DEG_TO_RAD + model.head.xRot;
                model.rightArm.yRot = model.head.yRot;
                model.leftArm.xRot = Mth.clamp(-45f * Mth.DEG_TO_RAD + model.head.xRot, -67.5f * Mth.DEG_TO_RAD, 0f * Mth.DEG_TO_RAD);
                model.leftArm.yRot = Mth.clamp(45f * Mth.DEG_TO_RAD + model.head.yRot, 45f * Mth.DEG_TO_RAD, 80f * Mth.DEG_TO_RAD);
            }
        });

        public static HumanoidModel.ArmPose getMinigunPose() {
            return MINIGUN_POSE;
        }

        private static final HumanoidModel.ArmPose M2_POSE = HumanoidModel.ArmPose.create("M2HB", false, (model, entity, arm) -> {
            if (arm != HumanoidArm.LEFT) {
                model.rightArm.xRot = 45f * Mth.DEG_TO_RAD + model.head.xRot;
                model.rightArm.yRot = model.head.yRot;
                model.leftArm.xRot = Mth.clamp(-45f * Mth.DEG_TO_RAD + model.head.xRot, -67.5f * Mth.DEG_TO_RAD, 0f * Mth.DEG_TO_RAD);
                model.leftArm.yRot = Mth.clamp(45f * Mth.DEG_TO_RAD + model.head.yRot, 45f * Mth.DEG_TO_RAD, 80f * Mth.DEG_TO_RAD);
            }
        });

        public static HumanoidModel.ArmPose getM2Pose() {
            return M2_POSE;
        }
    }
}
