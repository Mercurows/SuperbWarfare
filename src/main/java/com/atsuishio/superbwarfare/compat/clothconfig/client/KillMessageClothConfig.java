package com.atsuishio.superbwarfare.compat.clothconfig.client;

import com.atsuishio.superbwarfare.config.client.KillMessageConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class KillMessageClothConfig {

    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = root.getOrCreateCategory(Component.translatable("config.superbwarfare.client.kill_message"));

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.kill_message.show_kill_message"), KillMessageConfig.SHOW_KILL_MESSAGE.get())
                .setDefaultValue(false)
                .setSaveConsumer(KillMessageConfig.SHOW_KILL_MESSAGE::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.show_kill_message.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startIntField(Component.translatable("config.superbwarfare.client.kill_message.kill_message_count"), KillMessageConfig.KILL_MESSAGE_COUNT.get())
                .setDefaultValue(5)
                .setMin(1)
                .setMax(20)
                .setSaveConsumer(KillMessageConfig.KILL_MESSAGE_COUNT::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.kill_message_count.des"))
                .build()
        );
    }
}
