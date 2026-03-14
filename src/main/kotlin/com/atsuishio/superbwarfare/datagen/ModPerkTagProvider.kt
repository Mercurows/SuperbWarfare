package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.IntrinsicHolderTagsProvider
import net.minecraft.resources.ResourceKey
import net.minecraftforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModPerkTagProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    existingFileHelper: ExistingFileHelper
) :
    IntrinsicHolderTagsProvider<Perk>(
        output,
        ModPerks.PERK_KEY,
        lookupProvider,
        { perk -> ResourceKey.create(ModPerks.PERK_KEY, Mod.loc(perk.descriptionId)) },
        Mod.MODID,
        existingFileHelper
    ) {

    override fun addTags(provider: HolderLookup.Provider) {
        this.tag(ModTags.Perks.TEST).add(ModPerks.AP_BULLET.get())
    }
}