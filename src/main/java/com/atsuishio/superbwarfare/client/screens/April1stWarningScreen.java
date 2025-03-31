package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.client.screens.modsell.TranslationRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.WarningScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class April1stWarningScreen extends WarningScreen {

    private final Screen lastScreen;

    public April1stWarningScreen(Screen lastScreen) {
        super(
                Component.translatable("superbwarfare.apr1st.screen.title").withStyle(ChatFormatting.BOLD),
                Component.translatable("superbwarfare.apr1st.screen.content"),
                Component.translatable("superbwarfare.apr1st.screen.check"),
                Component.translatable("superbwarfare.apr1st.screen.title").withStyle(ChatFormatting.BOLD).append("\n").append(Component.literal(TranslationRecord.get(TranslationRecord.CONTENT)))
        );
        this.lastScreen = lastScreen;
    }

    @Override
    protected void initButtons(int pYOffset) {
        AbstractButton proceedButton = this.createProceedButton(pYOffset);
        this.addRenderableWidget(proceedButton);

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_BACK, button -> Minecraft.getInstance().setScreen(this.lastScreen))
                        .bounds(this.width / 2 - 155 + 160, 100 + pYOffset, 150, 20)
                        .build()
        );
    }

    private AbstractButton createProceedButton(int pYOffset) {
        return Button.builder(CommonComponents.GUI_PROCEED, button -> {
            if (this.stopShowing != null && this.stopShowing.selected()) {
                Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(this.lastScreen));
            }
        }).bounds(this.width / 2 - 155, 100 + pYOffset, 150, 20).build();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiOpen(ScreenEvent.Opening event) {
        if (!((event.getNewScreen() instanceof JoinMultiplayerScreen || event.getNewScreen() instanceof SelectWorldScreen)
                && !(event.getCurrentScreen() instanceof April1stWarningScreen)))
            return;

        // 拦截单人和多人游戏界面加载
        event.setCanceled(true);
        Minecraft.getInstance().setScreen(new April1stWarningScreen(event.getCurrentScreen()));
    }
}
