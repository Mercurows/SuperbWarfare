package com.atsuishio.superbwarfare.compat.jade.providers;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ContainerEntityProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation ID = Mod.loc("container_entity");

    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        var container = (ContainerBlockEntity) blockAccessor.getBlockEntity();

        // 实体名称显示
        var registerName = EntityType.getKey(container.entityType).toString();
        var translationKey = ContainerBlock.getEntityTranslationKey(registerName);
        iTooltip.add(Component.translatable(translationKey == null ? "des.superbwarfare.container.empty" : translationKey).withStyle(ChatFormatting.GRAY));

        // 所需尺寸显示
        var entityType = EntityType.byString(registerName).orElse(null);
        if (entityType != null) {
            float w = (float) Math.ceil(entityType.getDimensions().width() / 2) * 2;
            if ((int) w % 2 == 0) w++;
            int h = (int) (entityType.getDimensions().height() + 1);
            if (h != 0) {
                iTooltip.add(Component.literal((int) w + " x " + (int) w + " x " + h).withStyle(ChatFormatting.YELLOW));
            }
        }

        // 空间不足提示
        if (!ContainerBlock.canOpen(blockAccessor.getLevel(), container.getBlockPos(), container.entityType)) {
            iTooltip.add(Component.translatable("des.superbwarfare.container.fail.open").withStyle(ChatFormatting.RED));
        }

    }

    public ResourceLocation getUid() {
        return ID;
    }
}

