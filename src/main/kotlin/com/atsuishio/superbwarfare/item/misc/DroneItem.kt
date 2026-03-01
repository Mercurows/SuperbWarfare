package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.Level

class DroneItem : AbstractDeployerItem(Properties().rarity(Rarity.UNCOMMON)) {
    override fun spawnDeployedEntity(
        level: Level,
        player: Player
    ): Entity {
        return DroneEntity(ModEntities.DRONE.get(), level)
    }
}
