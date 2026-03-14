package com.atsuishio.superbwarfare.compat.clothconfig

import com.atsuishio.superbwarfare.compat.clothconfig.client.ControlClothConfig
import com.atsuishio.superbwarfare.compat.clothconfig.client.DisplayClothConfig
import com.atsuishio.superbwarfare.compat.clothconfig.client.KillMessageClothConfig
import com.atsuishio.superbwarfare.compat.clothconfig.client.ReloadClothConfig
import com.atsuishio.superbwarfare.compat.clothconfig.common.GameplayClothConfig
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.fml.ModLoadingContext

object ClothConfigHelper {
    val configBuilder: ConfigBuilder
        get() {
            val root = ConfigBuilder.create()
                .setTitle(Component.translatable("config.superbwarfare.title"))
            root.setGlobalized(true)
            root.setGlobalizedExpanded(false)
            val entryBuilder = root.entryBuilder()

            ReloadClothConfig.init(root, entryBuilder)
            KillMessageClothConfig.init(root, entryBuilder)
            DisplayClothConfig.init(root, entryBuilder)
            ControlClothConfig.init(root, entryBuilder)

            GameplayClothConfig.init(root, entryBuilder)

            return root
        }

    fun registerScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenFactory::class.java)
        { ConfigScreenFactory { _, parent -> getConfigScreen(parent) } }
    }

    fun getConfigScreen(parent: Screen?): Screen? {
        return configBuilder.setParentScreen(parent).build()
    }
}
