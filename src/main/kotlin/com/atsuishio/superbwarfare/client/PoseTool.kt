package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.capability.player.PlayerChargeState
import com.atsuishio.superbwarfare.client.gun.GunChargeHandler
import com.atsuishio.superbwarfare.data.gun.GunArmPose
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.event.DeployedWeaponHandler
import com.atsuishio.superbwarfare.init.ModEnumExtensions
import com.atsuishio.superbwarfare.tools.localPlayer
import net.minecraft.client.model.HumanoidModel
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object PoseTool {
    @JvmStatic
    fun pose(entityLiving: LivingEntity, hand: InteractionHand?, stack: ItemStack): HumanoidModel.ArmPose {
        val data = GunData.from(stack)
        val held = when (data.get(GunProp.ARM_POSE)) {
            GunArmPose.DEFAULT -> null
            GunArmPose.MINI_GUN -> ModEnumExtensions.Client.minigunPose
            GunArmPose.SUPER_STAR_SHOOTER -> ModEnumExtensions.Client.superStarShooterPose
            GunArmPose.SINGLE_HAND -> ModEnumExtensions.Client.singleHandPose
            GunArmPose.LUNGE_MINE -> ModEnumExtensions.Client.lungeMinePose
            GunArmPose.ITEM -> HumanoidModel.ArmPose.ITEM
        }
        if (held != null) {
            if (entityLiving is Player && DeployedWeaponHandler.isDeployed(entityLiving)) return HumanoidModel.ArmPose.EMPTY
            return if (!stack.isEmpty && entityLiving.usedItemHand == hand) held else HumanoidModel.ArmPose.EMPTY
        }

        // 冲锋窗口内一律端枪（哪怕正在疾跑），疾跑那个扛枪姿势不参与
        if (isBayonetCharging(entityLiving)) return HumanoidModel.ArmPose.BOW_AND_ARROW

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

    // 本人的窗口在本地（要即时），别人只能看服务端同步过来的那份
    private fun isBayonetCharging(entityLiving: LivingEntity): Boolean =
        if (entityLiving === localPlayer) {
            GunChargeHandler.isCharging()
        } else {
            PlayerChargeState.isCharging(entityLiving)
        }
}