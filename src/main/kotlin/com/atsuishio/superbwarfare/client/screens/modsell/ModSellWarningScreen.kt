package com.atsuishio.superbwarfare.client.screens.modsell

import com.atsuishio.superbwarfare.config.client.EnvironmentChecksumConfig
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.gui.screens.multiplayer.WarningScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.net.InetAddress
import java.security.MessageDigest
import java.util.*

@OnlyIn(Dist.CLIENT)
class ModSellWarningScreen(val lastScreen: Screen) : WarningScreen(
    Component.literal(TranslationRecord.get(TranslationRecord.TITLE)).withStyle(ChatFormatting.BOLD),
    Component.literal(TranslationRecord.get(TranslationRecord.CONTENT)),
    Component.literal(TranslationRecord.get(TranslationRecord.CHECK)),
    Component.literal(TranslationRecord.get(TranslationRecord.TITLE)).withStyle(ChatFormatting.BOLD).append("\n")
        .append(Component.literal(TranslationRecord.get(TranslationRecord.CONTENT)))
) {

    //    @Mod.EventBusSubscriber(value = [Dist.CLIENT])
    companion object {
        val ENVIRONMENT_CHECKSUM = generateEnvironmentHash()

        fun generateEnvironmentHash(): String {
            val environmentInfo = listOf(
                System.getProperty("os.name"),          // 操作系统名称
                System.getProperty("os.arch"),          // 操作系统架构
                System.getProperty("java.vm.version"),  // JVM详细版本号
                System.getProperty("java.home"),        // JVM路径
                System.getProperty("user.name"),        // 系统用户名称
                getHostName(),                          // 主机名称
                "stupidNoPayWarningChecksum"            // 神秘的盐
            )
            return sha256(environmentInfo.joinToString("|"))
        }

        private fun getHostName(): String {
            try {
                return InetAddress.getLocalHost().hostName
            } catch (e: Exception) {
                System.err.println(e.message)
                return "error"
            }
        }

        private fun sha256(input: String): String {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(input.toByteArray())
                return HexFormat.of().formatHex(hash)
            } catch (e: Exception) {
                System.err.println(e.message)
                return ""
            }
        }

//        @SubscribeEvent(priority = EventPriority.HIGH)
//        fun onGuiOpen(event: ScreenEvent.Opening) {
//            if (!((event.newScreen is JoinMultiplayerScreen || event.newScreen is SafetyScreen) && event.currentScreen is TitleScreen))
//                return
//
//            if (EnvironmentChecksumConfig.ENVIRONMENT_CHECKSUM.get().equals(ENVIRONMENT_CHECKSUM)) return
//
//            // 拦截多人游戏界面加载
//            event.isCanceled = true
//            mc.setScreen(event.currentScreen?.let { ModSellWarningScreen(it) })
//        }
    }

    protected override fun initButtons(yOffset: Int) {
        val proceedButton = this.createProceedButton(yOffset)
        this.addRenderableWidget(proceedButton)

        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_BACK) { mc.setScreen(this.lastScreen) }
                .bounds(this.width / 2 - 155 + 160, 100 + yOffset, 150, 20)
                .build()
        )
    }

    private fun createProceedButton(yOffset: Int): AbstractButton {
        return Button.builder(CommonComponents.GUI_PROCEED) {
            if (this.stopShowing != null && this.stopShowing!!.selected()) {
                EnvironmentChecksumConfig.ENVIRONMENT_CHECKSUM.set(ENVIRONMENT_CHECKSUM)
                EnvironmentChecksumConfig.ENVIRONMENT_CHECKSUM.save()
            }
            mc.setScreen(JoinMultiplayerScreen(this.lastScreen))
        }.bounds(this.width / 2 - 155, 100 + yOffset, 150, 20).build()
    }
}
