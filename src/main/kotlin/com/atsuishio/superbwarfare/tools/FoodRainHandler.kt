package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.entity.misc.FoodRainItemEntity
import com.atsuishio.superbwarfare.entity.projectile.NuclearBombEntity.NuclearExplosionParameters
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.network.message.receive.NuclearExplosionMessage
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries

@Mod.EventBusSubscriber
object FoodRainHandler {
    private const val ITEMS_PER_RAIN = 400
    private const val MAX_PENDING_RAIN = 4
    private const val SPAWN_BUDGET = 40
    private const val MAX_LIVE_ITEMS = 1200
    private val rains = mutableListOf<Rain>()

    private class Rain(
        val level: ServerLevel,
        val center: Vec3,
        val start: Long,
        val food: List<Item>,
        var remaining: Int = ITEMS_PER_RAIN
    )

    fun detonate(level: ServerLevel, center: Vec3) {
        NuclearExplosionMessage.send(level, center, NuclearExplosionParameters(renderMushroomCloud = false))
        SoundTool.playDistantSound(level, ModSounds.NUKE_EXPLOSION_CLOSE.get(), center, 32f, 1f, null)
        SoundTool.playDistantSound(level, ModSounds.NUKE_EXPLOSION_FAR.get(), center, 64f, 1f, null)
        SoundTool.playDistantSound(level, ModSounds.NUKE_EXPLOSION_VERY_FAR.get(), center, 192f, 1f, null)

        val food = ForgeRegistries.ITEMS.tags()?.getTag(ModTags.Items.FOOD_RAIN_FOODS)
            ?.filter { it != Items.AIR }.orEmpty()
        if (food.isEmpty()) return
        if (rains.size >= MAX_PENDING_RAIN) rains.removeAt(0)
        rains.add(Rain(level, center, level.gameTime, food))
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END || rains.isEmpty()) return
        var budget = SPAWN_BUDGET
        val capacity = mutableMapOf<ServerLevel, Int>()
        val iterator = rains.iterator()
        while (iterator.hasNext() && budget > 0) {
            val rain = iterator.next()
            val level = rain.level
            var available = capacity.getOrPut(level) {
                MAX_LIVE_ITEMS - level.getEntities(ModEntities.FOOD_RAIN_ITEM.get()) { true }.size
            }
            if (available <= 0 || !level.hasChunkAt(BlockPos.containing(rain.center))) {
                iterator.remove()
                continue
            }
            repeat(minOf(rain.remaining, budget, available)) {
                val item = FoodRainItemEntity(ModEntities.FOOD_RAIN_ITEM.get(), level)
                item.item = ItemStack(rain.food[level.random.nextInt(rain.food.size)])
                item.join(rain.center, rain.start)
                if (level.addFreshEntity(item)) available--
                rain.remaining--
                budget--
            }
            capacity[level] = available
            if (rain.remaining <= 0) iterator.remove()
        }
    }

    @SubscribeEvent
    fun unload(event: LevelEvent.Unload) {
        val level = event.level as? ServerLevel ?: return
        rains.removeAll { it.level === level }
    }

    @SubscribeEvent
    fun clear(event: ServerStoppedEvent) {
        rains.clear()
    }
}
