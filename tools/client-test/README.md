# Superb Warfare 客户端测试环境

从 KDE 应用程序启动器打开 **Superb Warfare 客户端测试**。它会：

1. 把 [Controllable](https://www.curseforge.com/minecraft/mc-mods/controllable) 0.21.9 和它依赖的 Framework 0.8.0 放进 `tools/client-test/mods`，由 ForgeGradle 反混淆后加载。不要把生产 jar 重复放进 `run/mods`，否则开发环境的 Mixin 名称不匹配。启动器会清理这些已知重复项。
2. 启动 `legion_pad_bridge.py`。Legion Go 现在的固件模式是 `dual_dinput`（分体）。HHD 在这个模式下只发快捷键，不会合成完整手柄，所以桥自己读左右两个 evdev 设备，按 HHD 的左右键位习惯合成一只虚拟 Xbox 360。Controllable 自带的 SDL 数据库能直接认出它。
3. 用本机 JDK 17 跑 `./gradlew runClient`。

进游戏后打开 Controllable 的手柄设置，应看到 **Xbox 360 Controller**。若左右摇杆反了，退出后执行：

```bash
LEGION_SWAP_SIDES=1 ./tools/client-test/run-client.sh
```

或把 `tools/client-test/pad-map.json` 里的 `swap_sides` 改成 `true`。按键对不上时用 `--dump` 看原始事件，再改 `left_buttons` / `right_buttons`。

如果在 HHD 里把固件切回 xinput，桥会让路，Controllable 直接用 HHD 的虚拟手柄。

`runServer` / `runGameTestServer` 默认不加载这两个客户端测试依赖；可以显式使用
`-PclientTestMods=false`。Framework 扫描 Controllable 配置时会涉及客户端类，
不能把客户端测试依赖直接用于无界面的专用测试服务端。

## 手柄操控载具

Controllable 已经作为 **可选依赖** 接进 Superb Warfare：`META-INF/mods.toml` 里声明
`controllable`（`mandatory = false`，`side = "CLIENT"`），构建时用 `compileOnly`
编译 `compat/controllable`。没装这个 mod 的整合包不受影响——所有调用点先经
`ModList.isLoaded("controllable")` 判断，`ControllableCompatHolderInner` 在那种情况下
永远不会被加载。

为什么需要模组侧接入，而不是像 TACZ 那样只在 Controllable 里加绑定：

- 载具方向键读的是 `ModKeyMappings.MOVE_*`（轮询 `KeyMapping.isDown`），
  而 Controllable 的左摇杆只写 `LocalPlayer.input`，从不 `setDown`；
- 开火走原生 `InputEvent.MouseButton.Pre` / `InputEvent.Key`，
  而 Controllable 的 `key.attack` 只调 `Minecraft.startAttack()`，不产生这些事件；
- Controllable 的按键绑定只能绑按钮，绑不了轴。

所以 SBW 直接读它的控制器 API（`compat/controllable`）：

| 手柄 | 作用 | 实现位置 |
| --- | --- | --- |
| 左摇杆 | 油门（前/后）+ 转向（左/右）。只推左右不打油门就是原地转弯 | `ClientEventHandler.controllerVehicleBits()` |
| 右扳机 RT | 开火（载具炮与手持枪，含炮手位、无人机） | `ClickEventHandler.handleControllerTriggers()` |
| 左扳机 LT | 瞄准／缩放 | 同上 |

死区沿用 Controllable 自己的 `thumbstickDeadZone` / `triggerDeadZone`（它已在
`getLThumbStickXValue()` 等 getter 里处理），阈值见 `ControllableCompatHolder` 里的
`STICK_THRESHOLD` / `TRIGGER_THRESHOLD`。玩家最近 5 秒在用键鼠时手柄不介入
（`ControllerInput.isControllerInUse()`），所以键鼠行为完全不变。

可在游戏内 **设置 → 控制 → 手柄操控** 关闭（`config/superbwarfare-client.toml`
的 `control.controllable_compat`）。

两个已知限制：

- **转向是数字量。** 载具输入协议是 9 位掩码（左右各一位），没有模拟量通道，
  手柄转向的手感等于长按键盘 A/D。要模拟量必须改协议与权威物理，未做。
- **左摇杆不能用来开菜单光标以外的原版移动。** 这是 Controllable 的行为，与本接入无关。

### 按键类操作

摇杆和扳机以外的操作仍走 Controllable 自己的按键绑定。启动器会在
`run/config/controllable/key_adapters.properties` 为空（没有任何 `key.superbwarfare.`
条目）时写入一份默认适配：

| 手柄 | 绑定 |
| --- | --- |
| B | `key.superbwarfare.reload`（换弹，同时保留 `key.tacz.reload`） |
| RB | `key.superbwarfare.fire_mode`（切换开火模式／弹种） |
| LB | `key.superbwarfare.dismount`（离开载具；移动中需双击） |
| LS | `key.superbwarfare.move_space`（刹车／悬停／起落架） |
| RS | `key.sneak`（潜行，同时恢复原版潜行下车） |

写入后不再覆盖，游戏内改动会保留。想恢复默认就删掉文件里的
`key.superbwarfare.` 行再启动一次。

**注意背键：** 桥只合成标准 Xbox 按键（`a/b/x/y/lb/rb/back/start/guide/ls/rs`），
Legion Go 的四个背键走厂商 HID 接口（root-only），桥读不到。因此
`bindings.properties` 里既有的 `key.sneak=controllable.button.paddle_one` 与
`key.fullscreen=controllable.button.paddle_three` 是**按不到的死绑定**——
上面的种子把 `key.sneak` 挪到了 RS。要用背键得在 HHD 里把它们映射成键盘按键。

### 构建前提

`compat/controllable` 需要编译期拿到 Controllable 的 jar：

```
tools/client-test/mods/controllable-forge-1.20.1-0.21.9.jar
tools/client-test/mods/framework-forge-1.20.1-0.8.0.jar
```

这两个 jar 被仓库根 `.gitignore` 的 `*.jar` 规则覆盖，**不会随仓库分发**，所以
新 clone 之后必须先补一次。只编译、不启动客户端时用：

```bash
./tools/client-test/fetch-deps.sh          # 缺什么下什么
./tools/client-test/fetch-deps.sh --force  # 强制重下
```

脚本会校验 SHA-256，失败时以非零码退出并说明原因。它只写
`tools/client-test/mods/`，不碰 `run/mods`，也不会启动游戏。

`run-client.sh` 仍保留相同的下载逻辑（它会顺带准备 `run/mods` 并启动客户端），
两者可以互相替代。若两个 jar 都缺失，Gradle 会直接报
`Could not find superbwarfare.test:controllable-forge-1.20.1:0.21.9`。
