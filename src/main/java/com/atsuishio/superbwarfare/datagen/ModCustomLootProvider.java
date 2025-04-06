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
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ModCustomLootProvider implements LootTableSubProvider {

    public static ResourceKey<LootTable> containers(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, Mod.loc("containers/" + name));
    }

    public static ResourceKey<LootTable> chests(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, Mod.loc("chests/" + name));
    }

    public ModCustomLootProvider(HolderLookup.Provider provider) {
        super();
    }

    @Override
    public void generate(@NotNull BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(chests("ancient_cpu"), LootTable.lootTable().withPool(singleItem(ModItems.ANCIENT_CPU.get(), 1, 1, 1, 1)
                .when(() -> LootItemRandomChanceCondition.randomChance(0.4f).build()))
        );

        output.accept(chests("blue_print_common"), LootTable.lootTable().withPool(multiItems(1, 0,
                new PoolTriple(ModItems.TASER_BLUEPRINT.get(), 50, 0),
                new PoolTriple(ModItems.GLOCK_17_BLUEPRINT.get(), 50, 0),
                new PoolTriple(ModItems.MP_443_BLUEPRINT.get(), 50, 0),
                new PoolTriple(ModItems.M_1911_BLUEPRINT.get(), 50, 0),
                new PoolTriple(ModItems.MARLIN_BLUEPRINT.get(), 50, 0),

                new PoolTriple(ModItems.GLOCK_18_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.M_79_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.M_4_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.SKS_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.K_98_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.MOSIN_NAGANT_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.AK_47_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.M_870_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.HK_416_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.AK_12_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.QBZ_95_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.RPG_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.M_2_HB_BLUEPRINT.get(), 15, 0),

                new PoolTriple(ModItems.TRACHELIUM_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.HUNTING_RIFLE_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.BOCEK_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.RPK_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.VECTOR_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.MK_14_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.M_60_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.SVD_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.M_98B_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.DEVOTION_BLUEPRINT.get(), 1, 0),
                new PoolTriple(ModItems.INSIDIOUS_BLUEPRINT.get(), 1, 0)
        )).withPool(multiItems(2, 0,
                new PoolTriple(ModItems.HANDGUN_AMMO_BOX.get(), 12, 0).setCountBetween(1, 2),
                new PoolTriple(ModItems.RIFLE_AMMO_BOX.get(), 20, 0).setCountBetween(1, 2),
                new PoolTriple(ModItems.SNIPER_AMMO_BOX.get(), 10, 0).setCountBetween(1, 2),
                new PoolTriple(ModItems.SHOTGUN_AMMO_BOX.get(), 17, 0).setCountBetween(1, 2),
                new PoolTriple(ModItems.GRENADE_40MM.get(), 6, 0).setCountBetween(1, 3),
                new PoolTriple(ModItems.ROCKET.get(), 4, 0).setCountBetween(1, 2),
                new PoolTriple(ModItems.MORTAR_SHELL.get(), 6, 0).setCountBetween(1, 4),
                new PoolTriple(ModItems.CLAYMORE_MINE.get(), 3, 0).setCountBetween(1, 3),
                new PoolTriple(ModItems.C4_BOMB.get(), 1, 0)
        )));
        output.accept(chests("blue_print_rare"), LootTable.lootTable().withPool(multiItems(1, 0,
                new PoolTriple(ModItems.TASER_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.GLOCK_17_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.MP_443_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.M_1911_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.MARLIN_BLUEPRINT.get(), 10, 0),

                new PoolTriple(ModItems.GLOCK_18_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.M_79_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.M_4_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.SKS_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.K_98_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.MOSIN_NAGANT_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.AK_47_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.M_870_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.HK_416_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.AK_12_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.QBZ_95_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.RPG_BLUEPRINT.get(), 30, 0),
                new PoolTriple(ModItems.M_2_HB_BLUEPRINT.get(), 30, 0),

                new PoolTriple(ModItems.TRACHELIUM_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.HUNTING_RIFLE_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.BOCEK_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.RPK_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.VECTOR_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.MK_14_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.M_60_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.SVD_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.M_98B_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.DEVOTION_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.INSIDIOUS_BLUEPRINT.get(), 10, 0),

                new PoolTriple(ModItems.AA_12_BLUEPRINT.get(), 3, 0),
                new PoolTriple(ModItems.NTW_20_BLUEPRINT.get(), 3, 0),
                new PoolTriple(ModItems.MINIGUN_BLUEPRINT.get(), 3, 0),
                new PoolTriple(ModItems.SENTINEL_BLUEPRINT.get(), 3, 0),
                new PoolTriple(ModItems.JAVELIN_BLUEPRINT.get(), 3, 0),
                new PoolTriple(ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(), 3, 0),
                new PoolTriple(ModItems.MK_42_BLUEPRINT.get(), 3, 0),
                new PoolTriple(ModItems.MLE_1934_BLUEPRINT.get(), 3, 0),
                new PoolTriple(ModItems.ANNIHILATOR_BLUEPRINT.get(), 1, 0)
        )).withPool(multiItems(2, 0,
                new PoolTriple(ModItems.HANDGUN_AMMO_BOX.get(), 12, 0).setCountBetween(1, 3),
                new PoolTriple(ModItems.RIFLE_AMMO_BOX.get(), 20, 0).setCountBetween(1, 3),
                new PoolTriple(ModItems.SNIPER_AMMO_BOX.get(), 10, 0).setCountBetween(1, 3),
                new PoolTriple(ModItems.SHOTGUN_AMMO_BOX.get(), 17, 0).setCountBetween(1, 3),
                new PoolTriple(ModItems.GRENADE_40MM.get(), 6, 0).setCountBetween(2, 6),
                new PoolTriple(ModItems.ROCKET.get(), 4, 0).setCountBetween(2, 4),
                new PoolTriple(ModItems.MORTAR_SHELL.get(), 6, 0).setCountBetween(2, 8),
                new PoolTriple(ModItems.CLAYMORE_MINE.get(), 3, 0).setCountBetween(2, 6),
                new PoolTriple(ModItems.C4_BOMB.get(), 1, 0).setCountBetween(1, 2)
        )));
        output.accept(chests("blue_print_epic"), LootTable.lootTable().withPool(multiItems(1, 0,
                new PoolTriple(ModItems.TRACHELIUM_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.HUNTING_RIFLE_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.BOCEK_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.RPK_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.VECTOR_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.MK_14_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.M_60_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.SVD_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.M_98B_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.DEVOTION_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.INSIDIOUS_BLUEPRINT.get(), 10, 0),

                new PoolTriple(ModItems.AA_12_BLUEPRINT.get(), 20, 0),
                new PoolTriple(ModItems.NTW_20_BLUEPRINT.get(), 20, 0),
                new PoolTriple(ModItems.MINIGUN_BLUEPRINT.get(), 20, 0),
                new PoolTriple(ModItems.SENTINEL_BLUEPRINT.get(), 20, 0),
                new PoolTriple(ModItems.JAVELIN_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(), 15, 0),
                new PoolTriple(ModItems.MK_42_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.MLE_1934_BLUEPRINT.get(), 10, 0),
                new PoolTriple(ModItems.ANNIHILATOR_BLUEPRINT.get(), 5, 0)
        )).withPool(multiItems(2, 0,
                new PoolTriple(ModItems.HANDGUN_AMMO_BOX.get(), 12, 0).setCountBetween(2, 4),
                new PoolTriple(ModItems.RIFLE_AMMO_BOX.get(), 20, 0).setCountBetween(2, 4),
                new PoolTriple(ModItems.SNIPER_AMMO_BOX.get(), 10, 0).setCountBetween(2, 4),
                new PoolTriple(ModItems.SHOTGUN_AMMO_BOX.get(), 17, 0).setCountBetween(2, 4),
                new PoolTriple(ModItems.HEAVY_AMMO.get(), 10, 0).setCountBetween(10, 24),
                new PoolTriple(ModItems.GRENADE_40MM.get(), 6, 0).setCountBetween(4, 12),
                new PoolTriple(ModItems.ROCKET.get(), 4, 0).setCountBetween(4, 8),
                new PoolTriple(ModItems.MORTAR_SHELL.get(), 6, 0).setCountBetween(4, 8),
                new PoolTriple(ModItems.CLAYMORE_MINE.get(), 3, 0).setCountBetween(4, 12),
                new PoolTriple(ModItems.C4_BOMB.get(), 1, 0).setCountBetween(2, 4),
                new PoolTriple(ModItems.JAVELIN_MISSILE.get(), 1, 0).setCountBetween(1, 2)
        )));

        output.accept(containers("blueprints"), LootTable.lootTable().withPool(multiItems(1, 0,
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
            var entry = LootItem.lootTableItem(t.item).setWeight(t.weight).setQuality(t.quality);
            for (var c : t.conditions) {
                entry.when(c);
            }
            for (var f : t.functions) {
                entry.apply(f);
            }
            builder.add(entry);
        }
        return builder;
    }

    public static class PoolTriple {

        public ItemLike item;
        public int weight;
        public int quality;
        public List<LootItemCondition.Builder> conditions = new ArrayList<>();
        public List<LootItemFunction.Builder> functions = new ArrayList<>();

        public PoolTriple(ItemLike item, int weight, int quality) {
            this.item = item;
            this.weight = weight;
            this.quality = quality;
        }

        public PoolTriple condition(LootItemCondition.Builder condition) {
            this.conditions.add(condition);
            return this;
        }

        public PoolTriple function(LootItemFunction.Builder function) {
            this.functions.add(function);
            return this;
        }

        public PoolTriple setCountBetween(int min, int max) {
            return this.function(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
        }
    }
}
