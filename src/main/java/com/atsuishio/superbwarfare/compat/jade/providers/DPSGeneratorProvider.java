package com.atsuishio.superbwarfare.compat.jade.providers;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DPSGeneratorProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation ID = Mod.loc("dps_generator");

    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        var generator = (DPSGeneratorEntity) accessor.getEntity();

        var level = generator.getGeneratorLevel();
        var health = generator.getMaxHealth() * Math.pow(2, level);

        tooltip.add(Component.translatable("des.jade_plugin_superbwarfare.dps_generator.level", level).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("des.jade_plugin_superbwarfare.dps_generator.health", health).withStyle(ChatFormatting.GRAY));
    }

    public ResourceLocation getUid() {
        return ID;
    }

    public int getDefaultPriority() {
        return -4501;
    }
}

