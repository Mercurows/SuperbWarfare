package com.atsuishio.superbwarfare.item.gun.launcher

import com.atsuishio.superbwarfare.client.GunRendererBuilder
import com.atsuishio.superbwarfare.client.model.item.SuperStarShooterItemModel
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunGeoItem
import com.atsuishio.superbwarfare.tools.playLocalSound
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.HumanoidModel.ArmPose
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
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
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import software.bernie.geckolib.renderer.GeoItemRenderer
import java.util.function.Supplier

class SuperStarShooterItem : GunGeoItem(Properties().rarity(ModRarities.SUPERB)) {

    override fun getRenderer(): Supplier<out GeoItemRenderer<*>> =
        GunRendererBuilder.simple { SuperStarShooterItemModel() }

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
    override fun getClientExtensions(): IClientItemExtensions {
        return object : IClientItemExtensions {
            private val renderer: BlockEntityWithoutLevelRenderer = this@SuperStarShooterItem.getRenderer().get()

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                return renderer
            }

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

            override fun getArmPose(entityLiving: LivingEntity, hand: InteractionHand, stack: ItemStack): ArmPose? {
                if (!stack.isEmpty) {
                    if (entityLiving.usedItemHand == hand) {
                        return POSE
                    }
                }
                return ArmPose.EMPTY
            }
        }
    }

}