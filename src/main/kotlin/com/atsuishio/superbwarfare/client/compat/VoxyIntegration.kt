package com.atsuishio.superbwarfare.client.compat

import net.neoforged.fml.ModList
import java.lang.reflect.Field

/**
 * Мягкая интеграция с Voxy: дальность рендера дальних призраков (техника/снаряды)
 * настраивается слайдером "SBW entity distance" в Voxy-вкладке Sodium Video Settings.
 * Voxy не установлен или поле недоступно — лимита нет.
 */
object VoxyIntegration {

    private var initialized = false
    private var configField: Field? = null
    private var distanceField: Field? = null

    private fun init() {
        initialized = true
        if (!ModList.get().isLoaded("voxy")) return
        try {
            val cfgClass = Class.forName("me.cortex.voxy.client.config.VoxyConfig")
            configField = cfgClass.getField("CONFIG")
            distanceField = cfgClass.getField("sbwEntityRenderDistance")
        } catch (e: Throwable) {
            configField = null
            distanceField = null
        }
    }

    /** Макс. дистанция рендера дальних призраков в блоках; 0 = не рендерить вовсе. */
    fun distantEntityRenderDistance(): Int {
        if (!initialized) init()
        val cfg = configField ?: return Int.MAX_VALUE
        val dist = distanceField ?: return Int.MAX_VALUE
        return try {
            dist.getInt(cfg.get(null) ?: return Int.MAX_VALUE)
        } catch (e: Throwable) {
            Int.MAX_VALUE
        }
    }
}
