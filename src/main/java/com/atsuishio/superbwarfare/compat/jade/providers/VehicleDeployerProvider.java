package com.atsuishio.superbwarfare.compat.jade.providers;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.block.entity.VehicleDeployerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum VehicleDeployerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = Mod.loc("vehicle_deployer");

    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        var entityType = EntityType.byString(blockAccessor.getServerData().getString("EntityType"));
        if (entityType.isEmpty()) return;

        // 实体名称显示
        var registerName = EntityType.getKey(entityType.get()).toString();
        var translationKey = ContainerBlock.getEntityTranslationKey(registerName);
        iTooltip.add(Component.translatable(translationKey == null ? "des.superbwarfare.container.empty" : translationKey).withStyle(ChatFormatting.GRAY));

        // 所需尺寸显示
        int w = (int) (entityType.get().getDimensions().width() + 1);
        if (w % 2 == 0) w++;
        int h = (int) (entityType.get().getDimensions().height() + 1);
        if (h != 0) {
            iTooltip.add(Component.literal(w + " x " + w + " x " + h).withStyle(ChatFormatting.YELLOW));
        }
    }

    public ResourceLocation getUid() {
        return ID;
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        var blockEntity = (VehicleDeployerBlockEntity) blockAccessor.getBlockEntity();
        compoundTag.putString("EntityType", blockEntity.entityData.getString("EntityType"));
    }
}

