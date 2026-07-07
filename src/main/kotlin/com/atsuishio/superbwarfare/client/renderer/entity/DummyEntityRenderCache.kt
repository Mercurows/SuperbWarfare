package com.atsuishio.superbwarfare.client.renderer.entity

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

/**
 * PJM: кеш клиентских сущностей-муляжей для [SbmVehicleRenderer.renderCustomPart].
 *
 * Раньше на каждый кадр для каждого орудия с dummy-снарядами создавалась новая
 * сущность через [EntityType.create] (аллокации, генерация UUID, инициализация
 * attachment'ов) только ради отрисовки муляжей боеприпасов. Сущность используется
 * исключительно как модель для рендера, поэтому один экземпляр на тип переживает
 * кадры; кеш сбрасывается при смене измерения/мира.
 */
@OnlyIn(Dist.CLIENT)
object DummyEntityRenderCache {

    private val cache = hashMapOf<EntityType<*>, Entity>()
    private var cachedLevel: Level? = null

    fun get(type: EntityType<*>, level: Level): Entity? {
        if (cachedLevel !== level) {
            cache.clear()
            cachedLevel = level
        }
        cache[type]?.let { return it }
        val entity = type.create(level) ?: return null
        cache[type] = entity
        return entity
    }
}
