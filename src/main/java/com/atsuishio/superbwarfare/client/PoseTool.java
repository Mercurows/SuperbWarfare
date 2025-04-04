package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.item.gun.GunData;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PoseTool {

    public static HumanoidModel.ArmPose pose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
        var tag = NBTTool.getTag(stack);
        var data = GunData.from(stack);
        if (data.emptyReloading()
                || data.getReloadState() == GunData.ReloadState.NORMAL_RELOADING
                || data.isReloading()
                || GunsTool.getGunBooleanTag(tag, "Charging")
        ) {
            return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
        } else if (entityLiving.isSprinting() && entityLiving.onGround() && entityLiving.getPersistentData().getDouble("noRun") == 0) {
            return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
        } else {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
    }
}
