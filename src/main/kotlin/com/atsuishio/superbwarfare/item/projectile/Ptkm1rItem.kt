package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.Ptkm1rItemRenderer
import com.atsuishio.superbwarfare.entity.projectile.Ptkm1rEntity
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.misc.AbstractDeployerItem
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.Level
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("ptkm_1r")
object Ptkm1rItem : AbstractDeployerItem(Properties().rarity(Rarity.RARE).stacksTo(2)) {

    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private val renderer by lazy { Ptkm1rItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels) }

            override fun getCustomRenderer() = renderer
        })
    }

    override fun spawnDeployedEntity(level: Level, player: Player): Entity {
        return Ptkm1rEntity(player, level)
    }
}
