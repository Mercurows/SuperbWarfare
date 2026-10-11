package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.overlay.components.CENTER
import com.atsuishio.superbwarfare.client.overlay.components.CENTER_TOP
import com.atsuishio.superbwarfare.client.overlay.components.StringComponent
import com.atsuishio.superbwarfare.event.DeployedWeaponHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.localPlayer
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object DeployWeaponTipOverlay : CommonOverlay("deploy_weapon_tip") {
    val TIP = StringComponent(CENTER.offsetY(28F), CENTER_TOP)

    init {
        registerComponents(TIP)
    }

    override fun shouldRender(): Boolean {
        val player = localPlayer ?: return false
        return super.shouldRender() &&
                !DeployedWeaponHandler.isDeployed(player) &&
                DeployedWeaponHandler.deployTarget(player) != null
    }

    override fun RenderContext.preRender() {
        TIP.component = Component.translatable(
            "tips.superbwarfare.deploy_weapon",
            ModKeyMappings.INTERACT.translatedKeyMessage
        )
    }
}
