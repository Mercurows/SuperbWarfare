package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class ModContainerLootProvider implements LootTableSubProvider {

    public ModContainerLootProvider(HolderLookup.Provider provider) {
        super();
    }

    @Override
    public void generate(@NotNull BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(ResourceKey.create(Registries.LOOT_TABLE, Mod.loc("containers/blueprints")),
                LootTable.lootTable().withPool(multiItems(1, 0,
                        new PoolTriple(ModItems.GLOCK_17_BLUEPRINT.get(), 60, 0),
                        new PoolTriple(ModItems.MP_443_BLUEPRINT.get(), 60, 0),
                        new PoolTriple(ModItems.TASER_BLUEPRINT.get(), 60, 0),
                        new PoolTriple(ModItems.MARLIN_BLUEPRINT.get(), 60, 0),
                        new PoolTriple(ModItems.M_1911_BLUEPRINT.get(), 60, 0),

                        new PoolTriple(ModItems.GLOCK_18_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.M_79_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.M_4_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.SKS_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.M_870_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.AK_47_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.K_98_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.MOSIN_NAGANT_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.HK_416_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.AK_12_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.QBZ_95_BLUEPRINT.get(), 42, 0),
                        new PoolTriple(ModItems.RPG_BLUEPRINT.get(), 42, 0),

                        new PoolTriple(ModItems.TRACHELIUM_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.HUNTING_RIFLE_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.BOCEK_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.RPK_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.VECTOR_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.MK_14_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.M_60_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.SVD_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.M_98B_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.DEVOTION_BLUEPRINT.get(), 15, 0),
                        new PoolTriple(ModItems.INSIDIOUS_BLUEPRINT.get(), 15, 0),

                        new PoolTriple(ModItems.AA_12_BLUEPRINT.get(), 5, 0),
                        new PoolTriple(ModItems.NTW_20_BLUEPRINT.get(), 5, 0),
                        new PoolTriple(ModItems.MINIGUN_BLUEPRINT.get(), 5, 0),
                        new PoolTriple(ModItems.SENTINEL_BLUEPRINT.get(), 5, 0),
                        new PoolTriple(ModItems.JAVELIN_BLUEPRINT.get(), 5, 0),
                        new PoolTriple(ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(), 5, 0)
                )));
    }

    public LootPool.Builder singleItem(ItemLike item, int weight) {
        return singleItem(item, 1, 0, weight, 0);
    }

    public LootPool.Builder singleItem(ItemLike item, float rolls, float bonus, int weight, int quality) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(rolls)).setBonusRolls(ConstantValue.exactly(bonus))
                .add(LootItem.lootTableItem(item).setWeight(weight).setQuality(quality));
    }

    public final LootPool.Builder multiItems(float rolls, float bonus, PoolTriple... triplet) {
        var builder = LootPool.lootPool().setRolls(ConstantValue.exactly(rolls)).setBonusRolls(ConstantValue.exactly(bonus));
        for (var t : triplet) {
            builder.add(LootItem.lootTableItem(t.item()).setWeight(t.weight()).setQuality(t.quality()));
        }
        return builder;
    }

    public record PoolTriple(ItemLike item, int weight, int quality) {
    }
}
