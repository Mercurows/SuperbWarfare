# 载具物理后续实施计划

这份计划按风险从高到低排列。当前实现已经覆盖大部分架构，但自动化测试仍未全绿，因此后续模型应按阶段推进，不要同时调大量参数。

## 阶段 0：恢复和基线

目标：确认接手者看到的是同一个失败。

- 阅读 `VEHICLE_PHYSICS_HANDOFF.md`。
- 保存 `git status --short`，不清理无关改动。
- 编译并运行 6 个 GameTest。
- 记录当前失败的测试名、实体坐标、测试地面高度和运行时长。
- 确认没有逐帧调试日志留在源码中。

完成标准：失败结果与交接文档一致，或者文档被更新为新的可复现结果。

## 阶段 1：碰撞坐标和地面缓存

目标：让 Bullet body 的底部和真实模型/地形在同一坐标系中，并保证放置即物理化。

检查顺序：

1. 在 `VehiclePhysics.buildSpec()` 记录一次性数据：`entity.position`、`pivot`、`localCenter`、`bodyWorldCenter`、`half`、所有 `CollisionPart.center/half/rotation`。
2. 在 `VehicleBody.preStep()` 记录前 20 个 physics frame：body center、body AABB、`terrainManager.isReady(readyBox)`、velocity、是否进入 `terrainWaiting`。
3. 检查 `VehiclePhysics.resolveCollisionParts()` 的 runtime OBB 顺序是否与 `data.obb` 一致。模型动画/自定义旋转应只影响运行时 OBB 的方向和中心，不应产生额外位置偏移。
4. 检查 `resolveSuspensionSides()` 的 wheel/OBB 数据映射。特别是 `data.obb` 的 `position.x/y/z`、`size.x/y/z` 到 `anchorX/bottomY/centerZ/halfWidth/halfLength` 的顺序，必须与 `OBBInfo` 定义一致。
5. 检查 GameTest 的 floorTop 扫描是否把 structure block、模板残留层和新铺设石块混在一起。测试地面应是明确的单一平面，不能用错误的扫描值掩盖物理高度问题。

禁止事项：

- 不通过 `setPos` 把实体强行放到测试断言高度。
- 不在 terrain 未 ready 时把 body 改为 kinematic 或把速度永久清零。
- 不扩大碰撞 shape margin 来“防止穿透”。优先修正坐标和 terrain activation。

完成标准：`wheelChairFallsOntoTerrain` 和 `turretWreckFallsOntoTerrain` 稳定通过；放置后第一段日志显示 dynamic body、显式重力和 terrain ready 过渡正常。

## 阶段 2：整体侧悬挂

目标：轮式/履带式车辆各自拥有左右一个整体悬挂接触组，视觉回弹小且不穿模。

- 每侧只保留一个 thin Box sweep probe，覆盖该侧轮组/履带的纵向 footprint。
- 弹簧力、压缩/回弹阻尼和防倾杆都施加在该侧的整体相对力点上。
- 物理车体仍由 chassis CompoundCollisionShape 负责连续碰撞，probe 不能成为唯一防穿透手段。
- `SuspensionVisualScale` 和 `SuspensionVisualMaxOffset` 只控制渲染偏移，不改变 Bullet 车体位置。
- 车辆倾斜时按 chassis up 与接触法线点积判断支撑，不能简单只看世界 Y。
- 坡面和单侧悬空时验证另一侧不会被错误防倾杆力抬飞。

完成标准：轮式和履带车辆静置后上下回弹收敛；左右轮组/履带仍附着在同一个车体模型上；没有单轮独立刚体穿出车体的现象。

## 阶段 3：地面驱动和姿态稳定

目标：解决输入位移不足、车头上抬、错误转向和高速起飞。

先检查数据流：

- `VehiclePhysics.control()` 是否读到乘员、能量、输入和 `power`。
- `DriveFrame.engineActive` 是否只在合法发动机状态下为 true。
- `targetSpeed` 的单位是否与 Bullet 使用的 blocks/sec 一致，不要重复乘 tick rate。
- `applyGroundDrive()` 的 traction clamp 是否使用当前有效法向载荷，而不是只用静态质量重力。
- 左右力点的 `bottomY`、`centerZ` 是否在车体局部坐标，力点是否相对 Bullet body center。

然后逐项验证：

1. 直线前进：车体不抬头，垂直速度保持接近零。
2. 直线刹车/倒车：没有反向弹飞。
3. 右转：左侧力更大，车体向右偏航。
4. 左转：右侧力更大，车体向左偏航。
5. 高速越过低台阶：只有真实碰撞/悬挂作用，不用人工 step assist 代替整体碰撞。

如果输入测试仍只有约 `0.166` 位移，应先打印 `DriveFrame`、body local velocity 和两侧 force，再决定是输入没有持续、能量被消耗、悬挂未接地还是 traction 参数过小。

完成标准：`inputAcceleratesWheelChair` 通过；至少一台履带车和一台轮式车完成直线/左右转手动验收；不再出现高速车头抬起起飞。

## 阶段 4：履带动画和模型绑定

目标：消除履带分离，确保悬挂上下时履带相位连续。

- 只从 `VehiclePhysicsRenderState.leftTrackPhase/rightTrackPhase` 给履带动画相位。
- 相位使用车体局部前进速度叠加局部偏航角速度，左右侧符号与差速驱动一致。
- `TrackL/TrackR` 根骨骼各只做一次整体 Y 偏移。
- TrackRot、负重轮和导动轮不得再次使用同一悬挂偏移补偿。
- 相位必须在 body ready 后接管；未 ready 时才允许短暂使用原版动画作为占位。
- 当车辆悬挂回弹时，根骨骼和其所有子骨骼一起移动，禁止分别修改履带网格片段的世界位置。

完成标准：静止、前进、倒车、原地转向、单侧压坡五种状态下履带连续；截图中履带、导动轮和负重轮没有肉眼可见分离。

## 阶段 5：飞行载具

目标：放置稳定，升力和姿态完全由 Bullet 控制。

- 直升机/飞艇：只在 `engineActive && upInput` 时产生显式上升力；悬停所需力应逐步逼近质量重力，不应默认大于重力。
- 固定翼：前向推力由发动机和前进输入产生；升力由局部前向速度、油门和升力曲线计算，低速放置时不得产生升力。
- 所有飞行器的姿态使用目标角速度和 Bullet torque，不直接把 `xRot/yRot/roll` 写成控制结果。
- 失去乘员或能量耗尽时，只保留重力和当前惯性，不应继续自动升空。
- 服务端为权威，客户端只插值状态，不在客户端独立改变飞行力。

完成标准：直升机放置后下落/停留符合输入；飞艇不会无输入升天；固定翼有足够速度和油门才起飞；姿态改变与速度、角速度连续。

## 阶段 6：兼容和持久化

- 检查 Valkyrien Skies 存在时的船体代理只负责船体碰撞反作用，不与载具 Bullet body 重复施加重力。
- 检查世界卸载、实体移除、重载和 NBT `SparkPhysics` 状态恢复。
- 检查服务端/客户端 body ready 时序，不能在客户端把临时预测状态写回服务端实体。
- 检查固定炮塔/固定武器仍保持锚定，不受重力或输入影响。

完成标准：重载和卸载无幽灵 body、无重复代理、无物理线程访问 Minecraft 对象。

## 阶段 7：最终验收

自动化：

```bash
./gradlew compileKotlin compileJava --offline --console=plain --no-daemon
./gradlew runGameTestServer --offline --console=plain --no-daemon
git diff --check
rg -n "physics-probe|physics-debug|debugContact|debugStep|debugReset" src || true
```

手动验收矩阵：

| 类别 | 最少场景 |
| --- | --- |
| 放置 | 平地、斜坡、空中放置、地形刚加载 |
| 轮式 | 静止回弹、直线、高速刹车、左右转、低台阶 |
| 履带 | 静止回弹、直线、倒车、原地左右转、单侧压坡、履带相位 |
| 飞行 | 直升机无输入、上升/下降、飞艇、固定翼滑跑/起飞/失速 |
| 碰撞 | 车体、侧面、车头、坡面、其他物理实体、VS 船体 |
| 生命周期 | 生成、移除、区块卸载、存档重载、客户端同步 |

最终完成定义：所有 GameTest 通过；手动矩阵没有已知回归；调试日志已删除；交接文档更新为最终参数、已知限制和剩余风险。

## 外部参考

用户指定可参考以下实现，但只能借鉴接触/高度差处理思路，不能直接绕过当前 Spark/Bullet 所有权边界：

- Machine-Max：<https://github.com/Sweetzonzi/Machine-Max>
- ywzj_vehicle：<https://github.com/YWZJ-Studio/ywzj_vehicle>

