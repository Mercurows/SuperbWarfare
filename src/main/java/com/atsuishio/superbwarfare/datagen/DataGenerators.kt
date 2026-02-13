package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
    modid = Mod.MODID,
    bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD
)
object DataGenerators {
    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val packOutput = generator.packOutput
        val lookupProvider = event.lookupProvider
        val existingFileHelper = event.existingFileHelper

        generator.addProvider(event.includeServer(), ModLootTableProvider.create(packOutput))
        generator.addProvider(event.includeServer(), ModRecipeProvider(packOutput))
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
        generator.addProvider(event.includeServer(), ModAdvancementProvider(packOutput, existingFileHelper))
        generator.addProvider(event.includeServer(), ModPerkTagProvider(packOutput, lookupProvider, existingFileHelper))
        generator.addProvider(event.includeServer(), ModWreckageLootProvider(packOutput, existingFileHelper))
    }
}
