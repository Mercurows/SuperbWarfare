package com.atsuishio.superbwarfare.compat.jade.providers

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.projectile.C4Entity
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IEntityComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

object C4InfoProvider : IEntityComponentProvider {
    private val ID = loc("c4_info")

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig?) {
        val c4 = accessor.entity as C4Entity

        if (c4.getEntityData().get(C4Entity.IS_CONTROLLABLE)) {
            // 遥控
            tooltip.add(
                Component.translatable("des.jade_plugin_superbwarfare.c4.remote_control")
                    .withStyle(ChatFormatting.YELLOW)
            )
        } else {
            // 定时
            val timeLeft = ExplosionConfig.C4_EXPLOSION_COUNTDOWN.get() - c4.bombTick
            tooltip.add(
                Component.translatable(
                    "des.jade_plugin_superbwarfare.c4.time_left",
                    String.format("%.2f", timeLeft / 20.0)
                ).withStyle(ChatFormatting.YELLOW)
            )
        }
    }

    override fun getUid(): ResourceLocation {
        return ID
    }

    override fun getDefaultPriority(): Int {
        return -4501
    }
}

