# Distant Vehicle Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Техника SBW видна на дистанции до ~1500 блоков (поверх LOD-террейна Voxy) через собственный S2C-канал снапшотов и клиентских «призраков».

**Architecture:** Сервер раз в N тиков шлёт каждому игроку авторитетный список снапшотов всей `VehicleEntity` в радиусе R. Клиентский менеджер держит локальные инстансы техники (НЕ добавленные в `ClientLevel`), интерполирует их между снапшотами и рендерит через `EntityRenderDispatcher` (→ существующий `SbmVehicleRenderer` с LOD-моделями) в `RenderLevelStageEvent.AFTER_ENTITIES`. Voxy не модифицируется.

**Tech Stack:** Kotlin, NeoForge 21.1.228 / MC 1.21.1, kotlinx-serialization payloads (существующая инфраструктура `NetworkRegistry.kt`).

**Спек:** `docs/superpowers/specs/2026-07-06-distant-vehicle-sync-design.md`

## Global Constraints

- Репозиторий: `SuperbWarfare-fork-PJM`, ветка `1.21`. Voxy НЕ трогаем.
- MC 1.21.1, NeoForge 21.1.228, Kotlin; исходники в `src/main/kotlin`.
- Конфиг: `distant_vehicle_sync_radius` default **1500**, range **0..10000** (0 = выключено); `distant_vehicle_sync_interval` default **10**, range **1..200** тиков.
- Юнит-тестов в репозитории нет — верификация каждой задачи: `./gradlew compileKotlin` (быстрая) и финальная `./gradlew build` + ручной тест по чеклисту Task 5.
- Сообщения коммитов — англ., в стиле репозитория (`feat: ...`), с трейлером `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.
- Все команды выполнять из корня репо: `cd "/home/liko/Разработка/NeoForge/!Curseforge Mods/SuperbWarfare-fork-PJM"`.

---

### Task 1: Конфиг-опции в VehicleConfig

**Files:**
- Modify: `src/main/kotlin/com/atsuishio/superbwarfare/config/server/VehicleConfig.kt` (после блока `VEHICLE_INFO_DISPLAY_DISTANCE`, ~строка 81)

**Interfaces:**
- Produces: `VehicleConfig.DISTANT_VEHICLE_SYNC_RADIUS` и `VehicleConfig.DISTANT_VEHICLE_SYNC_INTERVAL` — оба `ModConfigSpec.IntValue`-подобные значения, читаются `.get(): Int`. Используются в Task 3.

- [ ] **Step 1: Добавить две опции**

После блока `VEHICLE_INFO_DISPLAY_DISTANCE { ... }` добавить (тот же идиом `buildServerConfig`, что и соседние записи):

```kotlin
    @JvmField
    val DISTANT_VEHICLE_SYNC_RADIUS = buildServerConfig {
        comment("Radius (in blocks) within which vehicles are synced to clients beyond vanilla tracking range, 0 to disable")
        comment("超出原版同步范围后，载具向客户端同步的半径（方块），0为禁用")
        defineInRange("distant_vehicle_sync_radius", 1500, 0, 10000)
    }

    @JvmField
    val DISTANT_VEHICLE_SYNC_INTERVAL = buildServerConfig {
        comment("Interval (in ticks) between distant vehicle sync packets")
        comment("远处载具同步包的发送间隔（刻）")
        defineInRange("distant_vehicle_sync_interval", 10, 1, 200)
    }
```

- [ ] **Step 2: Проверить компиляцию**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/atsuishio/superbwarfare/config/server/VehicleConfig.kt
git commit -m "feat: add distant vehicle sync config options"
```

---

### Task 2: Пакет DistantVehiclesMessage + клиентский DistantVehicleManager

**Files:**
- Create: `src/main/kotlin/com/atsuishio/superbwarfare/network/message/receive/DistantVehiclesMessage.kt`
- Create: `src/main/kotlin/com/atsuishio/superbwarfare/client/DistantVehicleManager.kt`
- Modify: `src/main/kotlin/com/atsuishio/superbwarfare/network/NetworkRegistry.kt` (список `playToClient` в `registerPayloads()`, ~строка 93)

**Interfaces:**
- Consumes: `ClientPacketPayload`, `PayloadContext` (`network/PacketPayload.kt`); `VehicleEntity` (поля `turretYRot: Float`, `turretXRot: Float`, `turretYRotO: Float`, `turretXRotO: Float`, `skinId: String`); хелпер `mc` из `com.atsuishio.superbwarfare.tools`.
- Produces:
  - `@Serializable data class VehicleSnapshot(entityId: Int, type: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float, turretYRot: Float, turretXRot: Float, skinId: String)`
  - `@Serializable data class DistantVehiclesMessage(interval: Int, vehicles: List<VehicleSnapshot>) : ClientPacketPayload`
  - `DistantVehicleManager.handleMessage(msg: DistantVehiclesMessage)`
  - `DistantVehicleManager.ghosts(): Collection<DistantVehicleManager.Ghost>` где `Ghost` имеет поля `serverId: Int`, `entity: VehicleEntity`
  - Task 3 создаёт `VehicleSnapshot` на сервере; Task 4 читает `ghosts()`.

- [ ] **Step 1: Создать `DistantVehiclesMessage.kt`**

```kotlin
package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.DistantVehicleManager
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@Serializable
data class VehicleSnapshot(
    val entityId: Int,
    val type: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val turretYRot: Float,
    val turretXRot: Float,
    val skinId: String,
)

@Serializable
data class DistantVehiclesMessage(
    val interval: Int,
    val vehicles: List<VehicleSnapshot>,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        DistantVehicleManager.handleMessage(this@DistantVehiclesMessage)
    }
}
```

- [ ] **Step 2: Создать `DistantVehicleManager.kt`**

```kotlin
package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.DistantVehiclesMessage
import com.atsuishio.superbwarfare.network.message.receive.VehicleSnapshot
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent

/**
 * Держит клиентские "призраки" дальней техники (за пределами ванильного
 * tracking range). Инстансы НЕ добавляются в ClientLevel — только рендер.
 */
@EventBusSubscriber(Dist.CLIENT)
object DistantVehicleManager {

    class Ghost(val serverId: Int, val entity: VehicleEntity) {
        var targetX = entity.x
        var targetY = entity.y
        var targetZ = entity.z
        var targetYaw = entity.yRot
        var targetTurretY = entity.turretYRot
        var targetTurretX = entity.turretXRot
        var lerpSteps = 0
        var lastUpdate = 0L
        var interval = 10
    }

    private val ghostMap = LinkedHashMap<Int, Ghost>()
    private var cachedLevel: ClientLevel? = null
    private var tickCounter = 0L

    fun ghosts(): Collection<Ghost> = ghostMap.values

    fun handleMessage(msg: DistantVehiclesMessage) {
        val level = mc.level ?: return
        if (level !== cachedLevel) {
            ghostMap.clear()
            cachedLevel = level
        }

        val seen = HashSet<Int>(msg.vehicles.size)
        for (snapshot in msg.vehicles) {
            seen += snapshot.entityId
            val ghost = ghostMap[snapshot.entityId]
                ?: createGhost(level, snapshot)?.also { ghostMap[snapshot.entityId] = it }
                ?: continue

            ghost.interval = msg.interval
            ghost.lastUpdate = tickCounter
            ghost.targetX = snapshot.x
            ghost.targetY = snapshot.y
            ghost.targetZ = snapshot.z
            ghost.targetYaw = snapshot.yaw
            ghost.targetTurretY = snapshot.turretYRot
            ghost.targetTurretX = snapshot.turretXRot
            ghost.lerpSteps = msg.interval
            ghost.entity.skinId = snapshot.skinId
        }
        // Пакет авторитетный: чего нет в списке — того больше нет в радиусе
        ghostMap.keys.retainAll(seen)
    }

    private fun createGhost(level: ClientLevel, snapshot: VehicleSnapshot): Ghost? {
        val typeId = ResourceLocation.tryParse(snapshot.type) ?: return null
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)) return null
        val entity = BuiltInRegistries.ENTITY_TYPE.get(typeId).create(level) as? VehicleEntity ?: return null

        entity.moveTo(snapshot.x, snapshot.y, snapshot.z, snapshot.yaw, snapshot.pitch)
        entity.yRotO = snapshot.yaw
        entity.xRotO = snapshot.pitch
        entity.turretYRot = snapshot.turretYRot
        entity.turretYRotO = snapshot.turretYRot
        entity.turretXRot = snapshot.turretXRot
        entity.turretXRotO = snapshot.turretXRot
        entity.skinId = snapshot.skinId
        return Ghost(snapshot.entityId, entity)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val level = mc.level
        if (level == null || level !== cachedLevel) {
            ghostMap.clear()
            cachedLevel = level
            return
        }
        tickCounter++

        val iterator = ghostMap.values.iterator()
        while (iterator.hasNext()) {
            val ghost = iterator.next()
            if (tickCounter - ghost.lastUpdate > ghost.interval * 3L) {
                iterator.remove()
                continue
            }
            tickGhost(ghost)
        }
    }

    private fun tickGhost(ghost: Ghost) {
        val entity = ghost.entity
        entity.xo = entity.x
        entity.yo = entity.y
        entity.zo = entity.z
        entity.yRotO = entity.yRot
        entity.turretYRotO = entity.turretYRot
        entity.turretXRotO = entity.turretXRot
        entity.tickCount++

        if (ghost.lerpSteps > 0) {
            val steps = ghost.lerpSteps.toDouble()
            entity.setPos(
                entity.x + (ghost.targetX - entity.x) / steps,
                entity.y + (ghost.targetY - entity.y) / steps,
                entity.z + (ghost.targetZ - entity.z) / steps,
            )
            entity.yRot += Mth.wrapDegrees(ghost.targetYaw - entity.yRot) / ghost.lerpSteps
            entity.turretYRot += Mth.wrapDegrees(ghost.targetTurretY - entity.turretYRot) / ghost.lerpSteps
            entity.turretXRot += (ghost.targetTurretX - entity.turretXRot) / ghost.lerpSteps
            ghost.lerpSteps--
        }
    }

    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        ghostMap.clear()
        cachedLevel = null
    }
}
```

- [ ] **Step 3: Зарегистрировать пакет**

В `NetworkRegistry.kt`, в `registerPayloads()`, после `playToClient<OpenVehicleSkinScreenMessage>()` добавить строку:

```kotlin
    playToClient<DistantVehiclesMessage>()
```

(импорт `network.message.receive.*` уже есть — wildcard, строка 4).

- [ ] **Step 4: Проверить компиляцию**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL. Если `entity.tickCount++` не компилируется (в маппингах поле может быть без сеттера из Kotlin) — заменить на `entity.tickCount = entity.tickCount + 1`; если и это недоступно, строку удалить (для LOD-рендера некритична).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/atsuishio/superbwarfare/network/message/receive/DistantVehiclesMessage.kt \
        src/main/kotlin/com/atsuishio/superbwarfare/client/DistantVehicleManager.kt \
        src/main/kotlin/com/atsuishio/superbwarfare/network/NetworkRegistry.kt
git commit -m "feat: add distant vehicle sync packet and client-side ghost manager"
```

---

### Task 3: Серверный DistantVehicleTracker

**Files:**
- Create: `src/main/kotlin/com/atsuishio/superbwarfare/event/DistantVehicleTracker.kt`

**Interfaces:**
- Consumes: `VehicleConfig.DISTANT_VEHICLE_SYNC_RADIUS.get(): Int`, `VehicleConfig.DISTANT_VEHICLE_SYNC_INTERVAL.get(): Int` (Task 1); `VehicleSnapshot`, `DistantVehiclesMessage` (Task 2); `sendPacketTo(player, payload)` из `tools/MinecraftUtil.kt`; `VehicleEntity` (`turretYRot`, `turretXRot`, `skinId`).
- Produces: ничего для других задач (терминальный серверный компонент).

- [ ] **Step 1: Создать `DistantVehicleTracker.kt`**

```kotlin
package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.DistantVehiclesMessage
import com.atsuishio.superbwarfare.network.message.receive.VehicleSnapshot
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.ServerTickEvent

/**
 * Раз в distant_vehicle_sync_interval тиков шлёт каждому игроку авторитетный
 * список техники в радиусе distant_vehicle_sync_radius. Дедупликация с
 * ванильным трекингом — на клиенте (см. DistantVehicleRenderer).
 */
@EventBusSubscriber
object DistantVehicleTracker {

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        val radius = VehicleConfig.DISTANT_VEHICLE_SYNC_RADIUS.get()
        if (radius <= 0) return
        val interval = VehicleConfig.DISTANT_VEHICLE_SYNC_INTERVAL.get()
        val server = event.server
        if (server.tickCount % interval != 0) return

        val radiusSq = radius.toDouble() * radius

        for (level in server.allLevels) {
            val players = level.players()
            if (players.isEmpty()) continue

            val vehicles = level.getAllEntities().filterIsInstance<VehicleEntity>()

            for (player in players) {
                val snapshots = vehicles.asSequence()
                    .filter { vehicle ->
                        val dx = vehicle.x - player.x
                        val dz = vehicle.z - player.z
                        dx * dx + dz * dz <= radiusSq
                    }
                    .map { vehicle ->
                        VehicleSnapshot(
                            entityId = vehicle.id,
                            type = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.type).toString(),
                            x = vehicle.x,
                            y = vehicle.y,
                            z = vehicle.z,
                            yaw = vehicle.yRot,
                            pitch = vehicle.xRot,
                            turretYRot = vehicle.turretYRot,
                            turretXRot = vehicle.turretXRot,
                            skinId = vehicle.skinId,
                        )
                    }
                    .toList()

                // Пустой список тоже шлём: он авторитетно чистит призраков на клиенте
                sendPacketTo(player, DistantVehiclesMessage(interval, snapshots))
            }
        }
    }
}
```

- [ ] **Step 2: Проверить компиляцию**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL. Возможные правки по маппингам: `vehicle.type` → `vehicle.getType()`; `level.getAllEntities()` → если метода нет на `ServerLevel` в этих маппингах, использовать `level.entities.all` через `level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(VehicleEntity::class.java)) { true }`.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/atsuishio/superbwarfare/event/DistantVehicleTracker.kt
git commit -m "feat: add server-side distant vehicle tracker broadcasting snapshots"
```

---

### Task 4: Клиентский рендер призраков (DistantVehicleRenderer)

**Files:**
- Create: `src/main/kotlin/com/atsuishio/superbwarfare/client/renderer/DistantVehicleRenderer.kt`

**Interfaces:**
- Consumes: `DistantVehicleManager.ghosts(): Collection<Ghost>` (Task 2), поля `Ghost.serverId: Int`, `Ghost.entity: VehicleEntity`; хелпер `mc`.
- Produces: ничего (терминальный рендер-компонент).

- [ ] **Step 1: Создать `DistantVehicleRenderer.kt`**

Ключевые моменты: рендерим ТОЛЬКО призраков, чей `serverId` отсутствует в `ClientLevel` (дедупликация с ванильным трекингом); свой frustum-cull; свет — полный skylight (данных чанка вдали нет); туман отключается на время рендера, причём `endBatch()` обязан быть ДО восстановления тумана (draw происходит при флаше буфера).

```kotlin
package com.atsuishio.superbwarfare.client.renderer

import com.atsuishio.superbwarfare.client.DistantVehicleManager
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.LightTexture
import net.minecraft.util.Mth
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent

@EventBusSubscriber(Dist.CLIENT)
object DistantVehicleRenderer {

    private val FULL_SKY_LIGHT = LightTexture.pack(0, 15)

    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage !== RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
        val level = mc.level ?: return
        val ghosts = DistantVehicleManager.ghosts()
        if (ghosts.isEmpty()) return

        val partialTick = event.partialTick.getGameTimeDeltaPartialTick(false)
        val camPos = event.camera.position
        val poseStack = event.poseStack
        val bufferSource = mc.renderBuffers().bufferSource()
        val dispatcher = mc.entityRenderDispatcher
        val frustum = event.frustum

        // Призраки за пределами ванильного тумана — растягиваем его на время рендера
        val fogStart = RenderSystem.getShaderFogStart()
        val fogEnd = RenderSystem.getShaderFogEnd()
        RenderSystem.setShaderFogStart(Float.MAX_VALUE)
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE)

        var rendered = false
        for (ghost in ghosts) {
            // Техника вошла в ванильный tracking range — её рендерит ваниль
            if (level.getEntity(ghost.serverId) != null) continue

            val entity = ghost.entity
            if (frustum != null && !frustum.isVisible(entity.boundingBox.inflate(3.0))) continue

            val x = Mth.lerp(partialTick.toDouble(), entity.xo, entity.x)
            val y = Mth.lerp(partialTick.toDouble(), entity.yo, entity.y)
            val z = Mth.lerp(partialTick.toDouble(), entity.zo, entity.z)
            val yaw = Mth.lerp(partialTick, entity.yRotO, entity.yRot)

            dispatcher.render(
                entity,
                x - camPos.x, y - camPos.y, z - camPos.z,
                yaw, partialTick, poseStack, bufferSource, FULL_SKY_LIGHT,
            )
            rendered = true
        }

        if (rendered) {
            // Флашим до восстановления тумана: draw call происходит здесь
            bufferSource.endBatch()
        }
        RenderSystem.setShaderFogStart(fogStart)
        RenderSystem.setShaderFogEnd(fogEnd)
    }
}
```

- [ ] **Step 2: Проверить компиляцию**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL. Возможные правки по NeoForge API: `event.frustum` → если геттер называется иначе, проверить `RenderLevelStageEvent` в NeoForge 21.1 (поле `frustum`, может быть nullable); `event.camera` — геттер `getCamera()`.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/atsuishio/superbwarfare/client/renderer/DistantVehicleRenderer.kt
git commit -m "feat: render distant vehicle ghosts beyond vanilla tracking range"
```

---

### Task 5: Полная сборка и ручная проверка

**Files:**
- Никаких новых файлов; правки только если ручной тест выявит дефекты.

**Interfaces:**
- Consumes: всё из Task 1–4.

- [ ] **Step 1: Полная сборка**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Ручной тест (dev-клиент + dev-сервер, Voxy на клиенте)**

Чеклист (из спека):
1. Заспавнить танк (например `/summon superbwarfare:t_90a`), отлететь на 1000+ блоков: техника видна поверх LOD-террейна Voxy, LOD-моделью, не «съедена» туманом.
2. Второй клиент двигает технику/крутит башню — позиция и башня у первого обновляются плавно (~2 обновления/с при interval=10).
3. Вернуться ближе ванильного tracking range: дубликата нет (призрак скрыт, ваниль рендерит реальную технику).
4. Уничтожить технику вдали (другим клиентом): призрак исчезает не позже 3 интервалов (~1.5 с).
5. Выставить `distant_vehicle_sync_radius = 0` в серверном конфиге: пакеты не шлются, призраков нет.
6. Смена измерения и релог: призраки очищаются.
7. Регрессия: ближний рендер техники, посадка/стрельба — без изменений.

- [ ] **Step 3: Зафиксировать результат**

Если дефекты найдены — исправить, повторить сборку и тест, закоммитить исправления (`fix: ...`). Затем финальный коммит документации, если менялся спек/план:

```bash
git add docs/
git commit -m "docs: update distant vehicle sync spec/plan after manual testing"
```
