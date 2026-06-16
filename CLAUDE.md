# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Язык работы:** общение с пользователем — на русском. Код, идентификаторы и существующие
> комментарии (часто на китайском) сохраняются как есть.

---

## ⚠️ ГЛАВНОЕ ПРАВИЛО ЭТОГО ФОРКА: совместимость с upstream SBW

Это **форк** `PJM-Project-Minecraft/SuperbWarfare-fork-PJM`. Upstream — официальный
**SuperbWarfare** (`github.com/Mercurows/SuperbWarfare`, зеркало `gitee.com/atsuishio/SuperbWarfare`).
Ветка `1.21` (NeoForge 1.21.1) активно развивается **в том числе самими авторами upstream**
(Atsuishio, Light_Quanta, 17146 коммитят прямо сюда), поэтому форк постоянно подтягивает их
изменения через merge/rebase.

**Любое изменение в этом репозитории ОБЯЗАНО оставаться совместимым с будущими
изменениями разработчиков SBW и не ломать возможность вливать upstream.** Это не пожелание,
а жёсткое ограничение. На практике:

1. **Расширяй, а не переписывай.** Добавляй новые классы/файлы/методы вместо правки
   существующих. Чем меньше тронуто upstream-файлов, тем меньше конфликтов при merge.
2. **Не делай косметических рефакторингов** upstream-кода (переименования, перестановка
   методов, переформатирование, смена сигнатур). Каждая такая правка превращается в конфликт.
3. **Сохраняй структуру.** Не двигай пакеты/файлы, не меняй имена классов, не меняй
   `mod_id` (`superbwarfare`) и базовый пакет (`com.atsuishio.superbwarfare`).
4. **Следуй конвенциям upstream** (см. ниже), а не вводи свои. Новый код должен быть
   неотличим по стилю от соседнего.
5. **Точечные правки в общих файлах** (`Mod.kt`, реестры `init/`, `mixins.superbwarfare.json`,
   `build.gradle.kts`): добавляй строки в конец списков/блоков, не переставляй существующие.
   Это минимизирует область конфликта.
6. **Точки расширения вместо хаков.** Для поведения используй существующие механизмы —
   Perk (`modifyProperty`), PropertyModifier-цепочку, события из `api/event/`, capabilities,
   data-driven JSON — вместо хардкода в core-классах.
7. **Изолируй чисто форковые фичи.** Специфичный для PJM код по возможности держи в
   отдельных пакетах/классах, чтобы его было легко переносить через upstream-обновления.
8. **Помечай форковые изменения** в общих файлах коротким комментарием (например
   `// PJM:` ) — чтобы при разрешении merge-конфликтов было видно, что чьё.
9. **При работе с upstream-кодом сверяйся с git-историей** (`git log`, `git blame`) — не
   удаляй и не «чини» то, что активно меняется в upstream, без явной необходимости.

Если задача требует серьёзной переделки upstream-кода — это сигнал остановиться и
обсудить подход, потому что цена в виде будущих merge-конфликтов высока.

---

## Что это за проект

SuperbWarfare (卓越前线) — масштабный военный мод для Minecraft: огнестрельное оружие
с системой характеристик/перков/обвесов, бронетехника и авиация, снаряды и взрывчатка,
JS-скриптинг.

- **Платформа:** NeoForge **1.21.1**, **Java 21**, загрузчик `kotlinforforge`.
- **Языки:** Kotlin (~70%, основная логика) + Java (~30%, легаси и часть data-слоя).
- **Сборка:** NeoForge ModDevGradle (MDG) `2.0.80`, Kotlin `2.1.20` (+ serialization), KSP.
- **Mappings:** Parchment `2024.11.17` (для 1.21.1).
- **Точка входа:** `src/main/kotlin/.../Mod.kt` (аннотация `@Mod`).
- **Версия артефакта:** `${mod_version}-mc${minecraft_version}` (см. `gradle.properties`).

> ⚠️ `README.md` / `README-en.md` всё ещё описывают **1.20.1 / Forge** — они не обновлены
> под эту ветку. Источник истины по версиям — `gradle.properties` и `build.gradle.kts`.
> Существует параллельная ветка `superbwarfare` — это старая линия **Forge 1.20.1** (другой
> `build.gradle.kts`); не путать с этой веткой.

---

## Команды

Используется Gradle wrapper (`./gradlew`). Демон и configuration-cache включены в
`gradle.properties`; JVM-аргументы запусков рассчитаны на **JetBrains Runtime (JBR)** ради
`AllowEnhancedClassRedefinition` (hot-swap при разработке).

| Задача | Команда |
|--------|---------|
| Быстрая компиляция без упаковки JAR (проверка ошибок) | `./gradlew devBuild` |
| Запуск клиента (зависит от `devBuild`) | `./gradlew runClient` |
| Запуск сервера (`--nogui`, зависит от `devBuild`) | `./gradlew runServer` |
| Генерация ресурсов (datagen) → `src/generated/resources/` | `./gradlew runData` |
| Прогон всех GameTest и выход | `./gradlew runGameTestServer` |
| Полная сборка + JAR (включая jar-in-jar) | `./gradlew build` |
| Только JAR | `./gradlew jar` |
| Публикация в локальный `repo/` | `./gradlew publish` |

- **Рабочий цикл разработки:** `devBuild` (убедиться, что компилируется) → `runClient`/`runServer`.
  `devBuild` — единая точка компиляции, на которой завязаны run-задачи.
- **Тесты — это NeoForge GameTests**, а не JUnit. Неймспейс включён через
  `neoforge.enabledGameTestNamespaces=superbwarfare`. Запуск всех: `runGameTestServer`;
  внутри запущенной игры — команда `/test`. Юнит-тестов в проекте нет.
- **После изменения datagen-кода** (`datagen/`) перегенерируй ресурсы через `runData` и
  закоммить результат из `src/generated/resources/`.
- **При правке `gradle.properties`/`build.gradle.kts` или зависимостей** перезапусти
  Gradle sync — задача `generateModMetadata` подставляет версии в манифест и привязана к
  ide-sync.

---

## Кодогенерация (KSP) — обязательно к пониманию

В проекте есть **отдельный Gradle-подмодуль `:ksp`** (`ksp/`, `settings.gradle.kts:
include(":ksp")`) — собственный KSP-процессор.

- Аннотация `@GenerateMapCodec` (`com.atsuishio.superbwarfare.ksp.annotation`) на Kotlin-классе.
- Процессор генерирует файл `<ClassName>GeneratedCodec.kt` с extension-свойством
  `ClassName.Companion.CODEC: MapCodec<ClassName>`, собранным из параметров первичного
  конструктора (поддержка 1–8 параметров; типы: примитивы, `String`, `BlockPos`, `Ingredient`).
- Сгенерированные файлы **править вручную нельзя** (помечены `// 自动生成文件，请勿手动更改`).
- Чтобы класс получил `CODEC`, у него должен быть `companion object` и конструктор из
  поддерживаемых типов; новые типы добавляются в `Processor.generateCodec`.

---

## Архитектура: большая картина

Подробная карта пакетов и иерархий — в **`AGENT.md`** (на китайском); ниже — то, что
требует чтения нескольких файлов и важно понять заранее.

### 1. Property Modification Chain (PMC) — ядро системы оружия

Финальное значение характеристики оружия (`GunData.get(prop)`) вычисляется наложением
модификаторов **в строгом порядке**:

1. JSON-оверрайды в NBT;
2. модификатор уровня `GunItem` (`PropertyModifier`);
3. модификатор текущего режима огня (`FireMode`);
4. `AmmoConsumer`;
5. все надетые перки;
6. встроенные границы (`GunProp.modifyProperty`).

Чтобы менять поведение оружия — **встраивайся в эту цепочку** (перк, FireMode, AmmoConsumer),
а не правь расчёт напрямую. Ключевые файлы: `src/main/java/.../data/gun/GunData.kt` (~978 строк),
`DefaultGunData.kt`, `GunProp.kt`.

### 2. Data-driven шаблоны

Шаблоны оружия/техники грузятся из **JSON** через `CustomData` (глобальный реестр из
библиотеки `sbw-data`): `CustomData.GUN_DATA → DefaultGunData`, `GUN_RESOURCE →
DefaultGunResource` (модель/анимация), `VEHICLE_DATA → VehicleData`. Новое оружие/технику
определяй данными, а не хардкодом.

### 3. Состояние оружия в NBT ItemStack

Рантайм-состояние ствола (патроны, перки, обвесы, режим огня, нагрев, перезарядка) живёт в
NBT `ItemStack`. Доступ — через типизированные обёртки `IntValue` / `DoubleValue` /
`BooleanValue` / `StringEnumValue`. Получение инстанса: `GunData.from(stack)`; запись:
`save()`.

### 4. Иерархия сущностей

```
VehicleEntity (entity/vehicle/base/) — база техники: энергия, слоты оружия, сиденья,
  │   инвентарь, OBB-коллизии, состояние обломков, двигатель, контейнеры
  ├─ GeoVehicleEntity — техника на GeckoLib  ⚠️ помечена «в будущем удалить»
  │     └─ Lav150/Lav25/M1A2/T90a ...
  └─ AutoAimableEntity — автонаведение (owner/target UUID, авто-прицел и решение стрельбы,
        │   лазер/луч, фильтры целей)
        └─ ArtilleryEntity → GeckoArtilleryEntity (временный мост)

ProjectileEntity (entity/projectile/) — база снарядов (30+ видов): кастомный raytrace c OBB,
    хедшоты/попадания по ногам, пробитие брони, взрыв/огонь/отбрасывание, осколки, трассеры RGB
```

### 5. GeckoLib помечен на удаление

`GunGeoItem` (`item/gun/GunGeoItem.java`) и `GeoVehicleEntity`/`GeckoArtilleryEntity` —
переходный слой на GeckoLib, который **планируется убрать** в пользу Simple Bedrock Model (SBM).
Многое уже «sbm-ифицировано» (см. недавнюю git-историю). При добавлении нового контента
**предпочитай SBM-путь**, а не плоди новые GeoItem/Geo-сущности, иначе создашь работу, которую
upstream потом будет удалять (= конфликты).

### 6. Сеть

Регистрация в `network/NetworkRegistry.kt` через `RegisterPayloadHandlersEvent` (новый
payload-API NeoForge). Направления: `message/send/` (клиент→сервер: FireKey, Reload,
SwitchWeapon, Zoom…), `message/receive/` (сервер→клиент: эффекты выстрела, тряска экрана,
индикатор убийства, синхронизации). Новый пакет = класс Message + регистрация в `NetworkRegistry`.

### 7. Capabilities / Attachments (важно для 1.21)

Это NeoForge-ветка, поэтому используется **система Attachment + Capabilities API**
(`capability/`), а **не** старые Forge Capabilities из 1.20.1. При портировании/добавлении
данных на сущность/предмет ориентируйся на существующие attachment-ы, а не на 1.20-паттерны.

### 8. События: свои vs движковые

- Кастомные события мода — в `api/event/` (`ShootEvent`, `PreKillEvent`, `ProjectileHitEvent`…).
- Обработчики нативных событий NeoForge — в `event/`.
  Для новой механики предпочитай подписку на эти события расширению core-классов.

### 9. Mixin

Конфиг `src/main/resources/mixins.superbwarfare.json` (`compatibilityLevel: JAVA_21`,
пакет `com.atsuishio.superbwarfare.mixins`), плюс `enumextensions.json` для расширения enum'ов
движка. Mixin — крайнее средство; новый миксин = класс в `mixins/` + запись в конфиг
(добавляй **в конец** массива, чтобы не плодить merge-конфликты — см. главное правило).

---

## Структура исходников (кратко)

- `src/main/kotlin/.../` — основной код: `Mod.kt`, реестры `init/` (ModItems, ModBlocks,
  ModEntities, ModPerks, ModAttachments…), `item/`, `entity/`, `client/` (рендер/GUI/частицы/
  оверлеи/шейдеры), `network/`, `perk/`, `config/`, `compat/`, `script/` (Rhino JS), `datagen/`,
  `block/`, `command/`, `recipe/`, `world/`, `advancement/`, `tools/`, `api/event/`, `capability/`.
- `src/main/java/.../` — легаси Java + часть data-слоя: `data/gun/` (GunData, GunProp, Ammo…),
  `data/vehicle/`, `data/launchable/`, `item/gun/` (конкретные ~45 стволов), Java-`client/`
  (Renderer/Model каждого ствола), `mixins/` (~34 миксина).
- `src/main/templates/META-INF/neoforge.mods.toml` — **шаблон манифеста**; реальные значения
  подставляются `generateModMetadata` из `gradle.properties`. Правь шаблон, а не сгенерированный файл.
- `src/generated/resources/` — вывод datagen (коммитится).
- `ksp/` — KSP-подмодуль (см. выше).
- `libs/` — flat-dir локальные зависимости.

## Зависимости / технологии

GeckoLib 4.7.5 (анимации, на удаление), Simple Bedrock Model 2.3.3 (jar-in-jar, основной
рендер моделей), Rhino 1.8.1-SNAPSHOT — форк ywzj (jar-in-jar, JS-движок), Kotlin for Forge
5.10.0, Curios 9.2.0, Cloth Config (GUI настроек), JEI / Jade / Patchouli, KubeJS.
Зависимости `mae`/Create/Sable/CreateAeronautics — только `compileOnly` (опциональная совместимость).

## Конвенции кода (следуй upstream, не вводи свои)

- Реестры — Kotlin `object` (`ModItems`, `ModEntities`…); регистрируются через DeferredRegister.
- NBT-свойства — через обёртки `IntValue/BooleanValue/DoubleValue/StringEnumValue`.
- Поведение оружия — через `modifyProperty(PMC)` в перках / модификаторах, не хардкодом.
- Клиентский код — `@OnlyIn(Dist.CLIENT)`; рендер-база на Kotlin, по одному файлу Renderer +
  Model на конкретный ствол/технику.
- Существующие комментарии на китайском не переводить и не удалять.
- В `data/gun/` штатно смешаны Kotlin и Java — это нормально, не унифицируй.

## Лицензия

Код — GPL-3.0. Модели/текстуры/ресурсы — CC BY-NC-SA 3.0.
