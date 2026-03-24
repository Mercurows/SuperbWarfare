package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.Ptkm1rItemRenderer
import com.atsuishio.superbwarfare.entity.projectile.Ptkm1rEntity
import com.atsuishio.superbwarfare.item.misc.AbstractDeployerItem
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.Level
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

open class Ptkm1rItem : AbstractDeployerItem(Properties().rarity(Rarity.RARE).stacksTo(2)), GeoItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private val renderer: BlockEntityWithoutLevelRenderer = Ptkm1rItemRenderer()

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                return renderer
            }
        })
    }

    override fun registerControllers(data: ControllerRegistrar?) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return this.cache
    }

    override fun spawnDeployedEntity(level: Level, player: Player): Entity {
        return Ptkm1rEntity(player, level)
    }
}
