package com.atsuishio.superbwarfare.datagen.base

import com.atsuishio.superbwarfare.data.loot.WreckageLootData
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.toGson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.server.packs.PackType
import net.minecraft.world.entity.EntityType
import net.minecraftforge.common.data.ExistingFileHelper
import net.minecraftforge.registries.ForgeRegistries
import java.util.concurrent.CompletableFuture

abstract class SbwWreckageLootProvider(val output: PackOutput, val existingFileHelper: ExistingFileHelper) :
    DataProvider {
    protected val lootData = mutableListOf<WreckageLootData>()

    abstract fun generate()

    fun add(type: EntityType<out VehicleEntity>, builder: WreckageLootData.Builder) {
        val id = ForgeRegistries.ENTITY_TYPES.getKey(type) ?: return
        lootData.add(builder.build(id))
    }

    override fun run(pOutput: CachedOutput): CompletableFuture<*> {
        this.generate()

        val list = mutableListOf<CompletableFuture<*>>()
        val pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "sbw/loot")

        val consumer = { data: WreckageLootData ->
            val id = data.id
            if (existingFileHelper.exists(id, PackType.SERVER_DATA, ".json", "sbw/loot")) {
                throw IllegalArgumentException("Duplicate wreckage loot data: $id")
            }
            val path = pathProvider.json(id)
            list.add(DataProvider.saveStable(pOutput, Json.encodeToJsonElement(data).toGson(), path))
        }

        for (loot in lootData) {
            consumer(loot)
        }

        return CompletableFuture.allOf(*list.toTypedArray())
    }
}