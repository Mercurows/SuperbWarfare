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
                .setDefaultValue(10)
                .setMin(1)
                .setMax(20)
                .setSaveConsumer(KillMessageConfig.KILL_MESSAGE_COUNT::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.kill_message_count.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startEnumSelector(Component.translatable("config.superbwarfare.client.kill_message.kill_message_position"),
                        KillMessageConfig.KillMessagePosition.class,
                        KillMessageConfig.KILL_MESSAGE_POSITION.get())
                .setDefaultValue(KillMessageConfig.KillMessagePosition.RIGHT_TOP)
                .setEnumNameProvider(pos -> switch (pos) {
                    case KillMessageConfig.KillMessagePosition.LEFT_BOTTOM ->
                            Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.left_bottom");
                    case KillMessageConfig.KillMessagePosition.RIGHT_TOP ->
                            Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.right_top");
                    case KillMessageConfig.KillMessagePosition.RIGHT_BOTTOM ->
                            Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.right_bottom");
                    default ->
                            Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.left_top");
                })
                .setSaveConsumer(KillMessageConfig.KILL_MESSAGE_POSITION::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startIntField(Component.translatable("config.superbwarfare.client.kill_message.kill_message_margin_x"), KillMessageConfig.KILL_MESSAGE_MARGIN_X.get())
                .setDefaultValue(0)
                .setMin(-1000)
                .setMax(1000)
                .setSaveConsumer(KillMessageConfig.KILL_MESSAGE_MARGIN_X::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.kill_message_margin_x.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startIntField(Component.translatable("config.superbwarfare.client.kill_message.kill_message_margin_y"), KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get())
                .setDefaultValue(5)
                .setMin(-1000)
                .setMax(1000)
                .setSaveConsumer(KillMessageConfig.KILL_MESSAGE_MARGIN_Y::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.kill_message_margin_y.des"))
                .build()
        );
    }
}
