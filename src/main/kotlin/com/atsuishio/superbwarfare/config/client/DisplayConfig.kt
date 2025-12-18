package com.atsuishio.superbwarfare.config.client

import com.atsuishio.superbwarfare.config.buildClientConfig

object DisplayConfig {

    @JvmField
    val ENABLE_GUN_LOD = buildClientConfig {
        push("display")

        comment("Set true to enable gun lod")
        define("enable_gun_lod", false)
    }

    @JvmField
    val WEAPON_HUD_X_OFFSET = buildClientConfig {
        comment("The x offset of weapon hud")
        defineInRange("weapon_hud_x_offset", 0, -1000, 1000)
    }

    @JvmField
    val WEAPON_HUD_Y_OFFSET = buildClientConfig {
        comment("The y offset of weapon hud")
        defineInRange("weapon_hud_y_offset", 0, -1000, 1000)
    }

    @JvmField
    val ENABLE_HEAT_BAR_HUD = buildClientConfig {
        comment("Set true to enable heat bar hud")
        define("enable_heat_bar_hud", true)
    }

    @JvmField
    val HEAT_BAR_HUD_X_OFFSET = buildClientConfig {
        comment("The x offset of heat bar hud")
        defineInRange("heat_bar_hud_x_offset", 0, -1000, 1000)
    }

    @JvmField
    val HEAT_BAR_HUD_Y_OFFSET = buildClientConfig {
        comment("The y offset of heat bar hud")
        defineInRange("heat_bar_hud_y_offset", 0, -1000, 1000)
    }

    @JvmField
    val KILL_INDICATION = buildClientConfig {
        comment("Set true if you want to show kill indication while killing an entity")
        define("kill_indication", true)
    }

    @JvmField
    val AMMO_HUD = buildClientConfig {
        comment("Set true to show ammo and gun info on HUD")
        define("ammo_hud", true)
    }

    @JvmField
    val ADVANCED_AMMO_HUD = buildClientConfig {
        comment("Set true to show advanced ammo info on HUD")
        define("advanced_ammo_hud", true)
    }

    @JvmField
    val VEHICLE_INFO = buildClientConfig {
        comment("Set true to display vehicle info when aiming at a vehicle")
        define("vehicle_info", true)
    }

    @JvmField
    val FLOAT_CROSS_HAIR = buildClientConfig {
        comment("Set true to enable float cross hair")
        define("float_cross_hair", true)
    }

    @JvmField
    val CAMERA_ROTATE = buildClientConfig {
        comment("Set true to enable camera rotate when holding a gun")
        define("camera_rotate", true)
    }

    @JvmField
    val ARMOR_PLATE_HUD = buildClientConfig {
        comment("Set true to enable armor plate hud")
        define("armor_plate_hud", true)
    }

    @JvmField
    val STAMINA_HUD = buildClientConfig {
        comment("Set true to enable stamina hud")
        define("stamina_hud", true)
    }

    @JvmField
    val DOG_TAG_NAME_VISIBLE = buildClientConfig {
        comment("Set true to show the name of dog tag in kill messages")
        define("dog_tag_name_visible", true)
    }

    @JvmField
    val DOG_TAG_ICON_VISIBLE = buildClientConfig {
        comment("Set true to show the icon of dog tag in kill messages")
        define("dog_tag_icon_visible", false)
    }

    @JvmField
    val WEAPON_SCREEN_SHAKE = buildClientConfig {
        comment("The strength of screen shaking while firing with a weapon")
        defineInRange("weapon_screen_shake", 100, 0, 100)
    }

    @JvmField
    val EXPLOSION_SCREEN_SHAKE = buildClientConfig {
        comment("The strength of screen shaking while exploding")
        defineInRange("explosion_screen_shake", 100, 0, 100)
    }

    @JvmField
    val SHOCK_SCREEN_SHAKE = buildClientConfig {
        comment("The strength of screen shaking when shocked")
        defineInRange("shock_screen_shake", 100, 0, 100)
    }

    @JvmField
    val ENABLE_VERSION_CHECK_WARNING = buildClientConfig {
        comment("Set true to enable version check warning when version of this mod has been changed")
        define("enable_version_check_warning", true).also { pop() }
    }
}