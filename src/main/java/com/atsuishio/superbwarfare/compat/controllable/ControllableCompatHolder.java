package com.atsuishio.superbwarfare.compat.controllable;

import net.minecraftforge.fml.ModList;

/**
 * Controllable（手柄映射模组）的可选兼容入口。
 *
 * <p>手柄的左摇杆与扳机不会走 {@link net.minecraft.client.KeyMapping}：
 * Controllable 只把摇杆写进 {@code LocalPlayer.input}，并且它自己的按键绑定也只能绑按钮、
 * 绑不了轴。所以载具方向键（{@code ModKeyMappings.MOVE_*}，靠轮询 {@code isDown}）
 * 与开火（监听原生鼠标／键盘事件）都收不到手柄输入。这里直接对接 Controllable 的控制器 API，
 * 把手柄折算成同一套输入。
 *
 * <p>本类不引用任何 Controllable 类型，只有 {@link ControllableCompatHolderInner} 才会，
 * 并且所有入口都先经 {@link ModList#isLoaded(String)} 判断，所以未安装该 mod 的整合包
 * 不会解析到缺失的类。
 */
public final class ControllableCompatHolder {

    public static final String MOD_ID = "controllable";

    /**
     * 摇杆推过这个量才当作按下。Controllable 已经按它自己的 {@code thumbstickDeadZone}
     * 把死区内的值归零并重新映射到 0..1，所以这里再取一个偏大的阈值，避免轻碰就满油门。
     */
    public static final float STICK_THRESHOLD = 0.35F;

    /** 扳机拉过这个量才当作按下（Controllable 同样已处理扳机死区）。 */
    public static final float TRIGGER_THRESHOLD = 0.5F;

    private ControllableCompatHolder() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * 手柄是否正在被使用：没安装、没连接，或者玩家最近（约 5 秒）在用键鼠时都是 false。
     */
    public static boolean isControllerInUse() {
        return isLoaded() && ControllableCompatHolderInner.isControllerInUse();
    }

    /** 左摇杆 X，右为正。无手柄时返回 0。 */
    public static float leftStickX() {
        return isLoaded() ? ControllableCompatHolderInner.leftStickX() : 0F;
    }

    /** 左摇杆 Y，前为负（SDL 约定）。无手柄时返回 0。 */
    public static float leftStickY() {
        return isLoaded() ? ControllableCompatHolderInner.leftStickY() : 0F;
    }

    /** 右扳机，0..1。无手柄时返回 0。 */
    public static float rightTrigger() {
        return isLoaded() ? ControllableCompatHolderInner.rightTrigger() : 0F;
    }

    /** 左扳机，0..1。无手柄时返回 0。 */
    public static float leftTrigger() {
        return isLoaded() ? ControllableCompatHolderInner.leftTrigger() : 0F;
    }

    /**
     * 把地形线框开关登记进 Controllable 自己的按键列表。
     *
     * 只改 Minecraft {@code KeyMapping} 不会出现在手柄绑定界面里。
     */
    public static void registerClientBindings() {
        if (isLoaded()) {
            ControllableCompatHolderInner.registerBulletTerrainBinding();
        }
    }

    /** 手柄绑定的地形线框按钮是否按住。未安装 Controllable 时为 false。 */
    public static boolean bulletTerrainDown() {
        return isLoaded() && ControllableCompatHolderInner.bulletTerrainDown();
    }
}
