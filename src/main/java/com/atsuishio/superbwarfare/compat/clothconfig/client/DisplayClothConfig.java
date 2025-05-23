package com.atsuishio.superbwarfare.compat.clothconfig.client;

import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class DisplayClothConfig {

    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = root.getOrCreateCategory(Component.translatable("config.superbwarfare.client.display"));

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.kill_indication"), DisplayConfig.KILL_INDICATION.get())
                .setDefaultValue(true)
                .setSaveConsumer(DisplayConfig.KILL_INDICATION::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.kill_indication.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.ammo_hud"), DisplayConfig.AMMO_HUD.get())
                .setDefaultValue(true)
                .setSaveConsumer(DisplayConfig.AMMO_HUD::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.ammo_hud.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.float_cross_hair"), DisplayConfig.FLOAT_CROSS_HAIR.get())
                .setDefaultValue(true)
                .setSaveConsumer(DisplayConfig.FLOAT_CROSS_HAIR::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.float_cross_hair.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.camera_rotate"), DisplayConfig.CAMERA_ROTATE.get())
                .setDefaultValue(true)
                .setSaveConsumer(DisplayConfig.CAMERA_ROTATE::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.camera_rotate.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.armor_plate_hud"), DisplayConfig.ARMOR_PLATE_HUD.get())
                .setDefaultValue(true)
                .setSaveConsumer(DisplayConfig.ARMOR_PLATE_HUD::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.armor_plate_hud.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.stamina_hud"), DisplayConfig.STAMINA_HUD.get())
                .setDefaultValue(true)
                .setSaveConsumer(DisplayConfig.STAMINA_HUD::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.stamina_hud.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.dog_tag_name_visible"), DisplayConfig.DOG_TAG_NAME_VISIBLE.get())
                .setDefaultValue(true)
                .setSaveConsumer(DisplayConfig.DOG_TAG_NAME_VISIBLE::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.dog_tag_name_visible.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.dog_tag_icon_visible"), DisplayConfig.DOG_TAG_ICON_VISIBLE.get())
                .setDefaultValue(false)
                .setSaveConsumer(DisplayConfig.DOG_TAG_ICON_VISIBLE::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.dog_tag_icon_visible.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.superbwarfare.client.display.weapon_screen_shake"), DisplayConfig.WEAPON_SCREEN_SHAKE.get(),
                        0, 100)
                .setDefaultValue(100)
                .setSaveConsumer(DisplayConfig.WEAPON_SCREEN_SHAKE::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.weapon_screen_shake.des"))
                .build());

        category.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.superbwarfare.client.display.explosion_screen_shake"), DisplayConfig.EXPLOSION_SCREEN_SHAKE.get(),
                        0, 100)
                .setDefaultValue(100)
                .setSaveConsumer(DisplayConfig.EXPLOSION_SCREEN_SHAKE::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.explosion_screen_shake.des"))
                .build());

        category.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.superbwarfare.client.display.shock_screen_shake"), DisplayConfig.SHOCK_SCREEN_SHAKE.get(),
                        0, 100)
                .setDefaultValue(100)
                .setSaveConsumer(DisplayConfig.SHOCK_SCREEN_SHAKE::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.display.shock_screen_shake.des"))
                .build());
    }
}
