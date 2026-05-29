package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.perk.js.PerkDescriptor
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraftforge.registries.DataPackRegistryEvent

object ModDatapackRegistries {

    val PERKS_KEY: ResourceKey<Registry<PerkDescriptor>> =
        ResourceKey.createRegistryKey(Mod.loc("sbw/perks"))

    fun onNewRegistry(event: DataPackRegistryEvent.NewRegistry) {
        event.dataPackRegistry(PERKS_KEY, PerkDescriptor.CODEC)
    }
}