package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.data.event.GatherDataEvent

@EventBusSubscriber(modid = Mod.MODID)
object DataGenerators {
    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val packOutput = generator.packOutput
        val lookupProvider = event.lookupProvider
        val existingFileHelper = event.existingFileHelper

        generator.addProvider(event.includeServer(), ModLootTableProvider.create(packOutput, lookupProvider))
        generator.addProvider(event.includeServer(), ModRecipeProvider(packOutput, lookupProvider))
        generator.addProvider(event.includeClient(), ModBlockStateProvider(packOutput, existingFileHelper))
        generator.addProvider(event.includeClient(), ModItemModelProvider(packOutput, existingFileHelper))
        val tagProvider = generator.addProvider(
            event.includeServer(),
            ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper)
        )
        generator.addProvider(
            event.includeServer(),
            ModItemTagProvider(packOutput, lookupProvider, tagProvider.contentsGetter(), existingFileHelper)
        )
        generator.addProvider(
            event.includeServer(),
            ModEntityTypeTagProvider(packOutput, lookupProvider, existingFileHelper)
        )
        generator.addProvider(
            event.includeServer(),
            ModDamageTypeTagProvider(packOutput, lookupProvider, existingFileHelper)
        )
        generator.addProvider(
            event.includeServer(),
            ModAdvancementProvider(packOutput, lookupProvider, existingFileHelper)
        )
        generator.addProvider(event.includeServer(), ModPerkTagProvider(packOutput, lookupProvider, existingFileHelper))
        generator.addProvider(event.includeServer(), ModWreckageLootProvider(packOutput, existingFileHelper))
    }
}
