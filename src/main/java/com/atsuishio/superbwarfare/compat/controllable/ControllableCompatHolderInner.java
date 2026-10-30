package com.atsuishio.superbwarfare.compat.controllable;

import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.client.ControllerInput;
import com.mrcrayfish.controllable.client.binding.BindingContext;
import com.mrcrayfish.controllable.client.binding.BindingRegistry;
import com.mrcrayfish.controllable.client.binding.ButtonBinding;
import com.mrcrayfish.controllable.client.input.Controller;

/**
 * 真正接触 Controllable 类型的实现。
 *
 * <p>只有 {@link ControllableCompatHolder} 确认该 mod 已加载后才会加载本类，
 * 因此这里可以安全地直接引用它的类型。
 */
public final class ControllableCompatHolderInner {

    /**
     * Unbound until the player assigns a controller button. {@code -1} is
     * Controllable's empty button, so the row is visible and does not steal
     * an existing gamepad button.
     */
    private static final ButtonBinding BULLET_TERRAIN = new ButtonBinding(
            -1,
            "key.superbwarfare.toggle_bullet_terrain",
            "key.categories.superbwarfare",
            BindingContext.IN_GAME
    );

    private ControllableCompatHolderInner() {
    }

    public static void registerBulletTerrainBinding() {
        BindingRegistry.getInstance().register(BULLET_TERRAIN);
    }

    public static boolean bulletTerrainDown() {
        return BULLET_TERRAIN.isButtonDown();
    }

    private static Controller controller() {
        Controller controller = Controllable.getController();
        return controller != null && controller.isOpen() ? controller : null;
    }

    public static boolean isControllerInUse() {
        Controller controller = controller();
        if (controller == null) return false;
        ControllerInput input = Controllable.getInput();
        return input != null && input.isControllerInUse();
    }

    public static float leftStickX() {
        Controller controller = controller();
        return controller == null ? 0F : controller.getLThumbStickXValue();
    }

    public static float leftStickY() {
        Controller controller = controller();
        return controller == null ? 0F : controller.getLThumbStickYValue();
    }

    public static float rightTrigger() {
        Controller controller = controller();
        return controller == null ? 0F : controller.getRTriggerValue();
    }

    public static float leftTrigger() {
        Controller controller = controller();
        return controller == null ? 0F : controller.getLTriggerValue();
    }
}
