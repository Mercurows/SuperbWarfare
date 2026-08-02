package com.atsuishio.superbwarfare.compat.jade.providers

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.CatapultControllerBlock
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

object CatapultControllerProvider : IBlockComponentProvider {
    private val ID = loc("catapult_controller")

    override fun appendTooltip(iTooltip: ITooltip, blockAccessor: BlockAccessor, iPluginConfig: IPluginConfig?) {
        iTooltip.add(
            Component.translatable(
                "message.superbwarfare.catapult_power",
                blockAccessor.blockState.getValue(CatapultControllerBlock.LAUNCH_POWER)
            )
        )
    }

    override fun getUid(): ResourceLocation {
        return ID
    }
}

