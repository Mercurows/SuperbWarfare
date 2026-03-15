package com.atsuishio.superbwarfare.item.gun.launcher

import com.atsuishio.superbwarfare.client.GunRendererBuilder
import com.atsuishio.superbwarfare.client.model.item.SuperStarShooterItemModel
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModEnumExtensions
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunGeoItem
import com.atsuishio.superbwarfare.tools.playLocalSound
import net.minecraft.client.model.HumanoidModel.ArmPose
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
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

    override fun getArmPose(entityLiving: LivingEntity, hand: InteractionHand, stack: ItemStack): ArmPose {
        if (!stack.isEmpty && entityLiving.usedItemHand == hand) {
            return ModEnumExtensions.Client.superStarShooterPose
        }
        return ArmPose.EMPTY
    }
}