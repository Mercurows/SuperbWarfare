package com.atsuishio.superbwarfare.compat.jade.providers

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.ContainerBlock.Companion.getEntityTranslationKey
import com.atsuishio.superbwarfare.block.entity.VehicleDeployerBlockEntity
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

object VehicleDeployerProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    private val ID = loc("vehicle_deployer")

    override fun appendTooltip(iTooltip: ITooltip, blockAccessor: BlockAccessor, iPluginConfig: IPluginConfig?) {
        val entityType = EntityType.byString(blockAccessor.serverData.getString("EntityType"))
        if (entityType.isEmpty) return

        // 实体名称显示
        val registerName = EntityType.getKey(entityType.get()).toString()
        val translationKey = getEntityTranslationKey(registerName)
        iTooltip.add(
            Component.translatable(translationKey ?: "des.superbwarfare.container.empty")
                .withStyle(ChatFormatting.GRAY)
        )

        // 所需尺寸显示
        var w = (entityType.get().dimensions.width + 1).toInt()
        if (w % 2 == 0) w++
        val h = (entityType.get().dimensions.height + 1).toInt()
        if (h != 0) {
            iTooltip.add(Component.literal("$w x $w x $h").withStyle(ChatFormatting.YELLOW))
        }
    }

    override fun getUid(): ResourceLocation {
        return ID
    }

    override fun appendServerData(compoundTag: CompoundTag, blockAccessor: BlockAccessor) {
        val blockEntity = blockAccessor.blockEntity as VehicleDeployerBlockEntity
        compoundTag.putString("EntityType", blockEntity.entityData.getString("EntityType"))
    }
}

