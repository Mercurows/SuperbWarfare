package com.atsuishio.superbwarfare.compat.jade.providers;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum C4InfoProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation ID = ModUtils.loc("c4_info");

    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        var c4 = (C4Entity) accessor.getEntity();

        if (c4.getEntityData().get(C4Entity.IS_CONTROLLABLE)) {
            // 遥控
            tooltip.add(Component.translatable("des.jade_plugin_superbwarfare.c4.remote_control").withStyle(ChatFormatting.YELLOW));
        } else {
            // 定时
            var timeLeft = ExplosionConfig.C4_EXPLOSION_COUNTDOWN.get() - c4.tickCount;
            tooltip.add(Component.translatable(
                    "des.jade_plugin_superbwarfare.c4.time_left",
                    String.format("%.2f", timeLeft / 20.0)
            ).withStyle(ChatFormatting.YELLOW));
        }
    }

    public ResourceLocation getUid() {
        return ID;
    }

    public int getDefaultPriority() {
        return -4501;
    }
}

