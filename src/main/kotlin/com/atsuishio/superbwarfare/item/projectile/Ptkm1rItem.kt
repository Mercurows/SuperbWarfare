package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.Ptkm1rItemRenderer
import com.atsuishio.superbwarfare.entity.projectile.Ptkm1rEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.AbstractDeployerItem
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

open class Ptkm1rItem : AbstractDeployerItem(Properties().rarity(Rarity.RARE).stacksTo(2)), GeoItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    @EventBusSubscriber
    companion object {
        @SubscribeEvent
        fun registerRenderer(event: RegisterClientExtensionsEvent) {
            event.registerItem(object : IClientItemExtensions {
                private val renderer: BlockEntityWithoutLevelRenderer = Ptkm1rItemRenderer()

                override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                    return renderer
                }
            }, ModItems.PTKM_1R.get())
        }
    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {}

    override fun getAnimatableInstanceCache() = this.cache

    override fun spawnDeployedEntity(level: Level, player: Player): Entity {
        return Ptkm1rEntity(player, level)
    }
}
