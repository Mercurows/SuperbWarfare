package com.atsuishio.superbwarfare.item.gun.launcher

import com.atsuishio.superbwarfare.client.GunRendererBuilder
import com.atsuishio.superbwarfare.client.model.item.SuperStarShooterItemModel
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunGeoItem
import com.atsuishio.superbwarfare.tools.SoundTool
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import software.bernie.geckolib.renderer.GeoItemRenderer
import java.util.function.Supplier

class SuperStarShooterItem : GunGeoItem(Properties().rarity(ModRarities.LEGENDARY)) {

    override fun getRenderer(): Supplier<out GeoItemRenderer<*>> =
        GunRendererBuilder.simple { SuperStarShooterItemModel() }

    override fun tick(shooter: Entity?, data: GunData, inMainHand: Boolean) {
        val level = shooter?.level() ?: return

        if (level.isNight && level.gameTime % 84L == 0L && data.ammo.get() < data.compute().magazine) {
            data.ammo.add(1)

            if (inMainHand && shooter is ServerPlayer) {
                SoundTool.playLocalSound(shooter, ModSounds.STAR_RECOVER.get(), SoundSource.PLAYERS, 0.5f, 1f)
            }
        }
    }

}