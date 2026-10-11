# SuperbWarfare 载具物理交接文档

更新时间：2026-10-11

这份文档是当前工作树的交接入口。目标是让下一位模型可以直接恢复上下文、复现问题并继续实现，不需要重新猜测本轮设计意图。

## 1. 用户目标

载具的姿态、碰撞、悬挂、动力、重力、升力和起飞过程应由 Spark/Bullet 物理系统负责。SBW 原版逻辑只保留必要的游戏规则和输入/能量 bookkeeping，不再直接控制物理姿态。

必须满足的行为：

- 载具放置后立即进入 Bullet 动力学，不因为地形缓存异步加载而冻结或延迟物理化。
- 物理碰撞形状与载具模型/OBB 严格对齐，不能出现明显悬空、穿模或碰撞外扩。
- 轮式和履带式载具使用左右两组整体悬挂；不把导动轮、负重轮、履带拆成互相独立的物理车轮。
- 履带相位绑定到履带根骨骼/履带动画变量。履带根、履带滚动子骨骼和轮组必须一起移动，悬挂回弹不能把模型撕开。
- 履带差速转向左右方向正确：右转时左侧接触组速度更高，右侧更低；左转相反。
- 地面载具高速行驶不能因为错误支撑力、碰撞形状或姿态控制而车头抬起起飞。
- 飞行载具放置后不能自动原地升天；直升机/飞艇只有在发动机、乘员和上升输入满足条件时才产生升力，固定翼升力随速度和油门计算。
- 载具姿态不能再由 SBW 原版的直接角度写入驱动；Bullet 的旋转和角速度是唯一物理来源。

## 2. 当前实现状态

### 已落地

1. `src/main/kotlin/com/atsuishio/superbwarfare/physics/` 已建立独立物理层。
2. `VehiclePhysics.buildSpec()` 在游戏线程捕获数据，在物理线程创建 Bullet 对象；不把 Minecraft `Entity` 或 `Level` 引用带入 worker。
3. 车体碰撞形状使用当前运行时 OBB 转回车体局部坐标的 `CompoundCollisionShape`。每个 Box 的 Bullet margin 设为 `0.001f`，保留车体-地形连续碰撞边界，并启用 CCD。
4. 物理体创建后立即保持 dynamic。Spark 地形尚未 ready 时不会清空速度或改成非物理预览；terrain ready 后继续同一个 Bullet body 的运动。
5. native Bullet space gravity 被关闭，`VehicleBody.preStep()` 对每个固定子步显式施加 `mass * gravity` 的重力，避免 body 插入时重复重力或重力状态被 Spark 覆盖。
6. `resolveSuspensionSides()` 为每个地面载具生成 LEFT/RIGHT 两个整体接触组。悬挂探针是薄 Box sweep，弹簧/压缩阻尼/回弹阻尼/防倾杆由 Spark `PhysicsSuspensionSolver` 和当前配置驱动。
7. 悬挂在同一 physics substep 内先于驱动采样，避免刚放置的第一帧没有接地而丢失牵引力。
8. 地面驱动力施加在左右接触组的力点上；转向通过差速力产生自然偏航，不使用人工立正或 SBW 原版姿态控制。
9. `updateTrackPhase()` 使用 Bullet 车体局部线速度和局部角速度，分别计算左右履带相位。客户端模型在 Bullet body ready 后优先读取该状态。
10. `VehicleModel` 与 `GeoVehicleRenderer` 对履带根骨骼各只施加一次悬挂视觉偏移；TrackRot 等子骨骼不重复补偿，避免履带分离。
11. 飞行控制只生成局部推力、显式直升机/飞艇升力、固定翼速度相关升力和角速度目标。放置时没有默认升力。
12. 已加入车辆 physics state 网络同步、保存/加载状态、客户端预测/渲染状态，以及可选 Valkyrien Skies 船体代理桥接。
13. 已加入 `VehiclePhysicsGameTests`，覆盖 ground body ready、落地、悬挂稳定、固定炮塔锚定、残骸落地和输入牵引。

### 主要文件

| 文件 | 职责 |
| --- | --- |
| `physics/VehiclePhysics.kt` | 生命周期、OBB 捕获、输入转换、服务端同步、实体姿态回写 |
| `physics/VehicleBody.kt` | Bullet body、重力、地形 ready、悬挂、地面驱动、飞行力、履带相位 |
| `physics/VehiclePhysicsSpec.kt` | worker 所需的不可变碰撞/悬挂规格 |
| `physics/VehiclePhysicsSettings.kt` | 重力、摩擦、阻尼、悬挂、履带相位和角速度限制 |
| `physics/VehiclePrediction.kt` | 客户端预测/同步入口 |
| `physics/VehiclePhysicsRenderState.kt` | 悬挂视觉偏移、力和履带相位 |
| `physics/VsVehicleBridge.kt` | 可选 Valkyrien Skies 船体碰撞代理和反作用力 |
| `client/model/entity/VehicleModel.kt` | SBW 模型骨骼中的履带/轮组悬挂和履带相位消费 |
| `client/renderer/entity/GeoVehicleRenderer.kt` | Gecko 载具模型中的同一渲染桥接 |
| `gametest/VehiclePhysicsGameTests.java` | 当前自动化验收用例 |

## 3. 当前验证结果

执行命令：

```bash
./gradlew compileKotlin compileJava --offline --console=plain --no-daemon
./gradlew runGameTestServer --offline --console=plain --no-daemon
git diff --check
```

结果：

- `compileKotlin compileJava`：通过。只有已有的 Java deprecation/processor warning。
- `git diff --check`：通过。
- GameTest：6 个测试中 3 个通过，3 个仍失败。

通过的测试：

- `everyGroundVehicleGetsReadyBody`
- `wheelChairSettlesWithoutBounceOrSpin`
- `fixedTurretStaysAnchored`

仍失败的测试（不能视为完成）：

- `wheelChairFallsOntoTerrain`：实体约停在 `y=-56.955`，测试扫描的 `floorTop=-59.0`，断言认为没有落到预期地面。
- `turretWreckFallsOntoTerrain`：同样的停留高度问题，约 `y=-56.955`，扫描地面为 `-59.0`。
- `inputAcceleratesWheelChair`：水平位移约 `0.166`，小于断言要求的 `0.5`。

这些失败说明当前物理路径尚未完成验收。下一模型应先复现并记录 Bullet body 中心、OBB min/max、terrain `isReady`、悬挂 sweep 命中、两侧 travel/force、`grounded`、线速度和 `DriveFrame`，再决定是测试地面计算错误、悬挂探针几何错误、地形缓存问题还是驱动参数问题。

## 4. 已知风险和不要误改的约束

- 当前工作树有大量与物理任务无关的已有修改。不要用 `git reset`、`git checkout` 或批量格式化清掉它们。
- `VehicleEntity.move()` 对 managed vehicle 会让 Bullet 接管移动；不要在实体 tick 里再次写入位置、速度或角度，否则会产生双重积分。
- `VehiclePhysics.control()` 会临时调用原版 action 以更新能量、发动机 bookkeeping 和输入状态，然后恢复实体变换，只把 `DriveFrame` 交给 Bullet。不要把原版 action 产生的 `setPos`/角度当作物理结果。
- `VehicleBody` 的 Bullet 对象只能在 Spark physics worker 访问。游戏线程只读不可变 spec、state 和 render state。
- 任何调试日志必须有明确上限，并在验证后删除；仓库中不应留下 `physics-probe`、逐帧 `physics-debug` 等日志。
- 不要重新拆分履带轮组为多个独立刚体。视觉悬挂偏移可以很小，但接触和支撑必须来自左右整体组。
- 不要通过给飞行载具永久向上力来“防止下坠”。直升机/飞艇上升力必须受输入和发动机状态门控，固定翼升力必须受速度门控。

## 5. 下一模型启动步骤

1. 先阅读本文件和 `docs/VEHICLE_PHYSICS_PLAN.md`。
2. 检查工作树，不覆盖已有修改：

   ```bash
   git status --short
   rg -n "physics-probe|physics-debug|debugContact|debugStep|debugReset" src || true
   ```

3. 重跑编译和 GameTest，确认失败是否稳定。
4. 只在 `VehicleBody` 中加入有上限的诊断，至少打印：
   `entity id/type, body position, body half, entity position, terrain ready, grounded, left/right hit, left/right travel, left/right force, velocity, command.leftDrive/rightDrive/targetSpeed`。
5. 先解决两个落地测试，再解决输入牵引；每次只改一个物理环节并重跑 GameTest。
6. 地面测试通过后，手动验证履带车辆左右转向、坡面/台阶、快速刹车和高速前进；随后验证轮式车。
7. 手动验证直升机、飞艇、固定翼的放置、悬停、上升、下降、加速起飞和失速/滑翔。
8. 删除诊断日志，运行编译、GameTest、`git diff --check`，再把剩余风险写回本文档。

## 6. 可直接交给下一模型的任务提示词

```text
你正在接手 SuperbWarfare 仓库中的 Spark/Bullet 载具物理实现。先阅读
docs/VEHICLE_PHYSICS_HANDOFF.md 和 docs/VEHICLE_PHYSICS_PLAN.md，不要重置或覆盖
工作树中与本任务无关的修改。

当前架构要求：Bullet/Spark 是载具姿态、碰撞、重力、悬挂、地面驱动、飞行推力/升力和
角速度的唯一物理来源；SBW 原版只保留输入、发动机、能量和武器规则。履带和轮组必须
以左右整体接触组处理，履带相位来自 Bullet 局部速度并绑定到履带根骨骼。右转时左侧
履带/轮组驱动更快，左转时右侧更快。飞行载具不能在放置后自动升力。

先运行：
./gradlew compileKotlin compileJava --offline --console=plain --no-daemon
./gradlew runGameTestServer --offline --console=plain --no-daemon

当前已知 GameTest 状态是 6 个中 3 个通过，以下 3 个仍失败：
wheelChairFallsOntoTerrain、turretWreckFallsOntoTerrain、inputAcceleratesWheelChair。
先用有上限的日志记录 Bullet body/OBB、terrain readiness、悬挂 sweep、travel/force、
grounded、速度和 DriveFrame，判断坐标/地形测试问题与真正物理问题，再做最小修复。禁止
用延时、直接 setPos、永久上升力或重新启用 SBW 姿态写入来掩盖问题。每个修复都要重新跑
编译和 GameTest，并在最终提交前删除调试日志。
```

