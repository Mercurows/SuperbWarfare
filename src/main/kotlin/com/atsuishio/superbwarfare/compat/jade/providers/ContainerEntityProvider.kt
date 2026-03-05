package com.atsuishio.superbwarfare.compat.jade.providers

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.ContainerBlock.Companion.canOpen
import com.atsuishio.superbwarfare.block.ContainerBlock.Companion.getEntityTranslationKey
import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig
import kotlin.math.ceil

object ContainerEntityProvider : IBlockComponentProvider {
    private val ID = loc("container_entity")

    override fun appendTooltip(iTooltip: ITooltip, blockAccessor: BlockAccessor, iPluginConfig: IPluginConfig?) {
        val container = blockAccessor.blockEntity as ContainerBlockEntity

        // 实体名称显示
        val registerName = EntityType.getKey(container.entityType).toString()
        val translationKey = getEntityTranslationKey(registerName)
        iTooltip.add(
            Component.translatable(translationKey ?: "des.superbwarfare.container.empty")
                .withStyle(ChatFormatting.GRAY)
        )

        // 所需尺寸显示
        val entityType = EntityType.byString(registerName).orElse(null)
        if (entityType != null) {
            var w = ceil((entityType.dimensions.width / 2).toDouble()).toFloat() * 2
            if (w.toInt() % 2 == 0) w++
            val h = (entityType.dimensions.height + 1).toInt()
            if (h != 0) {
                iTooltip.add(
                    Component.literal("${w.toInt()} x ${w.toInt()} x $h").withStyle(ChatFormatting.YELLOW)
                )
            }
        }

        // 空间不足提示
        if (!canOpen(blockAccessor.level, container.blockPos, container.entityType, container.entityTag)) {
            iTooltip.add(Component.translatable("des.superbwarfare.container.fail.open").withStyle(ChatFormatting.RED))
        }
    }

    override fun getUid(): ResourceLocation {
        return ID
    }
}

