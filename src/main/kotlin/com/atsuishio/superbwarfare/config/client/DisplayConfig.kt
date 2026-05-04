package com.atsuishio.superbwarfare.config.client

import com.atsuishio.superbwarfare.config.buildClientConfig

object DisplayConfig {

    @JvmField
    val ENABLE_GUN_LOD = buildClientConfig {
        push("display")

        comment("Set true to enable gun lod")
        comment("是否启用枪械低模")
        define("enable_gun_lod", false)
    }

    @JvmField
    val WEAPON_HUD_X_OFFSET = buildClientConfig {
        comment("The x offset of weapon hud")
        comment("枪械HUD的水平方向偏移量")
        defineInRange("weapon_hud_x_offset", 0, -1000, 1000)
    }

    @JvmField
    val WEAPON_HUD_Y_OFFSET = buildClientConfig {
        comment("The y offset of weapon hud")
        comment("枪械HUD的竖直方向偏移量")
        defineInRange("weapon_hud_y_offset", 0, -1000, 1000)
    }

    @JvmField
    val ENABLE_HEAT_BAR_HUD = buildClientConfig {
        comment("Set true to enable heat bar hud")
        comment("是否启用武器过热条HUD")
        define("enable_heat_bar_hud", true)
    }

    @JvmField
    val HEAT_BAR_HUD_X_OFFSET = buildClientConfig {
        comment("The x offset of heat bar hud")
        comment("武器过热条HUD的水平偏移量")
        defineInRange("heat_bar_hud_x_offset", 0, -1000, 1000)
    }

    @JvmField
    val HEAT_BAR_HUD_Y_OFFSET = buildClientConfig {
        comment("The y offset of heat bar hud")
        comment("武器过热条HUD的竖直偏移量")
        defineInRange("heat_bar_hud_y_offset", 0, -1000, 1000)
    }

    @JvmField
    val KILL_INDICATION = buildClientConfig {
        comment("Set true if you want to show kill indication while killing an entity")
        comment("是否开启准星周围的击杀提示")
        define("kill_indication", true)
    }

    @JvmField
    val AMMO_HUD = buildClientConfig {
        comment("Set true to show ammo and gun info on HUD")
        comment("是否开启枪械和弹药HUD")
        define("ammo_hud", true)
    }

    @JvmField
    val ADVANCED_AMMO_HUD = buildClientConfig {
        comment("Set true to show advanced ammo info on HUD")
        comment("是否开启高级弹药显示HUD（显示弹种）")
        define("advanced_ammo_hud", true)
    }

    @JvmField
    val VEHICLE_INFO = buildClientConfig {
        comment("Set true to display vehicle info when aiming at a vehicle")
        comment("是否开启看向载具时的信息显示")
        define("vehicle_info", true)
    }

    @JvmField
    val IFF_HUD = buildClientConfig {
        comment("Set true to display IFF HUD")
        comment("是否开启敌我识别装置位置HUD")
        define("iff_hud", true)
    }

    @JvmField
    val FLOAT_CROSS_HAIR = buildClientConfig {
        comment("Set true to enable float cross hair")
        comment("是否开启浮动准星")
        define("float_cross_hair", true)
    }

    @JvmField
    val CAMERA_ROTATE = buildClientConfig {
        comment("Set true to enable camera rotate when holding a gun")
        comment("是否开启持枪时视角晃动")
        define("camera_rotate", true)
    }

    @JvmField
    val ARMOR_PLATE_HUD = buildClientConfig {
        comment("Set true to enable armor plate hud")
        comment("是否开启护甲板HUD")
        define("armor_plate_hud", true)
    }

    @JvmField
    val STAMINA_HUD = buildClientConfig {
        comment("Set true to enable stamina hud")
        comment("是否开启耐力条HUD")
        define("stamina_hud", true)
    }

    @JvmField
    val DOG_TAG_NAME_VISIBLE = buildClientConfig {
        comment("Set true to show the name of dog tag in kill messages")
        comment("是否开启击杀提示的狗牌名称显示")
        define("dog_tag_name_visible", true)
    }

    @JvmField
    val DOG_TAG_ICON_VISIBLE = buildClientConfig {
        comment("Set true to show the icon of dog tag in kill messages")
        comment("是否开启击杀提示的狗牌图标显示")
        define("dog_tag_icon_visible", false)
    }

    @JvmField
    val WEAPON_SCREEN_SHAKE = buildClientConfig {
        comment("The strength of screen shaking while firing with a weapon")
        comment("使用武器开火时的屏幕晃动强度")
        defineInRange("weapon_screen_shake", 100, 0, 100)
    }

    @JvmField
    val EXPLOSION_SCREEN_SHAKE = buildClientConfig {
        comment("The strength of screen shaking while exploding")
        comment("爆炸时的屏幕晃动强度")
        defineInRange("explosion_screen_shake", 100, 0, 100)
    }

    @JvmField
    val SHOCK_SCREEN_SHAKE = buildClientConfig {
        comment("The strength of screen shaking when shocked")
        comment("被麻痹时的屏幕晃动强度")
        defineInRange("shock_screen_shake", 100, 0, 100)
    }

    @JvmField
    val ENABLE_VERSION_CHECK_WARNING = buildClientConfig {
        comment("Set true to enable version check warning when version of this mod has been changed")
        comment("是否启用版本检查警告")
        define("enable_version_check_warning", true).also { pop() }
    }
}