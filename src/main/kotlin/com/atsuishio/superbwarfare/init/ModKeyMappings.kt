package com.atsuishio.superbwarfare.init

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.client.settings.KeyModifier
import org.lwjgl.glfw.GLFW

@EventBusSubscriber(Dist.CLIENT)
object ModKeyMappings {
    const val CATEGORY = "key.categories.superbwarfare"
    private val KEYS = mutableListOf<KeyMapping>()

    @JvmField
    val MOVE_FORWARD = registerKey("move_forward", GLFW.GLFW_KEY_W)

    @JvmField
    val MOVE_BACKWARD = registerKey("move_backward", GLFW.GLFW_KEY_S)

    @JvmField
    val MOVE_LEFT = registerKey("move_left", GLFW.GLFW_KEY_A)

    @JvmField
    val MOVE_RIGHT = registerKey("move_right", GLFW.GLFW_KEY_D)

    @JvmField
    val MOVE_SPACE = registerKey("move_space", GLFW.GLFW_KEY_SPACE)

    @JvmField
    val MOVE_SHIFT = registerKey("move_shift", GLFW.GLFW_KEY_LEFT_SHIFT)

    @JvmField
    val MOVE_CTRL = registerKey("move_ctrl", GLFW.GLFW_KEY_LEFT_CONTROL)

    @JvmField
    val RELOAD = registerKey("reload", GLFW.GLFW_KEY_R)

    @JvmField
    val FIRE_MODE = registerKey("fire_mode", GLFW.GLFW_KEY_N)

    @JvmField
    val SENSITIVITY_INCREASE = registerKey("sensitivity_increase", GLFW.GLFW_KEY_PAGE_UP)

    @JvmField
    val SENSITIVITY_REDUCE = registerKey("sensitivity_reduce", GLFW.GLFW_KEY_PAGE_DOWN)

    @JvmField
    val INTERACT = registerKey("interact", GLFW.GLFW_KEY_X)

    @JvmField
    val DISMOUNT = registerKey("dismount", GLFW.GLFW_KEY_LEFT_ALT)

    @JvmField
    val BREATH = registerKey("breath", GLFW.GLFW_KEY_LEFT_CONTROL)

    @JvmField
    val CHANGE_SEAT = registerKey("change_seat", GLFW.GLFW_KEY_LEFT_SHIFT)

    @JvmField
    val CONFIG = registerKey(
        "config",
        GLFW.GLFW_KEY_O,
        KeyConflictContext.IN_GAME,
        KeyModifier.ALT
    )

    @JvmField
    val EDIT_MODE = registerKey("edit_mode", GLFW.GLFW_KEY_H)

    @JvmField
    val CHANGE_AMMO_FORWARD = registerKey("change_ammo_forward", GLFW.GLFW_KEY_LEFT)

    @JvmField
    val CHANGE_AMMO_BACKWARD = registerKey("change_ammo_backward", GLFW.GLFW_KEY_RIGHT)

    @JvmField
    val CHANGE_FIRE_MODE_FORWARD = registerKey("change_fire_mode_forward", GLFW.GLFW_KEY_UP)

    @JvmField
    val CHANGE_FIRE_MODE_BACKWARD = registerKey("change_fire_mode_backward", GLFW.GLFW_KEY_DOWN)

    @JvmField
    val UNLOAD = registerKey("unload", InputConstants.UNKNOWN.value)

    @JvmField
    val UNLOAD_PASSENGERS = registerKey("unload_passengers", GLFW.GLFW_KEY_U)

    @JvmField
    val DISCONNECT_TOWING = registerKey("disconnect_towing", GLFW.GLFW_KEY_Y)

    @JvmField
    val FIRE = registerKey("fire", GLFW.GLFW_MOUSE_BUTTON_LEFT, type = InputConstants.Type.MOUSE)

    @JvmField
    val HOLD_ZOOM = registerKey("hold_zoom", GLFW.GLFW_MOUSE_BUTTON_RIGHT, type = InputConstants.Type.MOUSE)

    @JvmField
    val SWITCH_ZOOM = registerKey("switch_zoom", GLFW.GLFW_KEY_UNKNOWN)

    @JvmField
    val RELEASE_DECOY = registerKey("release_decoy", GLFW.GLFW_KEY_V)

    @JvmField
    val FREE_CAMERA = registerKey("free_camera", GLFW.GLFW_KEY_C)

    @JvmField
    val MELEE = registerKey("melee", GLFW.GLFW_KEY_V)

    /**
     * 副武器开火（G）。
     *
     * 语义（§9.4 定稿）：**V 永远近战；G 有副武器就使用副武器，没有则等同 V**。
     * 一期还没有副武器体系，所以 G 目前只是"第二个近战入口"，与 V 共享同一个入口函数与状态，
     * 同 tick 内两键同时按下只触发一次。
     */
    @JvmField
    val SUBWEAPON_FIRE = registerKey("subweapon_fire", GLFW.GLFW_KEY_G)

    @JvmField
    val VEHICLE_SEEK = registerKey("vehicle_seek", GLFW.GLFW_KEY_X)

    @JvmField
    val MARK = registerKey("mark", GLFW.GLFW_MOUSE_BUTTON_MIDDLE, type = InputConstants.Type.MOUSE)

    @JvmField
    val ACTIVE_THERMAL_IMAGING = registerKey("active_thermal_imaging", GLFW.GLFW_KEY_K)

    @JvmField
    val LOITER_CONFIG = registerKey("loiter_config", GLFW.GLFW_KEY_J)

    @JvmField
    val TOGGLE_TACTICAL_MAP = registerKey("toggle_tactical_map", GLFW.GLFW_KEY_M)

    private fun registerKey(
        name: String,
        code: Int,
        conflictContext: KeyConflictContext = KeyConflictContext.IN_GAME,
        modifier: KeyModifier = KeyModifier.NONE,
        type: InputConstants.Type = InputConstants.Type.KEYSYM
    ): KeyMapping {
        val key = KeyMapping(
            "key.superbwarfare.$name",
            conflictContext,
            modifier,
            type,
            code,
            CATEGORY
        )
        KEYS.add(key)
        return key
    }

    @SubscribeEvent
    fun registerKeyMappings(event: RegisterKeyMappingsEvent) {
        KEYS.forEach { event.register(it) }
    }
}
