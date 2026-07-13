package com.atsuishio.superbwarfare.client.compat

import net.neoforged.fml.ModList
import java.lang.reflect.Field

/**
 * Мягкая интеграция с Voxy: дальность рендера дальних призраков (техника/снаряды)
 * настраивается слайдером "SBW entity distance" в Voxy-вкладке Sodium Video Settings.
 *
 * Дальние призраки имеют смысл только когда Voxy реально рисует дальний ландшафт —
 * иначе техника висела бы в тумане/пустоте за пределами ванильной прогрузки. Поэтому
 * если Voxy не установлен ЛИБО его рендер выключен (`enabled` / `enable_rendering`),
 * дистанция считается нулевой и дальняя техника не рендерится вовсе.
 */
object VoxyIntegration {

    private var initialized = false
    private var voxyLoaded = false
    private var configField: Field? = null
    private var distanceField: Field? = null
    private var enabledField: Field? = null
    private var enableRenderingField: Field? = null

    private fun init() {
        initialized = true
        if (!ModList.get().isLoaded("voxy")) return
        voxyLoaded = true
        try {
            val cfgClass = Class.forName("me.cortex.voxy.client.config.VoxyConfig")
            configField = cfgClass.getField("CONFIG")
            distanceField = cfgClass.getField("sbwEntityRenderDistance")
            // Мастер-тумблер Voxy и тумблер рендера мира. Отсутствие поля (другая версия
            // Voxy) не должно блокировать фичу — читаем «мягко», null трактуем как «включено».
            enabledField = runCatching { cfgClass.getField("enabled") }.getOrNull()
            enableRenderingField = runCatching { cfgClass.getField("enableRendering") }.getOrNull()
        } catch (e: Throwable) {
            configField = null
            distanceField = null
        }
    }

    /** true, если Voxy загружен и его рендер мира активен (мастер-тумблер + рендер). */
    private fun isVoxyRendering(): Boolean {
        val cfg = configField ?: return false
        val instance = try {
            cfg.get(null) ?: return false
        } catch (e: Throwable) {
            return false
        }
        // Отсутствующее поле не блокирует — считаем такой флаг включённым.
        val enabled = readBool(enabledField, instance)
        val rendering = readBool(enableRenderingField, instance)
        return enabled && rendering
    }

    private fun readBool(field: Field?, instance: Any): Boolean {
        field ?: return true
        return try {
            field.getBoolean(instance)
        } catch (e: Throwable) {
            true
        }
    }

    /** Макс. дистанция рендера дальних призраков в блоках; 0 = не рендерить вовсе. */
    fun distantEntityRenderDistance(): Int {
        if (!initialized) init()
        // Voxy не установлен или его рендер выключен — дальней техники быть не должно.
        if (!voxyLoaded || !isVoxyRendering()) return 0
        val cfg = configField ?: return 0
        val dist = distanceField ?: return 0
        return try {
            dist.getInt(cfg.get(null) ?: return 0)
        } catch (e: Throwable) {
            0
        }
    }
}
