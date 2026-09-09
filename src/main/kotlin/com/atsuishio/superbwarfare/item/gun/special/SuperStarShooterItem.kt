package com.atsuishio.superbwarfare.item.gun.special

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import com.atsuishio.superbwarfare.tools.playLocalSound
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.HumanoidModel.ArmPose
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@RegistryName("super_star_shooter")
class SuperStarShooterItem : GeoGunItemV2(Properties().rarity(ModRarities.SUPERB)) {

    override fun tick(shooter: Entity?, data: GunData, inMainHand: Boolean) {
        val level = shooter?.level() ?: return

        if (level.isNight && level.gameTime % 84L == 0L && data.ammo.get() < data.get(GunProp.MAGAZINE)) {
            data.ammo.add(1)

            if (inMainHand && shooter is ServerPlayer) {
                shooter.playLocalSound(ModSounds.STAR_RECOVER.get(), SoundSource.PLAYERS, 0.5f, 1f)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun armPose(
        entityLiving: LivingEntity,
        hand: InteractionHand,
        itemStack: ItemStack
    ): ArmPose {
        return if (!itemStack.isEmpty && entityLiving.usedItemHand == hand) POSE else ArmPose.EMPTY
    }

    companion object {
        @OnlyIn(Dist.CLIENT)
        private val POSE: ArmPose = ArmPose.create("SuperStarShooterItem", false) { model: HumanoidModel<*>, entity: LivingEntity?, arm: HumanoidArm ->
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
}
