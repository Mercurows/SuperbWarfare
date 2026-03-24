package com.atsuishio.superbwarfare.compat.jade.providers

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IEntityComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig
import kotlin.math.pow

object DPSGeneratorProvider : IEntityComponentProvider {
    private val ID = loc("dps_generator")

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig?) {
        val generator = accessor.entity as DPSGeneratorEntity

        val level = generator.generatorLevel
        val health = generator.maxHealth * 2.0.pow(level.toDouble())

        tooltip.add(
            Component.translatable("des.jade_plugin_superbwarfare.dps_generator.level", level)
                .withStyle(ChatFormatting.AQUA)
        )
        tooltip.add(
            Component.translatable("des.jade_plugin_superbwarfare.dps_generator.health", health)
                .withStyle(ChatFormatting.GRAY)
        )
    }

    override fun getUid(): ResourceLocation {
        return ID
    }

    override fun getDefaultPriority(): Int {
        return -4501
    }
}

