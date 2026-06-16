package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.config.server.MarkerConfig
import com.atsuishio.superbwarfare.init.ModSounds
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent

// PJM: единая клиентская точка отключения звука попадания (звукового хитмаркера).
// Серверный конфиг enable_hit_sound синхронизируется на клиент; при false отменяем
// воспроизведение indication/indication_vehicle/headshot, не трогая ~29 серверных
// мест playSound (минимальная область конфликта при merge с upstream).
@EventBusSubscriber(Dist.CLIENT)
object MarkerSoundHandler {

    // lazy: ModSounds.get() требует заполненного реестра, обращение откладываем
    // до первого PlaySoundEvent (игра уже запущена, звуки зарегистрированы).
    private val HIT_SOUNDS by lazy {
        setOf(
            ModSounds.INDICATION.get().location,
            ModSounds.INDICATION_VEHICLE.get().location,
            ModSounds.HEADSHOT.get().location,
        )
    }

    @SubscribeEvent
    fun onPlaySound(event: PlaySoundEvent) {
        val sound = event.sound ?: return
        // Сначала дешёвая проверка типа звука (реестр заполнен к моменту любого
        // PlaySoundEvent) — чтобы не трогать конфиг для посторонних звуков,
        // например музыки в главном меню.
        if (sound.location !in HIT_SOUNDS) return
        // Серверный конфиг грузится только при заходе в мир/на сервер, а PlaySoundEvent
        // срабатывает и в меню. До загрузки конфига .get() кидает исключение, поэтому гейтим.
        if (!SERVER_CONFIG.isLoaded()) return
        if (!MarkerConfig.ENABLE_HIT_SOUND.get()) {
            event.sound = null
        }
    }
}
