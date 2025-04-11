package com.atsuishio.superbwarfare.compat.clothconfig;

import com.atsuishio.superbwarfare.compat.clothconfig.client.DisplayClothConfig;
import com.atsuishio.superbwarfare.compat.clothconfig.client.KillMessageClothConfig;
import com.atsuishio.superbwarfare.compat.clothconfig.client.ReloadClothConfig;
import com.atsuishio.superbwarfare.compat.clothconfig.client.VehicleControlClothConfig;
import com.atsuishio.superbwarfare.compat.clothconfig.common.GameplayClothConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.Nullable;

public class ClothConfigHelper {

    public static ConfigBuilder getConfigBuilder() {
        ConfigBuilder root = ConfigBuilder.create().setTitle(Component.translatable("config.superbwarfare.title"));
        root.setGlobalized(true);
        root.setGlobalizedExpanded(false);
        ConfigEntryBuilder entryBuilder = root.entryBuilder();

        ReloadClothConfig.init(root, entryBuilder);
        KillMessageClothConfig.init(root, entryBuilder);
        DisplayClothConfig.init(root, entryBuilder);
        VehicleControlClothConfig.init(root, entryBuilder);

        GameplayClothConfig.init(root, entryBuilder);

        return root;
    }

    public static void registerScreen() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () ->
                (client, parent) -> getConfigScreen(parent));
    }

    public static Screen getConfigScreen(@Nullable Screen parent) {
        return ClothConfigHelper.getConfigBuilder().setParentScreen(parent).build();
    }
}
