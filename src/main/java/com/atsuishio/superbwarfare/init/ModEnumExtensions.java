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
    }
}
