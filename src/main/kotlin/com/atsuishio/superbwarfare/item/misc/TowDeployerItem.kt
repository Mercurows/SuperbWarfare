package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.TowEntity
import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.Level

class TowDeployerItem : AbstractDeployerItem(Properties().rarity(Rarity.EPIC)) {
    override fun spawnDeployedEntity(level: Level, player: Player): Entity {
        return TowEntity(ModEntities.TOW.get(), level)
    }
}
