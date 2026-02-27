package com.atsuishio.superbwarfare.datagen

import net.minecraft.data.PackOutput
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.data.loot.LootTableProvider.SubProviderEntry
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets

object ModLootTableProvider {
    fun create(output: PackOutput): LootTableProvider {
        return LootTableProvider(
            output, mutableSetOf(), listOf(
                SubProviderEntry({ ModBlockLootProvider() }, LootContextParamSets.BLOCK),
                SubProviderEntry({ ModCustomLootProvider() }, LootContextParamSets.CHEST),
                SubProviderEntry({ ModEntityLootProvider() }, LootContextParamSets.ENTITY)
            )
        )
    }
}
