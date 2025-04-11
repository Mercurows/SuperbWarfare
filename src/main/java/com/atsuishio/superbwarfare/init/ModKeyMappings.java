package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyMappings {

    public static final KeyMapping RELOAD = new KeyMapping("key.superbwarfare.reload", GLFW.GLFW_KEY_R, "key.categories.superbwarfare");
    public static final KeyMapping FIRE_MODE = new KeyMapping("key.superbwarfare.fire_mode", GLFW.GLFW_KEY_N, "key.categories.superbwarfare");
    public static final KeyMapping SENSITIVITY_INCREASE = new KeyMapping("key.superbwarfare.sensitivity_increase", GLFW.GLFW_KEY_PAGE_UP, "key.categories.superbwarfare");
    public static final KeyMapping SENSITIVITY_REDUCE = new KeyMapping("key.superbwarfare.sensitivity_reduce", GLFW.GLFW_KEY_PAGE_DOWN, "key.categories.superbwarfare");
    public static final KeyMapping INTERACT = new KeyMapping("key.superbwarfare.interact", GLFW.GLFW_KEY_X, "key.categories.superbwarfare");
    public static final KeyMapping DISMOUNT = new KeyMapping("key.superbwarfare.dismount", GLFW.GLFW_KEY_LEFT_ALT, "key.categories.superbwarfare");
    public static final KeyMapping BREATH = new KeyMapping("key.superbwarfare.breath", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.superbwarfare");

    public static final KeyMapping CONFIG = new KeyMapping("key.superbwarfare.config", KeyConflictContext.IN_GAME,
            KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.superbwarfare");

    public static final KeyMapping EDIT_MODE = new KeyMapping("key.superbwarfare.edit_mode", GLFW.GLFW_KEY_H, "key.categories.superbwarfare");
    public static final KeyMapping EDIT_SCOPE = new KeyMapping("key.superbwarfare.edit_scope", GLFW.GLFW_KEY_UP, "key.categories.superbwarfare");
    public static final KeyMapping EDIT_BARREL = new KeyMapping("key.superbwarfare.edit_barrel", GLFW.GLFW_KEY_LEFT, "key.categories.superbwarfare");
    public static final KeyMapping EDIT_MAGAZINE = new KeyMapping("key.superbwarfare.edit_magazine", GLFW.GLFW_KEY_DOWN, "key.categories.superbwarfare");
    public static final KeyMapping EDIT_STOCK = new KeyMapping("key.superbwarfare.edit_stock", GLFW.GLFW_KEY_RIGHT, "key.categories.superbwarfare");
    public static final KeyMapping EDIT_GRIP = new KeyMapping("key.superbwarfare.edit_grip", KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, "key.categories.superbwarfare");

    public static final KeyMapping FIRE = new KeyMapping("key.superbwarfare.fire", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT, "key.categories.superbwarfare");
    public static final KeyMapping HOLD_ZOOM = new KeyMapping("key.superbwarfare.hold_zoom", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.superbwarfare");
    public static final KeyMapping SWITCH_ZOOM = new KeyMapping("key.superbwarfare.switch_zoom", GLFW.GLFW_KEY_UNKNOWN, "key.categories.superbwarfare");
    public static final KeyMapping RELEASE_DECOY = new KeyMapping("key.superbwarfare.release_decoy", GLFW.GLFW_KEY_X, "key.categories.superbwarfare");
    public static final KeyMapping FREE_CAMERA = new KeyMapping("key.superbwarfare.free_camera", GLFW.GLFW_KEY_C, "key.categories.superbwarfare");
    public static final KeyMapping MELEE = new KeyMapping("key.superbwarfare.melee", GLFW.GLFW_KEY_V, "key.categories.superbwarfare");

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(RELOAD);
        event.register(FIRE_MODE);
        event.register(SENSITIVITY_INCREASE);
        event.register(SENSITIVITY_REDUCE);
        event.register(INTERACT);
        event.register(DISMOUNT);
        event.register(BREATH);
        event.register(CONFIG);
        event.register(EDIT_MODE);
        event.register(EDIT_SCOPE);
        event.register(EDIT_BARREL);
        event.register(EDIT_MAGAZINE);
        event.register(EDIT_STOCK);
        event.register(EDIT_GRIP);
        event.register(FIRE);
        event.register(HOLD_ZOOM);
        event.register(SWITCH_ZOOM);
        event.register(RELEASE_DECOY);
        event.register(MELEE);
        event.register(FREE_CAMERA);
    }
}
