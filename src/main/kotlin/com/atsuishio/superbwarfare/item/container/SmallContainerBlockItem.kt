package com.atsuishio.superbwarfare.item.container

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.item.SmallContainerBlockItemRenderer
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.SeededContainerLoot
import net.minecraft.world.level.storage.loot.LootTable
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.util.GeckoLibUtil

class SmallContainerBlockItem : BlockItem(ModBlocks.SMALL_CONTAINER.get(), Properties().stacksTo(1).fireResistant()),
    GeoItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun canBeHurtBy(stack: ItemStack, source: DamageSource) = super.canBeHurtBy(stack, source)
            && !source.`is`(DamageTypeTags.IS_EXPLOSION)
            && !source.`is`(DamageTypes.CACTUS)

    private fun predicate(event: AnimationState<SmallContainerBlockItem>): PlayState {
        return PlayState.CONTINUE
    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {
        data.add(
            AnimationController(this, "controller", 0) { this.predicate(it) }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return this.cache
    }

    @EventBusSubscriber(modid = Mod.MODID)
    companion object {
        @JvmField
        val SMALL_CONTAINERS: MutableList<() -> ItemStack> = mutableListOf(
            { createInstance(loc("containers/blueprints")) },
            { createInstance(loc("containers/common")) }
        )

        @JvmOverloads
        fun createInstance(lootTable: ResourceLocation, lootTableSeed: Long = 0L): ItemStack {
            return createInstance(ResourceKey.create(Registries.LOOT_TABLE, lootTable), lootTableSeed)
        }

        @JvmOverloads
        fun createInstance(lootTable: ResourceKey<LootTable>, lootTableSeed: Long = 0L): ItemStack {
            val stack = ItemStack(ModBlocks.SMALL_CONTAINER.get())
            stack.set(
                DataComponents.CONTAINER_LOOT,
                SeededContainerLoot(lootTable, lootTableSeed)
            )
            return stack
        }

        @SubscribeEvent
        fun registerArmorExtensions(event: RegisterClientExtensionsEvent) {
            event.registerItem(object : IClientItemExtensions {
                private val renderer = SmallContainerBlockItemRenderer()

                override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                    return renderer
                }
            }, ModItems.SMALL_CONTAINER)
        }
    }
}
