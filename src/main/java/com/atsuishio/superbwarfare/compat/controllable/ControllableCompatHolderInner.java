package com.atsuishio.superbwarfare.compat.controllable;

import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.client.ControllerInput;
import com.mrcrayfish.controllable.client.input.Controller;

/**
 * 真正接触 Controllable 类型的实现。
 *
 * <p>只有 {@link ControllableCompatHolder} 确认该 mod 已加载后才会加载本类，
 * 因此这里可以安全地直接引用它的类型。
 */
public final class ControllableCompatHolderInner {

    private ControllableCompatHolderInner() {
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
