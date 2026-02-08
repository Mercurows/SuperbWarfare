package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.client.settings.KeyModifier
import org.lwjgl.glfw.GLFW

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ModKeyMappings {
    @JvmField
    val RELOAD = KeyMapping("key.superbwarfare.reload", GLFW.GLFW_KEY_R, "key.categories.superbwarfare")

    @JvmField
    val FIRE_MODE = KeyMapping("key.superbwarfare.fire_mode", GLFW.GLFW_KEY_N, "key.categories.superbwarfare")

    @JvmField
    val SENSITIVITY_INCREASE =
        KeyMapping("key.superbwarfare.sensitivity_increase", GLFW.GLFW_KEY_PAGE_UP, "key.categories.superbwarfare")

    @JvmField
    val SENSITIVITY_REDUCE =
        KeyMapping("key.superbwarfare.sensitivity_reduce", GLFW.GLFW_KEY_PAGE_DOWN, "key.categories.superbwarfare")

    @JvmField
    val INTERACT = KeyMapping("key.superbwarfare.interact", GLFW.GLFW_KEY_X, "key.categories.superbwarfare")

    @JvmField
    val DISMOUNT =
        KeyMapping("key.superbwarfare.dismount", GLFW.GLFW_KEY_LEFT_ALT, "key.categories.superbwarfare")

    @JvmField
    val BREATH =
        KeyMapping("key.superbwarfare.breath", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.superbwarfare")

    @JvmField
    val CONFIG = KeyMapping(
        "key.superbwarfare.config", KeyConflictContext.IN_GAME,
        KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.superbwarfare"
    )

    @JvmField
    val EDIT_MODE =
        KeyMapping("key.superbwarfare.edit_mode", GLFW.GLFW_KEY_H, "key.categories.superbwarfare")

    @JvmField
    val CHANGE_AMMO_FORWARD =
        KeyMapping("key.superbwarfare.change_ammo_forward", GLFW.GLFW_KEY_LEFT, "key.categories.superbwarfare")

    @JvmField
    val CHANGE_AMMO_BACKWARD =
        KeyMapping("key.superbwarfare.change_ammo_backward", GLFW.GLFW_KEY_RIGHT, "key.categories.superbwarfare")

    @JvmField
    val CHANGE_FIRE_MODE_FORWARD =
        KeyMapping("key.superbwarfare.change_fire_mode_forward", GLFW.GLFW_KEY_UP, "key.categories.superbwarfare")

    @JvmField
    val CHANGE_FIRE_MODE_BACKWARD =
        KeyMapping("key.superbwarfare.change_fire_mode_backward", GLFW.GLFW_KEY_DOWN, "key.categories.superbwarfare")

    @JvmField
    val UNLOAD =
        KeyMapping("key.superbwarfare.unload", InputConstants.UNKNOWN.value, "key.categories.superbwarfare")

    @JvmField
    val FIRE = KeyMapping(
        "key.superbwarfare.fire",
        InputConstants.Type.MOUSE,
        GLFW.GLFW_MOUSE_BUTTON_LEFT,
        "key.categories.superbwarfare"
    )

    @JvmField
    val HOLD_ZOOM = KeyMapping(
        "key.superbwarfare.hold_zoom",
        InputConstants.Type.MOUSE,
        GLFW.GLFW_MOUSE_BUTTON_RIGHT,
        "key.categories.superbwarfare"
    )

    @JvmField
    val SWITCH_ZOOM =
        KeyMapping("key.superbwarfare.switch_zoom", GLFW.GLFW_KEY_UNKNOWN, "key.categories.superbwarfare")

    @JvmField
    val RELEASE_DECOY =
        KeyMapping("key.superbwarfare.release_decoy", GLFW.GLFW_KEY_V, "key.categories.superbwarfare")

    @JvmField
    val FREE_CAMERA =
        KeyMapping("key.superbwarfare.free_camera", GLFW.GLFW_KEY_C, "key.categories.superbwarfare")

    @JvmField
    val MELEE = KeyMapping("key.superbwarfare.melee", GLFW.GLFW_KEY_V, "key.categories.superbwarfare")

    @JvmField
    val VEHICLE_SEEK =
        KeyMapping("key.superbwarfare.vehicle_seek", GLFW.GLFW_KEY_X, "key.categories.superbwarfare")

    @JvmField
    val MARK = KeyMapping(
        "key.superbwarfare.mark",
        InputConstants.Type.MOUSE,
        GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
        "key.categories.superbwarfare"
    )

    @JvmField
    val ACTIVE_THERMAL_IMAGING =
        KeyMapping("key.superbwarfare.active_thermal_imaging", GLFW.GLFW_KEY_K, "key.categories.superbwarfare")

    @SubscribeEvent
    fun registerKeyMappings(event: RegisterKeyMappingsEvent) {
        event.register(RELOAD)
        event.register(FIRE_MODE)
        event.register(SENSITIVITY_INCREASE)
        event.register(SENSITIVITY_REDUCE)
        event.register(INTERACT)
        event.register(DISMOUNT)
        event.register(BREATH)
        event.register(CONFIG)
        event.register(EDIT_MODE)
        event.register(FIRE)
        event.register(HOLD_ZOOM)
        event.register(SWITCH_ZOOM)
        event.register(RELEASE_DECOY)
        event.register(MELEE)
        event.register(VEHICLE_SEEK)
        event.register(FREE_CAMERA)
        event.register(MARK)
        event.register(CHANGE_AMMO_FORWARD)
        event.register(CHANGE_AMMO_BACKWARD)
        event.register(CHANGE_FIRE_MODE_FORWARD)
        event.register(CHANGE_FIRE_MODE_BACKWARD)
        event.register(UNLOAD)
        event.register(ACTIVE_THERMAL_IMAGING)
    }
}
