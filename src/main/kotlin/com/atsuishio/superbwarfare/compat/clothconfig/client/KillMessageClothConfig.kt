package com.atsuishio.superbwarfare.compat.clothconfig.client

import com.atsuishio.superbwarfare.config.client.KillMessageConfig
import com.atsuishio.superbwarfare.config.client.KillMessageConfig.KillMessagePosition
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.network.chat.Component

object KillMessageClothConfig {
    fun init(root: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val category = root.getOrCreateCategory(Component.translatable("config.superbwarfare.client.kill_message"))

        category.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Component.translatable("config.superbwarfare.client.kill_message.show_kill_message"),
                    KillMessageConfig.SHOW_KILL_MESSAGE.get()
                )
                .setDefaultValue(false)
                .setSaveConsumer { KillMessageConfig.SHOW_KILL_MESSAGE.set(it) }
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.show_kill_message.des"))
                .build()
        )

        category.addEntry(
            entryBuilder
                .startIntField(
                    Component.translatable("config.superbwarfare.client.kill_message.kill_message_count"),
                    KillMessageConfig.KILL_MESSAGE_COUNT.get()
                )
                .setDefaultValue(10)
                .setMin(1)
                .setMax(20)
                .setSaveConsumer { KillMessageConfig.KILL_MESSAGE_COUNT.set(it) }
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.kill_message_count.des"))
                .build()
        )

        category.addEntry(
            entryBuilder
                .startEnumSelector(
                    Component.translatable("config.superbwarfare.client.kill_message.kill_message_position"),
                    KillMessagePosition::class.java,
                    KillMessageConfig.KILL_MESSAGE_POSITION.get()
                )
                .setDefaultValue(KillMessagePosition.RIGHT_TOP)
                .setEnumNameProvider {
                    when (it) {
                        KillMessagePosition.LEFT_BOTTOM -> {
                            return@setEnumNameProvider Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.left_bottom")
                        }

                        KillMessagePosition.RIGHT_TOP -> {
                            return@setEnumNameProvider Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.right_top")
                        }

                        KillMessagePosition.RIGHT_BOTTOM -> {
                            return@setEnumNameProvider Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.right_bottom")
                        }

                        else -> {
                            return@setEnumNameProvider Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.left_top")
                        }
                    }
                }
                .setSaveConsumer { KillMessageConfig.KILL_MESSAGE_POSITION.set(it) }
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.kill_message_position.des"))
                .build()
        )

        category.addEntry(
            entryBuilder
                .startIntField(
                    Component.translatable("config.superbwarfare.client.kill_message.kill_message_margin_x"),
                    KillMessageConfig.KILL_MESSAGE_MARGIN_X.get()
                )
                .setDefaultValue(0)
                .setMin(-1000)
                .setMax(1000)
                .setSaveConsumer { KillMessageConfig.KILL_MESSAGE_MARGIN_X.set(it) }
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.kill_message_margin_x.des"))
                .build()
        )

        category.addEntry(
            entryBuilder
                .startIntField(
                    Component.translatable("config.superbwarfare.client.kill_message.kill_message_margin_y"),
                    KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get()
                )
                .setDefaultValue(5)
                .setMin(-1000)
                .setMax(1000)
                .setSaveConsumer { KillMessageConfig.KILL_MESSAGE_MARGIN_Y.set(it) }
                .setTooltip(Component.translatable("config.superbwarfare.client.kill_message.kill_message_margin_y.des"))
                .build()
        )
    }
}
