package com.atsuishio.superbwarfare.compat.clothconfig.client;

import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import static com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper.save;

public class DisplayClothConfig {

    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = root.getOrCreateCategory(Component.translatable("config.superbwarfare.client.display"));

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.enable_gun_lod"), DisplayConfig.ENABLE_GUN_LOD.get())
                .setDefaultValue(false)
                .setSaveConsumer(save(DisplayConfig.ENABLE_GUN_LOD))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.enable_gun_lod.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.superbwarfare.client.display.weapon_hud_x_offset"), DisplayConfig.WEAPON_HUD_X_OFFSET.get(),
                        -1000, 1000)
                .setDefaultValue(0)
                .setSaveConsumer(save(DisplayConfig.WEAPON_HUD_X_OFFSET))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.weapon_hud_x_offset.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.superbwarfare.client.display.weapon_hud_y_offset"), DisplayConfig.WEAPON_HUD_Y_OFFSET.get(),
                        -1000, 1000)
                .setDefaultValue(0)
                .setSaveConsumer(save(DisplayConfig.WEAPON_HUD_Y_OFFSET))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.weapon_hud_y_offset.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.kill_indication"), DisplayConfig.KILL_INDICATION.get())
                .setDefaultValue(true)
                .setSaveConsumer(save(DisplayConfig.KILL_INDICATION))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.kill_indication.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.ammo_hud"), DisplayConfig.AMMO_HUD.get())
                .setDefaultValue(true)
                .setSaveConsumer(save(DisplayConfig.AMMO_HUD))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.ammo_hud.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.vehicle_info"), DisplayConfig.VEHICLE_INFO.get())
                .setDefaultValue(true)
                .setSaveConsumer(save(DisplayConfig.VEHICLE_INFO))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.vehicle_info.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.float_cross_hair"), DisplayConfig.FLOAT_CROSS_HAIR.get())
                .setDefaultValue(true)
                .setSaveConsumer(save(DisplayConfig.FLOAT_CROSS_HAIR))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.float_cross_hair.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.camera_rotate"), DisplayConfig.CAMERA_ROTATE.get())
                .setDefaultValue(true)
                .setSaveConsumer(save(DisplayConfig.CAMERA_ROTATE))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.camera_rotate.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.armor_plate_hud"), DisplayConfig.ARMOR_PLATE_HUD.get())
                .setDefaultValue(true)
                .setSaveConsumer(save(DisplayConfig.ARMOR_PLATE_HUD))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.armor_plate_hud.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.stamina_hud"), DisplayConfig.STAMINA_HUD.get())
                .setDefaultValue(true)
                .setSaveConsumer(save(DisplayConfig.STAMINA_HUD))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.stamina_hud.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.dog_tag_name_visible"), DisplayConfig.DOG_TAG_NAME_VISIBLE.get())
                .setDefaultValue(true)
                .setSaveConsumer(save(DisplayConfig.DOG_TAG_NAME_VISIBLE))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.dog_tag_name_visible.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.display.dog_tag_icon_visible"), DisplayConfig.DOG_TAG_ICON_VISIBLE.get())
                .setDefaultValue(false)
                .setSaveConsumer(save(DisplayConfig.DOG_TAG_ICON_VISIBLE))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.dog_tag_icon_visible.des"))
                .build()
        );

        category.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.superbwarfare.client.display.weapon_screen_shake"), DisplayConfig.WEAPON_SCREEN_SHAKE.get(),
                        0, 100)
                .setDefaultValue(100)
                .setSaveConsumer(save(DisplayConfig.WEAPON_SCREEN_SHAKE))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.weapon_screen_shake.des"))
                .build());

        category.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.superbwarfare.client.display.explosion_screen_shake"), DisplayConfig.EXPLOSION_SCREEN_SHAKE.get(),
                        0, 100)
                .setDefaultValue(100)
                .setSaveConsumer(save(DisplayConfig.EXPLOSION_SCREEN_SHAKE))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.explosion_screen_shake.des"))
                .build());

        category.addEntry(entryBuilder
                .startIntSlider(Component.translatable("config.superbwarfare.client.display.shock_screen_shake"), DisplayConfig.SHOCK_SCREEN_SHAKE.get(),
                        0, 100)
                .setDefaultValue(100)
                .setSaveConsumer(save(DisplayConfig.SHOCK_SCREEN_SHAKE))
                .setTooltip(Component.translatable("config.superbwarfare.client.display.shock_screen_shake.des"))
                .build());
    }
}
