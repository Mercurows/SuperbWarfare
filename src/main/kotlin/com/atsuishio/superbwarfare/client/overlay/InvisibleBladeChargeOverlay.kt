package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.damage.InvisibleBladeShapeness
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object InvisibleBladeChargeOverlay : CommonOverlay("invisible_blade_charge") {
    private const val WIDTH = 80
    private const val HEIGHT = 5
    // Leave room for the bayonet charge/cooldown indicator nearer the crosshair.
    private const val OFFSET_Y = 44

    override fun shouldRender() = super.shouldRender()
            && !MiscConfig.HIDE_COMBAT_HUD.get()
            && !ClientEventHandler.isEditing

    override fun RenderContext.render() {
        val stack = ActiveGun.stackOf(player)
        val vehicle = player.vehicle
        if (!GunItem.isOperable(stack) || (vehicle is VehicleEntity && vehicle.banHand(player))) return

        val data = from(stack)
        if (!data.meleeOnly()) return
        val perk = InvisibleBladeShapeness.instanceOf(data) ?: return
        val maxCharge = InvisibleBladeShapeness.maxCharge(perk.level.toInt())
        val charge = InvisibleBladeShapeness.charge(data).coerceIn(0.0, maxCharge)
        val filledWidth = ((WIDTH - 2) * charge / maxCharge).toInt()
        val left = (screenWidth - WIDTH) / 2
        val top = screenHeight / 2 + OFFSET_Y

        guiGraphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xFFAAAAAA.toInt())
        guiGraphics.fill(left + 1, top + 1, left + WIDTH - 1, top + HEIGHT - 1, 0xFF222222.toInt())
        if (filledWidth > 0) {
            guiGraphics.fill(left + 1, top + 1, left + 1 + filledWidth, top + HEIGHT - 1, 0xFFFFFFFF.toInt())
        }
        guiGraphics.drawCenteredString(
            mc.font, "${charge.toInt()} / ${maxCharge.toInt()}", screenWidth / 2, top + HEIGHT + 3, 0xFFFFFF
        )
    }
}
