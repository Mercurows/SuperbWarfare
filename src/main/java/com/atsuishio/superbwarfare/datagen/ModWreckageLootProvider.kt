package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.data.loot.WreckageLootData
import com.atsuishio.superbwarfare.datagen.base.SbwWreckageLootProvider
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.data.PackOutput
import net.neoforged.neoforge.common.data.ExistingFileHelper

class ModWreckageLootProvider(output: PackOutput, existingFileHelper: ExistingFileHelper) :
    SbwWreckageLootProvider(output, existingFileHelper) {

    override fun generate() {
        this.add(
            ModEntities.BMP_2.get(),
            WreckageLootData.Builder().addPool(
                WreckageLootData.Pool.Builder().addEntry(
                    WreckageLootData.Entry(
                        ModItems.STEEL_INGOT.get(),
                        1,
                        1.0
                    )
                ).build()
            )
        )
    }

    override fun getName(): String = "Superb Warfare Wreckage Loot"
}