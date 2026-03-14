package com.atsuishio.superbwarfare.compat.clothconfig.client

import com.atsuishio.superbwarfare.config.client.ReloadConfig
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.network.chat.Component

object ReloadClothConfig {
    fun init(root: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val category = root.getOrCreateCategory(Component.translatable("config.superbwarfare.client.reload"))

        category.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Component.translatable("config.superbwarfare.client.reload.left_click_reload"),
                    ReloadConfig.LEFT_CLICK_RELOAD.get()
                )
                .setDefaultValue(true)
                .setSaveConsumer { ReloadConfig.LEFT_CLICK_RELOAD.set(it) }
                .setTooltip(Component.translatable("config.superbwarfare.client.reload.left_click_reload.des")).build()
        )
    }
}
