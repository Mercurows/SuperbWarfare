package com.atsuishio.superbwarfare.client.screens.modsell;

import com.atsuishio.superbwarfare.config.client.EnvironmentChecksumConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.WarningScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT)
public class ModSellWarningScreen extends WarningScreen {

    private static final String ENVIRONMENT_CHECKSUM = generateEnvironmentHash();

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return "error";
        }
    }

    private static String generateEnvironmentHash() {
        var environmentInfo = List.of(
                System.getProperty("os.name"),          // 操作系统名称
                System.getProperty("os.arch"),          // 操作系统架构
                System.getProperty("java.vm.version"),  // JVM详细版本号
                System.getProperty("java.home"),        // JVM路径
                System.getProperty("user.name"),        // 系统用户名称
                getHostName(),                          // 主机名称
                "stupidNoPayWarningChecksum"            // 神秘的盐
        );

        return sha256(String.join("|", environmentInfo));
    }

    private static String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return "";
        }
    }

    private final Screen lastScreen;

    public ModSellWarningScreen(Screen lastScreen) {
        super(
                Component.literal(TranslationRecord.get(TranslationRecord.TITLE)).withStyle(ChatFormatting.BOLD),
                Component.literal(TranslationRecord.get(TranslationRecord.CONTENT)),
                Component.literal(TranslationRecord.get(TranslationRecord.CHECK)),
                Component.literal(TranslationRecord.get(TranslationRecord.TITLE)).withStyle(ChatFormatting.BOLD).append("\n").append(Component.literal(TranslationRecord.get(TranslationRecord.CONTENT)))
        );
        this.lastScreen = lastScreen;
    }

    private AbstractButton createProceedButton(int pYOffset) {
        return Button.builder(CommonComponents.GUI_PROCEED, button -> {
            if (this.stopShowing != null && this.stopShowing.selected()) {
                EnvironmentChecksumConfig.ENVIRONMENT_CHECKSUM.set(ENVIRONMENT_CHECKSUM);
                EnvironmentChecksumConfig.ENVIRONMENT_CHECKSUM.save();
            }
            Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(this.lastScreen));
        }).bounds(this.width / 2 - 155, 100 + pYOffset, 150, 20).build();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onGuiOpen(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof JoinMultiplayerScreen && !(event.getCurrentScreen() instanceof ModSellWarningScreen)))
            return;

        if (EnvironmentChecksumConfig.ENVIRONMENT_CHECKSUM.get().equals(ENVIRONMENT_CHECKSUM)) return;

        // 拦截多人游戏界面加载
        event.setCanceled(true);
        Minecraft.getInstance().setScreen(new ModSellWarningScreen(event.getCurrentScreen()));
    }

    @Override
    protected @NotNull Layout addFooterButtons() {
        LinearLayout linearlayout = LinearLayout.horizontal().spacing(8);
        linearlayout.addChild(Button.builder(CommonComponents.GUI_PROCEED, p_280872_ -> {
            if (this.stopShowing != null && this.stopShowing.selected()) {
                EnvironmentChecksumConfig.ENVIRONMENT_CHECKSUM.set(ENVIRONMENT_CHECKSUM);
                EnvironmentChecksumConfig.ENVIRONMENT_CHECKSUM.save();
            }

            Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(this.lastScreen));
        }).build());
        linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_329731_ -> this.onClose()).build());
        return linearlayout;
    }
}
