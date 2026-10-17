# 履带接触回归

实际测试结果和未验收范围见 [2026-10-16 验证记录](RESULTS-2026-10-16.md) 与
[2026-10-15 验证记录](RESULTS-2026-10-15.md)。

当前目标：**履带载具**采用左右两侧的探针悬挂（每个负重轮一个支撑探针，力沿世界竖直施加在轮心），
刚性整侧底板已退役；≤1 格高差由地形楔面坡道处理，探针在原始扫掠落空时可回退到
`LocalHeightField` 平滑面（仅当原始列高与低侧列高相差 ≤1 格；更高崖面保持原始碰撞）。
牵引为库仑封顶的显式力（滑移刚度趋近履带面目标速度、侧向同预算抑制、滑行只损失滚阻），
零载荷零力。视觉仍按整侧统一高度；履带轨迹动画预处理不变。
WHEEL 独立轮悬挂保留但当前停放（`GroundVehicleAllowlist` 只注册 8 台履带车，
其余载具不可获取且物品栏不可见）。

坐标：`+X` 左，`+Y` 上，`+Z` 前。现有模型转换为 `(x, y, -z) / 16`。
`Radius` 为负重轮半径，`ContactRadius` 可显式计入履带板接触厚度，默认等于半径。
不要把悬挂臂或同轮位装饰子骨骼计入轮数、半径。

视觉继续采用简化的预处理方案：`TrackSuspensionProfile` 在创建时确定
上下行程与可视高度区间（含缩放/限幅断点），整侧轮组共用本侧高度；
`TrackAnimationProfile` 预采样车型已有履带轨迹，运行时按相位查表插值，
不求解链节刚体、张紧或绕轮变形。Bedrock 与 Geckolib 均接入；
履带根骨骼只加一次本侧高度，子履带旋转骨骼不能重复加高度。
WHEEL 保留独立悬挂。此切换仍在验证，历史逐轮结果不能当作新方案通过。

TRACK 整侧悬挂参数（车辆 `Physics` 配置，单位不与旧 WHEEL 混用）：

- `TrackCompressionDampingRatio=.65`、`TrackReboundDampingRatio=.45`：显式整侧簧载模态
  阻尼比，按 `c=2*zeta*m_side*(2*pi*frequency)` 转为原生 N·s/m。
  无隐藏最低阻尼；设置 0 确实禁用该项阻尼。求解前仍有 reduced-mass / dt 的稳定上限，
  因此输入比值不等于所有工况下的实际模态比值。
- `TrackMaxSuspensionForce=1000000`：每侧弹簧／阻尼行的合并上限 N，不按车重偷偷提高。
  默认可覆盖 100 吨车型的约 45 万 N/侧静态簧载；过低配置将真实下沉至行程限位，
  不能把行程限位或地形碰撞冲量也解释为受此值限制。
- `SuspensionFrequency`、上下行程、防倾仍共享配置；`SuspensionCompressionDamping` /
  `SuspensionReboundDamping` 和 `SuspensionMaxForce` 保留 WHEEL 旧路径，不再用于 TRACK。

这些参数改变后的整车成绩必须独立记录，不能沿用 `.8` 隐含下限版本的 7/8 结果。

当前状态：核心测试 **60/60 通过**（含两个多层表面规划测试与修复后保留的 mesh 跨度用例），
mesh 双侧缘/俯仰载荷回归与整车分开记录。Box 与坡面地形通过与 mesh 记录分开；
不增加 margin、填洞或复用过期接触来通过检查。

静态数据检查：

```bash
python3 tools/physics/check_track_layouts.py
```

原生 GameTest（Forge 1.20.1 / JDK 17，固定 1/60 秒）：

```bash
./gradlew runGameTestServer -PgameTestNamespaces=superbwarfare_physics \
  -PclientTestMods=false \
  --offline --console=plain --no-daemon \
  -Dnet.minecraftforge.gradle.check.certs=false \
  -Dorg.gradle.java.installations.auto-download=false \
  -Dorg.gradle.java.installations.paths=/home/kaguya/.jdks/jdk-17
```

八种履带车各测四个朝向。必须真实接地、连续稳定后再观察 600 个物理步；
观察期水平位移 ≤0.05 格，水平速度 ≤0.02 m/s。地形未就绪不算接地。
每个物理子步记录最大位移/速度、最少承载侧数和地形丢失，不能仅抽查 MC tick。
稳定阶段最多等待 1800 个实际物理子步；GameTest 的 MC tick 超时仅作为看门狗。
观察期在负重轮所在段内至少十次切换碰撞等价的 STONE/DIRT，验证无关方块状态变化
不会丢失承载；不关闭随机刻、不放宽地形丢失或稳定断言。
随后验证无人车响应外部冲量、连续面跨越局部小洞，以及整侧挖空后该侧卸载、另一侧真实承载。
夹具在测试服务器的独立远端区域建造，只在专用测试世界运行。
Gradle 每次启动使用新的 `run/gametest-<UUID>` 世界，保留失败现场，不复用客户端世界。
测试批次结束（包括失败）清理已生成车辆并释放强加载区块；中途终止则由新世界隔离残留。

可用相同 seed 创建独立运行目录（不复制世界、不修改客户端配置，也不启动 Gradle）：

```bash
python3 tools/physics/prepare_seed_replay.py --seed 3837692264114305011 \
  --output-root "$PWD/run/physics-replays"
```

执行其输出的命令，通过 `-PgameTestRunDirectory=...` 使用新目录。
同 seed 仅保证生成条件相同，不保证线程调度、实体随机过程及失败子步一致。
`[track-test-environment]` 在失败时记录原净空中的水流/碰撞方块和上方落块；
不会清除它们或改判通过。若自然水流/落石进入试验区域，该轮仍如实失败，
不可直接等同于无外力平地漂移，也不能当作完整物理验收通过。

`-PphysicsTraceSteps=1200` 可为 GameTest 打开每辆车有上限的日志。
普通客户端/服务端 JVM 参数为 `-Dsuperbwarfare.physics.traceSteps=1200`，默认 0。
连续面方案修正前的整车同 seed 三轮均为 **0/8**；原生隔离通过不代表整车通过。
顶层凸 Box 地形修正后的首轮为 **7/8**：七款完成四朝向各 600 子步及挖洞等检查，
Prism 90° 捕获流体外部冲量后失败，仍记失败；此成绩不覆盖后续阻尼参数修改。
显式整侧阻尼参数版已独立复测 **7/8**，同为 Prism 90° 捕获流体冲量后失败；
七款完成四朝向各 600 子步，最大位移 `.002353` 格、最大速度 `.007921 m/s`。
本轮地形缓存峰值 33043 个 Box、激活峰值 5450 个；有 trace 的采样不能充当性能基准。
具体失败世界、诊断与尚未验证项见 `RESULTS-2026-10-15.md` 末节。

2026-10-16 探针悬挂版：8 台履带四朝向静置/挖空 + BMP 一格坡 + 双格墙拒绝共 **10 项全绿**
（两轮独立复测，第 6 轮为清理前终验；核心 60/60）。探针形状、高度场门控与迭代失败记录见
[2026-10-16 验证记录](RESULTS-2026-10-16.md)。
日志前缀 `[vehicle-physics]`，包括实体标识、帧、reset/terrain 事件、局部速度、
旋转、驱动命令、总力/力矩和每侧接触/行程/支撑/原生接触数量。
`pre-step` 在外部冲量出队前，`pre-contact` 在外部冲量处理后、悬挂/摩擦前，
`forces` 在自定义悬挂/摩擦后，`step` 在 Bullet 子步后；结合来源栈区分速度变化阶段。
追踪上限按日志样本数而非完整物理步数计算；四阶段全开时 3,600 样本约覆盖 900 子步，
初次地形等待和 reset 等事件也占样本，不能把日志覆盖长度当成测试观察长度。
`-PterrainTraceUpdates=2000` 可额外记录缓存范围内最多 2,000 次方块新旧状态，默认关闭。
地形缓存汇总的 `unchangedCollisions` 统计方块状态变动但该方块碰撞盒未变的次数；
真实几何变化仍立即失效，一格高差坡面只在原始几何满足全部条件（低侧连续实地、
坡顶落脚面、三格水平过渡、过渡空间无阻挡、邻接区块已加载）时生成，否则保留原始碰撞。
开启物理追踪时，`[vehicle-impulse]` 还会记录每车最多 64 次非微小外部冲量的调用栈。
`[track-surface-loss]` 限量记录稳定阶段底板丢失承载时的原生流形、运动和邻近地形，
这些日志只用于诊断，不提供回退接触或额外支撑。
每侧日志包含接触刚体名称，供区分悬挂/摩擦自发失稳与外部运动输入。
可用 `python3 tools/physics/summarize_track_trace.py run/logs/latest.log` 汇总每车实际采样帧、
局部速度、承载轮数、地形失效原因和测试消息。必须同时核对非零完成数量及最终成功/失败摘要；
日志截断、零测试或仅有完成消息不能算通过，最终以子步观察断言为准。

通过这些测试不等于坡道、移动平台、驾驶、生命周期、网络和真实客户端全部验收。
不应通过锁死无人车、清零水平速度或加大全局阻尼让测试通过。
