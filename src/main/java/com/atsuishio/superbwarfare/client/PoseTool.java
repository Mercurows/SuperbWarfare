package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PoseTool {

    public static HumanoidModel.ArmPose pose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);
            if (data.reloading() || data.charging()) {
                return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }
        }

        if (entityLiving.isSprinting() && entityLiving.onGround()) {
            return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
        } else {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
    }
}
